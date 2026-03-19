package com.old.silence.data.mybatis.projection;

/**
 * Metadata for collection association projection loading.
 * Supports both OneToMany (FK on target) and ManyToMany via join table.
 */
public class ProjectionCollectionAssociation {

    private final String projectionPropertyName;
    private final Class<?> sourceEntityType;
    private final String sourceKeyProperty;
    private final String sourceKeyColumn;
    private final Class<?> targetEntityType;
    private final String targetFkProperty;   // null for join-table associations
    private final String targetFkColumn;     // null for join-table associations
    private final Class<?> elementType;
    private final String joinTableName;      // null for OneToMany associations
    private final String joinTableSourceCol; // null for OneToMany associations
    private final String joinTableTargetCol; // null for OneToMany associations

    public ProjectionCollectionAssociation(String projectionPropertyName,
                                           Class<?> sourceEntityType,
                                           String sourceKeyProperty,
                                           String sourceKeyColumn,
                                           Class<?> targetEntityType,
                                           String targetFkProperty,
                                           String targetFkColumn,
                                           Class<?> elementType,
                                           String joinTableName,
                                           String joinTableSourceCol,
                                           String joinTableTargetCol) {
        this.projectionPropertyName = projectionPropertyName;
        this.sourceEntityType = sourceEntityType;
        this.sourceKeyProperty = sourceKeyProperty;
        this.sourceKeyColumn = sourceKeyColumn;
        this.targetEntityType = targetEntityType;
        this.targetFkProperty = targetFkProperty;
        this.targetFkColumn = targetFkColumn;
        this.elementType = elementType;
        this.joinTableName = joinTableName;
        this.joinTableSourceCol = joinTableSourceCol;
        this.joinTableTargetCol = joinTableTargetCol;
    }

    public String getProjectionPropertyName() {
        return projectionPropertyName;
    }

    public Class<?> getSourceEntityType() {
        return sourceEntityType;
    }

    public String getSourceKeyProperty() {
        return sourceKeyProperty;
    }

    public String getSourceKeyColumn() {
        return sourceKeyColumn;
    }

    public Class<?> getTargetEntityType() {
        return targetEntityType;
    }

    public String getTargetFkProperty() {
        return targetFkProperty;
    }

    public String getTargetFkColumn() {
        return targetFkColumn;
    }

    public Class<?> getElementType() {
        return elementType;
    }

    public String getJoinTableName() {
        return joinTableName;
    }

    public String getJoinTableSourceCol() {
        return joinTableSourceCol;
    }

    public String getJoinTableTargetCol() {
        return joinTableTargetCol;
    }
}
