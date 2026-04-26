package com.old.silence.data.mybatis.projection;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.old.silence.core.enums.EnumValue;
import org.springframework.core.convert.support.DefaultConversionService;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class ProjectionValueConverter {

    private static final DefaultConversionService CONVERSION_SERVICE;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        CONVERSION_SERVICE = new DefaultConversionService();
        registerDateTimeConverters(CONVERSION_SERVICE);
    }

    private ProjectionValueConverter() {
    }

    private static void registerDateTimeConverters(DefaultConversionService service) {
        service.addConverter(LocalDateTime.class, Instant.class, ProjectionValueConverter::localDateTimeToInstant);
        service.addConverter(Instant.class, LocalDateTime.class, ProjectionValueConverter::instantToLocalDateTime);
        service.addConverter(LocalDate.class, Instant.class, ProjectionValueConverter::localDateToInstant);
        service.addConverter(Instant.class, LocalDate.class, ProjectionValueConverter::instantToLocalDate);
        service.addConverter(LocalTime.class, Instant.class, ProjectionValueConverter::localTimeToInstant);
        service.addConverter(Instant.class, LocalTime.class, ProjectionValueConverter::instantToLocalTime);
        service.addConverter(ZonedDateTime.class, Instant.class, ZonedDateTime::toInstant);
        service.addConverter(Instant.class, ZonedDateTime.class, zdt -> zdt.atZone(ZoneId.systemDefault()));
    }

    private static Instant localDateTimeToInstant(LocalDateTime ldt) {
        return ldt == null ? null : ldt.atZone(ZoneId.systemDefault()).toInstant();
    }

    private static LocalDateTime instantToLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    private static Instant localDateToInstant(LocalDate ld) {
        return ld == null ? null : ld.atStartOfDay(ZoneId.systemDefault()).toInstant();
    }

    private static LocalDate instantToLocalDate(Instant instant) {
        return instant == null ? null : instant.atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private static Instant localTimeToInstant(LocalTime lt) {
        return lt == null ? null : lt.atDate(LocalDate.now()).atZone(ZoneId.systemDefault()).toInstant();
    }

    private static LocalTime instantToLocalTime(Instant instant) {
        return instant == null ? null : instant.atZone(ZoneId.systemDefault()).toLocalTime();
    }

    static Map<String, Object> normalizeRow(ProjectionMetadata metadata, Map<String, Object> source) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        for (ProjectionField field : metadata.getFields()) {
            Object rawValue = resolveRawValue(source, field);
            normalized.put(field.getPropertyName(), convertValue(rawValue, field, metadata.getProjectionType()));
        }
        for (ProjectionCollectionAssociation association : metadata.getCollectionAssociations()) {
            Object collectionValue = source.get(association.getProjectionPropertyName());
            if (collectionValue != null) {
                normalized.put(association.getProjectionPropertyName(), collectionValue);
            }
        }
        return normalized;
    }

    static List<Map<String, Object>> normalizeRows(ProjectionMetadata metadata, List<Map<String, Object>> sourceList) {
        return sourceList.stream().map(source -> normalizeRow(metadata, source)).toList();
    }

    private static Object resolveRawValue(Map<String, Object> source, ProjectionField field) {
        return ProjectionPropertyAccessSupport.resolveValue(source,
                field.getPropertyName(),
                field.getColumnName(),
                field.getJavaType());
    }

    private static Object convertValue(Object rawValue, ProjectionField field, Class<?> projectionType) {
        if (rawValue == null) {
            return null;
        }

        Class<?> targetType = ProjectionPropertyAccessSupport.wrapPrimitive(field.getJavaType());
        if (targetType.isInstance(rawValue)) {
            return rawValue;
        }

        if (Map.class.isAssignableFrom(targetType)) {
            return convertToMap(rawValue, field, projectionType);
        }

        if (targetType.isEnum()) {
            return convertToEnum(targetType, rawValue, field, projectionType);
        }

        if (CONVERSION_SERVICE.canConvert(rawValue.getClass(), targetType)) {
            Object converted = CONVERSION_SERVICE.convert(rawValue, targetType);
            if (converted != null) {
                return converted;
            }
        }

        throw conversionFailure(field, projectionType, rawValue, targetType, null);
    }

    private static Object convertToMap(Object rawValue,
                                       ProjectionField field,
                                       Class<?> projectionType) {
        if (rawValue instanceof Map<?, ?> mapValue) {
            return mapValue;
        }

        if (rawValue instanceof CharSequence sequence) {
            String json = sequence.toString();
            if (json.isBlank()) {
                return Map.of();
            }
            try {
                return OBJECT_MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {
                });
            } catch (Exception ex) {
                throw conversionFailure(field, projectionType, rawValue, field.getJavaType(), ex);
            }
        }

        throw conversionFailure(field, projectionType, rawValue, field.getJavaType(), null);
    }

    private static Object convertToEnum(Class<?> enumType,
                                        Object rawValue,
                                        ProjectionField field,
                                        Class<?> projectionType) {
        Object[] constants = enumType.getEnumConstants();
        if (constants == null || constants.length == 0) {
            throw conversionFailure(field, projectionType, rawValue, enumType, null);
        }

        if (rawValue instanceof String stringValue) {
            for (Object constant : constants) {
                Enum<?> enumConstant = (Enum<?>) constant;
                if (enumConstant.name().equals(stringValue)) {
                    return enumConstant;
                }
            }
        }

        for (Object constant : constants) {
            if (constant instanceof EnumValue<?> enumValue
                    && valuesEquivalent(enumValue.getValue(), rawValue)) {
                return constant;
            }
        }

        Integer ordinal = parseInteger(rawValue);
        if (ordinal != null) {
            for (Object constant : constants) {
                Enum<?> enumConstant = (Enum<?>) constant;
                if (enumConstant.ordinal() == ordinal) {
                    return enumConstant;
                }
            }
        }

        throw conversionFailure(field, projectionType, rawValue, enumType, null);
    }

    private static boolean valuesEquivalent(Object expected, Object actual) {
        if (Objects.equals(expected, actual)) {
            return true;
        }
        if (expected == null || actual == null) {
            return false;
        }
        if (expected instanceof Number || actual instanceof Number) {
            return Objects.equals(String.valueOf(expected), String.valueOf(actual));
        }
        return Objects.equals(String.valueOf(expected), String.valueOf(actual));
    }

    private static Integer parseInteger(Object rawValue) {
        if (rawValue instanceof Number number) {
            return number.intValue();
        }
        if (rawValue instanceof String stringValue) {
            try {
                return Integer.parseInt(stringValue);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static IllegalArgumentException conversionFailure(ProjectionField field,
                                                              Class<?> projectionType,
                                                              Object rawValue,
                                                              Class<?> targetType,
                                                              Exception cause) {
        String rawType = rawValue == null ? "null" : rawValue.getClass().getName();
        String preview = rawValue == null ? "null" : String.valueOf(rawValue);
        if (preview.length() > 120) {
            preview = preview.substring(0, 117) + "...";
        }
        String message = "Failed to materialize projection field '" + field.getPropertyName()
                + "' for " + projectionType.getName()
                + " from " + rawType
                + " to " + targetType.getName()
                + " with value [" + preview + "]";
        return cause == null ? new IllegalArgumentException(message) : new IllegalArgumentException(message, cause);
    }

}