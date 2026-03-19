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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

/**
 * Simple projection repository implementation.
 */
public class SimpleProjectionRepository<T, ID extends Serializable> implements ProjectionRepository<T, ID> {

    private static final Pattern SQL_QUALIFIER_PATTERN = Pattern.compile("\\b([A-Za-z_][A-Za-z0-9_]*)\\.");

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

        ProjectionMetadata metadata = metadataResolver.resolve(projectionType,
                entityType,
                Collections.emptyList(),
                extractConditionAssociationHints(queryWrapper));
        if (projectionType.isInterface()) {
            if (!metadata.getCollectionAssociations().isEmpty()) {
                throw new IllegalArgumentException("Collection association projection for interface type is not supported yet: "
                        + projectionType.getName());
            }
            List<java.util.Map<String, Object>> resultMaps = queryExecutor.selectMaps(queryWrapper, metadata);
            return InterfaceProjectionFactory.createList(projectionType, resultMaps);
        }
        List<P> results = queryExecutor.select(queryWrapper, metadata);
        loadCollectionAssociations(results, metadata);
        return results;
    }

    @Override
    public <P> IPage<P> findByQuery(Wrapper<T> queryWrapper, Page<?> page, Class<P> projectionType) {
        Objects.requireNonNull(queryWrapper, "Query wrapper must not be null");
        Objects.requireNonNull(page, "Page must not be null");
        Wrapper<T> dataWrapper = applyPageOrders(queryWrapper, page);

        ProjectionMetadata metadata = metadataResolver.resolve(projectionType,
                entityType,
                Collections.emptyList(),
                extractConditionAssociationHints(queryWrapper));
        if (projectionType.isInterface()) {
            if (!metadata.getCollectionAssociations().isEmpty()) {
                throw new IllegalArgumentException("Collection association projection for interface type is not supported yet: "
                        + projectionType.getName());
            }
            IPage<java.util.Map<String, Object>> mapPage = queryExecutor.selectPageMaps(page, dataWrapper, queryWrapper, metadata);
            List<P> records = InterfaceProjectionFactory.createList(projectionType, mapPage.getRecords());

            @SuppressWarnings("unchecked")
            IPage<P> result = (IPage<P>) page;
            result.setRecords(records);
            result.setTotal(mapPage.getTotal());
            return result;
        }
        IPage<P> result = queryExecutor.selectPage(page, dataWrapper, queryWrapper, metadata);
        loadCollectionAssociations(result.getRecords(), metadata);
        return result;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <P> void loadCollectionAssociations(List<P> records, ProjectionMetadata metadata) {
        if (records == null || records.isEmpty() || metadata.getCollectionAssociations().isEmpty()) {
            return;
        }

        for (ProjectionCollectionAssociation association : metadata.getCollectionAssociations()) {
            List<Object> sourceKeys = new ArrayList<>();
            Map<Object, List<P>> sourceByKey = new HashMap<>();
            for (P record : records) {
                BeanWrapperImpl beanWrapper = new BeanWrapperImpl(record);
                Object sourceKey = beanWrapper.getPropertyValue(association.getSourceKeyProperty());
                if (sourceKey == null) {
                    continue;
                }
                sourceByKey.computeIfAbsent(sourceKey, ignored -> {
                    sourceKeys.add(sourceKey);
                    return new ArrayList<>();
                }).add(record);
            }

            if (sourceKeys.isEmpty()) {
                continue;
            }

            if (association.getJoinTableName() != null) {
                // ManyToMany via join table: two-step loading
                List<Map<String, Object>> pairs = queryExecutor.selectJoinTablePairs(
                        association.getJoinTableName(),
                        association.getJoinTableSourceCol(),
                        association.getJoinTableTargetCol(),
                        sourceKeys);
                if (pairs.isEmpty()) {
                    for (List<P> grouped : sourceByKey.values()) {
                        for (P record : grouped) {
                            BeanWrapperImpl bw = new BeanWrapperImpl(record);
                            Object curr = bw.getPropertyValue(association.getProjectionPropertyName());
                            if (curr instanceof Collection) {
                                ((Collection) curr).clear();
                            } else {
                                bw.setPropertyValue(association.getProjectionPropertyName(), new ArrayList<>());
                            }
                        }
                    }
                    continue;
                }
                List<Object> targetIds = new ArrayList<>();
                Map<String, List<String>> sourceToTargetIds = new HashMap<>();
                for (Map<String, Object> pair : pairs) {
                    Object src = getMapValueIgnoreCase(pair, association.getJoinTableSourceCol());
                    Object tgt = getMapValueIgnoreCase(pair, association.getJoinTableTargetCol());
                    if (src == null || tgt == null) {
                        continue;
                    }
                    String srcStr = src.toString();
                    String tgtStr = tgt.toString();
                    sourceToTargetIds.computeIfAbsent(srcStr, k -> new ArrayList<>()).add(tgtStr);
                    if (!targetIds.contains(tgt)) {
                        targetIds.add(tgt);
                    }
                }
                if (targetIds.isEmpty()) {
                    continue;
                }
                TableInfo targetTableInfo = TableInfoHelper.getTableInfo(association.getTargetEntityType());
                QueryWrapper targetQuery = new QueryWrapper();
                targetQuery.in(targetTableInfo.getKeyColumn(), targetIds);
                ProjectionMetadata childMetadata = metadataResolver.resolve(association.getElementType(),
                        association.getTargetEntityType(), Collections.emptyList());
                List<?> children = queryExecutor.select(targetQuery, childMetadata);
                Map<String, Object> targetByStrId = new HashMap<>();
                for (Object child : children) {
                    Object id = new BeanWrapperImpl(child).getPropertyValue(targetTableInfo.getKeyProperty());
                    if (id != null) {
                        targetByStrId.put(id.toString(), child);
                    }
                }
                for (Map.Entry<Object, List<P>> entry : sourceByKey.entrySet()) {
                    List<String> targetStrIds = sourceToTargetIds.getOrDefault(
                            entry.getKey().toString(), Collections.emptyList());
                    List<Object> related = new ArrayList<>();
                    for (String tId : targetStrIds) {
                        Object t = targetByStrId.get(tId);
                        if (t != null) {
                            related.add(t);
                        }
                    }
                    for (P record : entry.getValue()) {
                        BeanWrapperImpl bw = new BeanWrapperImpl(record);
                        Object curr = bw.getPropertyValue(association.getProjectionPropertyName());
                        if (curr instanceof Collection) {
                            ((Collection) curr).clear();
                            ((Collection) curr).addAll(related);
                        } else {
                            bw.setPropertyValue(association.getProjectionPropertyName(), new ArrayList<>(related));
                        }
                    }
                }
            } else {
                // OneToMany: FK is on target side
                QueryWrapper childrenQuery = new QueryWrapper();
                childrenQuery.in(association.getTargetFkColumn(), sourceKeys);

                ProjectionMetadata childMetadata = metadataResolver.resolve(association.getElementType(),
                        association.getTargetEntityType(),
                        Collections.emptyList());
                List<?> children = queryExecutor.select(childrenQuery, childMetadata);

                Map<Object, List<Object>> childrenBySourceKey = new HashMap<>();
                for (Object child : children) {
                    BeanWrapperImpl childWrapper = new BeanWrapperImpl(child);
                    Object fk = childWrapper.getPropertyValue(association.getTargetFkProperty());
                    if (fk == null) {
                        continue;
                    }
                    childrenBySourceKey.computeIfAbsent(fk, ignored -> new ArrayList<>()).add(child);
                }

                for (Map.Entry<Object, List<P>> entry : sourceByKey.entrySet()) {
                    List<Object> related = childrenBySourceKey.getOrDefault(entry.getKey(), Collections.emptyList());
                    for (P record : entry.getValue()) {
                        BeanWrapperImpl beanWrapper = new BeanWrapperImpl(record);
                        Object currentValue = beanWrapper.getPropertyValue(association.getProjectionPropertyName());
                        if (currentValue instanceof Collection) {
                            ((Collection) currentValue).clear();
                            ((Collection) currentValue).addAll(related);
                        } else {
                            beanWrapper.setPropertyValue(association.getProjectionPropertyName(), new ArrayList<>(related));
                        }
                    }
                }
            }
        }
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
    public int insert(T entity) {
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
    public int deleteById(Serializable id) {
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

    private static Object getMapValueIgnoreCase(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v != null) return v;
        v = map.get(key.toUpperCase());
        if (v != null) return v;
        return map.get(key.toLowerCase());
    }

    private List<String> extractConditionAssociationHints(Wrapper<T> queryWrapper) {
        String sqlSegment = queryWrapper.getCustomSqlSegment();
        if (sqlSegment == null || sqlSegment.isBlank()) {
            return Collections.emptyList();
        }

        Set<String> qualifiers = new LinkedHashSet<>();
        Matcher matcher = SQL_QUALIFIER_PATTERN.matcher(sqlSegment);
        while (matcher.find()) {
            String qualifier = matcher.group(1);
            if (qualifier == null || qualifier.isBlank()) {
                continue;
            }
            if ("t0".equalsIgnoreCase(qualifier)
                    || "ew".equalsIgnoreCase(qualifier)
                    || "paramNameValuePairs".equalsIgnoreCase(qualifier)) {
                continue;
            }
            qualifiers.add(qualifier);
        }
        return qualifiers.isEmpty() ? Collections.emptyList() : new ArrayList<>(qualifiers);
    }
}
