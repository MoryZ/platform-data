package com.old.silence.data.mybatis.test;

import org.springframework.boot.test.context.SpringBootTestContextBootstrapper;
import org.springframework.test.context.TestContextAnnotationUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Test context bootstrapper that automatically infers repository and changelog configuration
 * for tests extending MyBatisProjectionRepositoryTests.
 *
 * Convention:
 * - Repository package: Inferred from the first generic type parameter (repository class)
 * - Changelog path: Uses the shared Liquibase master changelog:
 *   classpath:/db/changelogs/changelog-master.xml
 */
class DataMyBatisTestContextBootstrapper extends SpringBootTestContextBootstrapper {

    private static final String REPOSITORY_TEST_CHANGELOG = "classpath:/db/changelogs/changelog-master.xml";

    @Override
    protected String[] getProperties(Class<?> testClass) {
        // Auto-detect repository-style tests and apply convention-based configuration
        if (isRepositoryTest(testClass)) {
            List<String> properties = new ArrayList<>();
            properties.add("spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;NON_KEYWORDS=USER;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
            properties.add("spring.datasource.driver-class-name=org.h2.Driver");
            properties.add("spring.datasource.username=sa");
            properties.add("spring.datasource.password=");
            properties.add("spring.sql.init.mode=never");
            properties.add("spring.liquibase.enabled=true");
            properties.add("spring.liquibase.change-log=" + deriveChangeLogPath());
            properties.add("mybatis-plus.type-handlers-package=com.old.silence.data.commons.handler");

            String repositoryPackage = deriveRepositoryPackage(testClass);
            if (StringUtils.hasText(repositoryPackage)) {
                properties.add("data.mybatis.repository-base-package=" + repositoryPackage);
            }

            return properties.toArray(String[]::new);
        }

        var dataMyBatisTest = TestContextAnnotationUtils.findMergedAnnotation(testClass, DataMyBatisTest.class);
        return dataMyBatisTest != null ? dataMyBatisTest.properties() : null;
    }

    private boolean isRepositoryTest(Class<?> testClass) {
        return isSubclassOf(testClass, MyBatisProjectionRepositoryTests.class);
    }

    private boolean isSubclassOf(Class<?> testClass, Class<?> targetClass) {
        try {
            return targetClass.isAssignableFrom(testClass);
        } catch (Exception e) {
            return false;
        }
    }

    private String deriveChangeLogPath() {
        return REPOSITORY_TEST_CHANGELOG;
    }

    private String deriveRepositoryPackage(Class<?> testClass) {
        Class<?>[] arguments = org.springframework.core.GenericTypeResolver.resolveTypeArguments(testClass,
            MyBatisProjectionRepositoryTests.class);
        if (arguments != null && arguments.length > 0) {
            return ClassUtils.getPackageName(arguments[0]);
        }
        return ClassUtils.getPackageName(testClass);
    }

}
