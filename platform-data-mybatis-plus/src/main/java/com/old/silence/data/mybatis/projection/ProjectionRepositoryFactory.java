package com.old.silence.data.mybatis.projection;

/**
 * Factory to create ProjectionRepository for specific entity type.
 */
public class ProjectionRepositoryFactory {

    private final ProjectionMetadataResolver metadataResolver;
    private final ProjectionQueryExecutor queryExecutor;

    public ProjectionRepositoryFactory(ProjectionMetadataResolver metadataResolver,
                                       ProjectionQueryExecutor queryExecutor) {
        this.metadataResolver = metadataResolver;
        this.queryExecutor = queryExecutor;
    }

    public <T, ID> ProjectionRepository<T, ID> create(Class<T> entityType) {
        return new SimpleProjectionRepository<>(entityType, metadataResolver, queryExecutor);
    }
}
