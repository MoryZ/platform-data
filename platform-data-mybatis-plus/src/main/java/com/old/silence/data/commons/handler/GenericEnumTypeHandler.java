package com.old.silence.data.commons.handler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import com.old.silence.core.enums.EnumValue;

/**
 * @author moryzang
 */
@MappedTypes(Enum.class)    // 定义处理的Java类型
@MappedJdbcTypes({JdbcType.TINYINT, JdbcType.VARCHAR})
public class GenericEnumTypeHandler<E extends Enum<E> & EnumValue<?>>
        extends BaseTypeHandler<E> {

    private final Class<E> enumClass;
    private final E[] enums;

    // 必须添加的无参构造方法（关键修复点）
    public GenericEnumTypeHandler() {
        this.enumClass = null;
        this.enums = null;
    }

    public GenericEnumTypeHandler(Class<E> enumClass) {
        this.enumClass = enumClass;
        this.enums = enumClass.getEnumConstants();
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i,
                                    E parameter, JdbcType jdbcType)
            throws SQLException {
        // 写入数据库时使用枚举的code值
        ps.setObject(i, parameter.getValue());
    }

    @Override
    public E getNullableResult(ResultSet rs, String columnName)
            throws SQLException {
        Object code = rs.getObject(columnName);
        if (code == null) {
            return null;
        }
        return parseEnum(code);
    }

    @Override
    public E getNullableResult(ResultSet rs, int columnIndex)
            throws SQLException {
        Object code = rs.getObject(columnIndex);
        if (code == null) {
            return null;
        }
        return parseEnum(code);
    }

    @Override
    public E getNullableResult(CallableStatement cs, int columnIndex)
            throws SQLException {
        Object code = cs.getObject(columnIndex);
        return code == null ? null : parseEnum(code);
    }

    private E parseEnum(Object code) {
        for (E e : enums) {
            if (e.getValue().equals(code)) {
                return e;
            }
        }
        throw new IllegalArgumentException(
                enumClass.getSimpleName() + " 未知编码: " + code);
    }
}