package com.old.silence.data.mybatis.projection;

import org.springframework.core.convert.support.DefaultConversionService;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Build interface projection instances based on query result maps.
 */
final class InterfaceProjectionFactory {

    private static final DefaultConversionService CONVERSION_SERVICE = new DefaultConversionService();

    private InterfaceProjectionFactory() {
    }

    @SuppressWarnings("unchecked")
    static <P> P create(Class<P> projectionInterface, Map<String, Object> source) {
        if (projectionInterface == null || !projectionInterface.isInterface()) {
            throw new IllegalArgumentException("Projection type must be an interface");
        }

        Map<String, Object> values = new LinkedHashMap<>(source);
        InvocationHandler handler = new InterfaceProjectionInvocationHandler(projectionInterface, values);
        return (P) Proxy.newProxyInstance(
                projectionInterface.getClassLoader(),
                new Class[]{projectionInterface},
                handler
        );
    }

    static <P> List<P> createList(Class<P> projectionInterface, List<Map<String, Object>> sourceList) {
        return sourceList.stream().map(source -> create(projectionInterface, source)).collect(Collectors.toList());
    }

    private static final class InterfaceProjectionInvocationHandler implements InvocationHandler {

        private final Class<?> projectionInterface;
        private final Map<String, Object> values;

        private InterfaceProjectionInvocationHandler(Class<?> projectionInterface, Map<String, Object> values) {
            this.projectionInterface = projectionInterface;
            this.values = values;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if (method.getDeclaringClass() == Object.class) {
                return invokeObjectMethod(proxy, method, args);
            }

            if (method.getParameterCount() > 0) {
                throw new UnsupportedOperationException("Only no-arg getter methods are supported: " + method);
            }

            String propertyName = resolvePropertyName(method);
            Object rawValue = resolveValue(propertyName, method.getReturnType());
            if (rawValue == null) {
                return null;
            }

            Class<?> returnType = method.getReturnType();
            if (returnType.isInstance(rawValue)) {
                return rawValue;
            }

            if (returnType.isEnum()) {
                Object enumValue = convertToEnum(returnType, rawValue);
                if (enumValue != null) {
                    return enumValue;
                }
            }

            if (CONVERSION_SERVICE.canConvert(rawValue.getClass(), returnType)) {
                return CONVERSION_SERVICE.convert(rawValue, returnType);
            }

            throw new IllegalArgumentException("Cannot convert projection value for property '" + propertyName
                    + "' from " + rawValue.getClass().getName() + " to " + returnType.getName());
        }

        private Object resolveValue(String propertyName, Class<?> returnType) {
            if (values.containsKey(propertyName)) {
                return values.get(propertyName);
            }

            String underscore = toUnderscoreCase(propertyName);
            if (values.containsKey(underscore)) {
                return values.get(underscore);
            }

            if ((returnType == Boolean.class || returnType == boolean.class)) {
                String isStyle = "is_" + underscore;
                if (values.containsKey(isStyle)) {
                    return values.get(isStyle);
                }
            }

            String expected = normalize(propertyName);
            for (Map.Entry<String, Object> entry : values.entrySet()) {
                if (Objects.equals(normalize(entry.getKey()), expected)) {
                    return entry.getValue();
                }
            }

            return null;
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        private Object convertToEnum(Class<?> returnType, Object rawValue) {
            if (!(returnType.isEnum())) {
                return null;
            }

            if (rawValue instanceof String value) {
                try {
                    return Enum.valueOf((Class<? extends Enum>) returnType, value);
                } catch (IllegalArgumentException ignored) {
                    return null;
                }
            }

            Method getValueMethod;
            try {
                getValueMethod = returnType.getMethod("getValue");
            } catch (NoSuchMethodException ex) {
                return null;
            }

            Object[] constants = returnType.getEnumConstants();
            if (constants == null) {
                return null;
            }

            for (Object constant : constants) {
                try {
                    Object enumRawValue = getValueMethod.invoke(constant);
                    if (Objects.equals(enumRawValue, rawValue)
                            || Objects.equals(String.valueOf(enumRawValue), String.valueOf(rawValue))) {
                        return constant;
                    }
                } catch (Exception ignored) {
                    return null;
                }
            }

            return null;
        }

        private Object invokeObjectMethod(Object proxy, Method method, Object[] args) {
            String methodName = method.getName();
            if ("toString".equals(methodName)) {
                return projectionInterface.getSimpleName() + values;
            }
            if ("hashCode".equals(methodName)) {
                return Objects.hash(projectionInterface, values);
            }
            if ("equals".equals(methodName)) {
                return proxy == args[0];
            }
            throw new UnsupportedOperationException("Unsupported Object method: " + method);
        }

        private String resolvePropertyName(Method method) {
            String methodName = method.getName();
            if (methodName.startsWith("get") && methodName.length() > 3) {
                return decapitalize(methodName.substring(3));
            }
            if (methodName.startsWith("is") && methodName.length() > 2) {
                return decapitalize(methodName.substring(2));
            }
            throw new IllegalArgumentException("Unsupported projection method (getter required): " + method);
        }

        private String decapitalize(String value) {
            if (value.length() == 1) {
                return value.toLowerCase();
            }
            return Character.toLowerCase(value.charAt(0)) + value.substring(1);
        }

        private String toUnderscoreCase(String camelCase) {
            return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
        }

        private String normalize(String key) {
            if (key == null) {
                return null;
            }
            String normalized = key.toLowerCase().replace("_", "");
            if (normalized.startsWith("is") && normalized.length() > 2) {
                return normalized.substring(2);
            }
            return normalized;
        }
    }
}
