package com.old.silence.data.mybatis.test;

import com.old.silence.data.mybatis.projection.ProjectionRepository;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.core.GenericTypeResolver;

import java.io.Serializable;

/**
 * Base class for repository-oriented projection tests.
 *
 * @param <R> repository type
 * @param <T> entity type
 * @param <ID> id type
 */
@DataMyBatisTest
@ImportAutoConfiguration(MybatisPlusAutoConfiguration.class)
public abstract class MyBatisProjectionRepositoryTests<R extends ProjectionRepository<T, ID>, T, ID extends Serializable> {

    @Autowired
    protected R repository;

    protected final Class<R> repositoryType;
    protected final Class<T> entityType;
    protected final Class<ID> idType;

    @SuppressWarnings("unchecked")
    protected MyBatisProjectionRepositoryTests() {
        Class<?>[] arguments = GenericTypeResolver.resolveTypeArguments(getClass(), MyBatisProjectionRepositoryTests.class);
        if (arguments == null || arguments.length < 3) {
            throw new IllegalStateException("Cannot resolve generic types");
        }
        this.repositoryType = (Class<R>) arguments[0];
        this.entityType = (Class<T>) arguments[1];
        this.idType = (Class<ID>) arguments[2];
    }
}