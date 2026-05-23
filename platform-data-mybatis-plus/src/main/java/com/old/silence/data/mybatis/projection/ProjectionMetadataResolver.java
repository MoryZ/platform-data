package com.old.silence.data.mybatis.projection;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.old.silence.data.commons.annotation.RelationalQueryProperty;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.TypeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Resolve projection metadata based on entity and projection types.
 */
public class ProjectionMetadataResolver {

    private static final String ROOT_ALIAS = "t0";
    private static final Set<String> TO_ONE_ASSOCIATION_ANNOTATIONS = Set.of(
        "jakarta.persistence.ManyToOne",
        "jakarta.persistence.OneToOne",
        "javax.persistence.ManyToOne",
        "javax.persistence.OneToOne"
    );
    private static final Set<String> TO_MANY_ASSOCIATION_ANNOTATIONS = Set.of(
        "jakarta.persistence.OneToMany",
        "jakarta.persistence.ManyToMany",
        "javax.persistence.OneToMany",
        "javax.persistence.ManyToMany"
    );
    private static final Set<String> RESERVED_SQL_ALIASES = Set.of(
        "user",
        "order",
        "group"
    );
    private static final Logger log = LoggerFactory.getLogger(ProjectionMetadataResolver.class);


    private final ConcurrentMap<String, ProjectionMetadata> cache = new ConcurrentHashMap<>();
    private volatile Configuration configuration;

    public ProjectionMetadataResolver() {
    }

