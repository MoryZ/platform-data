package com.old.silence.data.commons.annotation;


import com.old.silence.data.commons.converter.Part;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author moryzang
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface RelationalQueryProperty {

    String name() default "";

    Part.Type type();

    boolean ignoreCase() default false;

    boolean nullable() default false;
}

