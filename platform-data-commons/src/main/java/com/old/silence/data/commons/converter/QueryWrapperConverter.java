package com.old.silence.data.commons.converter;

import static com.old.silence.data.commons.converter.Part.Type;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.old.silence.data.commons.annotation.QueryConvert;
import com.old.silence.data.commons.annotation.RelationalQueryProperty;

/**
 * @author moryzang
 */
public class QueryWrapperConverter {


    private static final Set<Type> INVALID_OPERATORS = EnumSet.of(Type.IS_NULL, Type.IS_NOT_NULL, Type.TRUE,
            Type.FALSE, Type.BETWEEN);

    // Cache key: queryClass.getName() + "|" + domainType.getName()
    private static final ConcurrentHashMap<String, List<QueryPropertyMetadata>> QUERY_METADATA_CACHE =
            new ConcurrentHashMap<>();
    private static final int MAX_NESTED_PROPERTY_DEPTH = 32;

    private QueryWrapperConverter() {
    }

    public static <T> QueryWrapper<T> convert(Object query, Class<T> domainType) {
        return convert(query, domainType, null);
    }

    @SuppressWarnings("unchecked")
    public static <T> QueryWrapper<T> convert(Object query, Class<T> domainType, BeanFactory beanFactory) {
        return convert(query, domainType, beanFactory, null);
    }

    @SuppressWarnings("unchecked")
    public static <T> QueryWrapper<T> convert(Object query, Class<T> domainType, BeanFactory beanFactory, String tableAlias) {
        String cacheKey = query.getClass().getName() + "|" + domainType.getName();
        List<QueryPropertyMetadata> metadatas = QUERY_METADATA_CACHE.computeIfAbsent(cacheKey,
                key -> parseQueryPropertyMetadatas(query.getClass(), domainType, beanFactory));

        QueryWrapper<T> queryWrapper = new QueryWrapper<>();
        BeanWrapper accessor = new BeanWrapperImpl(query);

        for (QueryPropertyMetadata metadata : metadatas) {
            Object value = accessor.getPropertyValue(metadata.fieldName);
            if (value == null && !metadata.nullable) {
                continue;
            }

            final var valueToConvert = value;
            value = metadata.converter
                    .map(it -> ((JdbcQueryAttributeConverter<Object, Object>) it).convert(metadata.columnName, valueToConvert))
                    .orElse(value);

            applyCondition(queryWrapper, metadata, value, tableAlias);
        }

        return queryWrapper;
    }

    private static <T> void applyCondition(QueryWrapper<T> queryWrapper,
                                           QueryPropertyMetadata metadata,
                                           Object value,
                                           String tableAlias) {
        // Collection association path: use pre-built IN subquery
        if (metadata.subquery && StringUtils.hasText(metadata.subquerySqlTemplate)) {
            queryWrapper.apply(metadata.subquerySqlTemplate, value);
            return;
        }

        // Add table alias if column name has no alias (no dot separator) and tableAlias is provided
        String column = qualifyColumnWithAlias(metadata.columnName, tableAlias);
        Type operator = metadata.operator;

        switch (operator) {
            case EQUAL:
                queryWrapper.eq(column, value);
                break;
            case NOT_EQUAL:
                queryWrapper.ne(column, value);
                break;
            case GREATER_THAN:
                queryWrapper.gt(column, value);
                break;
            case GREATER_THAN_EQUAL:
                queryWrapper.ge(column, value);
                break;
            case LESS_THAN:
                queryWrapper.lt(column, value);
                break;
            case LESS_THAN_EQUAL:
                queryWrapper.le(column, value);
                break;
            case IN:
                if (value instanceof Collection) {
                    queryWrapper.in(column, (Collection<?>) value);
                }
                break;
            case NOT_IN:
                if (value instanceof Collection) {
                    queryWrapper.notIn(column, (Collection<?>) value);
                }
                break;
            case LIKE:
                queryWrapper.like(column, value);
                break;
            case NOT_LIKE:
                queryWrapper.notLike(column, value);
                break;
            case STARTING_WITH:
                queryWrapper.likeRight(column, value);
                break;
            case ENDING_WITH:
                queryWrapper.likeLeft(column, value);
                break;
            case CONTAINING:
                queryWrapper.like(column, "%" + value + "%");
                break;
            case NOT_CONTAINING:
                queryWrapper.notLike(column, "%" + value + "%");
                break;
            case IS_NULL:
                queryWrapper.isNull(column);
                break;
            case IS_NOT_NULL:
                queryWrapper.isNotNull(column);
                break;
            case SIMPLE_PROPERTY:
                if (value != null) {
                    queryWrapper.eq(column, value);
                }
                break;
            default:
                throw new IllegalArgumentException("Unsupported operator: " + operator);
        }

        // 处理忽略大小写
        if (metadata.ignoreCase && value instanceof String) {
            queryWrapper.apply("LOWER({0}) = LOWER({1})", column, value);
        }
    }

