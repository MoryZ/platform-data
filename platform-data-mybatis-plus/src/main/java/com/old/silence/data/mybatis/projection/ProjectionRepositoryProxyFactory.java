package com.old.silence.data.mybatis.projection;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.binding.BindingException;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.core.ResolvableType;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Proxy factory for interfaces extending ProjectionRepository.
 */
public class ProjectionRepositoryProxyFactory {

    private final ProjectionRepositoryFactory repositoryFactory;
    private final SqlSessionFactory sqlSessionFactory;
    private final SqlSessionTemplate sqlSessionTemplate;
    private final Map<Class<?>, Object> singletonCache = new ConcurrentHashMap<>();

    public ProjectionRepositoryProxyFactory(ProjectionRepositoryFactory repositoryFactory) {
        this(repositoryFactory, null, null);
    }

    public ProjectionRepositoryProxyFactory(ProjectionRepositoryFactory repositoryFactory,
                                            SqlSessionFactory sqlSessionFactory,
                                            SqlSessionTemplate sqlSessionTemplate) {
        this.repositoryFactory = Objects.requireNonNull(repositoryFactory,
                "ProjectionRepositoryFactory must not be null");
        this.sqlSessionFactory = sqlSessionFactory;
        this.sqlSessionTemplate = sqlSessionTemplate;
    }

    @SuppressWarnings("unchecked")
    public <R> R create(Class<R> repositoryInterface) {
        Objects.requireNonNull(repositoryInterface, "Repository interface must not be null");
        if (!repositoryInterface.isInterface()) {
            throw new IllegalArgumentException("Repository type must be an interface");
        }
        if (!ProjectionRepository.class.isAssignableFrom(repositoryInterface)) {
            throw new IllegalArgumentException("Repository interface must extend ProjectionRepository: "
                    + repositoryInterface.getName());
        }
        if (repositoryInterface == ProjectionRepository.class) {
            throw new IllegalArgumentException("Repository interface must not be ProjectionRepository itself");
        }

        return (R) singletonCache.computeIfAbsent(repositoryInterface, this::createProxy);
    }

    private Object createProxy(Class<?> repositoryInterface) {
        Class<?> domainType = resolveDomainType(repositoryInterface);
        Object mapperDelegate = createMapperDelegate(repositoryInterface);
        ProjectionRepository<?, ?> delegate = repositoryFactory.create(domainType);

        InvocationHandler handler = (proxy, method, args) ->
            invoke(proxy, method, args, repositoryInterface, mapperDelegate, delegate);

        return Proxy.newProxyInstance(
                repositoryInterface.getClassLoader(),
                new Class<?>[]{repositoryInterface},
                handler
        );
    }

    private Object invoke(Object proxy,
                          Method method,
                          Object[] args,
                          Class<?> repositoryInterface,
                          Object mapperDelegate,
                          ProjectionRepository<?, ?> delegate) throws Throwable {
        if (method.getDeclaringClass() == Object.class) {
            return invokeObjectMethod(proxy, method, args, repositoryInterface);
        }

        if (method.isDefault()) {
            return InvocationHandler.invokeDefault(proxy, method, args);
        }

        Throwable lastError = null;
        if (mapperDelegate != null) {
            try {
                return method.invoke(mapperDelegate, args);
            } catch (InvocationTargetException ex) {
                Throwable targetException = ex.getTargetException();
                if (isInvalidBoundStatement(targetException)) {
                    lastError = targetException;
                } else {
                    throw targetException;
                }
            } catch (Throwable ex) {
                if (isBindingException(ex)) {
                    lastError = ex;
                } else {
                    throw ex;
                }
            }
        }

        Method delegateMethod = resolveProjectionRepositoryMethod(method);
        if (delegateMethod == null) {
            if (lastError != null) {
                throw new UnsupportedOperationException(
                        "Unsupported repository method: " + method
                                + ", not found in MyBatis mapped statements and not defined by ProjectionRepository",
                        lastError
                );
            }
            throw new UnsupportedOperationException(
                "Unsupported repository method: " + method
                    + ", not found in MyBatis mapped statements and not defined by ProjectionRepository"
            );
        }
        return delegateMethod.invoke(delegate, args);
    }

