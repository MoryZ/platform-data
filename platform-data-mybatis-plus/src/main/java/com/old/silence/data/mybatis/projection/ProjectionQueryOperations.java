package com.old.silence.data.mybatis.projection;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.io.Serializable;
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

    public <T, ID extends Serializable> Optional<T> findById(ID id, Class<T> domainType) {
        ProjectionRepository<T, ID> repository = projectionRepositoryFactory.create(domainType);
        return repository.findById(id);
    }

    public <T, ID extends Serializable, P> Optional<P> findById(ID id, Class<P> projectionType, Class<T> domainType) {
        ProjectionRepository<T, ID> repository = projectionRepositoryFactory.create(domainType);
        return repository.findById(id, projectionType);
    }

    public <T> List<T> findAll(Class<T> domainType) {
        return projectionRepositoryFactory.create(domainType).findAll();
    }

    public <T, P> List<P> findAll(Class<P> projectionType, Class<T> domainType) {
        return projectionRepositoryFactory.create(domainType).findAll(projectionType);
    }

    public <T, ID extends Serializable> List<T> findAllById(Iterable<ID> ids, Class<T> domainType) {
        ProjectionRepository<T, ID> repository = projectionRepositoryFactory.create(domainType);
        return repository.findAllById(ids);
    }

    public <T, ID extends Serializable, P> List<P> findAllById(Iterable<ID> ids,
                                                               Class<P> projectionType,
                                                               Class<T> domainType) {
        ProjectionRepository<T, ID> repository = projectionRepositoryFactory.create(domainType);
        return repository.findAllById(ids, projectionType);
    }

    public <T> List<T> findByQuery(Wrapper<T> queryWrapper, Class<T> domainType) {
        return projectionRepositoryFactory.create(domainType).findByQuery(queryWrapper);
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

    public <T> long count(Class<T> domainType) {
        return projectionRepositoryFactory.create(domainType).count();
    }

    public <T, ID extends Serializable> boolean existsById(ID id, Class<T> domainType) {
        return projectionRepositoryFactory.create(domainType).existsById(id);
    }

    public <T> boolean existsByQuery(Wrapper<T> queryWrapper, Class<T> domainType) {
        return projectionRepositoryFactory.create(domainType).existsByQuery(queryWrapper);
    }

    public <T, S extends T> int insert(S entity, Class<T> domainType) {
        return projectionRepositoryFactory.create(domainType).insert(entity);
    }

    public <T, S extends T> int insertAll(Iterable<S> entities, Class<T> domainType) {
        return projectionRepositoryFactory.create(domainType).insertAll(entities);
    }

    public <T, S extends T> int update(S entity, Class<T> domainType) {
        return projectionRepositoryFactory.create(domainType).update(entity);
    }

    public <T, S extends T> int updateAll(Iterable<S> entities, Class<T> domainType) {
        return projectionRepositoryFactory.create(domainType).updateAll(entities);
    }

    public <T, S extends T> int updateNonNull(S entity, Class<T> domainType) {
        return projectionRepositoryFactory.create(domainType).updateNonNull(entity);
    }

    public <T, S extends T> int save(S entity, Class<T> domainType) {
        return projectionRepositoryFactory.create(domainType).save(entity);
    }

    public <T, ID extends Serializable> int deleteById(ID id, Class<T> domainType) {
        return projectionRepositoryFactory.create(domainType).deleteById(id);
    }

    public <T> int delete(T entity, Class<T> domainType) {
        return projectionRepositoryFactory.create(domainType).delete(entity);
    }

    public <T, ID extends Serializable> int deleteAllById(Iterable<? extends ID> ids, Class<T> domainType) {
        return projectionRepositoryFactory.create(domainType).deleteAllById(ids);
    }

    public <T> int deleteAll(Iterable<? extends T> entities, Class<T> domainType) {
        return projectionRepositoryFactory.create(domainType).deleteAll(entities);
    }

    public <T> int deleteAll(Class<T> domainType) {
        return projectionRepositoryFactory.create(domainType).deleteAll();
    }

    public <T> int deleteByQuery(Wrapper<T> queryWrapper, Class<T> domainType) {
        return projectionRepositoryFactory.create(domainType).deleteByQuery(queryWrapper);
    }

    public <T> int create(T entity, Class<T> domainType) {
        return insert(entity, domainType);
    }

    public <T> int updateById(T entity, Class<T> domainType) {
        return updateNonNull(entity, domainType);
    }
}