    /**
     * Qualify column name with table alias if it doesn't already have one.
     * Handles nested paths like "job_group.group_name" which already have an alias.
     * If defaultAlias is null or blank, returns the column name as-is.
     */
    private static String qualifyColumnWithAlias(String columnName, String defaultAlias) {
        if (!StringUtils.hasText(columnName)) {
            return columnName;
        }
        // If column already has an alias (contains dot), don't modify
        if (columnName.contains(".")) {
            return columnName;
        }
        // If no default alias provided, return column name as-is
        if (!StringUtils.hasText(defaultAlias)) {
            return columnName;
        }
        return defaultAlias + "." + columnName;
    }

    private static List<QueryPropertyMetadata> parseQueryPropertyMetadatas(Class<?> queryType,
                                                                           Class<?> domainType,
                                                                           BeanFactory beanFactory) {
        List<QueryPropertyMetadata> metadatas = new ArrayList<>();

        ReflectionUtils.doWithFields(queryType, field -> {
            RelationalQueryProperty annotation = AnnotationUtils.getAnnotation(field, RelationalQueryProperty.class);
            if (annotation == null) return;

            if (INVALID_OPERATORS.contains(annotation.type())) {
                throw new IllegalArgumentException("Unsupported operator: " + annotation.type() + " on field: " + field.getName());
            }

            // 使用domainType解析属性路径
            String propertyName = StringUtils.hasText(annotation.name()) ?
                    annotation.name() : field.getName();

            // 处理类型转换器
            var converter = Optional.ofNullable(field.getAnnotation(QueryConvert.class)).map(QueryConvert::converter)
                    .map(converterType -> Objects.requireNonNull(beanFactory, () -> String.format(
                            "BeanFactory is required as query attribute [%s] of query object [%s] is annotated with the QueryConvert annotation",
                            propertyName, queryType)).getBean(converterType));

                // Detect collection-association path (e.g. "users.username" or "userRoles.role.roleName"): emit IN subquery
            if (propertyName.contains(".")) {
                String[] parts = propertyName.split("\\.", 2);
                try {
                    Field domainField = findField(domainType, parts[0]);
                    if (Collection.class.isAssignableFrom(domainField.getType())) {
                        String subquerySql = buildCollectionSubquerySql(
                                domainType, domainField, parts[1], annotation.type());
                        if (subquerySql != null) {
                            metadatas.add(new QueryPropertyMetadata(
                                    field.getName(), null, annotation.type(),
                                    annotation.ignoreCase(), annotation.nullable(), converter,
                                    true, subquerySql));
                            return;
                        }
                    }
                } catch (NoSuchFieldException ignored) {
                    // fall through to normal column resolution
                }
            }

            String columnName = resolveColumnName(domainType, propertyName);
            metadatas.add(new QueryPropertyMetadata(
                    field.getName(),
                    columnName,
                    annotation.type(),
                    annotation.ignoreCase(),
                    annotation.nullable(),
                    converter
            ));


        });

        return Collections.unmodifiableList(metadatas);
    }

    private static String resolveColumnName(Class<?> domainType, String propertyPath) {
        try {
            if (!propertyPath.contains(".")) {
                Field field = findField(domainType, propertyPath);
                String explicitColumnName = resolveExplicitColumnName(field);
                if (StringUtils.hasText(explicitColumnName)) {
                    return explicitColumnName;
                }
                return convertFieldNameToColumn(propertyPath);
            }

            return resolveNestedColumnPath(domainType, propertyPath);
        } catch (NoSuchFieldException e) {
            throw new IllegalArgumentException("Property path '" + propertyPath + "' not found in " + domainType.getName());
        }
    }

