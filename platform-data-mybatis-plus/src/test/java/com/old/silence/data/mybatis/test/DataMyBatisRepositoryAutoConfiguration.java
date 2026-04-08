package com.old.silence.data.mybatis.test;

import com.old.silence.data.mybatis.projection.EnableProjectionRepositories;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * Auto-configuration for repository-oriented MyBatis tests when test class extends MyBatisProjectionRepositoryTests.
 * This configuration is automatically imported via SPI (spring.factories / spring/xxx.imports).
 *
 * When a test class extends MyBatisProjectionRepositoryTests, the repository package is inferred from
 * the test class's generic type parameters and Liquibase is initialized from the shared master changelog.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(MyBatisProjectionRepositoryTests.class)
@ConditionalOnProperty(name = "data.mybatis.repository-base-package")
@EnableProjectionRepositories(basePackages = "${data.mybatis.repository-base-package:com.old.silence.data.mybatis.test}")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DataMyBatisRepositoryAutoConfiguration {
    // Marker configuration that enables projection repository scanning.
    // The basePackages is populated via environment property 'data.mybatis.repository-base-package'
    // which is set by DataMyBatisTestContextBootstrapper during test initialization.
}
