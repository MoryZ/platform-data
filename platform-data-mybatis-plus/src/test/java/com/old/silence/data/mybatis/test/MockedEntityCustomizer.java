package com.old.silence.data.mybatis.test;

/**
 * Callback interface for customizing mocked entity
 * 
 * @author moryzang
 */
@FunctionalInterface
public interface MockedEntityCustomizer<T> {

    /**
     * Customize the entity instance
     * 
     * @param entity entity instance to customize
     */
    void customize(T entity);
}
