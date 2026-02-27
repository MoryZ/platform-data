package com.old.silence.data.mybatis.projection;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.type.TypeHandler;
import org.springframework.util.ReflectionUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
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

    private final ConcurrentMap<String, ProjectionMetadata> cache = new ConcurrentHashMap<>();

    public ProjectionMetadata resolve(Class<?> projectionType, Class<?> entityType) {
        return resolve(projectionType, entityType, List.of());
    }

    public ProjectionMetadata resolve(Class<?> projectionType, Class<?> entityType, List<String> selectedFields) {
        List<String> normalizedSelectedFields = normalizeSelectedFields(selectedFields);
        String selectionKey = buildSelectionKey(normalizedSelectedFields);
        String cacheKey = entityType.getName() + "->" + projectionType.getName() + "#" + selectionKey;
        ProjectionMetadata cached = cache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        TableInfo tableInfo = TableInfoHelper.getTableInfo(entityType);
        if (tableInfo == null) {
            throw new IllegalArgumentException("No TableInfo found for entity type: " + entityType.getName());
        }

        Map<String, TableFieldInfo> fieldMap = tableInfo.getFieldList().stream()
                .collect(Collectors.toMap(TableFieldInfo::getProperty, Function.identity()));

        List<ProjectionField> fields = new ArrayList<>();
        String keyProperty = tableInfo.getKeyProperty();
        String keyColumn = tableInfo.getKeyColumn();

        for (PropertyDescriptor descriptor : org.springframework.beans.BeanUtils.getPropertyDescriptors(projectionType)) {
            String propertyName = descriptor.getName();
            if ("class".equals(propertyName)) {
                continue;
            }

            if (Objects.equals(propertyName, keyProperty)) {
                fields.add(new ProjectionField(propertyName, keyColumn, descriptor.getPropertyType(),
                        resolveTypeHandler(entityType, propertyName, null), true));
                continue;
            }

            TableFieldInfo fieldInfo = fieldMap.get(propertyName);
            if (fieldInfo == null) {
                throw new IllegalArgumentException("Projection property '" + propertyName + "' not found in entity "
                        + entityType.getName());
            }

            String columnName = resolveColumnName(entityType, propertyName, fieldInfo);
            Class<? extends TypeHandler<?>> typeHandler = resolveTypeHandler(entityType, propertyName, fieldInfo);

            fields.add(new ProjectionField(propertyName, columnName, descriptor.getPropertyType(), typeHandler, false));
        }

        List<ProjectionField> selectedProjectionFields = selectProjectionFields(fields, normalizedSelectedFields, projectionType);
        ProjectionMetadata metadata = new ProjectionMetadata(projectionType, entityType, tableInfo.getTableName(),
                selectedProjectionFields, selectionKey);
        cache.putIfAbsent(cacheKey, metadata);
        return metadata;
    }

    private List<String> normalizeSelectedFields(List<String> selectedFields) {
        if (selectedFields == null || selectedFields.isEmpty()) {
            return List.of();
        }
        return selectedFields.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(field -> !field.isEmpty())
                .collect(Collectors.toList());
    }

    private String buildSelectionKey(List<String> selectedFields) {
        if (selectedFields.isEmpty()) {
            return "ALL";
        }
        return String.join(",", selectedFields);
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
        TableInfo tableInfo = TableInfoHelper.getTableInfo(entityType);
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
            TableField tableField = field.getAnnotation(TableField.class);
            if (tableField != null && org.springframework.util.StringUtils.hasText(tableField.value())) {
                return tableField.value();
            }
        }
        return fieldInfo.getColumn();
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
}
