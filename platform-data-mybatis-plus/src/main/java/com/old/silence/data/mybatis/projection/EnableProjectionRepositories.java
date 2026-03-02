package com.old.silence.data.mybatis.projection;

import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enable scanning and registration for interfaces extending ProjectionRepository.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(ProjectionRepositoryRegistrar.class)
public @interface EnableProjectionRepositories {

    String[] basePackages() default {};

    Class<?>[] basePackageClasses() default {};
}
