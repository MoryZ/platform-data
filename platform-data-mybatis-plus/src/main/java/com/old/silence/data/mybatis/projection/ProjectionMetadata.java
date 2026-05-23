package com.old.silence.data.mybatis.projection;

import java.util.List;

/**
 * Metadata for a projection type.
 */
public class ProjectionMetadata {

    private final Class<?> projectionType;
    private final Class<?> entityType;
    private final String tableName;
    private final String fromClause;
    private final List<ProjectionField> fields;
    private final List<ProjectionCollectionAssociation> collectionAssociations;
    private final String selectionKey;
    private final boolean collectionJoinInFrom;
    private final List<ProjectionField> additionalFields;

    public ProjectionMetadata(Class<?> projectionType, Class<?> entityType, String tableName,
                              List<ProjectionField> fields,
                              String selectionKey) {
        this(projectionType, entityType, tableName, tableName, fields, List.of(), selectionKey, false, List.of());
    }

    public ProjectionMetadata(Class<?> projectionType, Class<?> entityType, String tableName,
                              String fromClause,
                              List<ProjectionField> fields,
                              String selectionKey) {
        this(projectionType, entityType, tableName, fromClause, fields, List.of(), selectionKey, false, List.of());
    }

    public ProjectionMetadata(Class<?> projectionType, Class<?> entityType, String tableName,
                              String fromClause,
                              List<ProjectionField> fields,
                              List<ProjectionCollectionAssociation> collectionAssociations,
                              String selectionKey) {
        this(projectionType, entityType, tableName, fromClause, fields, collectionAssociations, selectionKey, false, List.of());
    }

    public ProjectionMetadata(Class<?> projectionType, Class<?> entityType, String tableName,
                              String fromClause,
                              List<ProjectionField> fields,
                              List<ProjectionCollectionAssociation> collectionAssociations,
                              String selectionKey,
                              boolean collectionJoinInFrom) {
        this(projectionType, entityType, tableName, fromClause, fields, collectionAssociations, selectionKey, collectionJoinInFrom, List.of());
    }

    public ProjectionMetadata(Class<?> projectionType, Class<?> entityType, String tableName,
                              String fromClause,
                              List<ProjectionField> fields,
                              List<ProjectionCollectionAssociation> collectionAssociations,
                              String selectionKey,
                              boolean collectionJoinInFrom,
                              List<ProjectionField> additionalFields) {
        this.projectionType = projectionType;
        this.entityType = entityType;
        this.tableName = tableName;
        this.fromClause = fromClause;
        this.fields = fields;
        this.collectionAssociations = collectionAssociations;
        this.selectionKey = selectionKey;
        this.collectionJoinInFrom = collectionJoinInFrom;
        this.additionalFields = additionalFields;
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
        return fields.stream().map(ProjectionField::getSelectExpression).distinct().reduce((a, b) -> a + ", " + b)
                .orElse("*");
    }

    public String getFromClause() {
        return fromClause;
    }

    public String getSelectionKey() {
        return selectionKey;
    }

    public List<ProjectionCollectionAssociation> getCollectionAssociations() {
        return collectionAssociations;
    }

    public boolean isCollectionJoinInFrom() {
        return collectionJoinInFrom;
    }

    public List<ProjectionField> getAdditionalFields() {
        return additionalFields;
    }
}