    private Object createMapperDelegate(Class<?> repositoryInterface) {
        if (sqlSessionFactory == null || sqlSessionTemplate == null) {
            return null;
        }

        // Create mapper delegate if interface has @Mapper or any MyBatis SQL annotations
        boolean hasMapperAnnotation = repositoryInterface.isAnnotationPresent(Mapper.class);
        boolean hasMyBatisMethods = hasMyBatisSqlAnnotations(repositoryInterface);

        if (!hasMapperAnnotation && !hasMyBatisMethods) {
            return null;
        }

        Configuration configuration = sqlSessionFactory.getConfiguration();
        if (!configuration.hasMapper(repositoryInterface)) {
            synchronized (configuration) {
                if (!configuration.hasMapper(repositoryInterface)) {
                    configuration.addMapper(repositoryInterface);
                }
            }
        }

        return sqlSessionTemplate.getMapper(repositoryInterface);
    }

    private boolean hasMyBatisSqlAnnotations(Class<?> repositoryInterface) {
        for (Method method : repositoryInterface.getDeclaredMethods()) {
            if (method.isAnnotationPresent(org.apache.ibatis.annotations.Select.class)
                    || method.isAnnotationPresent(org.apache.ibatis.annotations.Insert.class)
                    || method.isAnnotationPresent(org.apache.ibatis.annotations.Update.class)
                    || method.isAnnotationPresent(org.apache.ibatis.annotations.Delete.class)) {
                return true;
            }
        }
        return false;
    }

    private boolean isInvalidBoundStatement(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof BindingException
                    && current.getMessage() != null
                    && current.getMessage().contains("Invalid bound statement")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean isBindingException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof BindingException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private Method resolveProjectionRepositoryMethod(Method method) {
        Method[] methods = ProjectionRepository.class.getMethods();
        for (Method candidate : methods) {
            if (candidate.getDeclaringClass() == Object.class) {
                continue;
            }
            if (!candidate.getName().equals(method.getName())) {
                continue;
            }
            if (Modifier.isStatic(candidate.getModifiers())) {
                continue;
            }
            if (parametersCompatible(candidate.getParameterTypes(), method.getParameterTypes())) {
                return candidate;
            }
        }
        return null;
    }

    private boolean parametersCompatible(Class<?>[] candidateTypes, Class<?>[] methodTypes) {
        if (candidateTypes.length != methodTypes.length) {
            return false;
        }

        for (int i = 0; i < candidateTypes.length; i++) {
            Class<?> candidateType = wrapPrimitive(candidateTypes[i]);
            Class<?> methodType = wrapPrimitive(methodTypes[i]);
            if (!candidateType.isAssignableFrom(methodType)) {
                return false;
            }
        }
        return true;
    }

    private Class<?> wrapPrimitive(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == int.class) {
            return Integer.class;
        }
        if (type == long.class) {
            return Long.class;
        }
        if (type == boolean.class) {
            return Boolean.class;
        }
        if (type == byte.class) {
            return Byte.class;
        }
        if (type == short.class) {
            return Short.class;
        }
        if (type == char.class) {
            return Character.class;
        }
        if (type == float.class) {
            return Float.class;
        }
        if (type == double.class) {
            return Double.class;
        }
        return Void.class;
    }

    private Object invokeObjectMethod(Object proxy,
                                      Method method,
                                      Object[] args,
                                      Class<?> repositoryInterface) {
        String name = method.getName();
        return switch (name) {
            case "toString" -> repositoryInterface.getSimpleName() + " proxy";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> args != null && args.length == 1 && proxy == args[0];
            default -> throw new UnsupportedOperationException("Unsupported Object method: " + name);
        };
    }

    private Class<?> resolveDomainType(Class<?> repositoryInterface) {
        ResolvableType type = ResolvableType.forClass(repositoryInterface)
                .as(ProjectionRepository.class)
                .getGeneric(0);
        Class<?> resolved = type.resolve();
        if (resolved == null) {
            throw new IllegalArgumentException("Cannot resolve entity type for repository: "
                    + repositoryInterface.getName());
        }
        return resolved;
    }
}