    private static String resolveNestedColumnPath(Class<?> domainType,
                                                  String propertyPath) throws NoSuchFieldException {
        String[] properties = propertyPath.split("\\.");
        if (properties.length < 2) {
            return convertFieldNameToColumn(propertyPath);
        }
        if (properties.length > MAX_NESTED_PROPERTY_DEPTH) {
            throw new IllegalArgumentException("Property path depth exceeds max " + MAX_NESTED_PROPERTY_DEPTH
                    + ": " + propertyPath);
        }

        Class<?> currentType = domainType;
        StringBuilder aliasPath = new StringBuilder();
        for (int i = 0; i < properties.length - 1; i++) {
            String property = properties[i];
            Field field = findField(currentType, property);
            if (i > 0) {
                aliasPath.append('_');
            }
            aliasPath.append(convertFieldNameToColumn(property));

            Class<?> fieldType = field.getType();
            if (Collection.class.isAssignableFrom(fieldType)) {
                currentType = resolveCollectionElementType(field);
            } else {
                currentType = fieldType;
            }
        }

        String leafProperty = properties[properties.length - 1];
        Field leafField = findField(currentType, leafProperty);
        String explicitLeafColumn = resolveExplicitColumnName(leafField);
        String leafColumn = StringUtils.hasText(explicitLeafColumn)
                ? explicitLeafColumn
                : convertFieldNameToColumn(leafProperty);

        return aliasPath + "." + leafColumn;
    }

    private static Field findField(Class<?> clazz, String fieldName) throws NoSuchFieldException {

        Class<?> current = clazz;

        while (current != null && current != Object.class) {

            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }

        throw new NoSuchFieldException("Field '" + fieldName + "' not found in " + clazz.getName() + " or its superclasses");

    }

    private static Class<?> resolveCollectionElementType(Field field) {
        java.lang.reflect.Type genericType = field.getGenericType();
        if (genericType instanceof ParameterizedType parameterizedType
                && parameterizedType.getActualTypeArguments().length == 1) {
            java.lang.reflect.Type elementType = parameterizedType.getActualTypeArguments()[0];
            if (elementType instanceof Class<?> elementClass) {
                return elementClass;
            }
        }
        return Object.class;
    }

    private static String convertFieldNameToColumn(String fieldName) {
        // 实现驼峰转下划线逻辑
        return fieldName.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase();
    }

    // -----------------------------------------------------------------------
    // Collection subquery helpers
    // -----------------------------------------------------------------------

