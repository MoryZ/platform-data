package com.old.silence.data.mybatis.projection;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.Optional;
import java.util.List;

/**
 * Projection repository API for MyBatis Plus.
 */
public interface ProjectionRepository<T, ID> {

    Optional<T> findById(ID id);

    <P> Optional<P> findById(ID id, Class<P> projectionType);

    <P> List<P> findByQuery(Wrapper<T> queryWrapper, Class<P> projectionType);

    <P> IPage<P> findByQuery(Wrapper<T> queryWrapper, Page<?> page, Class<P> projectionType);

    int create(T entity);

    int updateById(T entity);

    int deleteById(ID id);

    int deleteByQuery(Wrapper<T> queryWrapper);

    long countByQuery(Wrapper<T> queryWrapper);

    boolean existsByQuery(Wrapper<T> queryWrapper);

}
