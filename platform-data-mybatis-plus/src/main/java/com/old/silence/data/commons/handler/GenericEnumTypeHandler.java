package com.old.silence.data.commons.handler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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

    public GenericEnumTypeHandler() {
        this.enumClass = null;
        this.enums = null;
    }

    public GenericEnumTypeHandler(Class<E> enumClass) {
        this.enumClass = enumClass;
        this.enums = enumClass.getEnumConstants();
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, E parameter, JdbcType jdbcType) throws SQLException {
        Object value = ((EnumValue<?>) parameter).getValue();

        if (jdbcType == null) {
            ps.setObject(i, value);
        } else {
            switch (jdbcType) {
                case TINYINT:
                    if (value instanceof Number) {
                        ps.setByte(i, ((Number) value).byteValue());
                    } else {
                        ps.setByte(i, Byte.parseByte(value.toString()));
                    }
                    break;
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
        Object value = cs.getObject(columnIndex);
        return cs.wasNull() ? null : parseEnum(value);
    }

    /**
     * 从ResultSet获取值 - 只处理TINYINT, INTEGER, VARCHAR, CHAR
     */
    private Object getValueFromResultSet(ResultSet rs, String columnName) throws SQLException {
        // 简化处理：直接获取对象，让JDBC驱动处理类型
        return rs.getObject(columnName);
    }

    private Object getValueFromResultSet(ResultSet rs, int columnIndex) throws SQLException {
        return rs.getObject(columnIndex);
    }

    /**
     * 解析枚举值 - 支持TINYINT(Byte), INTEGER(Integer), VARCHAR/String
     */
    private E parseEnum(Object value) {
        if (value == null) {
            return null;
        }

        for (E enumConstant : enums) {
            Object enumValue = ((EnumValue<?>) enumConstant).getValue();

            // 直接比较
            if (Objects.equals(enumValue, value)) {
                return enumConstant;
            }

            // 类型转换后比较
            if (value instanceof Byte && enumValue instanceof Integer) {
                if (enumValue.equals(((Byte) value).intValue())) {
                    return enumConstant;
                }
            } else if (value instanceof Integer && enumValue instanceof Byte) {
                if (enumValue.equals(((Integer) value).byteValue())) {
                    return enumConstant;
                }
            } else if (value instanceof String && enumValue instanceof Number) {
                try {
                    if (enumValue instanceof Byte && enumValue.equals(Byte.parseByte((String) value))) {
                        return enumConstant;
                    } else if (enumValue instanceof Integer && enumValue.equals(Integer.parseInt((String) value))) {
                        return enumConstant;
                    }
                } catch (NumberFormatException e) {
                    // 忽略转换异常
                }
            } else if (value instanceof Number && enumValue instanceof String) {
                if (enumValue.equals(value.toString())) {
                    return enumConstant;
                }
            }
        }

        throw new IllegalArgumentException(enumClass.getSimpleName() + " 未知的枚举值: " + value);
    }
}