    /**
     * Build an IN-subquery SQL template for a collection-association condition.
     * Returns null when the path cannot be resolved (caller falls back to Phase-2 join).
     */
    private static String buildCollectionSubquerySql(Class<?> domainType, Field collectionField,
                                                     String nestedPropertyPath, Type operator) {
        // Multi-value operators require a different approach — fall back to Phase-2
        if (operator == Type.IN || operator == Type.NOT_IN) {
            return null;
        }
        try {
            Class<?> targetType = resolveCollectionElementType(collectionField);
            if (targetType == null || targetType == Object.class) {
                return null;
            }

            String sourcePkColumn = findPkColumn(domainType);
            String targetTable = resolveTargetTableName(targetType);
            String fkColumn = resolveCollectionFkColumn(collectionField, targetType, domainType);
            if (fkColumn == null) {
                return null;
            }

            String[] pathSegments = nestedPropertyPath.split("\\.");
            if (pathSegments.length == 0) {
                return null;
            }

            String rootAlias = "c0";
            StringBuilder fromClause = new StringBuilder(targetTable).append(" ").append(rootAlias);
            Class<?> currentType = targetType;
            String currentAlias = rootAlias;

            for (int i = 0; i < pathSegments.length - 1; i++) {
                String associationProperty = pathSegments[i];
                Field associationField = findField(currentType, associationProperty);
                if (Collection.class.isAssignableFrom(associationField.getType())) {
                    return null;
                }
                Class<?> associationTargetType = associationField.getType();
                String associationTable = resolveTargetTableName(associationTargetType);
                String associationAlias = "c" + (i + 1);
                String joinColumn = resolveAssociationJoinColumn(currentType, associationField);
                String referencedColumn = resolveAssociationReferencedColumn(associationField, associationTargetType);
                if (!StringUtils.hasText(joinColumn) || !StringUtils.hasText(referencedColumn)) {
                    return null;
                }
                fromClause.append(" LEFT JOIN ")
                        .append(associationTable)
                        .append(" ")
                        .append(associationAlias)
                        .append(" ON ")
                        .append(currentAlias)
                        .append(".")
                        .append(joinColumn)
                        .append(" = ")
                        .append(associationAlias)
                        .append(".")
                        .append(referencedColumn);
                currentType = associationTargetType;
                currentAlias = associationAlias;
            }

            String leafProperty = pathSegments[pathSegments.length - 1];
            Field leafField = findField(currentType, leafProperty);
                String explicitLeafColumn = resolveExplicitColumnName(leafField);
                String leafColumn = StringUtils.hasText(explicitLeafColumn)
                    ? explicitLeafColumn
                    : convertFieldNameToColumn(leafProperty);

            boolean negated = (operator == Type.NOT_EQUAL
                    || operator == Type.NOT_LIKE
                    || operator == Type.NOT_CONTAINING);
            String inClause = negated ? "NOT IN" : "IN";
            String innerCondition = buildInnerWhereClause(currentAlias + "." + leafColumn, operator);

            return "t0." + sourcePkColumn + " " + inClause
                    + " (SELECT " + rootAlias + "." + fkColumn + " FROM " + fromClause
                    + " WHERE " + innerCondition + ")";
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    private static String resolveAssociationJoinColumn(Class<?> sourceType, Field associationField) {
        String explicitJoinColumn = getJoinColumnName(associationField);
        if (StringUtils.hasText(explicitJoinColumn)) {
            return explicitJoinColumn;
        }
        String inferredFkProperty = associationField.getName() + "Id";
        try {
            Field fkField = findField(sourceType, inferredFkProperty);
            String explicitColumnName = resolveExplicitColumnName(fkField);
            if (StringUtils.hasText(explicitColumnName)) {
                return explicitColumnName;
            }
            return convertFieldNameToColumn(inferredFkProperty);
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    private static String resolveAssociationReferencedColumn(Field associationField, Class<?> targetType) {
        for (java.lang.annotation.Annotation ann : associationField.getAnnotations()) {
            String annName = ann.annotationType().getName();
            if ("jakarta.persistence.JoinColumn".equals(annName)
                    || "javax.persistence.JoinColumn".equals(annName)) {
                try {
                    java.lang.reflect.Method method = ann.annotationType().getMethod("referencedColumnName");
                    Object value = method.invoke(ann);
                    if (value instanceof String text && StringUtils.hasText(text)) {
                        return text;
                    }
                } catch (ReflectiveOperationException ignored) {
                }
            }
        }
        return findPkColumn(targetType);
    }

    private static String buildInnerWhereClause(String leafColumn, Type operator) {
        return switch (operator) {
            case EQUAL, NOT_EQUAL -> leafColumn + " = {0}";
            case LIKE, NOT_LIKE -> leafColumn + " LIKE {0}";
            case CONTAINING, NOT_CONTAINING -> leafColumn + " LIKE CONCAT('%', {0}, '%')";
            case STARTING_WITH -> leafColumn + " LIKE CONCAT({0}, '%')";
            case ENDING_WITH -> leafColumn + " LIKE CONCAT('%', {0})";
            case GREATER_THAN -> leafColumn + " > {0}";
            case GREATER_THAN_EQUAL -> leafColumn + " >= {0}";
            case LESS_THAN -> leafColumn + " < {0}";
            case LESS_THAN_EQUAL -> leafColumn + " <= {0}";
            default -> leafColumn + " = {0}";
        };
    }

    /** Find the primary-key column name of a MyBatis-Plus entity class via @TableId. */
    private static String findPkColumn(Class<?> domainType) {
        Class<?> current = domainType;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                TableId tableId = field.getAnnotation(TableId.class);
                if (tableId != null) {
                    return StringUtils.hasText(tableId.value())
                            ? tableId.value()
                            : convertFieldNameToColumn(field.getName());
                }
            }
            current = current.getSuperclass();
        }
        return "id";
    }

    /** Resolve a MyBatis-Plus entity's table name via @TableName or simple snake-case convention. */
    private static String resolveTargetTableName(Class<?> targetType) {
        TableName tableNameAnn = targetType.getAnnotation(TableName.class);
        if (tableNameAnn != null && StringUtils.hasText(tableNameAnn.value())) {
            return tableNameAnn.value();
        }
        return convertFieldNameToColumn(targetType.getSimpleName());
    }

    /**
     * Resolve the FK column in targetType that references domainType.
     * Prefers the @OneToMany(mappedBy) + @JoinColumn(name) path;
     * falls back to @TableField convention.
     */
    private static String resolveCollectionFkColumn(Field collectionField,
                                                    Class<?> targetType,
                                                    Class<?> domainType) {
        // 1. Walk mappedBy annotation
        for (java.lang.annotation.Annotation ann : collectionField.getAnnotations()) {
            try {
                java.lang.reflect.Method mappedByMethod = ann.annotationType().getMethod("mappedBy");
                Object mbValue = mappedByMethod.invoke(ann);
                if (mbValue instanceof String mappedBy && StringUtils.hasText(mappedBy)) {
                    try {
                        Field mappedField = findField(targetType, mappedBy);
                        String jcName = getJoinColumnName(mappedField);
                        if (StringUtils.hasText(jcName)) {
                            return jcName;
                        }
                    } catch (NoSuchFieldException ignored) {
                    }
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }
        // 2. Scan @TableField values in targetType for a convention FK match
        String domainSnake = convertFieldNameToColumn(domainType.getSimpleName());
        for (Field field : targetType.getDeclaredFields()) {
            TableField tf = field.getAnnotation(TableField.class);
            if (tf != null && StringUtils.hasText(tf.value())) {
                String col = tf.value();
                if (col.equals(domainSnake + "_id")) {
                    return col;
                }
            }
        }
        // 3. Convention: domainSnake_id
        return domainSnake + "_id";
    }

    /** Extract @JoinColumn(name=...) from a field, supporting both jakarta and javax namespaces. */
    private static String getJoinColumnName(Field field) {
        for (java.lang.annotation.Annotation ann : field.getAnnotations()) {
            String annName = ann.annotationType().getName();
            if ("jakarta.persistence.JoinColumn".equals(annName)
                    || "javax.persistence.JoinColumn".equals(annName)) {
                try {
                    java.lang.reflect.Method nameMethod = ann.annotationType().getMethod("name");
                    Object nameVal = nameMethod.invoke(ann);
                    if (nameVal instanceof String name && StringUtils.hasText(name)) {
                        return name;
                    }
                } catch (ReflectiveOperationException ignored) {
                }
            }
        }
        return null;
    }

    private static String resolveExplicitColumnName(Field field) {
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

    private static String getAnnotationStringAttribute(Field field,
                                                       String attributeName,
                                                       String... annotationClassNames) {
        for (java.lang.annotation.Annotation ann : field.getAnnotations()) {
            String annName = ann.annotationType().getName();
            for (String annotationClassName : annotationClassNames) {
                if (!annotationClassName.equals(annName)) {
                    continue;
                }
                try {
                    java.lang.reflect.Method method = ann.annotationType().getMethod(attributeName);
                    Object value = method.invoke(ann);
                    if (value instanceof String text && StringUtils.hasText(text)) {
                        return text;
                    }
                } catch (ReflectiveOperationException ignored) {
                }
            }
        }
        return null;
    }

    private static class QueryPropertyMetadata {
        private final String fieldName;
        private final String columnName;
        private final Type operator;
        private final boolean ignoreCase;
        private final boolean nullable;
        private final Optional<? extends JdbcQueryAttributeConverter<?, ?>> converter;
        /** True when this condition should be emitted as an IN-subquery rather than a plain column condition. */
        private final boolean subquery;
        /** Pre-built SQL template for subquery conditions, e.g. "t0.id IN (SELECT fk FROM t WHERE col = {0})". */
        private final String subquerySqlTemplate;

        public QueryPropertyMetadata(String fieldName,
                                     String columnName,
                                     Type operator,
                                     boolean ignoreCase,
                                     boolean nullable,
                                     Optional<? extends JdbcQueryAttributeConverter<?, ?>> converter) {
            this(fieldName, columnName, operator, ignoreCase, nullable, converter, false, null);
        }

        public QueryPropertyMetadata(String fieldName,
                                     String columnName,
                                     Type operator,
                                     boolean ignoreCase,
                                     boolean nullable,
                                     Optional<? extends JdbcQueryAttributeConverter<?, ?>> converter,
                                     boolean subquery,
                                     String subquerySqlTemplate) {
            this.fieldName = fieldName;
            this.columnName = columnName;
            this.operator = operator;
            this.ignoreCase = ignoreCase;
            this.nullable = nullable;
            this.converter = converter;
            this.subquery = subquery;
            this.subquerySqlTemplate = subquerySqlTemplate;
        }
    }
}