package com.old.silence.data.commons.handler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import com.old.silence.core.enums.EnumValue;

/**
 * @author moryzang
 */
@MappedTypes({Enum.class})
@MappedJdbcTypes({JdbcType.TINYINT, JdbcType.INTEGER, JdbcType.VARCHAR, JdbcType.CHAR})
public class GenericEnumTypeHandler<E extends Enum<E> & EnumValue<?>> extends BaseTypeHandler<E> {
    private final Class<E> enumClass;
    private final E[] enums;
    private final Class<?> valueType;

    public GenericEnumTypeHandler() {
        this.enumClass = null;
        this.enums = null;
        this.valueType = null;
    }

    public GenericEnumTypeHandler(Class<E> enumClass) {
        this.enumClass = enumClass;
        this.enums = enumClass.getEnumConstants();

        // 确定枚举值的实际类型
        if (enums.length > 0) {
            Object firstValue = ((EnumValue<?>) enums[0]).getValue();
            this.valueType = firstValue != null ? firstValue.getClass() : Object.class;
        } else {
            this.valueType = Object.class;
        }
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, E parameter, JdbcType jdbcType) throws SQLException {
        Object value = ((EnumValue<?>) parameter).getValue();

        if (jdbcType == null) {
            // 根据值类型自动选择
            ps.setObject(i, value);
        } else {
            switch (jdbcType) {
                case TINYINT:
                case INTEGER:
                    if (value instanceof Number) {
                        ps.setInt(i, ((Number) value).intValue());
                    } else {
                        ps.setInt(i, Integer.parseInt(value.toString()));
                    }
                    break;
                case VARCHAR:
                case CHAR:
                    ps.setString(i, value.toString());
                    break;
                default:
                    ps.setObject(i, value);
            }
        }
    }

    @Override
    public E getNullableResult(ResultSet rs, String columnName) throws SQLException {
        Object value = getValueFromResultSet(rs, columnName);
        return value == null ? null : parseEnum(value);
    }

    @Override
    public E getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        Object value = getValueFromResultSet(rs, columnIndex);
        return value == null ? null : parseEnum(value);
    }

    @Override
    public E getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        Object value = getValueFromCallableStatement(cs, columnIndex);
        return value == null ? null : parseEnum(value);
    }

    /**
     * 从ResultSet中获取值（支持字符串和数字）
     */
    private Object getValueFromResultSet(ResultSet rs, String columnName) throws SQLException {
        // 先获取元数据判断列类型
        ResultSetMetaData metaData = rs.getMetaData();
        int columnType = metaData.getColumnType(rs.findColumn(columnName));

        return getValueByType(rs, columnName, columnType);
    }

    private Object getValueFromResultSet(ResultSet rs, int columnIndex) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        int columnType = metaData.getColumnType(columnIndex);

        return getValueByType(rs, columnIndex, columnType);
    }

    private Object getValueFromCallableStatement(CallableStatement cs, int columnIndex) throws SQLException {
        // CallableStatement没有直接的方法获取元数据，需要根据值类型处理
        Object value = cs.getObject(columnIndex);
        if (value == null) {
            return null;
        }

        // 根据枚举值的期望类型进行转换
        if (valueType == String.class && !(value instanceof String)) {
            return value.toString();
        } else if ((valueType == Integer.class || valueType == int.class) && value instanceof String) {
            return Integer.parseInt((String) value);
        } else if ((valueType == Byte.class || valueType == byte.class) && value instanceof String) {
            return Byte.parseByte((String) value);
        }

        return value;
    }

    private Object getValueByType(ResultSet rs, String columnName, int columnType) throws SQLException {
        switch (columnType) {
            case Types.TINYINT:
            case Types.SMALLINT:
            case Types.INTEGER:
                int intValue = rs.getInt(columnName);
                return rs.wasNull() ? null : intValue;
            case Types.VARCHAR:
            case Types.CHAR:
            case Types.NVARCHAR:
            case Types.NCHAR:
                return rs.getString(columnName);
            default:
                return rs.getObject(columnName);
        }
    }

    private Object getValueByType(ResultSet rs, int columnIndex, int columnType) throws SQLException {
        switch (columnType) {
            case Types.TINYINT:
            case Types.SMALLINT:
            case Types.INTEGER:
                int intValue = rs.getInt(columnIndex);
                return rs.wasNull() ? null : intValue;
            case Types.VARCHAR:
            case Types.CHAR:
            case Types.NVARCHAR:
            case Types.NCHAR:
                return rs.getString(columnIndex);
            default:
                return rs.getObject(columnIndex);
        }
    }

    /**
     * 解析枚举值（支持字符串和数字）
     */
    private E parseEnum(Object value) {
        if (value == null) {
            return null;
        }

        for (E enumConstant : enums) {
            Object enumValue = ((EnumValue<?>) enumConstant).getValue();

            // 支持多种比较方式
            if (Objects.equals(enumValue, value)) {
                return enumConstant;
            }

            // 字符串与数字的兼容比较
            if (value instanceof String && enumValue instanceof Number) {
                try {
                    if (enumValue.equals(Integer.parseInt((String) value)) ||
                            enumValue.equals(Byte.parseByte((String) value))) {
                        return enumConstant;
                    }
                } catch (NumberFormatException e) {
                    // 忽略转换异常，继续比较
                }
            }

            // 数字与字符串的兼容比较
            if (value instanceof Number && enumValue instanceof String) {
                if (enumValue.equals(value.toString())) {
                    return enumConstant;
                }
            }
        }

        throw new IllegalArgumentException(enumClass.getSimpleName() + " 未知的枚举值: " + value + " (类型: " + value.getClass().getSimpleName() + ")");
    }
}