package com.old.silence.data.mybatis.projection;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;

/**
 * Auto configuration for projection query support.
 */
@AutoConfiguration
@ConditionalOnBean(SqlSessionFactory.class)
public class ProjectionQueryAutoConfiguration {

    @Bean
    public static ProjectionRepositoryAutoRegistrar projectionRepositoryAutoRegistrar() {
        return new ProjectionRepositoryAutoRegistrar();
    }

    @Bean
    public ProjectionMetadataResolver projectionMetadataResolver() {
        return new ProjectionMetadataResolver();
    }

    @Bean
    public ProjectionResultMapRegistry projectionResultMapRegistry() {
        return new ProjectionResultMapRegistry();
    }

    @Bean
    public ProjectionQueryExecutor projectionQueryExecutor(SqlSessionFactory sqlSessionFactory,
                                                           ProjectionResultMapRegistry resultMapRegistry) {
        return new ProjectionQueryExecutor(sqlSessionFactory, resultMapRegistry);
    }

    @Bean
    public ProjectionRepositoryFactory projectionRepositoryFactory(ProjectionMetadataResolver metadataResolver,
                                                                   ProjectionQueryExecutor queryExecutor) {
        return new ProjectionRepositoryFactory(metadataResolver, queryExecutor);
    }

    @Bean
    public ProjectionRepositoryProxyFactory projectionRepositoryProxyFactory(
            ProjectionRepositoryFactory projectionRepositoryFactory,
            ObjectProvider<SqlSessionFactory> sqlSessionFactoryProvider,
            ObjectProvider<SqlSessionTemplate> sqlSessionTemplateProvider) {
        return new ProjectionRepositoryProxyFactory(
                projectionRepositoryFactory,
                sqlSessionFactoryProvider.getIfAvailable(),
                sqlSessionTemplateProvider.getIfAvailable()
        );
    }

    @Bean
    public ProjectionQueryOperations projectionQueryOperations(ProjectionRepositoryFactory projectionRepositoryFactory) {
        return new ProjectionQueryOperations(projectionRepositoryFactory);
    }

    @Bean
    public SimpleProjectionQueryRepositoryFactory simpleProjectionQueryRepositoryFactory(
            ProjectionQueryOperations projectionQueryOperations) {
        return new SimpleProjectionQueryRepositoryFactory(projectionQueryOperations);
    }

    @Bean
    public ProjectionMapperByteBuddyFactory projectionMapperByteBuddyFactory(
            ProjectionRepositoryFactory projectionRepositoryFactory) {
        return new ProjectionMapperByteBuddyFactory(projectionRepositoryFactory);
    }
}
