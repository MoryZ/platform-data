package com.old.silence.data.mybatis.projection;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.Objects;
import java.util.Optional;
import java.util.List;

/**
 * Simple repository-style projection facade, aligned with SimpleJdbcRepository usage.
 */
public class SimpleProjectionQueryRepository<T> {

    private final ProjectionQueryOperations projectionQueryOperations;
    private final Class<T> domainType;

    public SimpleProjectionQueryRepository(ProjectionQueryOperations projectionQueryOperations,
                                           Class<T> domainType) {
        this.projectionQueryOperations = Objects.requireNonNull(projectionQueryOperations,
                "ProjectionQueryOperations must not be null");
        this.domainType = Objects.requireNonNull(domainType, "Domain type must not be null");
    }

    public <ID> Optional<T> findById(ID id) {
        return projectionQueryOperations.findById(id, domainType);
    }

    public <ID, P> Optional<P> findById(ID id, Class<P> projectionType) {
        return projectionQueryOperations.findById(id, projectionType, domainType);
    }

    public <P> List<P> findByQuery(Wrapper<T> queryWrapper, Class<P> projectionType) {
        return projectionQueryOperations.findByQuery(queryWrapper, projectionType, domainType);
    }

    public <P> IPage<P> findByQuery(Wrapper<T> queryWrapper, Page<?> page, Class<P> projectionType) {
        return projectionQueryOperations.findByQuery(queryWrapper, page, projectionType, domainType);
    }

    public long countByQuery(Wrapper<T> queryWrapper) {
        return projectionQueryOperations.countByQuery(queryWrapper, domainType);
    }

    public boolean existsByQuery(Wrapper<T> queryWrapper) {
        return projectionQueryOperations.existsByQuery(queryWrapper, domainType);
    }

    public int create(T entity) {
        return projectionQueryOperations.create(entity, domainType);
    }

    public int updateById(T entity) {
        return projectionQueryOperations.updateById(entity, domainType);
    }

    public <ID> int deleteById(ID id) {
        return projectionQueryOperations.deleteById(id, domainType);
    }

    public int deleteByQuery(Wrapper<T> queryWrapper) {
        return projectionQueryOperations.deleteByQuery(queryWrapper, domainType);
    }
}
