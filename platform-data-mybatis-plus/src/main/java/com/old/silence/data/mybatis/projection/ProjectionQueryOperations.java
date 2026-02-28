package com.old.silence.data.mybatis.projection;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Projection query operations with explicit domain type.
 */
public class ProjectionQueryOperations {

    private final ProjectionRepositoryFactory projectionRepositoryFactory;

    public ProjectionQueryOperations(ProjectionRepositoryFactory projectionRepositoryFactory) {
        this.projectionRepositoryFactory = Objects.requireNonNull(projectionRepositoryFactory,
                "ProjectionRepositoryFactory must not be null");
    }

    public <T, ID> Optional<T> findById(ID id, Class<T> domainType) {
        ProjectionRepository<T, ID> repository = projectionRepositoryFactory.create(domainType);
        return repository.findById(id);
    }

    public <T, ID, P> Optional<P> findById(ID id, Class<P> projectionType, Class<T> domainType) {
        ProjectionRepository<T, ID> repository = projectionRepositoryFactory.create(domainType);
        return repository.findById(id, projectionType);
    }

    public <T, P> List<P> findByQuery(Wrapper<T> queryWrapper, Class<P> projectionType, Class<T> domainType) {
        return projectionRepositoryFactory.create(domainType).findByQuery(queryWrapper, projectionType);
    }

    public <T, P> IPage<P> findByQuery(Wrapper<T> queryWrapper,
                                       Page<?> page,
                                       Class<P> projectionType,
                                       Class<T> domainType) {
        return projectionRepositoryFactory.create(domainType).findByQuery(queryWrapper, page, projectionType);
    }

    public <T> long countByQuery(Wrapper<T> queryWrapper, Class<T> domainType) {
        return projectionRepositoryFactory.create(domainType).countByQuery(queryWrapper);
    }

    public <T> boolean existsByQuery(Wrapper<T> queryWrapper, Class<T> domainType) {
        return projectionRepositoryFactory.create(domainType).existsByQuery(queryWrapper);
    }

    public <T> int create(T entity, Class<T> domainType) {
        return projectionRepositoryFactory.create(domainType).create(entity);
    }

    public <T> int updateById(T entity, Class<T> domainType) {
        return projectionRepositoryFactory.create(domainType).updateById(entity);
    }

    public <T, ID> int deleteById(ID id, Class<T> domainType) {
        return projectionRepositoryFactory.create(domainType).deleteById(id);
    }

    public <T> int deleteByQuery(Wrapper<T> queryWrapper, Class<T> domainType) {
        return projectionRepositoryFactory.create(domainType).deleteByQuery(queryWrapper);
    }
}
