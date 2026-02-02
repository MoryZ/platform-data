package com.old.silence.data.commons.converter;

/**
 * @author moryzang
 */
public interface JdbcQueryAttributeConverter<S, T> {
    T convert(String columnName, S source);
}