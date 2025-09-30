package com.old.silence.data.commons.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.old.silence.data.commons.converter.JdbcQueryAttributeConverter;

/**
 * @author moryzang
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface QueryConvert {
    Class<? extends JdbcQueryAttributeConverter<?, ?>> converter();
}