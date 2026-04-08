package com.old.silence.data.mybatis.test;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.OverrideAutoConfiguration;
import org.springframework.boot.test.autoconfigure.core.AutoConfigureCache;
import org.springframework.boot.test.autoconfigure.filter.TypeExcludeFilters;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureJdbc;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.core.annotation.AliasFor;
import org.springframework.core.env.Environment;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import com.old.silence.data.mybatis.test.autoconfigure.AutoConfigureDataMyBatis;
import com.old.silence.data.mybatis.projection.ProjectionQueryAutoConfiguration;

/**
 * @author moryzang
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@BootstrapWith(DataMyBatisTestContextBootstrapper.class)
@ExtendWith(SpringExtension.class)
@OverrideAutoConfiguration(enabled =  false)
@TypeExcludeFilters(DataMyBatisTypeExcludeFilter.class)
@Transactional
@AutoConfigureCache
@AutoConfigureJdbc
@AutoConfigureDataMyBatis
@AutoConfigureTestDatabase
@ContextConfiguration(classes = DataMyBatisTestApplication.class)
@ImportAutoConfiguration(ProjectionQueryAutoConfiguration.class)
public @interface DataMyBatisTest {

    /**
     * Properties in from {@literal key=value} that should be added to the Spring
     * {@link Environment} before the test runs.
     *
     * @return the properties to add
     */
    String[] properties() default {};

    /**
     * Determines if default filtering should be used with
     * {@link SpringBootApplication @SpringBootApplication}. By default, only
     * {@code AbstractJdbcConfiguration} beans are included.
     *
     * @see #includeFilters()
     * @see #excludeFilters()
     * @return if default filters should be used.
     */
    boolean useDefaultFilters() default true;

    /**
     * A set of include filters which can be used to add otherwise filtered beans to the
     * application context.
     *
     * @return include filters to apply
     */
    Filter[] includeFilters() default {};

    /**
     * A set of exclude filters which can be used to filtered beans that would otherwise be
     * added to the application context.
     *
     * @return exclude filters to apply
     */
    Filter[] excludeFilters() default {};

    /**
     * Auto-configuration exclusions that should be applied for this test.
     *
     * @return auto-configuration exclusions to apply
     */
    @AliasFor(annotation = ImportAutoConfiguration.class, attribute = "exclude")
    Class<?>[] excludeAutoConfiguration() default {};

}
