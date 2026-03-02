package com.old.silence.data.mybatis.projection;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.io.Serializable;
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

    public <ID extends Serializable> Optional<T> findById(ID id) {
        return projectionQueryOperations.findById(id, domainType);
    }

    public <ID extends Serializable, P> Optional<P> findById(ID id, Class<P> projectionType) {
        return projectionQueryOperations.findById(id, projectionType, domainType);
    }

    public List<T> findAll() {
        return projectionQueryOperations.findAll(domainType);
    }

    public <P> List<P> findAll(Class<P> projectionType) {
        return projectionQueryOperations.findAll(projectionType, domainType);
    }

    public <ID extends Serializable> List<T> findAllById(Iterable<ID> ids) {
        return projectionQueryOperations.findAllById(ids, domainType);
    }

    public <ID extends Serializable, P> List<P> findAllById(Iterable<ID> ids, Class<P> projectionType) {
        return projectionQueryOperations.findAllById(ids, projectionType, domainType);
    }

    public List<T> findByQuery(Wrapper<T> queryWrapper) {
        return projectionQueryOperations.findByQuery(queryWrapper, domainType);
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

    public long count() {
        return projectionQueryOperations.count(domainType);
    }

    public <ID extends Serializable> boolean existsById(ID id) {
        return projectionQueryOperations.existsById(id, domainType);
    }

    public boolean existsByQuery(Wrapper<T> queryWrapper) {
        return projectionQueryOperations.existsByQuery(queryWrapper, domainType);
    }

    public int insert(T entity) {
        return projectionQueryOperations.insert(entity, domainType);
    }

    public int insertAll(Iterable<? extends T> entities) {
        return projectionQueryOperations.insertAll(entities, domainType);
    }

    public int update(T entity) {
        return projectionQueryOperations.update(entity, domainType);
    }

    public int updateAll(Iterable<? extends T> entities) {
        return projectionQueryOperations.updateAll(entities, domainType);
    }

    public int updateNonNull(T entity) {
        return projectionQueryOperations.updateNonNull(entity, domainType);
    }

    public int save(T entity) {
        return projectionQueryOperations.save(entity, domainType);
    }

    public int create(T entity) {
        return insert(entity);
    }

    public int updateById(T entity) {
        return updateNonNull(entity);
    }

    public <ID extends Serializable> int deleteById(ID id) {
        return projectionQueryOperations.deleteById(id, domainType);
    }

    public int delete(T entity) {
        return projectionQueryOperations.delete(entity, domainType);
    }

    public <ID extends Serializable> int deleteAllById(Iterable<? extends ID> ids) {
        return projectionQueryOperations.deleteAllById(ids, domainType);
    }

    public int deleteAll(Iterable<? extends T> entities) {
        return projectionQueryOperations.deleteAll(entities, domainType);
    }

    public int deleteAll() {
        return projectionQueryOperations.deleteAll(domainType);
    }

    public int deleteByQuery(Wrapper<T> queryWrapper) {
        return projectionQueryOperations.deleteByQuery(queryWrapper, domainType);
    }
}
