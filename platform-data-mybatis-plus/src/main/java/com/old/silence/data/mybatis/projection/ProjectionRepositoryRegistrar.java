package com.old.silence.data.mybatis.projection;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import org.apache.ibatis.annotations.Mapper;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Registrar for projection repositories.
 */
public class ProjectionRepositoryRegistrar implements ImportBeanDefinitionRegistrar, ResourceLoaderAware, EnvironmentAware {

    private ResourceLoader resourceLoader;
    private Environment environment;

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        AnnotationAttributes attributes = AnnotationAttributes.fromMap(
                importingClassMetadata.getAnnotationAttributes(EnableProjectionRepositories.class.getName(), false));
        if (attributes == null) {
            return;
        }

        Set<String> basePackages = resolveBasePackages(attributes, importingClassMetadata);
        ProjectionRepositoryScanner scanner = new ProjectionRepositoryScanner(this.environment);
        scanner.setResourceLoader(this.resourceLoader);
        scanner.addIncludeFilter(new AssignableTypeFilter(ProjectionRepository.class));

        BeanNameGenerator beanNameGenerator = (definition, beanDefinitionRegistry) -> {
            String className = definition.getBeanClassName();
            if (!StringUtils.hasText(className)) {
                return null;
            }
            String shortName = ClassUtils.getShortName(className);
            return StringUtils.uncapitalize(shortName);
        };

        for (String basePackage : basePackages) {
            for (BeanDefinition candidate : scanner.findCandidateComponents(basePackage)) {
                registerRepositoryBean(candidate, registry, beanNameGenerator);
            }
        }
    }

    private void registerRepositoryBean(BeanDefinition candidate,
                                        BeanDefinitionRegistry registry,
                                        BeanNameGenerator beanNameGenerator) {
        String className = candidate.getBeanClassName();
        if (!StringUtils.hasText(className)) {
            return;
        }

        ClassLoader classLoader = this.resourceLoader != null
                ? this.resourceLoader.getClassLoader()
                : ClassUtils.getDefaultClassLoader();

        Class<?> repositoryInterface;
        try {
            repositoryInterface = ClassUtils.forName(className, classLoader);
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException("Failed to load projection repository interface: " + className, ex);
        }

        if (!repositoryInterface.isInterface() || repositoryInterface == ProjectionRepository.class) {
            return;
        }

        if (repositoryInterface.isAnnotationPresent(Mapper.class)) {
            return;
        }

        RootBeanDefinition beanDefinition = new RootBeanDefinition(ProjectionRepositoryFactoryBean.class);
        beanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(0, repositoryInterface);
        beanDefinition.setAutowireMode(RootBeanDefinition.AUTOWIRE_BY_TYPE);
        beanDefinition.setTargetType(repositoryInterface);

        String beanName = beanNameGenerator.generateBeanName(candidate, registry);
        if (!StringUtils.hasText(beanName)) {
            beanName = StringUtils.uncapitalize(repositoryInterface.getSimpleName());
        }
        if (!registry.containsBeanDefinition(beanName)) {
            registry.registerBeanDefinition(beanName, beanDefinition);
        }
    }

    private Set<String> resolveBasePackages(AnnotationAttributes attributes, AnnotationMetadata importingClassMetadata) {
        Set<String> basePackages = new LinkedHashSet<>();

        for (String pkg : attributes.getStringArray("basePackages")) {
            if (StringUtils.hasText(pkg)) {
                basePackages.add(pkg);
            }
        }

        for (Class<?> type : attributes.getClassArray("basePackageClasses")) {
            basePackages.add(ClassUtils.getPackageName(type));
        }

        if (basePackages.isEmpty()) {
            basePackages.add(ClassUtils.getPackageName(importingClassMetadata.getClassName()));
        }

        return basePackages;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    private static class ProjectionRepositoryScanner extends ClassPathScanningCandidateComponentProvider {

        ProjectionRepositoryScanner(Environment environment) {
            super(false, environment);
        }

        @Override
        protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
            AnnotationMetadata metadata = beanDefinition.getMetadata();
            return metadata.isIndependent() && metadata.isInterface();
        }
    }
}
