package com.old.silence.data.mybatis.projection;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Auto-register interfaces extending ProjectionRepository in Spring Boot base packages.
 */
public class ProjectionRepositoryAutoRegistrar implements BeanDefinitionRegistryPostProcessor,
        BeanFactoryAware, ResourceLoaderAware, EnvironmentAware, PriorityOrdered {

    private BeanFactory beanFactory;
    private ResourceLoader resourceLoader;
    private Environment environment;

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        if (beanFactory == null || !AutoConfigurationPackages.has(beanFactory)) {
            return;
        }

        List<String> basePackages = AutoConfigurationPackages.get(beanFactory);
        ProjectionRepositoryScanner scanner = new ProjectionRepositoryScanner(this.environment);
        scanner.setResourceLoader(this.resourceLoader);
        scanner.addIncludeFilter(new AssignableTypeFilter(ProjectionRepository.class));

        for (String basePackage : basePackages) {
            for (BeanDefinition candidate : scanner.findCandidateComponents(basePackage)) {
                registerRepositoryBean(candidate, registry);
            }
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    private void registerRepositoryBean(BeanDefinition candidate, BeanDefinitionRegistry registry) {
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

        String beanName = StringUtils.uncapitalize(repositoryInterface.getSimpleName());
        if (registry.containsBeanDefinition(beanName)) {
            return;
        }

        RootBeanDefinition beanDefinition = new RootBeanDefinition(ProjectionRepositoryFactoryBean.class);
        beanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(0, repositoryInterface);
        beanDefinition.setAutowireMode(RootBeanDefinition.AUTOWIRE_BY_TYPE);
        beanDefinition.setTargetType(repositoryInterface);
        registry.registerBeanDefinition(beanName, beanDefinition);
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
