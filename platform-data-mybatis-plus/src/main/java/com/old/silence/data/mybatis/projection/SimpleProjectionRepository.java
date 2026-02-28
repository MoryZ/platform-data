package com.old.silence.data.mybatis.projection;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Simple projection repository implementation.
 */
public class SimpleProjectionRepository<T, ID> implements ProjectionRepository<T, ID> {

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
    public Optional<T> findById(ID id) {
        Objects.requireNonNull(id, "Id must not be null");

        TableInfo tableInfo = getRequiredTableInfo();
        QueryWrapper<T> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(tableInfo.getKeyColumn(), id);

        List<T> records = findByQuery(queryWrapper, entityType);
        return records.stream().findFirst();
    }

    @Override
    public <P> Optional<P> findById(ID id, Class<P> projectionType) {
        Objects.requireNonNull(id, "Id must not be null");

        TableInfo tableInfo = getRequiredTableInfo();
        QueryWrapper<T> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(tableInfo.getKeyColumn(), id);

        List<P> records = findByQuery(queryWrapper, projectionType);
        return records.stream().findFirst();
    }

    @Override
    public <P> List<P> findByQuery(Wrapper<T> queryWrapper, Class<P> projectionType) {
        Objects.requireNonNull(queryWrapper, "Query wrapper must not be null");

        ProjectionMetadata metadata = metadataResolver.resolve(projectionType, entityType, Collections.emptyList());
        if (projectionType.isInterface()) {
            List<java.util.Map<String, Object>> resultMaps = queryExecutor.selectMaps(queryWrapper, metadata);
            return InterfaceProjectionFactory.createList(projectionType, resultMaps);
        }
        return queryExecutor.select(queryWrapper, metadata);
    }

    @Override
    public <P> IPage<P> findByQuery(Wrapper<T> queryWrapper, Page<?> page, Class<P> projectionType) {
        Objects.requireNonNull(queryWrapper, "Query wrapper must not be null");
        Objects.requireNonNull(page, "Page must not be null");
        Wrapper<T> dataWrapper = applyPageOrders(queryWrapper, page);

        ProjectionMetadata metadata = metadataResolver.resolve(projectionType, entityType, Collections.emptyList());
        if (projectionType.isInterface()) {
            IPage<java.util.Map<String, Object>> mapPage = queryExecutor.selectPageMaps(page, dataWrapper, queryWrapper, metadata);
            List<P> records = InterfaceProjectionFactory.createList(projectionType, mapPage.getRecords());

            @SuppressWarnings("unchecked")
            IPage<P> result = (IPage<P>) page;
            result.setRecords(records);
            result.setTotal(mapPage.getTotal());
            return result;
        }
        return queryExecutor.selectPage(page, dataWrapper, queryWrapper, metadata);
    }

    @Override
    public long countByQuery(Wrapper<T> queryWrapper) {
        Objects.requireNonNull(queryWrapper, "Query wrapper must not be null");
        return queryExecutor.selectCount(queryWrapper, entityType);
    }

    @Override
    public int create(T entity) {
        return queryExecutor.insert(entity, entityType);
    }

    @Override
    public int updateById(T entity) {
        return queryExecutor.updateById(entity, entityType);
    }

    @Override
    public int deleteById(ID id) {
        return queryExecutor.deleteById(id, entityType);
    }

    @Override
    public int deleteByQuery(Wrapper<T> queryWrapper) {
        return queryExecutor.deleteByQuery(queryWrapper, entityType);
    }

    @Override
    public boolean existsByQuery(Wrapper<T> queryWrapper) {
        return countByQuery(queryWrapper) > 0;
    }

    private Wrapper<T> applyPageOrders(Wrapper<T> queryWrapper, Page<?> page) {
        if (page.orders() == null || page.orders().isEmpty()) {
            return queryWrapper;
        }

        if (!(queryWrapper instanceof QueryWrapper<?> rawQueryWrapper)) {
            return queryWrapper;
        }

        @SuppressWarnings("unchecked")
        QueryWrapper<T> wrapper = ((QueryWrapper<T>) rawQueryWrapper).clone();
        for (OrderItem item : page.orders()) {
            String safeColumn = metadataResolver.resolveOrderColumn(entityType, item.getColumn());
            wrapper.orderBy(true, item.isAsc(), safeColumn);
        }
        return wrapper;
    }

    private TableInfo getRequiredTableInfo() {
        TableInfo tableInfo = TableInfoHelper.getTableInfo(entityType);
        if (tableInfo == null || tableInfo.getKeyColumn() == null || tableInfo.getKeyColumn().isBlank()) {
            throw new IllegalArgumentException("No @TableId found for entity type: " + entityType.getName());
        }
        return tableInfo;
    }
}
