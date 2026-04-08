package com.old.silence.data.mybatis.projection;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static net.bytebuddy.matcher.ElementMatchers.isAbstract;
import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;

/**
 * Build mapper implementations at runtime through ByteBuddy.
 */
public class ProjectionMapperByteBuddyFactory {

    private final ProjectionRepositoryFactory repositoryFactory;
    private final Map<ClassLoader, ConcurrentMap<String, WeakReference<Object>>> cacheByClassLoader =
            Collections.synchronizedMap(new java.util.WeakHashMap<>());

    public ProjectionMapperByteBuddyFactory(ProjectionRepositoryFactory repositoryFactory) {
        this.repositoryFactory = Objects.requireNonNull(repositoryFactory, "Repository factory must not be null");
    }

    @SuppressWarnings("unchecked")
    public <M, T> M create(Class<M> mapperInterface, Class<T> entityType) {
        if (mapperInterface == null || !mapperInterface.isInterface()) {
            throw new IllegalArgumentException("Mapper type must be an interface");
        }
        Objects.requireNonNull(entityType, "Entity type must not be null");
        validateMapperInterface(mapperInterface);

        ClassLoader classLoader = mapperInterface.getClassLoader();
        String cacheKey = mapperInterface.getName() + "#" + entityType.getName();

        ConcurrentMap<String, WeakReference<Object>> cacheSegment = getOrCreateCacheSegment(classLoader);
        Object cached = dereference(cacheSegment.get(cacheKey));
        if (cached != null) {
            return (M) cached;
        }

        synchronized (cacheSegment) {
            cached = dereference(cacheSegment.get(cacheKey));
            if (cached != null) {
                return (M) cached;
            }
            cleanupStaleEntries(cacheSegment);

            ProjectionRepository<T, Serializable> repository = repositoryFactory.create(entityType);
            ProjectionMapperInterceptor<T> interceptor = new ProjectionMapperInterceptor<>(repository);

            try {
                Class<?> loadedType = new ByteBuddy()
                        .subclass(Object.class)
                        .implement(mapperInterface)
                        .method(isDeclaredBy(mapperInterface).and(isAbstract()))
                        .intercept(MethodDelegation.to(interceptor))
                        .make()
                        .load(classLoader, ClassLoadingStrategy.Default.INJECTION)
                        .getLoaded();

                Class<? extends M> generatedType = loadedType.asSubclass(mapperInterface);

                M instance = generatedType.getDeclaredConstructor().newInstance();
                cacheSegment.put(cacheKey, new WeakReference<>(instance));
                return instance;
            } catch (Exception ex) {
                throw new IllegalStateException("Failed to create projection mapper for " + mapperInterface.getName(), ex);
            }
        }
    }

    private ConcurrentMap<String, WeakReference<Object>> getOrCreateCacheSegment(ClassLoader classLoader) {
        synchronized (cacheByClassLoader) {
            return cacheByClassLoader.computeIfAbsent(classLoader, key -> new ConcurrentHashMap<>());
        }
    }

    private void cleanupStaleEntries(ConcurrentMap<String, WeakReference<Object>> cacheSegment) {
        cacheSegment.entrySet().removeIf(entry -> dereference(entry.getValue()) == null);
    }

    private Object dereference(WeakReference<Object> ref) {
        return ref == null ? null : ref.get();
    }

    private void validateMapperInterface(Class<?> mapperInterface) {
        for (Method method : mapperInterface.getMethods()) {
            if (method.getDeclaringClass() == Object.class || !Modifier.isAbstract(method.getModifiers())) {
                continue;
            }

            if (!"findByQuery".equals(method.getName())) {
                throw new IllegalArgumentException("Unsupported mapper method: " + method
                        + ", only findByQuery is supported");
            }
            validateFindByQueryMethod(method);
        }
    }

    private void validateFindByQueryMethod(Method method) {
        Class<?>[] paramTypes = method.getParameterTypes();

        if (paramTypes.length < 2 || paramTypes.length > 3) {
            throw invalidFindByQuerySignature(method,
                "parameter count must be 2 or 3, but was " + paramTypes.length);
        }

        if (!Wrapper.class.isAssignableFrom(paramTypes[0])) {
            throw invalidFindByQuerySignature(method,
                "first argument must be QueryWrapper/LambdaQueryWrapper, but was " + paramTypes[0].getSimpleName());
        }

        if (paramTypes.length == 2) {
            if (paramTypes[1] != Class.class || !List.class.isAssignableFrom(method.getReturnType())) {
                throw invalidFindByQuerySignature(method,
                        "for 2 parameters, signature must be (queryWrapper, Class) and return type must be List");
            }
            return;
        }

        if (paramTypes.length == 3) {
            if (Page.class.isAssignableFrom(paramTypes[1]) && paramTypes[2] == Class.class
                    && IPage.class.isAssignableFrom(method.getReturnType())) {
                return;
            }
            throw invalidFindByQuerySignature(method,
                    "for 3 parameters, signature must be (queryWrapper, Page, Class)");
        }
    }

    private IllegalArgumentException invalidFindByQuerySignature(Method method, String reason) {
        return new IllegalArgumentException("Invalid findByQuery method signature: " + method + ", " + reason);
    }

    public static class ProjectionMapperInterceptor<T> {

        private final ProjectionRepository<T, Serializable> repository;

        public ProjectionMapperInterceptor(ProjectionRepository<T, Serializable> repository) {
            this.repository = repository;
        }

        @RuntimeType
        public Object invoke(@Origin Method method, @AllArguments Object[] args) {
            if (!"findByQuery".equals(method.getName())) {
                throw new UnsupportedOperationException("Unsupported mapper method: " + method);
            }
            return invokeFindByQuery(args);
        }

        private Object invokeFindByQuery(Object[] args) {
            if (args.length == 2 && args[0] instanceof Wrapper<?> wrapper && args[1] instanceof Class<?>) {
                @SuppressWarnings("unchecked")
                Wrapper<T> typedWrapper = (Wrapper<T>) wrapper;
                return repository.findByQuery(typedWrapper, (Class<?>) args[1]);
            }

            if (args.length == 3 && args[0] instanceof Wrapper<?> wrapper && args[1] instanceof Page<?> && args[2] instanceof Class<?>) {
                @SuppressWarnings("unchecked")
                Wrapper<T> typedWrapper = (Wrapper<T>) wrapper;
                return repository.findByQuery(typedWrapper, (Page<?>) args[1], (Class<?>) args[2]);
            }

            throw new IllegalArgumentException("Unsupported findByQuery arguments");
        }
    }
}
