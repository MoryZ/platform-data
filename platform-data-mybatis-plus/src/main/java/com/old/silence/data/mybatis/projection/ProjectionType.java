package com.old.silence.data.mybatis.projection;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Parameter-level metadata for projection type argument.
 */
@Target({ElementType.TYPE, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ProjectionType {

    String PARAMETER_NAME = "fileds";

    Class<?> value() default Void.class;
}
