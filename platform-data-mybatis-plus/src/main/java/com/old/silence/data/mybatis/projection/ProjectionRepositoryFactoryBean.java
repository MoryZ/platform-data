package com.old.silence.data.mybatis.projection;

import org.springframework.beans.factory.FactoryBean;

import java.util.Objects;

/**
 * FactoryBean for projection repository interfaces.
 */
public class ProjectionRepositoryFactoryBean<R> implements FactoryBean<R> {

    private final Class<R> repositoryInterface;
    private ProjectionRepositoryProxyFactory proxyFactory;

    public ProjectionRepositoryFactoryBean(Class<R> repositoryInterface) {
        this.repositoryInterface = Objects.requireNonNull(repositoryInterface,
                "Repository interface must not be null");
    }

    public void setProxyFactory(ProjectionRepositoryProxyFactory proxyFactory) {
        this.proxyFactory = proxyFactory;
    }

    @Override
    public R getObject() {
        if (proxyFactory == null) {
            throw new IllegalStateException("ProjectionRepositoryProxyFactory must not be null");
        }
        return proxyFactory.create(repositoryInterface);
    }

    @Override
    public Class<?> getObjectType() {
        return repositoryInterface;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
