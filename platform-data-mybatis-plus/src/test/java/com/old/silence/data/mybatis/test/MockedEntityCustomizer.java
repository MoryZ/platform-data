package com.old.silence.data.mybatis.test;

/**
 * @author moryzang
 */
@FunctionalInterface
public interface MockedEntityCustomizer<T> {

    void customize(T entity);
}
