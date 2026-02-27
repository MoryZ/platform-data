package com.old.silence.data.mybatis.projection;

import org.apache.ibatis.type.TypeHandler;

/**
 * Metadata for a single projection field.
 */
public class ProjectionField {

    private final String propertyName;
    private final String columnName;
    private final Class<?> javaType;
    private final Class<? extends TypeHandler<?>> typeHandler;
    private final boolean idField;

    public ProjectionField(String propertyName, String columnName, Class<?> javaType,
                           Class<? extends TypeHandler<?>> typeHandler, boolean idField) {
        this.propertyName = propertyName;
        this.columnName = columnName;
        this.javaType = javaType;
        this.typeHandler = typeHandler;
        this.idField = idField;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public String getColumnName() {
        return columnName;
    }

    public Class<?> getJavaType() {
        return javaType;
    }

    public Class<? extends TypeHandler<?>> getTypeHandler() {
        return typeHandler;
    }

    public boolean isIdField() {
        return idField;
    }
}
