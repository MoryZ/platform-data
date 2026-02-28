package com.old.silence.data.mybatis.projection;

import java.util.Objects;

/**
 * Factory for creating SimpleProjectionQueryRepository instances by domain type.
 */
public class SimpleProjectionQueryRepositoryFactory {

    private final ProjectionQueryOperations projectionQueryOperations;

    public SimpleProjectionQueryRepositoryFactory(ProjectionQueryOperations projectionQueryOperations) {
        this.projectionQueryOperations = Objects.requireNonNull(projectionQueryOperations,
                "ProjectionQueryOperations must not be null");
    }

    public <T> SimpleProjectionQueryRepository<T> create(Class<T> domainType) {
        return new SimpleProjectionQueryRepository<>(projectionQueryOperations, domainType);
    }
}
