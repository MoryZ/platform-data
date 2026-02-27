package com.old.silence.data.mybatis.test;

import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Support for projection queries in MyBatis Plus
 * 
 * @author moryzang
 */
class ProjectionSupport {

    /**
     * Resolve which columns should be selected for the projection type
     * 
     * @param projectionType projection class
     * @param tableInfo table info
     * @return array of column names to select
     */
    static String[] resolveSelectColumns(Class<?> projectionType, TableInfo tableInfo) {
        PropertyDescriptor[] descriptors = BeanUtils.getPropertyDescriptors(projectionType);
        List<String> columns = new ArrayList<>();

        for (PropertyDescriptor descriptor : descriptors) {
            if ("class".equals(descriptor.getName())) {
                continue;
            }

            String propertyName = descriptor.getName();

            // Check if it's the primary key
            if (propertyName.equals(tableInfo.getKeyProperty())) {
                columns.add(tableInfo.getKeyColumn());
                continue;
            }

            // Find in regular fields
            TableFieldInfo fieldInfo = tableInfo.getFieldList().stream()
                    .filter(f -> f.getProperty().equals(propertyName))
                    .findFirst()
                    .orElse(null);

            if (fieldInfo != null) {
                columns.add(fieldInfo.getColumn());
            } else {
                // Try camelCase to underscore conversion
                String columnName = toUnderscoreCase(propertyName);
                TableFieldInfo matchedField = tableInfo.getFieldList().stream()
                        .filter(f -> f.getColumn().equalsIgnoreCase(columnName))
                        .findFirst()
                        .orElse(null);

                if (matchedField != null) {
                    columns.add(matchedField.getColumn());
                } else {
                    throw new IllegalArgumentException(
                            "Property '" + propertyName + "' not found in entity " + tableInfo.getEntityType().getSimpleName()
                    );
                }
            }
        }

        return columns.toArray(new String[0]);
    }

    /**
     * Convert Map result to projection object
     * 
     * @param map result map from selectMaps()
     * @param projectionType projection class
     * @param <P> projection type
     * @return projection instance
     */
    static <P> P convertMapToProjection(Map<String, Object> map, Class<P> projectionType) {
        try {
            P projection = BeanUtils.instantiateClass(projectionType);
            BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(projection);

            // Convert underscore keys to camelCase and set properties
            map.forEach((key, value) -> {
                String propertyName = toCamelCase(key);
                if (wrapper.isWritableProperty(propertyName)) {
                    wrapper.setPropertyValue(propertyName, value);
                }
            });

            return projection;
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert map to projection type: " + projectionType.getSimpleName(), e);
        }
    }

    /**
     * Convert List of Maps to List of projection objects
     */
    static <P> List<P> convertMapsToProjections(List<Map<String, Object>> maps, Class<P> projectionType) {
        List<P> projections = new ArrayList<>(maps.size());
        for (Map<String, Object> map : maps) {
            projections.add(convertMapToProjection(map, projectionType));
        }
        return projections;
    }

    /**
     * Convert camelCase to underscore_case
     */
    private static String toUnderscoreCase(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) {
            return camelCase;
        }
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    /**
     * Convert underscore_case to camelCase
     */
    private static String toCamelCase(String underscoreStr) {
        if (underscoreStr == null || !underscoreStr.contains("_")) {
            return underscoreStr;
        }

        String[] parts = underscoreStr.toLowerCase().split("_");
        StringBuilder camelCase = new StringBuilder(parts[0]);

        for (int i = 1; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                camelCase.append(Character.toUpperCase(parts[i].charAt(0)));
                if (parts[i].length() > 1) {
                    camelCase.append(parts[i].substring(1));
                }
            }
        }

        return camelCase.toString();
    }
}