    public ProjectionMetadataResolver(Configuration configuration) {
        this.configuration = configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public ProjectionMetadata resolve(Class<?> projectionType, Class<?> entityType) {
        return resolve(projectionType, entityType, List.of());
    }

    public ProjectionMetadata resolve(Class<?> projectionType, Class<?> entityType, List<String> selectedFields) {
        return resolve(projectionType, entityType, selectedFields, List.of());
    }

    public ProjectionMetadata resolve(Class<?> projectionType,
                                      Class<?> entityType,
                                      List<String> selectedFields,
                                      List<String> conditionAssociationHints) {
        List<String> normalizedSelectedFields = normalizeSelectedFields(selectedFields, projectionType);
        List<String> normalizedConditionHints = normalizeConditionAssociationHints(conditionAssociationHints);
        String selectionKey = buildSelectionKey(normalizedSelectedFields);
        String conditionKey = buildConditionHintKey(normalizedConditionHints);
        String cacheKey = entityType.getName() + "->" + projectionType.getName() + "#" + selectionKey + "@" + conditionKey;
        ProjectionMetadata cached = cache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        TableInfo tableInfo = getTableInfo(entityType);
        if (tableInfo == null) {
            throw new IllegalArgumentException("No TableInfo found for entity type: " + entityType.getName());
        }

        Map<String, TableFieldInfo> fieldMap = tableInfo.getFieldList().stream()
                .collect(Collectors.toMap(TableFieldInfo::getProperty, Function.identity()));
        List<AssociationMetadata> associations = resolveAssociations(entityType, tableInfo, fieldMap);

        List<ProjectionField> fields = new ArrayList<>();
        List<ProjectionCollectionAssociation> collectionAssociations = new ArrayList<>();
        Map<String, AssociationMetadata> usedAssociations = new LinkedHashMap<>();
        String keyProperty = tableInfo.getKeyProperty();
        String keyColumn = tableInfo.getKeyColumn();

        for (ProjectionProperty projectionProperty : resolveProjectionProperties(projectionType)) {
            String propertyName = projectionProperty.name();
            if (isNonPersistentProjectionProperty(projectionType, propertyName)) {
                continue;
            }
            String mappedPropertyPath = resolveProjectionPropertyPath(projectionType, propertyName);

            Field entityField = ReflectionUtils.findField(entityType, propertyName);

            if (Objects.equals(mappedPropertyPath, keyProperty)) {
                fields.add(new ProjectionField(propertyName, ROOT_ALIAS + "." + keyColumn, keyColumn, projectionProperty.type(),
                        resolveTypeHandler(entityType, propertyName, null), true));
                continue;
            }

            ProjectionCollectionAssociation collectionAssociation = resolveCollectionAssociation(projectionType,
                    propertyName,
                    projectionProperty.type(),
                    entityType,
                    tableInfo,
                    associations);
            if (collectionAssociation != null) {
                collectionAssociations.add(collectionAssociation);
                continue;
            }

            TableFieldInfo fieldInfo = fieldMap.get(mappedPropertyPath);
            if (fieldInfo != null) {
                // 如果 fieldInfo 存在且实体字段是关联字段，但投影属性类型不是接口，
                // 则跳过关联处理，直接使用 fieldMap（这是旧的行为，保持兼容性）
                if (entityField != null && isAssociationField(entityField) && !projectionProperty.type().isInterface()) {
                    continue;
                }
                if (entityField != null && isAssociationField(entityField)) {
                    // 投影属性类型是接口，应该走关联处理（跳过 fieldMap）
                    fieldInfo = null;
                } else {
                    // 普通字段，直接使用 fieldMap
                    if (Collection.class.isAssignableFrom(projectionProperty.type())) {
                        continue;
                    }
                    String columnName = resolveColumnName(entityType, mappedPropertyPath, fieldInfo);
                    Class<? extends TypeHandler<?>> typeHandler = resolveTypeHandler(entityType, mappedPropertyPath, fieldInfo);

                    fields.add(new ProjectionField(propertyName, ROOT_ALIAS + "." + columnName, columnName,
                            projectionProperty.type(), typeHandler, false));
                    continue;
                }
            }

            // fieldInfo == null: field is not a direct table column
            // Skip Collection fields that are not recognized associations
            // (e.g., @OneToMany fields, or non-persistent Collection fields)
            if (entityField != null && Collection.class.isAssignableFrom(entityField.getType())) {
                continue;
            }

            ResolvedAssociationField associationField = resolveAssociationField(propertyName,
                    mappedPropertyPath,
                    projectionProperty.type(),
                    entityType,
                    associations,
                    normalizedSelectedFields);
            if (associationField == null) {
                log.error("[ProjectionMetadataResolver] FAILED to resolve property: {}, associations available: {}", propertyName,
                        associations.stream().map(a -> a.propertyName() + "->" + a.targetEntityType().getSimpleName()).collect(Collectors.joining(", ")));
                throw new IllegalArgumentException("Projection property '" + propertyName + "' not found in entity "
                        + entityType.getName());
            }
            fields.add(associationField.field());
            // 添加关联的额外字段（如嵌套接口的 project_* 字段）
            for (ProjectionField af : associationField.additionalFields()) {
                if (!fields.stream().anyMatch(f -> f.getPropertyName().equals(af.getPropertyName()))) {
                    fields.add(af);
                }
            }
            for (AssociationMetadata association : associationField.associations()) {
                usedAssociations.putIfAbsent(association.alias(), association);
            }
        }

        applyConditionAssociationHints(associations, normalizedConditionHints, usedAssociations);

        List<ProjectionField> selectedProjectionFields = selectProjectionFields(fields, normalizedSelectedFields, projectionType);
        String fromClause = buildFromClause(tableInfo.getTableName(), usedAssociations.values());
        String statementKey = selectionKey + "@" + conditionKey;
        boolean collectionJoinInFrom = usedAssociations.values().stream().anyMatch(AssociationMetadata::collectionLike);
        
        List<ProjectionField> additionalFields = fields.stream().filter(f -> f.getPropertyName().contains("_")).toList();
        List<ProjectionField> allSelectFields = new ArrayList<>(selectedProjectionFields);
        for (ProjectionField af : additionalFields) {
            if (allSelectFields.stream().noneMatch(f -> f.getPropertyName().equals(af.getPropertyName()))) {
                allSelectFields.add(af);
            }
        }
        
        ProjectionMetadata metadata = new ProjectionMetadata(projectionType,
                entityType,
                tableInfo.getTableName(),
                fromClause,
                allSelectFields,
                collectionAssociations,
                statementKey,
                collectionJoinInFrom,
                additionalFields);
        cache.putIfAbsent(cacheKey, metadata);
        return metadata;
    }

    private List<ProjectionProperty> resolveProjectionProperties(Class<?> projectionType) {
        if (projectionType.isRecord()) {
            RecordComponent[] components = projectionType.getRecordComponents();
            List<ProjectionProperty> properties = new ArrayList<>(components.length);
            for (RecordComponent component : components) {
                properties.add(new ProjectionProperty(component.getName(), component.getType()));
            }
            return properties;
        }

        List<ProjectionProperty> properties = new ArrayList<>();
        for (java.beans.PropertyDescriptor descriptor : org.springframework.beans.BeanUtils.getPropertyDescriptors(projectionType)) {
            if ("class".equals(descriptor.getName())) {
                continue;
            }
            properties.add(new ProjectionProperty(descriptor.getName(), descriptor.getPropertyType()));
        }
        return properties;
    }

    private ProjectionCollectionAssociation resolveCollectionAssociation(Class<?> projectionType,
                                                                        String projectionPropertyName,
                                                                        Class<?> projectionPropertyType,
                                                                        Class<?> sourceEntityType,
                                                                        TableInfo sourceTableInfo,
                                                                        List<AssociationMetadata> associations) {
        if (!Collection.class.isAssignableFrom(projectionPropertyType)) {
            return null;
        }

        AssociationMetadata association = associations.stream()
                .filter(it -> it.collectionLike() && Objects.equals(it.propertyName(), projectionPropertyName))
                .findFirst()
                .orElse(null);
        if (association == null) {
            return null;
        }

        Class<?> elementType = resolveCollectionElementType(projectionType, projectionPropertyName);
        if (elementType == null) {
            throw new IllegalArgumentException("Cannot resolve element type for projection collection property '"
                    + projectionPropertyName + "' in " + projectionType.getName());
        }

        String sourceReferencedProperty = resolveSourceReferencedProperty(sourceTableInfo, association.referencedColumn());

        // ManyToMany via @JoinTable: secondary loading through the join table
        if (association.joinTable() != null) {
            return new ProjectionCollectionAssociation(projectionPropertyName,
                    sourceEntityType,
                    sourceTableInfo.getKeyProperty(),
                    sourceTableInfo.getKeyColumn(),
                    association.targetEntityType(),
                    null,
                    null,
                    elementType,
                    association.joinTable().tableName(),
                    association.joinTable().sourceJoinColumn(),
                    association.joinTable().targetJoinColumn());
        }

        // OneToMany: FK is on target side
        String targetFkProperty = resolveTargetPropertyByColumn(association.targetFieldMap(), association.joinColumn());
        if (!StringUtils.hasText(targetFkProperty)) {
            targetFkProperty = resolveTargetPropertyByJoinColumnAnnotation(association.targetEntityType(), association.joinColumn());
        }
        if (!StringUtils.hasText(targetFkProperty)) {
            targetFkProperty = inferTargetPropertyByColumnName(association.joinColumn());
        }
        if (!StringUtils.hasText(targetFkProperty)) {
            throw new IllegalArgumentException("Cannot resolve foreign key property for collection association '"
                    + projectionPropertyName + "' by column " + association.joinColumn());
        }

        return new ProjectionCollectionAssociation(projectionPropertyName,
                sourceEntityType,
                sourceReferencedProperty,
                association.referencedColumn(),
                association.targetEntityType(),
                targetFkProperty,
                association.joinColumn(),
                elementType,
                null, null, null);
    }

    private Class<?> resolveCollectionElementType(Field field) {
        Type genericType = field.getGenericType();
        if (!(genericType instanceof ParameterizedType parameterizedType)) {
            return null;
        }
        if (parameterizedType.getActualTypeArguments().length != 1) {
            return null;
        }
        Type type = parameterizedType.getActualTypeArguments()[0];
        return type instanceof Class<?> clazz ? clazz : null;
    }

    private Class<?> resolveCollectionElementType(Class<?> projectionType, String projectionPropertyName) {
        Field field = ReflectionUtils.findField(projectionType, projectionPropertyName);
        if (field != null) {
            return resolveCollectionElementType(field);
        }

        Method getter = resolveProjectionGetter(projectionType, projectionPropertyName);
        if (getter == null) {
            return null;
        }

        Type genericType = getter.getGenericReturnType();
        if (!(genericType instanceof ParameterizedType parameterizedType)) {
            return null;
        }
        if (parameterizedType.getActualTypeArguments().length != 1) {
            return null;
        }
        Type type = parameterizedType.getActualTypeArguments()[0];
        return type instanceof Class<?> clazz ? clazz : null;
    }

    private Method resolveProjectionGetter(Class<?> projectionType, String projectionPropertyName) {
        String capitalized = StringUtils.capitalize(projectionPropertyName);
        Method getter = ReflectionUtils.findMethod(projectionType, "get" + capitalized);
        if (getter != null) {
            return getter;
        }
        return ReflectionUtils.findMethod(projectionType, "is" + capitalized);
    }

    private String resolveSourceReferencedProperty(TableInfo sourceTableInfo, String referencedColumn) {
        if (Objects.equals(sourceTableInfo.getKeyColumn(), referencedColumn)) {
            return sourceTableInfo.getKeyProperty();
        }
        for (TableFieldInfo fieldInfo : sourceTableInfo.getFieldList()) {
            if (Objects.equals(fieldInfo.getColumn(), referencedColumn)) {
                return fieldInfo.getProperty();
            }
        }
        throw new IllegalArgumentException("Referenced column '" + referencedColumn
                + "' cannot be resolved to source entity property");
    }

    private String resolveTargetPropertyByColumn(Map<String, AssociationColumnMetadata> targetFieldMap, String columnName) {
        for (AssociationColumnMetadata columnMetadata : targetFieldMap.values()) {
            if (Objects.equals(columnMetadata.columnName(), columnName)) {
                return columnMetadata.propertyName();
            }
        }
        return null;
    }

    /**
     * Try to resolve the foreign key property by looking at @JoinColumn annotations on the target entity.
     * This handles cases where the FK field is a ManyToOne/OneToOne association (not stored in TableFieldInfo).
     */
    private String resolveTargetPropertyByJoinColumnAnnotation(Class<?> targetEntityType, String columnName) {
        for (Field field : targetEntityType.getDeclaredFields()) {
            String joinColumnName = getJoinColumnAttribute(field, "name");
            if (Objects.equals(joinColumnName, columnName)) {
                return field.getName();
            }
        }
        return null;
    }

    /**
     * Fallback: infer property name from column name by removing common suffixes like "_id".
     * e.g., "project_id" -> "projectId", "owner_id" -> "ownerId"
     */
    private String inferTargetPropertyByColumnName(String columnName) {
        if (!StringUtils.hasText(columnName)) {
            return null;
        }
        String lower = columnName.toLowerCase();
        if (lower.endsWith("_id")) {
            String base = columnName.substring(0, columnName.length() - 3);
            return toCamelCase(base);
        }
        return toCamelCase(columnName);
    }

    private String toCamelCase(String snakeCase) {
        if (!StringUtils.hasText(snakeCase)) {
            return snakeCase;
        }
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = false;
        for (int i = 0; i < snakeCase.length(); i++) {
            char c = snakeCase.charAt(i);
            if (c == '_') {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(Character.toLowerCase(c));
            }
        }
        return result.toString();
    }

    private String buildFromClause(String tableName, Collection<AssociationMetadata> associations) {
        StringBuilder fromClause = new StringBuilder(tableName).append(" ").append(ROOT_ALIAS);
        if (associations.isEmpty()) {
            return fromClause.toString();
        }
        for (AssociationMetadata association : associations) {
            String leftColumn;
            String rightColumn;
            if (association.collectionLike()) {
                // OneToMany join direction: source PK = target FK
                leftColumn = association.referencedColumn();
                rightColumn = association.joinColumn();
            } else {
                // ManyToOne/OneToOne join direction: source FK = target PK
                leftColumn = association.joinColumn();
                rightColumn = association.referencedColumn();
            }
            fromClause.append(" LEFT JOIN ")
                    .append(association.targetTableName())
                    .append(" ")
                    .append(association.alias())
                    .append(" ON ")
                    .append(association.sourceAlias())
                    .append(".")
                    .append(leftColumn)
                    .append(" = ")
                    .append(association.alias())
                    .append(".")
                    .append(rightColumn);
        }
        return fromClause.toString();
    }

    private boolean isNonPersistentProjectionProperty(Class<?> projectionType, String propertyName) {
        Field field = ReflectionUtils.findField(projectionType, propertyName);
        if (field == null) {
            return false;
        }
        TableField tableField = field.getAnnotation(TableField.class);
        return tableField != null && !tableField.exist();
    }

    private ResolvedAssociationField resolveAssociationField(String projectionPropertyName,
                                                             String mappedPropertyPath,
                                                             Class<?> projectionPropertyType,
                                                             Class<?> rootEntityType,
                                                             List<AssociationMetadata> associations,
                                                             List<String> selectedFields) {
        if (StringUtils.hasText(mappedPropertyPath) && mappedPropertyPath.contains(".")) {
            ResolvedAssociationField nestedField = resolveNestedAssociationField(projectionPropertyName,
                    mappedPropertyPath,
                    projectionPropertyType,
                    rootEntityType);
            if (nestedField != null) {
                return nestedField;
            }
        }

        for (AssociationMetadata association : associations) {
            String targetPropertyName = resolveAssociatedPropertyName(projectionPropertyName,
                    mappedPropertyPath,
                    association.propertyName());

            if (!StringUtils.hasText(targetPropertyName)) {
                // 当投影属性名正好等于关联属性名时（如 "job"），resolveAssociatedPropertyName 会返回 null
                // 此时应该使用关联实体的主键作为默认选择
                if (projectionPropertyName.equals(association.propertyName())) {
                    // 如果投影属性类型是接口（如 JobView），则选择关联表的所有字段
                    if (projectionPropertyType.isInterface()) {
                        List<ProjectionField> fields = resolveInterfaceProjectionFields(projectionPropertyName,
                                projectionPropertyType, association);
                        if (!fields.isEmpty()) {
                            return new ResolvedAssociationField(fields.get(0), List.of(association), fields);
                        }
                    }
                    targetPropertyName = association.targetKeyProperty();
                } else {
                    continue;
                }
            }
            if (association.collectionLike()) {
                throw new IllegalArgumentException("Collection association property '" + association.propertyName()
                        + "' is not supported in projection. Use dedicated secondary loading for one-to-many fields.");
            }

            String keyProperty = association.targetKeyProperty();
            String keyColumn = association.targetKeyColumn();
            if (Objects.equals(targetPropertyName, keyProperty)) {
                ProjectionField field = new ProjectionField(projectionPropertyName,
                        association.alias() + "." + keyColumn + " AS " + projectionPropertyName,
                        projectionPropertyName,
                        projectionPropertyType,
                        resolveTypeHandler(association.targetEntityType(), targetPropertyName, null),
                        false);
                return new ResolvedAssociationField(field, List.of(association));
            }

                AssociationColumnMetadata targetFieldInfo = association.targetFieldMap().get(targetPropertyName);
                if (targetFieldInfo == null) {
                continue;
            }
                String targetColumn = targetFieldInfo.columnName();
                Class<? extends TypeHandler<?>> typeHandler = targetFieldInfo.typeHandler();
            ProjectionField field = new ProjectionField(projectionPropertyName,
                    association.alias() + "." + targetColumn + " AS " + projectionPropertyName,
                    projectionPropertyName,
                    projectionPropertyType,
                    typeHandler,
                    false);
                return new ResolvedAssociationField(field, List.of(association));
        }

        if (selectedFields.stream().anyMatch(field -> field.equals(projectionPropertyName)
                || field.endsWith("." + projectionPropertyName))) {
            throw new IllegalArgumentException("Selected association field '" + projectionPropertyName
                    + "' cannot be resolved. Ensure projection property uses <association><Property> naming, e.g. departmentName.");
        }
        return null;
    }

    private ResolvedAssociationField resolveNestedAssociationField(String projectionPropertyName,
                                                                   String mappedPropertyPath,
                                                                   Class<?> projectionPropertyType,
                                                                   Class<?> rootEntityType) {
        String[] parts = mappedPropertyPath.split("\\.");
        if (parts.length < 2) {
            return null;
        }

        List<AssociationMetadata> chain = new ArrayList<>();
        Class<?> currentType = rootEntityType;
        String sourceAlias = ROOT_ALIAS;
        StringBuilder pathBuilder = new StringBuilder();

        for (int i = 0; i < parts.length - 1; i++) {
            String associationProperty = parts[i];
            Field associationField = ReflectionUtils.findField(currentType, associationProperty);
            if (associationField == null || !isAssociationField(associationField)) {
                return null;
            }
            if (Collection.class.isAssignableFrom(associationField.getType())) {
                return null;
            }

            Class<?> targetType = resolveAssociationTargetType(associationField);
            if (targetType == null || targetType == Object.class) {
                return null;
            }

            TableInfo sourceTableInfo = getTableInfo(currentType);
            Map<String, TableFieldInfo> sourceFieldMap = sourceTableInfo != null
                    ? sourceTableInfo.getFieldList().stream().collect(Collectors.toMap(TableFieldInfo::getProperty, Function.identity()))
                    : Map.of();

            TableInfo targetTableInfo = getTableInfo(targetType);
            String targetTableName = targetTableInfo != null
                    ? targetTableInfo.getTableName()
                    : resolveTableNameByAnnotation(targetType);
            if (!StringUtils.hasText(targetTableName)) {
                return null;
            }

            String targetKeyProperty = targetTableInfo != null
                    ? targetTableInfo.getKeyProperty()
                    : resolveKeyPropertyByReflection(targetType);
            String targetKeyColumn = targetTableInfo != null
                    ? targetTableInfo.getKeyColumn()
                    : resolveColumnByReflection(targetType, targetKeyProperty);
            if (!StringUtils.hasText(targetKeyProperty) || !StringUtils.hasText(targetKeyColumn)) {
                return null;
            }

            Map<String, AssociationColumnMetadata> targetFieldMap = buildTargetFieldMap(targetType,
                    targetTableInfo,
                    targetKeyProperty,
                    targetKeyColumn);

            String joinColumn = resolveJoinColumnName(currentType, associationField, sourceFieldMap);
            if (!StringUtils.hasText(joinColumn)) {
                return null;
            }

            String referencedColumn = resolveReferencedColumnName(associationField, targetKeyColumn, sourceTableInfo);
            if (pathBuilder.length() > 0) {
                pathBuilder.append('.');
            }
            pathBuilder.append(associationProperty);
            String alias = sanitizeAssociationAlias(toSnakeCase(pathBuilder.toString()).replace('.', '_'));

            AssociationMetadata association = new AssociationMetadata(pathBuilder.toString(),
                    sourceAlias,
                    targetType,
                    targetTableName,
                    alias,
                    joinColumn,
                    referencedColumn,
                    targetKeyProperty,
                    targetKeyColumn,
                    targetFieldMap,
                    false,
                    null);
            chain.add(association);

            currentType = targetType;
            sourceAlias = alias;
        }

        String leafProperty = parts[parts.length - 1];
        AssociationMetadata terminal = chain.get(chain.size() - 1);
        if (Objects.equals(leafProperty, terminal.targetKeyProperty())) {
            ProjectionField field = new ProjectionField(projectionPropertyName,
                    terminal.alias() + "." + terminal.targetKeyColumn() + " AS " + projectionPropertyName,
                    projectionPropertyName,
                    projectionPropertyType,
                    resolveTypeHandler(terminal.targetEntityType(), leafProperty, null),
                    false);
            return new ResolvedAssociationField(field, chain);
        }

        AssociationColumnMetadata targetField = terminal.targetFieldMap().get(leafProperty);
        if (targetField == null) {
            return null;
        }
        ProjectionField field = new ProjectionField(projectionPropertyName,
                terminal.alias() + "." + targetField.columnName() + " AS " + projectionPropertyName,
                projectionPropertyName,
                projectionPropertyType,
                targetField.typeHandler(),
                false);
        return new ResolvedAssociationField(field, chain);
    }

    private String resolveAssociatedPropertyName(String projectionPropertyName,
                                                 String mappedPropertyPath,
                                                 String associationPropertyName) {
        if (StringUtils.hasText(mappedPropertyPath) && mappedPropertyPath.contains(".")) {
            String relationPrefix = associationPropertyName + ".";
            if (mappedPropertyPath.startsWith(relationPrefix)) {
                String tail = mappedPropertyPath.substring(relationPrefix.length());
                return StringUtils.hasText(tail) ? tail : null;
            }
        }

        if (projectionPropertyName.startsWith(associationPropertyName + "_")) {
            String candidate = projectionPropertyName.substring(associationPropertyName.length() + 1);
            return StringUtils.hasText(candidate) ? candidate : null;
        }
        if (!projectionPropertyName.startsWith(associationPropertyName)
                || projectionPropertyName.length() <= associationPropertyName.length()) {
            return null;
        }
        String tail = projectionPropertyName.substring(associationPropertyName.length());
        if (!StringUtils.hasText(tail)) {
            return null;
        }
        return Character.toLowerCase(tail.charAt(0)) + tail.substring(1);
    }

    private List<AssociationMetadata> resolveAssociations(Class<?> entityType,
                                                         TableInfo tableInfo,
                                                         Map<String, TableFieldInfo> fieldMap) {
        List<AssociationMetadata> associations = new ArrayList<>();
        Field[] fields = entityType.getDeclaredFields();
        int aliasIndex = 1;
        Set<String> usedAliases = new LinkedHashSet<>();
        for (Field field : fields) {
            if (!isAssociationField(field)) {
                continue;
            }

            Class<?> targetType = resolveAssociationTargetType(field);
            if (targetType == null || targetType == Object.class) {
                continue;
            }
            TableInfo targetTableInfo = getTableInfo(targetType);
            String targetTableName = targetTableInfo != null
                    ? targetTableInfo.getTableName()
                    : resolveTableNameByAnnotation(targetType);
            if (!StringUtils.hasText(targetTableName)) {
                continue;
            }

            String targetKeyProperty = targetTableInfo != null
                    ? targetTableInfo.getKeyProperty()
                    : resolveKeyPropertyByReflection(targetType);
            String targetKeyColumn = targetTableInfo != null
                    ? targetTableInfo.getKeyColumn()
                    : resolveColumnByReflection(targetType, targetKeyProperty);
            boolean collectionLike = Collection.class.isAssignableFrom(field.getType());
            if ((!StringUtils.hasText(targetKeyProperty) || !StringUtils.hasText(targetKeyColumn)) && !collectionLike) {
                continue;
            }

            Map<String, AssociationColumnMetadata> targetFieldMap = buildTargetFieldMap(targetType, targetTableInfo,
                    targetKeyProperty, targetKeyColumn);

            String joinColumn = resolveJoinColumnName(entityType, field, fieldMap);
            if (!StringUtils.hasText(joinColumn) && collectionLike) {
                joinColumn = resolveCollectionJoinColumnByMappedBy(field, targetType, targetFieldMap);
            }
            if (!StringUtils.hasText(joinColumn) && collectionLike) {
                joinColumn = resolveCollectionJoinColumnByConvention(entityType, targetFieldMap);
            }
            JoinTableMetadata joinTable = null;
            if (!StringUtils.hasText(joinColumn) && collectionLike) {
                joinTable = resolveJoinTableMetadata(field, entityType);
                if (joinTable != null) {
                    joinColumn = tableInfo.getKeyColumn();
                }
            }
            if (!StringUtils.hasText(joinColumn)) {
                continue;
            }
            String referencedColumn = resolveReferencedColumnName(field, targetKeyColumn, tableInfo);

            String alias = resolveAssociationAlias(field.getName(), aliasIndex, usedAliases);
            associations.add(new AssociationMetadata(field.getName(),
                    ROOT_ALIAS,
                    targetType,
                    targetTableName,
                    alias,
                    joinColumn,
                    referencedColumn,
                    targetKeyProperty,
                    targetKeyColumn,
                    targetFieldMap,
                    collectionLike,
                    joinTable));
            aliasIndex++;
        }
        return associations;
    }

    private String resolveAssociationAlias(String associationPropertyName,
                                           int aliasIndex,
                                           Set<String> usedAliases) {
        String normalized = sanitizeAssociationAlias(toSnakeCase(associationPropertyName));
        String candidate = StringUtils.hasText(normalized) ? normalized : "t" + aliasIndex;
        if ("t0".equalsIgnoreCase(candidate) || !usedAliases.add(candidate)) {
            candidate = candidate + "_" + aliasIndex;
            usedAliases.add(candidate);
        }
        return candidate;
    }

    private String sanitizeAssociationAlias(String candidate) {
        if (!StringUtils.hasText(candidate)) {
            return candidate;
        }
        return RESERVED_SQL_ALIASES.contains(candidate.toLowerCase()) ? candidate + "_assoc" : candidate;
    }

    private boolean isAssociationField(Field field) {
        if (Collection.class.isAssignableFrom(field.getType()) && hasAnyAnnotation(field, TO_MANY_ASSOCIATION_ANNOTATIONS)) {
            return true;
        }
        if (Collection.class.isAssignableFrom(field.getType())) {
            Class<?> targetType = resolveAssociationTargetType(field);
            if (targetType != null && isEntityLikeType(targetType)) {
                return true;
            }
        }
        if (hasAnyAnnotation(field, TO_ONE_ASSOCIATION_ANNOTATIONS)) {
            return true;
        }
        if (!Collection.class.isAssignableFrom(field.getType()) && isEntityLikeType(field.getType())) {
            return true;
        }
        TableField tableField = field.getAnnotation(TableField.class);
        return tableField != null && !tableField.exist();
    }

    private boolean isEntityLikeType(Class<?> type) {
        if (type == null
                || type.isPrimitive()
                || type.isArray()
                || type.isEnum()
                || type.getName().startsWith("java.")) {
            return false;
        }
        if (type.getAnnotation(TableName.class) != null) {
            return true;
        }
        return getTableInfo(type) != null;
    }

    private boolean hasAnyAnnotation(AnnotatedElement element, Set<String> annotationTypeNames) {
        return Arrays.stream(element.getAnnotations())
                .map(annotation -> annotation.annotationType().getName())
                .anyMatch(annotationTypeNames::contains);
    }

    private Class<?> resolveAssociationTargetType(Field field) {
        if (Collection.class.isAssignableFrom(field.getType())) {
            if (field.getGenericType() instanceof java.lang.reflect.ParameterizedType parameterizedType
                    && parameterizedType.getActualTypeArguments().length == 1) {
                java.lang.reflect.Type actualType = parameterizedType.getActualTypeArguments()[0];
                if (actualType instanceof Class<?> targetClass) {
                    return targetClass;
                }
            }
            return null;
        }
        return field.getType();
    }

    private String resolveJoinColumnName(Class<?> entityType, Field associationField, Map<String, TableFieldInfo> fieldMap) {
        String explicitJoinColumn = getJoinColumnAttribute(associationField, "name");
        if (StringUtils.hasText(explicitJoinColumn)) {
            return explicitJoinColumn;
        }

        String inferredFkProperty = associationField.getName() + "Id";
        TableFieldInfo fkFieldInfo = fieldMap.get(inferredFkProperty);
        if (fkFieldInfo != null) {
            return resolveColumnName(entityType, inferredFkProperty, fkFieldInfo);
        }
        return null;
    }

    private String resolveReferencedColumnName(Field associationField,
                                               String targetKeyColumn,
                                               TableInfo sourceTableInfo) {
        String explicitReferencedColumn = getJoinColumnAttribute(associationField, "referencedColumnName");
        if (StringUtils.hasText(explicitReferencedColumn)) {
            return explicitReferencedColumn;
        }
        if (Collection.class.isAssignableFrom(associationField.getType())) {
            return sourceTableInfo.getKeyColumn();
        }
        return targetKeyColumn;
    }

    private JoinTableMetadata resolveJoinTableMetadata(Field field, Class<?> sourceEntityType) {
        for (java.lang.annotation.Annotation annotation : field.getAnnotations()) {
            String annotationName = annotation.annotationType().getName();
            if (!"jakarta.persistence.JoinTable".equals(annotationName)
                    && !"javax.persistence.JoinTable".equals(annotationName)) {
                continue;
            }
            try {
                String tableName = (String) annotation.annotationType().getMethod("name").invoke(annotation);
                if (!StringUtils.hasText(tableName)) {
                    continue;
                }
                Object[] joinCols = (Object[]) annotation.annotationType().getMethod("joinColumns").invoke(annotation);
                String sourceJoinCol = null;
                if (joinCols != null && joinCols.length > 0) {
                    sourceJoinCol = (String) joinCols[0].getClass().getMethod("name").invoke(joinCols[0]);
                }
                if (!StringUtils.hasText(sourceJoinCol)) {
                    sourceJoinCol = toSnakeCase(sourceEntityType.getSimpleName()) + "_id";
                }
                Object[] invJoinCols = (Object[]) annotation.annotationType().getMethod("inverseJoinColumns").invoke(annotation);
                String targetJoinCol = null;
                if (invJoinCols != null && invJoinCols.length > 0) {
                    targetJoinCol = (String) invJoinCols[0].getClass().getMethod("name").invoke(invJoinCols[0]);
                }
                if (!StringUtils.hasText(targetJoinCol)) {
                    Class<?> targetType = resolveAssociationTargetType(field);
                    if (targetType != null) {
                        targetJoinCol = toSnakeCase(targetType.getSimpleName()) + "_id";
                    }
                }
                if (!StringUtils.hasText(targetJoinCol)) {
                    continue;
                }
                return new JoinTableMetadata(tableName, sourceJoinCol, targetJoinCol);
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return null;
    }

    private String resolveCollectionJoinColumnByConvention(Class<?> sourceEntityType,
                                                           Map<String, AssociationColumnMetadata> targetFieldMap) {
        String sourceName = sourceEntityType.getSimpleName();
        if (!StringUtils.hasText(sourceName)) {
            return null;
        }
        List<String> candidates = new ArrayList<>();
        candidates.add(Character.toLowerCase(sourceName.charAt(0)) + sourceName.substring(1) + "Id");
        if (sourceName.startsWith("Test") && sourceName.length() > 4) {
            String trimmed = sourceName.substring(4);
            candidates.add(Character.toLowerCase(trimmed.charAt(0)) + trimmed.substring(1) + "Id");
        }

        for (String candidate : candidates) {
            AssociationColumnMetadata targetColumn = targetFieldMap.get(candidate);
            if (targetColumn != null) {
                return targetColumn.columnName();
            }
        }
        return null;
    }

    private String resolveCollectionJoinColumnByMappedBy(Field associationField,
                                                         Class<?> targetType,
                                                         Map<String, AssociationColumnMetadata> targetFieldMap) {
        String mappedBy = getAssociationAttribute(associationField, "mappedBy");
        if (!StringUtils.hasText(mappedBy)) {
            return null;
        }

        Field mappedField = ReflectionUtils.findField(targetType, mappedBy);
        if (mappedField != null) {
            String explicitJoinColumn = getJoinColumnAttribute(mappedField, "name");
            if (StringUtils.hasText(explicitJoinColumn)) {
                return explicitJoinColumn;
            }
        }

        AssociationColumnMetadata inferred = targetFieldMap.get(mappedBy + "Id");
        if (inferred != null) {
            return inferred.columnName();
        }

        // Fallback: if targetFieldMap is empty or missing the mappedBy+Id field,
        // try to resolve join column directly from the mappedBy field's annotations
        if (mappedField != null) {
            String joinColumnFromAnnotation = getJoinColumnAttribute(mappedField, "name");
            if (StringUtils.hasText(joinColumnFromAnnotation)) {
                return joinColumnFromAnnotation;
            }

            // Try to infer column name from field name (e.g., "user" -> "user_id")
            String inferredColumn = toSnakeCase(mappedBy) + "_id";
            if (hasFieldWithColumn(targetType, inferredColumn)) {
                return inferredColumn;
            }

            // Try simple convention: field name + "_id"
            String simpleInferred = mappedField.getName() + "Id";
            if (hasFieldWithColumn(targetType, toSnakeCase(simpleInferred))) {
                return toSnakeCase(simpleInferred);
            }
        }

        return null;
    }

    private boolean hasFieldWithColumn(Class<?> targetType, String columnName) {
        TableInfo tableInfo = getTableInfo(targetType);
        if (tableInfo != null) {
            return tableInfo.getFieldList().stream()
                    .anyMatch(f -> f.getColumn().equalsIgnoreCase(columnName));
        }
        // Fallback: check by reflection
        for (Field field : targetType.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers())) {
                continue;
            }
            TableField tableField = field.getAnnotation(TableField.class);
            if (tableField != null && StringUtils.hasText(tableField.value())) {
                if (tableField.value().equalsIgnoreCase(columnName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private String getAssociationAttribute(Field field, String attributeName) {
        for (java.lang.annotation.Annotation annotation : field.getAnnotations()) {
            String annotationName = annotation.annotationType().getName();
            if (!TO_MANY_ASSOCIATION_ANNOTATIONS.contains(annotationName)
                    && !TO_ONE_ASSOCIATION_ANNOTATIONS.contains(annotationName)) {
                continue;
            }
            try {
                Method method = annotation.annotationType().getMethod(attributeName);
                Object value = method.invoke(annotation);
                if (value instanceof String text && StringUtils.hasText(text)) {
                    return text;
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return null;
    }

    private Map<String, AssociationColumnMetadata> buildTargetFieldMap(Class<?> targetType,
                                                                       TableInfo targetTableInfo,
                                                                       String keyProperty,
                                                                       String keyColumn) {
        Map<String, AssociationColumnMetadata> targetFieldMap = new HashMap<>();
        if (StringUtils.hasText(keyProperty) && StringUtils.hasText(keyColumn)) {
            targetFieldMap.put(keyProperty, new AssociationColumnMetadata(keyProperty,
                keyColumn,
                resolveTypeHandler(targetType, keyProperty, null)));
        }

        if (targetTableInfo != null) {
            for (TableFieldInfo fieldInfo : targetTableInfo.getFieldList()) {
                String propertyName = fieldInfo.getProperty();
                targetFieldMap.put(propertyName,
                        new AssociationColumnMetadata(propertyName,
                                resolveColumnName(targetType, propertyName, fieldInfo),
                                resolveTypeHandler(targetType, propertyName, fieldInfo)));
            }
            return targetFieldMap;
        }

        for (Field targetField : targetType.getDeclaredFields()) {
            if (Modifier.isStatic(targetField.getModifiers()) || Modifier.isTransient(targetField.getModifiers())) {
                continue;
            }
            if (Objects.equals(targetField.getName(), keyProperty) || isAssociationField(targetField)) {
                continue;
            }
            String columnName = resolveColumnByReflection(targetType, targetField.getName());
            targetFieldMap.put(targetField.getName(),
                    new AssociationColumnMetadata(targetField.getName(),
                            columnName,
                            resolveTypeHandler(targetType, targetField.getName(), null)));
        }
        return targetFieldMap;
    }

    private String resolveTableNameByAnnotation(Class<?> entityType) {
        TableName tableName = entityType.getAnnotation(TableName.class);
        if (tableName != null && StringUtils.hasText(tableName.value())) {
            return tableName.value();
        }
        return null;
    }

    private String resolveKeyPropertyByReflection(Class<?> entityType) {
        for (Field field : entityType.getDeclaredFields()) {
            if (field.getAnnotation(TableId.class) != null) {
                return field.getName();
            }
        }
        return ReflectionUtils.findField(entityType, "id") != null ? "id" : null;
    }

    private String resolveColumnByReflection(Class<?> entityType, String propertyName) {
        Field field = ReflectionUtils.findField(entityType, propertyName);
        if (field != null) {
            String explicitColumnName = resolveExplicitColumnName(field);
            if (StringUtils.hasText(explicitColumnName)) {
                return explicitColumnName;
            }
        }
        return toSnakeCase(propertyName);
    }

    private String toSnakeCase(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                sb.append('_');
            }
            sb.append(Character.toLowerCase(c));
        }
        return sb.toString();
    }

    private String getJoinColumnAttribute(Field field, String attributeName) {
        for (java.lang.annotation.Annotation annotation : field.getAnnotations()) {
            String annotationName = annotation.annotationType().getName();
            if (!"jakarta.persistence.JoinColumn".equals(annotationName)
                    && !"javax.persistence.JoinColumn".equals(annotationName)) {
                continue;
            }
            try {
                Method method = annotation.annotationType().getMethod(attributeName);
                Object value = method.invoke(annotation);
                if (value instanceof String text && StringUtils.hasText(text)) {
                    return text;
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return null;
    }

    private List<String> normalizeSelectedFields(List<String> selectedFields, Class<?> projectionType) {
        if (selectedFields == null || selectedFields.isEmpty()) {
            return List.of();
        }
        return selectedFields.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(field -> !field.isEmpty())
                .map(field -> normalizeSelectedField(field, projectionType))
                .collect(Collectors.toList());
    }

    private String normalizeSelectedField(String field, Class<?> projectionType) {
        if (ReflectionUtils.findField(projectionType, field) != null) {
            return field;
        }

        for (Field projectionField : projectionType.getDeclaredFields()) {
            RelationalQueryProperty relational = projectionField.getAnnotation(RelationalQueryProperty.class);
            if (relational == null || !StringUtils.hasText(relational.name())) {
                continue;
            }
            if (Objects.equals(field, projectionField.getName()) || Objects.equals(field, relational.name())) {
                return projectionField.getName();
            }
        }

        return field;
    }

    private String resolveProjectionPropertyPath(Class<?> projectionType, String projectionPropertyName) {
        Field projectionField = ReflectionUtils.findField(projectionType, projectionPropertyName);
        if (projectionField == null) {
            return projectionPropertyName;
        }

        RelationalQueryProperty relational = projectionField.getAnnotation(RelationalQueryProperty.class);
        if (relational != null && StringUtils.hasText(relational.name())) {
            return relational.name();
        }

        return projectionPropertyName;
    }

    private String buildSelectionKey(List<String> selectedFields) {
        if (selectedFields.isEmpty()) {
            return "ALL";
        }
        return String.join(",", selectedFields);
    }

    private List<String> normalizeConditionAssociationHints(List<String> hints) {
        if (hints == null || hints.isEmpty()) {
            return List.of();
        }
        return hints.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(it -> !it.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .collect(Collectors.toList());
    }

    private String buildConditionHintKey(List<String> conditionHints) {
        if (conditionHints.isEmpty()) {
            return "NONE";
        }
        return String.join(",", conditionHints);
    }

    private void applyConditionAssociationHints(List<AssociationMetadata> associations,
                                                List<String> conditionHints,
                                                Map<String, AssociationMetadata> usedAssociations) {
        if (conditionHints.isEmpty() || associations.isEmpty()) {
            return;
        }

        for (String hint : conditionHints) {
            String normalizedHint = extractHintHead(hint);
            for (AssociationMetadata association : associations) {
                if (Objects.equals(association.alias(), normalizedHint)
                        || Objects.equals(association.propertyName(), normalizedHint)) {
                    usedAssociations.putIfAbsent(association.alias(), association);
                }
            }
        }
    }

    private String extractHintHead(String hint) {
        int index = hint.indexOf('.');
        return index >= 0 ? hint.substring(0, index) : hint;
    }

    private List<ProjectionField> selectProjectionFields(List<ProjectionField> allFields,
                                                         List<String> selectedFields,
                                                         Class<?> projectionType) {
        if (selectedFields.isEmpty()) {
            return allFields;
        }

        Set<String> requestedProperties = selectedFields.stream()
                .flatMap(field -> Arrays.stream(new String[]{field, extractLeafProperty(field)}))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<ProjectionField> result = allFields.stream()
                .filter(field -> requestedProperties.contains(field.getPropertyName())
                        || requestedProperties.contains(field.getColumnName()))
                .collect(Collectors.toCollection(ArrayList::new));

        if (result.isEmpty()) {
            throw new IllegalArgumentException("None of selected fields " + selectedFields
                    + " can be resolved for projection " + projectionType.getName());
        }

        for (String selectedField : selectedFields) {
            String leaf = extractLeafProperty(selectedField);
            boolean matched = result.stream().anyMatch(field -> Objects.equals(field.getPropertyName(), selectedField)
                    || Objects.equals(field.getPropertyName(), leaf)
                    || Objects.equals(field.getColumnName(), selectedField)
                    || Objects.equals(field.getColumnName(), leaf));
            if (!matched) {
                throw new IllegalArgumentException("Selected field '" + selectedField
                        + "' cannot be resolved for projection " + projectionType.getName());
            }
        }

        return result;
    }

    private String extractLeafProperty(String fieldPath) {
        int index = fieldPath.lastIndexOf('.');
        return index >= 0 ? fieldPath.substring(index + 1) : fieldPath;
    }

    /**
     * Resolve order column name safely based on entity mapping.
     * Accepts property name or actual column name; rejects unknown names.
     */
    public String resolveOrderColumn(Class<?> entityType, String orderColumn) {
        TableInfo tableInfo = getTableInfo(entityType);
        if (tableInfo == null) {
            throw new IllegalArgumentException("No TableInfo found for entity type: " + entityType.getName());
        }

        String keyProperty = tableInfo.getKeyProperty();
        String keyColumn = tableInfo.getKeyColumn();

        if (Objects.equals(orderColumn, keyProperty)) {
            return keyColumn;
        }

        // Match property name
        for (TableFieldInfo fieldInfo : tableInfo.getFieldList()) {
            if (Objects.equals(orderColumn, fieldInfo.getProperty())) {
                return resolveColumnName(entityType, fieldInfo.getProperty(), fieldInfo);
            }
        }

        // Match column name
        if (Objects.equals(orderColumn, keyColumn)) {
            return keyColumn;
        }

        for (TableFieldInfo fieldInfo : tableInfo.getFieldList()) {
            if (Objects.equals(orderColumn, fieldInfo.getColumn())) {
                return fieldInfo.getColumn();
            }
        }

        throw new IllegalArgumentException("Order column '" + orderColumn + "' not found in entity " + entityType.getName());
    }

    private String resolveColumnName(Class<?> entityType, String propertyName, TableFieldInfo fieldInfo) {
        Field field = ReflectionUtils.findField(entityType, propertyName);
        if (field != null) {
            String explicitColumnName = resolveExplicitColumnName(field);
            if (org.springframework.util.StringUtils.hasText(explicitColumnName)) {
                return explicitColumnName;
            }
        }
        return fieldInfo.getColumn();
    }

    private String resolveExplicitColumnName(Field field) {
        TableField tableField = field.getAnnotation(TableField.class);
        if (tableField != null && StringUtils.hasText(tableField.value())) {
            return tableField.value();
        }

        String jpaColumnName = getAnnotationStringAttribute(field,
                "name",
                "jakarta.persistence.Column",
                "javax.persistence.Column");
        if (StringUtils.hasText(jpaColumnName)) {
            return jpaColumnName;
        }

        return null;
    }

    private String getAnnotationStringAttribute(Field field,
                                                String attributeName,
                                                String... annotationClassNames) {
        for (java.lang.annotation.Annotation annotation : field.getAnnotations()) {
            String annotationName = annotation.annotationType().getName();
            for (String className : annotationClassNames) {
                if (!Objects.equals(className, annotationName)) {
                    continue;
                }
                try {
                    Method method = annotation.annotationType().getMethod(attributeName);
                    Object value = method.invoke(annotation);
                    if (value instanceof String text && StringUtils.hasText(text)) {
                        return text;
                    }
                } catch (ReflectiveOperationException ignored) {
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Class<? extends TypeHandler<?>> resolveTypeHandler(Class<?> entityType, String propertyName,
                                                               TableFieldInfo fieldInfo) {
        // Try TableFieldInfo.getTypeHandler() via reflection (avoid compile dependency on specific MP version)
        if (fieldInfo != null) {
            Method method = ReflectionUtils.findMethod(TableFieldInfo.class, "getTypeHandler");
            if (method != null) {
                Object value = ReflectionUtils.invokeMethod(method, fieldInfo);
                if (value instanceof Class) {
                    return (Class<? extends TypeHandler<?>>) value;
                }
            }
        }

        // Fallback to @TableField(typeHandler = ...)
        Field field = ReflectionUtils.findField(entityType, propertyName);
        if (field != null) {
            TableField tableField = field.getAnnotation(TableField.class);
            if (tableField != null && !Objects.equals(tableField.typeHandler(), org.apache.ibatis.type.UnknownTypeHandler.class)) {
                return (Class<? extends TypeHandler<?>>) tableField.typeHandler();
            }
        }

        return null;
    }

    private TableInfo getTableInfo(Class<?> entityType) {
        return ProjectionTableInfoSupport.getTableInfo(configuration, entityType);
    }

    private record AssociationMetadata(String propertyName,
                                       String sourceAlias,
                                       Class<?> targetEntityType,
                                       String targetTableName,
                                       String alias,
                                       String joinColumn,
                                       String referencedColumn,
                                       String targetKeyProperty,
                                       String targetKeyColumn,
                                       Map<String, AssociationColumnMetadata> targetFieldMap,
                                       boolean collectionLike,
                                       JoinTableMetadata joinTable) {
    }

    private record JoinTableMetadata(String tableName, String sourceJoinColumn, String targetJoinColumn) {
    }

    private record AssociationColumnMetadata(String propertyName,
                                             String columnName,
                                             Class<? extends TypeHandler<?>> typeHandler) {
    }

    private record ResolvedAssociationField(ProjectionField field,
                                            List<AssociationMetadata> associations,
                                            List<ProjectionField> additionalFields) {
        private ResolvedAssociationField(ProjectionField field, List<AssociationMetadata> associations) {
            this(field, associations, List.of());
        }
    }

    private record ProjectionProperty(String name, Class<?> type) {
    }

    private List<ProjectionField> resolveInterfaceProjectionFields(String projectionPropertyName,
                                                                   Class<?> projectionInterface,
                                                                   AssociationMetadata association) {
        List<ProjectionField> fields = new ArrayList<>();
        Class<?> targetType = association.targetEntityType();
        Map<String, AssociationColumnMetadata> targetFieldMap = association.targetFieldMap();

        for (Method method : projectionInterface.getDeclaredMethods()) {
            if (method.getParameterCount() != 0 || !org.springframework.util.StringUtils.hasText(method.getName())
                    || (method.getName().startsWith("is") && method.getName().length() > 2)) {
                continue;
            }

            String propertyName;
            if (method.getName().startsWith("get")) {
                propertyName = decapitalize(method.getName().substring(3));
            } else if (method.getName().startsWith("is")) {
                propertyName = decapitalize(method.getName().substring(2));
            } else {
                continue;
            }

            AssociationColumnMetadata targetField = targetFieldMap.get(propertyName);
            if (targetField != null) {
                String alias = projectionPropertyName + "_" + propertyName;
                fields.add(new ProjectionField(alias,
                        association.alias() + "." + targetField.columnName() + " AS " + alias,
                        alias,
                        method.getReturnType(),
                        targetField.typeHandler(),
                        false));
            }
        }
        
        // 添加投影属性本身的字段（用于 ResultMap），实际的嵌套字段已在上面添加
        String joinColumn = association.joinColumn();
        String keyColumn = association.targetKeyColumn();
        fields.add(0, new ProjectionField(projectionPropertyName,
                association.alias() + "." + keyColumn + " AS " + projectionPropertyName,
                projectionPropertyName,
                projectionInterface,
                null,
                false));
        
        return fields;
    }

    private String decapitalize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        if (value.length() == 1) {
            return value.toLowerCase();
        }
        return Character.toLowerCase(value.charAt(0)) + value.substring(1);
    }
}
