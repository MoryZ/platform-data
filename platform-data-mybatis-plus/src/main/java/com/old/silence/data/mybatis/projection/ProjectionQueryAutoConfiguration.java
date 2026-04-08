package com.old.silence.data.mybatis.projection;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;

/**
 * Auto configuration for projection query support.
 */
@AutoConfiguration
@ConditionalOnBean(SqlSessionFactory.class)
public class ProjectionQueryAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public static ProjectionRepositoryAutoRegistrar projectionRepositoryAutoRegistrar() {
        return new ProjectionRepositoryAutoRegistrar();
    }

    @Bean
    @ConditionalOnMissingBean
    public ProjectionMetadataResolver projectionMetadataResolver(SqlSessionFactory sqlSessionFactory) {
        ProjectionMetadataResolver resolver = new ProjectionMetadataResolver();
        resolver.setConfiguration(sqlSessionFactory.getConfiguration());
        return resolver;
    }

    @Bean
    @ConditionalOnMissingBean
    public ProjectionResultMapRegistry projectionResultMapRegistry() {
        return new ProjectionResultMapRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public ProjectionQueryExecutor projectionQueryExecutor(SqlSessionFactory sqlSessionFactory,
                                                           ProjectionResultMapRegistry resultMapRegistry) {
        return new ProjectionQueryExecutor(sqlSessionFactory, resultMapRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    public ProjectionRepositoryFactory projectionRepositoryFactory(ProjectionMetadataResolver metadataResolver,
                                                                   ProjectionQueryExecutor queryExecutor) {
        return new ProjectionRepositoryFactory(metadataResolver, queryExecutor);
    }

    @Bean
    @ConditionalOnMissingBean
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
    @ConditionalOnMissingBean
    public ProjectionQueryOperations projectionQueryOperations(ProjectionRepositoryFactory projectionRepositoryFactory) {
        return new ProjectionQueryOperations(projectionRepositoryFactory);
    }

    @Bean
    @ConditionalOnMissingBean
    public SimpleProjectionQueryRepositoryFactory simpleProjectionQueryRepositoryFactory(
            ProjectionQueryOperations projectionQueryOperations) {
        return new SimpleProjectionQueryRepositoryFactory(projectionQueryOperations);
    }

    @Bean
    @ConditionalOnMissingBean
    public ProjectionMapperByteBuddyFactory projectionMapperByteBuddyFactory(
            ProjectionRepositoryFactory projectionRepositoryFactory) {
        return new ProjectionMapperByteBuddyFactory(projectionRepositoryFactory);
    }
}
