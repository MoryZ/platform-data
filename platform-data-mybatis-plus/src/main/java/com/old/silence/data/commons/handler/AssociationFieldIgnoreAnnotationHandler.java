package com.old.silence.data.commons.handler;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Set;

import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.UnknownTypeHandler;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.core.handlers.AnnotationHandler;
import com.baomidou.mybatisplus.core.toolkit.AnnotationUtils;

/**
 * Framework-level annotation handler that treats ORM association fields
 * (e.g., JPA @OneToMany, @ManyToOne) as non-persistent for MyBatis-Plus CRUD SQL generation.
 */
public class AssociationFieldIgnoreAnnotationHandler implements AnnotationHandler {

    private static final Set<String> JPA_ASSOCIATION_ANNOTATIONS = new HashSet<>();

    static {
        JPA_ASSOCIATION_ANNOTATIONS.add("jakarta.persistence.OneToOne");
        JPA_ASSOCIATION_ANNOTATIONS.add("jakarta.persistence.OneToMany");
        JPA_ASSOCIATION_ANNOTATIONS.add("jakarta.persistence.ManyToOne");
        JPA_ASSOCIATION_ANNOTATIONS.add("jakarta.persistence.ManyToMany");
    }

    private static final TableField VIRTUAL_NON_EXIST_TABLE_FIELD = createVirtualNonExistTableField();

    @Override
    public <T extends Annotation> T getAnnotation(Field field, Class<T> annotationClass) {
        T annotation = AnnotationUtils.findFirstAnnotation(annotationClass, field);
        if (annotation != null) {
            return annotation;
        }

        if (annotationClass == TableField.class && isJpaAssociationField(field)) {
            return annotationClass.cast(VIRTUAL_NON_EXIST_TABLE_FIELD);
        }

        return null;
    }

    @Override
    public <T extends Annotation> boolean isAnnotationPresent(Field field, Class<T> annotationClass) {
        return getAnnotation(field, annotationClass) != null;
    }

    private static boolean isJpaAssociationField(Field field) {
        for (Annotation annotation : field.getDeclaredAnnotations()) {
            if (JPA_ASSOCIATION_ANNOTATIONS.contains(annotation.annotationType().getName())) {
                return true;
            }
        }
        return false;
    }

    private static TableField createVirtualNonExistTableField() {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                String methodName = method.getName();

                if ("annotationType".equals(methodName)) {
                    return TableField.class;
                }
                if ("exist".equals(methodName)) {
                    return false;
                }

                Object defaultValue = method.getDefaultValue();
                if (defaultValue != null) {
                    return defaultValue;
                }

                if ("toString".equals(methodName)) {
                    return "@TableField(exist=false)";
                }
                if ("hashCode".equals(methodName)) {
                    return 0;
                }
                if ("equals".equals(methodName)) {
                    return proxy == args[0];
                }

                Class<?> returnType = method.getReturnType();
                if (returnType == boolean.class) {
                    return false;
                }
                if (returnType == int.class) {
                    return 0;
                }
                if (returnType == String.class) {
                    return "";
                }
                if (returnType == FieldStrategy.class) {
                    return FieldStrategy.DEFAULT;
                }
                if (returnType == FieldFill.class) {
                    return FieldFill.DEFAULT;
                }
                if (returnType == JdbcType.class) {
                    return JdbcType.UNDEFINED;
                }
                if (returnType == Class.class) {
                    return UnknownTypeHandler.class;
                }
                return null;
            }
        };

        return (TableField) Proxy.newProxyInstance(
                TableField.class.getClassLoader(),
                new Class<?>[]{TableField.class},
                handler
        );
    }
}