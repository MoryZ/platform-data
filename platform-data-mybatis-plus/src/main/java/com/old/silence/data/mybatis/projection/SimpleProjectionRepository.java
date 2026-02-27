package com.old.silence.data.mybatis.projection;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.old.silence.data.commons.converter.QueryWrapperConverter;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Simple projection repository implementation.
 */
public class SimpleProjectionRepository<T> implements ProjectionRepository<T> {

    private final Class<T> entityType;
    private final ProjectionMetadataResolver metadataResolver;
    private final ProjectionQueryExecutor queryExecutor;

    public SimpleProjectionRepository(Class<T> entityType,
                                      ProjectionMetadataResolver metadataResolver,
                                      ProjectionQueryExecutor queryExecutor) {
        this.entityType = Objects.requireNonNull(entityType, "Entity type must not be null");
        this.metadataResolver = Objects.requireNonNull(metadataResolver, "Metadata resolver must not be null");
        this.queryExecutor = Objects.requireNonNull(queryExecutor, "Query executor must not be null");
    }

    @Override
    public <P> List<P> findByQuery(Object query, Class<P> projectionType) {
        return findByQuery(query, projectionType, Collections.emptyList());
    }

    @Override
    public <P> List<P> findByQuery(Object query, Class<P> projectionType, List<String> fields) {
        return findByQuery(query, Collections.emptyList(), projectionType, fields);
    }

    @Override
    public <P> List<P> findByQuery(Object query, List<OrderItem> orderItems, Class<P> projectionType) {
        return findByQuery(query, orderItems, projectionType, Collections.emptyList());
    }

    @Override
    public <P> List<P> findByQuery(Object query, List<OrderItem> orderItems, Class<P> projectionType, List<String> fields) {
        QueryWrapper<T> wrapper = QueryWrapperConverter.convert(query, entityType);
        applyOrderItems(wrapper, orderItems);

        ProjectionMetadata metadata = metadataResolver.resolve(projectionType, entityType, fields);
        if (projectionType.isInterface()) {
            List<java.util.Map<String, Object>> resultMaps = queryExecutor.selectMaps(wrapper, metadata);
            return InterfaceProjectionFactory.createList(projectionType, resultMaps);
        }
        return queryExecutor.select(wrapper, metadata);
    }

    @Override
    public <P> IPage<P> findByQuery(Object query, Page<?> page, Class<P> projectionType) {
        return findByQuery(query, page, projectionType, Collections.emptyList());
    }

    @Override
    public <P> IPage<P> findByQuery(Object query, Page<?> page, Class<P> projectionType, List<String> fields) {
        return findByQuery(query, page, Collections.emptyList(), projectionType, fields);
    }

    @Override
    public <P> IPage<P> findByQuery(Object query, Page<?> page, List<OrderItem> orderItems, Class<P> projectionType) {
        return findByQuery(query, page, orderItems, projectionType, Collections.emptyList());
    }

    @Override
    public <P> IPage<P> findByQuery(Object query, Page<?> page, List<OrderItem> orderItems, Class<P> projectionType,
                                    List<String> fields) {
        QueryWrapper<T> countWrapper = QueryWrapperConverter.convert(query, entityType);
        QueryWrapper<T> wrapper = QueryWrapperConverter.convert(query, entityType);
        applyOrderItems(wrapper, orderItems);

        ProjectionMetadata metadata = metadataResolver.resolve(projectionType, entityType, fields);
        if (projectionType.isInterface()) {
            IPage<java.util.Map<String, Object>> mapPage = queryExecutor.selectPageMaps(page, wrapper, countWrapper, metadata);
            List<P> records = InterfaceProjectionFactory.createList(projectionType, mapPage.getRecords());

            @SuppressWarnings("unchecked")
            IPage<P> result = (IPage<P>) page;
            result.setRecords(records);
            result.setTotal(mapPage.getTotal());
            return result;
        }
        return queryExecutor.selectPage(page, wrapper, countWrapper, metadata);
    }

    private void applyOrderItems(QueryWrapper<T> wrapper, List<OrderItem> orderItems) {
        if (orderItems == null || orderItems.isEmpty()) {
            return;
        }
        for (OrderItem item : orderItems) {
            String safeColumn = metadataResolver.resolveOrderColumn(entityType, item.getColumn());
            wrapper.orderBy(true, item.isAsc(), safeColumn);
        }
    }
}
