package com.old.silence.data.mybatis.projection;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.beans.PropertyDescriptor;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
        if (ProjectionResultMaterializer.requiresMapQuery(projectionType)) {
            ProjectionMetadata mapMetadata = buildMapQueryMetadata(metadata);
            List<java.util.Map<String, Object>> resultMaps = queryExecutor.selectMaps(queryWrapper, mapMetadata);
            loadCollectionAssociationsToRows(resultMaps, mapMetadata);
            return ProjectionResultMaterializer.materializeList(projectionType, mapMetadata, resultMaps);
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
        if (ProjectionResultMaterializer.requiresMapQuery(projectionType)) {
            ProjectionMetadata mapMetadata = buildMapQueryMetadata(metadata);
            IPage<java.util.Map<String, Object>> mapPage = queryExecutor.selectPageMaps(page, dataWrapper, queryWrapper, mapMetadata);
            loadCollectionAssociationsToRows(mapPage.getRecords(), mapMetadata);
            return ProjectionResultMaterializer.materializePage(page, mapPage, projectionType, mapMetadata);
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
            Map<String, List<P>> sourceByKey = new HashMap<>();
            for (P record : records) {
                BeanWrapperImpl beanWrapper = new BeanWrapperImpl(record);
                Object sourceKey = beanWrapper.getPropertyValue(association.getSourceKeyProperty());
                if (sourceKey == null) {
                    continue;
                }
                String normalizedSourceKey = normalizeAssociationKey(sourceKey);
                sourceByKey.computeIfAbsent(normalizedSourceKey, ignored -> {
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
                TableInfo targetTableInfo = queryExecutor.requireTableInfo(association.getTargetEntityType());
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
                for (Map.Entry<String, List<P>> entry : sourceByKey.entrySet()) {
                    List<String> targetStrIds = sourceToTargetIds.getOrDefault(
                            entry.getKey(), Collections.emptyList());
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

                Map<String, List<Object>> childrenBySourceKey = new HashMap<>();
                for (Object child : children) {
                    BeanWrapperImpl childWrapper = new BeanWrapperImpl(child);
                    Object fk = childWrapper.getPropertyValue(association.getTargetFkProperty());
                    if (fk == null) {
                        continue;
                    }
                    childrenBySourceKey.computeIfAbsent(normalizeAssociationKey(fk), ignored -> new ArrayList<>()).add(child);
                }

                for (Map.Entry<String, List<P>> entry : sourceByKey.entrySet()) {
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

    private ProjectionMetadata buildMapQueryMetadata(ProjectionMetadata metadata) {
        if (metadata.getCollectionAssociations().isEmpty()) {
            return metadata;
        }

        List<ProjectionField> fields = new ArrayList<>(metadata.getFields());
        boolean changed = false;
        for (ProjectionCollectionAssociation association : metadata.getCollectionAssociations()) {
            if (containsSourceKeyField(fields, association)) {
                continue;
            }
            String alias = sourceKeyAlias(association);
            fields.add(new ProjectionField(alias,
                    "t0." + association.getSourceKeyColumn() + " AS " + alias,
                    alias,
                    Object.class,
                    null,
                    false));
            changed = true;
        }

        if (!changed) {
            return metadata;
        }

        return new ProjectionMetadata(metadata.getProjectionType(),
                metadata.getEntityType(),
                metadata.getTableName(),
                metadata.getFromClause(),
                fields,
                metadata.getCollectionAssociations(),
                metadata.getSelectionKey() + "|map-collection-source",
                metadata.isCollectionJoinInFrom(),
                metadata.getAdditionalFields());
    }

    private boolean containsSourceKeyField(List<ProjectionField> fields, ProjectionCollectionAssociation association) {
        for (ProjectionField field : fields) {
            if (association.getSourceKeyProperty().equals(field.getPropertyName())
                    || association.getSourceKeyColumn().equalsIgnoreCase(field.getColumnName())
                    || sourceKeyAlias(association).equals(field.getPropertyName())) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void loadCollectionAssociationsToRows(List<Map<String, Object>> rows, ProjectionMetadata metadata) {
        if (rows == null || rows.isEmpty() || metadata.getCollectionAssociations().isEmpty()) {
            return;
        }

        for (ProjectionCollectionAssociation association : metadata.getCollectionAssociations()) {
            Map<String, List<Map<String, Object>>> rowsBySourceKey = new LinkedHashMap<>();
            List<Object> sourceKeys = new ArrayList<>();

            for (Map<String, Object> row : rows) {
                Object sourceKey = resolveSourceKeyFromRow(row, association);
                if (sourceKey == null) {
                    row.put(association.getProjectionPropertyName(), new ArrayList<>());
                    continue;
                }
                String normalizedSourceKey = normalizeAssociationKey(sourceKey);
                if (!rowsBySourceKey.containsKey(normalizedSourceKey)) {
                    rowsBySourceKey.put(normalizedSourceKey, new ArrayList<>());
                    sourceKeys.add(sourceKey);
                }
                rowsBySourceKey.get(normalizedSourceKey).add(row);
            }

            if (sourceKeys.isEmpty()) {
                continue;
            }

            if (association.getJoinTableName() != null) {
                List<Map<String, Object>> pairs = queryExecutor.selectJoinTablePairs(
                        association.getJoinTableName(),
                        association.getJoinTableSourceCol(),
                        association.getJoinTableTargetCol(),
                        sourceKeys);

                if (pairs.isEmpty()) {
                    for (List<Map<String, Object>> groupedRows : rowsBySourceKey.values()) {
                        for (Map<String, Object> row : groupedRows) {
                            row.put(association.getProjectionPropertyName(), new ArrayList<>());
                        }
                    }
                    continue;
                }

                Map<String, List<String>> sourceToTargetIds = new HashMap<>();
                Set<Object> targetIds = new LinkedHashSet<>();
                for (Map<String, Object> pair : pairs) {
                    Object src = getMapValueIgnoreCase(pair, association.getJoinTableSourceCol());
                    Object tgt = getMapValueIgnoreCase(pair, association.getJoinTableTargetCol());
                    if (src == null || tgt == null) {
                        continue;
                    }
                    sourceToTargetIds.computeIfAbsent(src.toString(), ignored -> new ArrayList<>()).add(tgt.toString());
                    targetIds.add(tgt);
                }

                if (targetIds.isEmpty()) {
                    continue;
                }

                TableInfo targetTableInfo = queryExecutor.requireTableInfo(association.getTargetEntityType());
                QueryWrapper targetQuery = new QueryWrapper();
                targetQuery.in(targetTableInfo.getKeyColumn(), targetIds);

                ProjectionMetadata childMetadata = metadataResolver.resolve(association.getElementType(),
                        association.getTargetEntityType(),
                        Collections.emptyList());
                List<?> children = selectAssociationChildren(targetQuery, childMetadata, association.getElementType());

                Map<String, Object> childrenByTargetId = new HashMap<>();
                for (Object child : children) {
                    Object childId = new BeanWrapperImpl(child).getPropertyValue(targetTableInfo.getKeyProperty());
                    if (childId != null) {
                        childrenByTargetId.put(childId.toString(), child);
                    }
                }

                for (Map.Entry<String, List<Map<String, Object>>> entry : rowsBySourceKey.entrySet()) {
                    List<String> targetStrIds = sourceToTargetIds.getOrDefault(entry.getKey(), Collections.emptyList());
                    List<Object> related = new ArrayList<>();
                    for (String targetId : targetStrIds) {
                        Object child = childrenByTargetId.get(targetId);
                        if (child != null) {
                            related.add(child);
                        }
                    }
                    for (Map<String, Object> row : entry.getValue()) {
                        row.put(association.getProjectionPropertyName(), related);
                    }
                }
                continue;
            }

            QueryWrapper childrenQuery = new QueryWrapper();
            childrenQuery.in(association.getTargetFkColumn(), sourceKeys);
            ProjectionMetadata childMetadata = metadataResolver.resolve(association.getElementType(),
                    association.getTargetEntityType(),
                    Collections.emptyList());
            List<?> children = selectAssociationChildren(childrenQuery, childMetadata, association.getElementType());

            Map<String, List<Object>> childrenBySourceKey = new HashMap<>();
            for (Object child : children) {
                Object fkValue = new BeanWrapperImpl(child).getPropertyValue(association.getTargetFkProperty());
                if (fkValue == null) {
                    continue;
                }
                childrenBySourceKey.computeIfAbsent(normalizeAssociationKey(fkValue), ignored -> new ArrayList<>()).add(child);
            }

            for (Map.Entry<String, List<Map<String, Object>>> entry : rowsBySourceKey.entrySet()) {
                List<Object> related = childrenBySourceKey.getOrDefault(entry.getKey(), Collections.emptyList());
                for (Map<String, Object> row : entry.getValue()) {
                    row.put(association.getProjectionPropertyName(), related);
                }
            }
        }
    }

    private List<?> selectAssociationChildren(QueryWrapper<?> queryWrapper,
                                              ProjectionMetadata metadata,
                                              Class<?> projectionType) {
        if (!ProjectionResultMaterializer.requiresMapQuery(projectionType)) {
            return queryExecutor.select(queryWrapper, metadata);
        }

        ProjectionMetadata mapMetadata = buildMapQueryMetadata(metadata);
        List<Map<String, Object>> childRows = queryExecutor.selectMaps(queryWrapper, mapMetadata);
        loadCollectionAssociationsToRows(childRows, mapMetadata);
        return ProjectionResultMaterializer.materializeList(projectionType, mapMetadata, childRows);
    }

    private Object resolveSourceKeyFromRow(Map<String, Object> row, ProjectionCollectionAssociation association) {
        Object sourceKey = getMapValueIgnoreCase(row, sourceKeyAlias(association));
        if (sourceKey != null) {
            return sourceKey;
        }

        sourceKey = getMapValueIgnoreCase(row, association.getSourceKeyProperty());
        if (sourceKey != null) {
            return sourceKey;
        }

        return getMapValueIgnoreCase(row, association.getSourceKeyColumn());
    }

    private String sourceKeyAlias(ProjectionCollectionAssociation association) {
        return "__pd_source_" + association.getProjectionPropertyName();
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
    public <DTO> int updateProjection(DTO dto) {
        Objects.requireNonNull(dto, "Projection DTO must not be null");
        T entity = instantiateEntityFromProjectionDto(dto);
        return updateNonNull(entity);
    }

    @Override
    public <DTO> int updateAllProjection(Iterable<DTO> dtos) {
        if (dtos == null) {
            return 0;
        }
        return StreamSupport.stream(dtos.spliterator(), false)
                .map(this::updateProjection)
                .reduce(0, Integer::sum);
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
        TableInfo tableInfo = queryExecutor.requireTableInfo(entityType);
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

    private static String normalizeAssociationKey(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            try {
                return new BigDecimal(value.toString()).stripTrailingZeros().toPlainString();
            } catch (NumberFormatException ignored) {
                return value.toString();
            }
        }
        return value.toString();
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

    private <DTO> T instantiateEntityFromProjectionDto(DTO dto) {
        TableInfo tableInfo = getRequiredTableInfo();
        String keyProperty = tableInfo.getKeyProperty();

        BeanWrapperImpl dtoWrapper = new BeanWrapperImpl(dto);
        if (!dtoWrapper.isReadableProperty(keyProperty)) {
            throw new IllegalArgumentException("Projection DTO does not expose id property '" + keyProperty
                    + "': " + dto.getClass().getName());
        }
        Object keyValue = dtoWrapper.getPropertyValue(keyProperty);
        if (keyValue == null) {
            throw new IllegalArgumentException("Projection DTO id property must not be null: "
                    + dto.getClass().getName() + "." + keyProperty);
        }

        T entity = BeanUtils.instantiateClass(entityType);
        BeanWrapperImpl entityWrapper = new BeanWrapperImpl(entity);
        entityWrapper.setPropertyValue(keyProperty, keyValue);

        for (PropertyDescriptor descriptor : BeanUtils.getPropertyDescriptors(dto.getClass())) {
            String propertyName = descriptor.getName();
            if ("class".equals(propertyName) || keyProperty.equals(propertyName)) {
                continue;
            }
            if (!dtoWrapper.isReadableProperty(propertyName) || !entityWrapper.isWritableProperty(propertyName)) {
                continue;
            }
            Object value = dtoWrapper.getPropertyValue(propertyName);
            if (value != null) {
                entityWrapper.setPropertyValue(propertyName, value);
            }
        }
        return entity;
    }
}
