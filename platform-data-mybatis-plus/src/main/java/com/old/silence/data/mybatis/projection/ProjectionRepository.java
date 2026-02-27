package com.old.silence.data.mybatis.projection;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

/**
 * Projection repository API for MyBatis Plus.
 */
public interface ProjectionRepository<T> {

    <P> List<P> findByQuery(Object query, Class<P> projectionType);

    <P> List<P> findByQuery(Object query, Class<P> projectionType, List<String> fields);

    <P> List<P> findByQuery(Object query, List<OrderItem> orderItems, Class<P> projectionType);

    <P> List<P> findByQuery(Object query, List<OrderItem> orderItems, Class<P> projectionType, List<String> fields);

    <P> IPage<P> findByQuery(Object query, Page<?> page, Class<P> projectionType);

    <P> IPage<P> findByQuery(Object query, Page<?> page, Class<P> projectionType, List<String> fields);

    <P> IPage<P> findByQuery(Object query, Page<?> page, List<OrderItem> orderItems, Class<P> projectionType);

    <P> IPage<P> findByQuery(Object query, Page<?> page, List<OrderItem> orderItems, Class<P> projectionType,
                             List<String> fields);
}
