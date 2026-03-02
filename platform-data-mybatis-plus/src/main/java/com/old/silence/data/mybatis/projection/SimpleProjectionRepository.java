package com.old.silence.data.mybatis.projection;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.BeanWrapperImpl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.StreamSupport;

/**
 * Simple projection repository implementation.
 */
public class SimpleProjectionRepository<T, ID extends Serializable> implements ProjectionRepository<T, ID> {

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
    public List<T> findAll() {
        return findByQuery(new QueryWrapper<>(), entityType);
    }

    @Override
    public <P> List<P> findAll(Class<P> projectionType) {
        return findByQuery(new QueryWrapper<>(), projectionType);
    }

    @Override
    public List<T> findAllById(Iterable<ID> ids) {
        return findAllById(ids, entityType);
    }

    @Override
    public <P> List<P> findAllById(Iterable<ID> ids, Class<P> projectionType) {
        List<ID> idList = toList(ids);
        if (idList.isEmpty()) {
            return Collections.emptyList();
        }

        TableInfo tableInfo = getRequiredTableInfo();
        QueryWrapper<T> queryWrapper = new QueryWrapper<>();
        queryWrapper.in(tableInfo.getKeyColumn(), idList);
        return findByQuery(queryWrapper, projectionType);
    }

    @Override
    public List<T> findByQuery(Wrapper<T> queryWrapper) {
        return findByQuery(queryWrapper, entityType);
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
    public boolean existsById(ID id) {
        return findById(id).isPresent();
    }

    @Override
    public boolean existsByQuery(Wrapper<T> queryWrapper) {
        return countByQuery(queryWrapper) > 0;
    }

    @Override
    public long count() {
        return countByQuery(new QueryWrapper<>());
    }

    @Override
    public <S extends T> int insert(S entity) {
        Objects.requireNonNull(entity, "Entity must not be null");
        return queryExecutor.insert(entity, entityType);
    }

    @Override
    public <S extends T> int insertAll(Iterable<S> entities) {
        if (entities == null) {
            return 0;
        }
        return StreamSupport.stream(entities.spliterator(), false)
                .map(this::insert)
                .reduce(0, Integer::sum);
    }

    @Override
    public <S extends T> int update(S entity) {
        Objects.requireNonNull(entity, "Entity must not be null");
        return queryExecutor.updateByIdAllowNull(entity, entityType);
    }

    @Override
    public <S extends T> int updateAll(Iterable<S> entities) {
        if (entities == null) {
            return 0;
        }
        return StreamSupport.stream(entities.spliterator(), false)
                .map(this::update)
                .reduce(0, Integer::sum);
    }

    @Override
    public <S extends T> int updateNonNull(S entity) {
        Objects.requireNonNull(entity, "Entity must not be null");
        return queryExecutor.updateById(entity, entityType);
    }

    @Override
    public <S extends T> int save(S entity) {
        Objects.requireNonNull(entity, "Entity must not be null");

        ID id = extractId(entity);
        if (id == null) {
            return insert(entity);
        }
        return existsById(id) ? updateNonNull(entity) : insert(entity);
    }

    @Override
    public int deleteById(ID id) {
        Objects.requireNonNull(id, "Id must not be null");
        return queryExecutor.deleteById(id, entityType);
    }

    @Override
    public int delete(T entity) {
        Objects.requireNonNull(entity, "Entity must not be null");

        ID id = extractId(entity);
        if (id == null) {
            return 0;
        }
        return deleteById(id);
    }

    @Override
    public int deleteAllById(Iterable<? extends ID> ids) {
        if (ids == null) {
            return 0;
        }
        return StreamSupport.stream(ids.spliterator(), false)
                .map(this::deleteById)
                .reduce(0, Integer::sum);
    }

    @Override
    public int deleteAll(Iterable<? extends T> entities) {
        if (entities == null) {
            return 0;
        }
        return StreamSupport.stream(entities.spliterator(), false)
                .map(this::delete)
                .reduce(0, Integer::sum);
    }

    @Override
    public int deleteAll() {
        return queryExecutor.deleteAll(entityType);
    }

    @Override
    public int deleteByQuery(Wrapper<T> queryWrapper) {
        return queryExecutor.deleteByQuery(queryWrapper, entityType);
    }

    private List<ID> toList(Iterable<ID> ids) {
        if (ids == null) {
            return Collections.emptyList();
        }
        List<ID> idList = new ArrayList<>();
        ids.forEach(idList::add);
        return idList;
    }

    @SuppressWarnings("unchecked")
    private <S extends T> ID extractId(S entity) {
        TableInfo tableInfo = getRequiredTableInfo();
        BeanWrapperImpl beanWrapper = new BeanWrapperImpl(entity);
        Object id = beanWrapper.getPropertyValue(tableInfo.getKeyProperty());
        return (ID) id;
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
