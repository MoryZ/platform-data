package com.old.silence.data.commons.converter;

import static com.old.silence.data.commons.converter.Part.Type;

import java.lang.reflect.Field;
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
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.old.silence.data.commons.annotation.QueryConvert;
import com.old.silence.data.commons.annotation.RelationalQueryProperty;

/**
 * @author moryzang
 */
public class QueryWrapperConverter {


    private static final Set<Type> INVALID_OPERATORS = EnumSet.of(Type.IS_NULL, Type.IS_NOT_NULL, Type.TRUE,
            Type.FALSE, Type.BETWEEN);

    private static final ConcurrentHashMap<Class<?>, List<QueryPropertyMetadata>> QUERY_METADATA_CACHE =
            new ConcurrentHashMap<>();

    private QueryWrapperConverter() {
    }

    public static <T> QueryWrapper<T> convert(Object query, Class<T> domainType) {
        return convert(query, domainType, null);
    }

    @SuppressWarnings("unchecked")
    public static <T> QueryWrapper<T> convert(Object query, Class<T> domainType, BeanFactory beanFactory) {
        List<QueryPropertyMetadata> metadatas = QUERY_METADATA_CACHE.computeIfAbsent(query.getClass(),
                clazz -> parseQueryPropertyMetadatas(clazz, domainType, beanFactory));

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

            applyCondition(queryWrapper, metadata, value);
        }

        return queryWrapper;
    }

    private static <T> void applyCondition(QueryWrapper<T> queryWrapper,
                                           QueryPropertyMetadata metadata,
                                           Object value) {
        String column = metadata.columnName;
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
            String columnName = resolveColumnName(domainType, propertyName);

            // 处理类型转换器
            var converter = Optional.ofNullable(field.getAnnotation(QueryConvert.class)).map(QueryConvert::converter)
                    .map(converterType -> Objects.requireNonNull(beanFactory, () -> String.format(
                            "BeanFactory is required as query attribute [%s] of query object [%s] is annotated with the QueryConvert annotation",
                            propertyName, queryType)).getBean(converterType));
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
            // 获取领域模型属性的列名（支持嵌套属性）
            String[] properties = propertyPath.split("\\.");
            Class<?> currentType = domainType;
            Field domainField = null;

            for (String prop : properties) {
                domainField = findField(currentType, prop);
                currentType = domainField.getType();
            }

            // 优先读取@TableField注解
            TableField tableField = domainField.getAnnotation(TableField.class);
            if (tableField != null && StringUtils.hasText(tableField.value())) {
                return tableField.value();
            }

            // 默认驼峰转下划线
            return convertFieldNameToColumn(propertyPath);
        } catch (NoSuchFieldException e) {
            throw new IllegalArgumentException("Property path '" + propertyPath + "' not found in " + domainType.getName());
        }
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

    private static String convertFieldNameToColumn(String fieldName) {
        // 实现驼峰转下划线逻辑
        return fieldName.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase();
    }

    private static class QueryPropertyMetadata {
        private final String fieldName;
        private final String columnName;
        private final Type operator;
        private final boolean ignoreCase;
        private final boolean nullable;
        private final Optional<? extends JdbcQueryAttributeConverter<?, ?>> converter;

        public QueryPropertyMetadata(String fieldName,
                                     String columnName,
                                     Type operator,
                                     boolean ignoreCase,
                                     boolean nullable,
                                     Optional<? extends JdbcQueryAttributeConverter<?, ?>> converter) {
            this.fieldName = fieldName;
            this.columnName = columnName;
            this.operator = operator;
            this.ignoreCase = ignoreCase;
            this.nullable = nullable;
            this.converter = converter;
        }
    }
}