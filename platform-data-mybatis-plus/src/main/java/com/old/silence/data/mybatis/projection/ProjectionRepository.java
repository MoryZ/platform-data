package com.old.silence.data.mybatis.projection;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.old.silence.core.exception.ResourceNotFoundException;

import java.io.Serializable;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.StreamSupport;

import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Projection repository API for MyBatis Plus.
 */
public interface ProjectionRepository<T, ID extends Serializable> {

    Optional<T> findById(ID id);

    default T findRequiredById(ID id) {
        return findById(id).orElseThrow(ResourceNotFoundException::new);
    }

    <P> Optional<P> findById(ID id, Class<P> projectionType);

    default <P> P findRequiredById(ID id, Class<P> projectionType) {
        return findById(id, projectionType).orElseThrow(ResourceNotFoundException::new);
    }

    List<T> findAll();

    <P> List<P> findAll(Class<P> projectionType);

    List<T> findAllById(Iterable<ID> ids);

    <P> List<P> findAllById(Iterable<ID> ids, Class<P> projectionType);

    List<T> findByQuery(Wrapper<T> queryWrapper);

    <P> List<P> findByQuery(Wrapper<T> queryWrapper, Class<P> projectionType);

    <P> IPage<P> findByQuery(Wrapper<T> queryWrapper, Page<?> page, Class<P> projectionType);

    boolean existsById(ID id);

    boolean existsByQuery(Wrapper<T> queryWrapper);

    long count();

    long countByQuery(Wrapper<T> queryWrapper);

    <S extends T> int insert(S entity);

    @Transactional
    <S extends T> int insertAll(Iterable<S> entities);

    <S extends T> int update(S entity);

    @Transactional
    <S extends T> int updateAll(Iterable<S> entities);

    <S extends T> int updateNonNull(S entity);

    @Transactional
    default <S extends T> int updateAllNonNull(Iterable<S> entities) {
        return entities == null ? 0 
        : StreamSupport.stream(Spliterators.spliteratorUnknownSize(entities.iterator(), Spliterator.NONNULL), false)
            .map(this::updateNonNull).reduce(0, (total, current) -> total + current);
    }

    <S extends T> int save(S entity);

    int deleteById(ID id);

    int delete(T entity);

    @Transactional
    int deleteAllById(Iterable<? extends ID> ids);

    @Transactional
    int deleteAll(Iterable<? extends T> entities);

    int deleteAll();

    int deleteByQuery(Wrapper<T> queryWrapper);

}
