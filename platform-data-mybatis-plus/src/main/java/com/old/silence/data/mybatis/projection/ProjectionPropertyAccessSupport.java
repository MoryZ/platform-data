package com.old.silence.data.mybatis.projection;

import java.util.Map;
import java.util.Objects;

final class ProjectionPropertyAccessSupport {

    private ProjectionPropertyAccessSupport() {
    }

    static Class<?> wrapPrimitive(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == boolean.class) {
            return Boolean.class;
        }
        if (type == byte.class) {
            return Byte.class;
        }
        if (type == short.class) {
            return Short.class;
        }
        if (type == int.class) {
            return Integer.class;
        }
        if (type == long.class) {
            return Long.class;
        }
        if (type == float.class) {
            return Float.class;
        }
        if (type == double.class) {
            return Double.class;
        }
        if (type == char.class) {
            return Character.class;
        }
        return Void.class;
    }

    static Object resolveValue(Map<String, Object> source,
                               String primaryName,
                               String alternateName,
                               Class<?> targetType) {
        if (source.containsKey(primaryName)) {
            return source.get(primaryName);
        }

        if (alternateName != null && source.containsKey(alternateName)) {
            return source.get(alternateName);
        }

        String underscore = toUnderscoreCase(primaryName);
        if (source.containsKey(underscore)) {
            return source.get(underscore);
        }

        if ((targetType == Boolean.class || targetType == boolean.class)) {
            String isStyle = "is_" + underscore;
            if (source.containsKey(isStyle)) {
                return source.get(isStyle);
            }
        }

        String primaryKey = normalize(primaryName);
        String alternateKey = alternateName == null ? null : normalize(alternateName);
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String key = normalize(entry.getKey());
            if (Objects.equals(key, primaryKey) || Objects.equals(key, alternateKey)) {
                return entry.getValue();
            }
        }
        return null;
    }

    static String normalize(String key) {
        if (key == null) {
            return null;
        }
        String normalized = key.toLowerCase().replace("_", "");
        if (normalized.startsWith("is") && normalized.length() > 2) {
            return normalized.substring(2);
        }
        return normalized;
    }

    static String toUnderscoreCase(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
}