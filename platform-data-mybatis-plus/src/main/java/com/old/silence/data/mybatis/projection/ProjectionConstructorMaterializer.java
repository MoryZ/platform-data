package com.old.silence.data.mybatis.projection;

import org.springframework.core.DefaultParameterNameDiscoverer;

import java.beans.ConstructorProperties;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class ProjectionConstructorMaterializer {

    private static final DefaultParameterNameDiscoverer PARAMETER_NAME_DISCOVERER = new DefaultParameterNameDiscoverer();

    private ProjectionConstructorMaterializer() {
    }

    static boolean requiresConstructorMaterialization(Class<?> projectionType) {
        if (projectionType.isInterface()) {
            return false;
        }
        if (projectionType.isRecord()) {
            return true;
        }
        return !hasDefaultConstructor(projectionType)
                && (Modifier.isFinal(projectionType.getModifiers()) || hasSingleConstructor(projectionType));
    }

    static <P> List<P> createList(Class<P> projectionType,
                                  ProjectionMetadata metadata,
                                  List<Map<String, Object>> sourceList) {
        List<Map<String, Object>> normalized = ProjectionValueConverter.normalizeRows(metadata, sourceList);
        List<P> results = new ArrayList<>(normalized.size());
        for (Map<String, Object> source : normalized) {
            results.add(create(projectionType, source));
        }
        return results;
    }

    static <P> P create(Class<P> projectionType, Map<String, Object> source) {
        try {
            ConstructorBinding binding = resolveConstructorBinding(projectionType);
            Object[] args = new Object[binding.parameterNames().length];
            for (int i = 0; i < binding.parameterNames().length; i++) {
                String parameterName = binding.parameterNames()[i];
                Object value = ProjectionPropertyAccessSupport.resolveValue(
                        source,
                        parameterName,
                        null,
                        binding.constructor().getParameterTypes()[i]
                );
                if (value == null && binding.constructor().getParameterTypes()[i].isPrimitive()) {
                    throw new IllegalArgumentException("Missing primitive constructor argument '" + parameterName
                            + "' for projection " + projectionType.getName());
                }
                args[i] = value;
            }
            binding.constructor().setAccessible(true);
            @SuppressWarnings("unchecked")
            P projection = (P) binding.constructor().newInstance(args);
            return projection;
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to construct projection " + projectionType.getName(), ex);
        }
    }

    private static ConstructorBinding resolveConstructorBinding(Class<?> projectionType) {
        if (projectionType.isRecord()) {
            return resolveRecordBinding(projectionType);
        }

        Constructor<?>[] constructors = projectionType.getDeclaredConstructors();
        if (constructors.length == 1) {
            Constructor<?> constructor = constructors[0];
            return new ConstructorBinding(constructor, resolveParameterNames(constructor, projectionType));
        }

        for (Constructor<?> constructor : constructors) {
            ConstructorProperties properties = constructor.getAnnotation(ConstructorProperties.class);
            if (properties != null) {
                return new ConstructorBinding(constructor, properties.value());
            }
        }

        throw new IllegalArgumentException("Constructor projection requires a single constructor, record canonical constructor,"
                + " or @ConstructorProperties annotation: " + projectionType.getName());
    }

    private static ConstructorBinding resolveRecordBinding(Class<?> projectionType) {
        try {
            RecordComponent[] components = projectionType.getRecordComponents();
            Class<?>[] parameterTypes = new Class<?>[components.length];
            String[] parameterNames = new String[components.length];
            for (int i = 0; i < components.length; i++) {
                parameterTypes[i] = components[i].getType();
                parameterNames[i] = components[i].getName();
            }
            Constructor<?> constructor = projectionType.getDeclaredConstructor(parameterTypes);
            return new ConstructorBinding(constructor, parameterNames);
        } catch (NoSuchMethodException ex) {
            throw new IllegalArgumentException("Cannot resolve canonical record constructor for " + projectionType.getName(), ex);
        }
    }

    private static String[] resolveParameterNames(Constructor<?> constructor, Class<?> projectionType) {
        String[] parameterNames = PARAMETER_NAME_DISCOVERER.getParameterNames(constructor);
        if (parameterNames == null || parameterNames.length != constructor.getParameterCount()) {
            throw new IllegalArgumentException("Constructor parameter names are required for projection "
                    + projectionType.getName() + ". Compile with -parameters or use @ConstructorProperties.");
        }
        for (String parameterName : parameterNames) {
            if (parameterName == null || parameterName.startsWith("arg")) {
                throw new IllegalArgumentException("Stable constructor parameter names are required for projection "
                        + projectionType.getName() + ". Compile with -parameters or use @ConstructorProperties.");
            }
        }
        return parameterNames;
    }

    private static boolean hasDefaultConstructor(Class<?> projectionType) {
        try {
            projectionType.getDeclaredConstructor();
            return true;
        } catch (NoSuchMethodException ex) {
            return false;
        }
    }

    private static boolean hasSingleConstructor(Class<?> projectionType) {
        return projectionType.getDeclaredConstructors().length == 1;
    }

    private record ConstructorBinding(Constructor<?> constructor, String[] parameterNames) {
    }
}