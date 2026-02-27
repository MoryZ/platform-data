package com.old.silence.data.mybatis.projection;

import java.util.List;

/**
 * Metadata for a projection type.
 */
public class ProjectionMetadata {

    private final Class<?> projectionType;
    private final Class<?> entityType;
    private final String tableName;
    private final List<ProjectionField> fields;
    private final String selectionKey;

    public ProjectionMetadata(Class<?> projectionType, Class<?> entityType, String tableName,
                              List<ProjectionField> fields,
                              String selectionKey) {
        this.projectionType = projectionType;
        this.entityType = entityType;
        this.tableName = tableName;
        this.fields = fields;
        this.selectionKey = selectionKey;
    }

    public Class<?> getProjectionType() {
        return projectionType;
    }

    public Class<?> getEntityType() {
        return entityType;
    }

    public String getTableName() {
        return tableName;
    }

    public List<ProjectionField> getFields() {
        return fields;
    }

    public String getColumnList() {
        return fields.stream().map(ProjectionField::getColumnName).distinct().reduce((a, b) -> a + ", " + b)
                .orElse("*");
    }

    public String getSelectionKey() {
        return selectionKey;
    }
}
