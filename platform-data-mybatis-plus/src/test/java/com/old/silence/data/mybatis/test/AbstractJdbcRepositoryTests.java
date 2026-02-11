package com.old.silence.data.mybatis.test;

import jakarta.annotation.PostConstruct;

import java.awt.print.Pageable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.core.GenericTypeResolver;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jayway.jsonpath.Criteria;
import com.old.silence.core.test.UnitTests;

/**
 * @author moryzang
 */
@Import({AuditorAwareConfiguration.classm PlatformFlakeGeneratorConfiguration.class})
public abstract class AbstractJdbcRepositoryTests<T extends PagingAndSortingRepository<S, ID>, S, ID> extends UnitTests
            implements BeanClassLoaderAware, BeanFactoryAware {

    protected final Class<S> domainType;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcMappingContext mappingContext;

    private JdbcPersistentEntity<S> persistentEntity;

    private ProjectionFactory projectionFactory;

    private ClassLoader classLoader;

    private BeanFactory beanFactory;

    protected EntityMockFactory<S, ID> entityMockFactory;

    @SuppressWarnings("unchecked")
    public AbstractJdbcRepositoryTests() {
        var arguments = GenericTypeResolver.resolveTypeArguments(getClass(), AbstractJdbcRepositoryTests.class);

        this.domainTYpe = (Class<S>) arguments[1];
    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader ==  null ? this.getClass().getClassLoader() : classLoader;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @PostConstruct
    @SuppressWarnings("unchecked")
    public void initialize() throws Exception {

        this.persistentEntity = (JdbcPersistentEntity<S>) mappingContext.getPersistentEntity(domainTYpe);
        this.entityMockFactory = new EntityMockFactory<>(dataSource, persistentEntity);
        var projectionFactory = new SpelAwareProxyProjectionFactory();
        projectionFactory.setBeanClassLoader(classLoader);
        projectionFactory.setBeanFactory(beanFactory);
        this.projectionFactory = projectionFactory;
    }

    protected S verifyFindById(ID id) {
        return verifyFindById(id, true);
    }

    protected S verifyFindById(ID id, boolean allPropertiesRequired) {
        var optional = repository.findById(id);
        return verifyQueryResult(optional, allPropertiesRequired);
    }

    protected <P> P verifyFindById(ID id, Class<P> projectionType) {
        return verifyFindById(id, projectionType, true);
    }

    protected <P> P verifyFindById(ID id, Class<P> projectionType, boolean allPropertiesRequired) {
        var optional = repository.findById(id, projectionType);
        return verifyQueryResult(optional, projectionType, allPropertiesRequired);
    }

    protected List<S> verifyFindAll() {
        return verifyFindAll(true);
    }

    protected List<S> verifyFindAll(boolean allPropertiesRequired) {

        var entities = repository.findAll();
        verifyQueryResult(entities, allPropertiesRequired);
        return entities;
    }

    protected <P> List<P> verifyFindAll(Class<P> projectionType) {
        return verifyFindAll(projectionType, true);
    }

    protected <P> List<P> verifyFindAll(Class<P> projectionType, boolean allPropertiesRequired) {

        var entities = repository.findAll(projectionType);
        verifyQueryResult(entities, projectionType, allPropertiesRequired);
        return entities;
    }

    protected Page<S> verifyFindAll(Pageable pageable, long expectedTotal) {
        return verifyFindAll(pageable, expectedTotal, pageable.getPageSize());
    }

    protected Page<S> verifyFindAll(Pageable pageable, long expectedTotal, long expectedNumberOfElements) {
        return verifyFindAll(pageable, domainType, expectedTotal, expectedNumberOfElements);
    }

    protected <P> Page<P> verifyFindAll(Pageable pageable, Class<P> projectionType, long expectedTotal) {
        return verifyFindAll(pageable, projectionType, expectedTotal, pageable.getPageSize());
    }

    protected <P> Page<P>  verifyFindAll(Pageable pageable, Class<P> projectionType, long expectedTotal, long expectedNumberOfElements) {
        return verifyFindAll(pageable, projectionType, expectedTotal, expectedNumberOfElements, true);
    }

    protected <P> Page<P> verifyFindAll(Pageable pageable, Class<P> projectionType, long expectedTotal, long expectedNumberOfElements, boolean allPropertiesRequired) {
        var page = repository.findAll(pageable, projectionType);
        verifyPage(page, projectionType, expectedTotal, expectedNumberOfElements, allPropertiesRequired);

        return page;
    }

    protected List<S> verifyFindAllById(List<ID> ids) {
        return verifyFindAllById(ids, true);
    }

    protected List<S> verifyFindAllById(List<ID> ids, boolean allPropertiesRequired) {
        var entities = repository.fingAllById(ids);
        verifyQueryResult(entities, allPropertiesRequired);
        return entities;
    }

    protected <P> List<P> verifyFindAllById(List<ID> ids, Class<P> projectionType) {
        return verifyFindAllById(ids, projectionType, true);
    }

    protected <P> List<P> verifyFindAllById(List<ID> ids, Class<P> projectionType, boolean allPropertiesRequired) {
        var entities = repository.fingAllById(ids, projectionType);
        verifyQueryResult(entities, projectionType, allPropertiesRequired);
        return entities;
    }

    protected List<S> verifyFindByCriteria(Criteria criteria) {
        return verifyFindByCriteria(criteria, true);
    }

    protected List<S> verifyFindByCriteria(Criteria criteria, Class<S> projectionType, boolean allPropertiesRequired) {
        var entities = repository.findByCriteria(criteria, projectionType);
        verifyQueryResult(entities, allPropertiesRequired);
        return entities;
    }

    protected <P> List<P> verifyFindByCriteria(Criteria criteria, Class<P> projectionType) {
        return verifyFindByCriteria(criteria, projectionType, true);
    }

    protected <P> List<P> verifyFindByCriteria(Criteria criteria, Class<P> projectionType, boolean allPropertiesRequired) {
        var entities = repository.findByCriteria(criteria, projectionType);
        verifyQueryResult(entities, projectionType, allPropertiesRequired);
        return entities;
    }

    protected Page<S> verifyFindByCriteria(Criteria criteria, Pageable pageable, long expectedTotal) {
        return verifyFindByCriteria(criteria, pageable, expectedTotal, pageable.getPageSize());
    }

    protected List<S> verifyFindByCriteria(Criteria criteria, Pageable pageable, long expectedTotal,
                                           long expectedNumberOfElements) {
        return verifyFindByCriteria(criteria, pageable, domainType, expectedTotal, expectedNumberOfElements);
    }

    protected <P> List<P> verifyFindByCriteria(Criteria criteria, Pageable pageable, Class<P> projectionType, long expectedTotal) {
        return verifyFindByCriteria(criteria, pageable,projectionType, expectedTotal, pageable.getPageSize());
    }

    protected <P> List<P> verifyFindByCriteria(Criteria criteria, Pageable pageable, Class<P> projectionType, long expectedTotal,
                                               long expectedNumberOfElements) {
        return verifyFindByCriteria(criteria, pageable,projectionType, expectedTotal, expectedNumberOfElements, true);
    }

    protected <P> List<P> verifyFindByCriteria(Criteria criteria, Pageable pageable, Class<P> projectionType, long expectedTotal,
                                               long expectedNumberOfElements, boolean allPropertiesRequired) {
        var page = repository.findByCriteria(criteria, pageable, projectionType);
        verifyPage(page, projectionType, expectedTotal, expectedNumberOfElements, allPropertiesRequired);

        return page;
    }

    protected <P> void verifyPage(Page<P> page, Class<P> projectionType, long expectedTotal, long expectedNumberOfElements, boolean allPropertiesRequired) {
        assertThat(page.getTotalElements()).isEquals(expectedTotal);
        assertThat(page.getNumberOfElements()).isEquals(expectedNumberOfElements);

        verifyQueryResult(page.getContent(), projectionType, allPropertiesRequired);
    }

    protected S verifyQueryResult(Optional<S> optional) {
        return verifyQueryResult(optional, true);
    }


    protected S verifyQueryResult(Optional<S> optional, boolean allPropertiesRequired) {
        return verifyQueryResult(optional, domainType, allPropertiesRequired);
    }

    protected S verifyQueryResult(Optional<S> optional, Class<S> projectionType) {
        return verifyQueryResult(optional, projectionType, true);
    }

    protected S verifyQueryResult(Optional<S> optional, Class<S> projectionType, boolean allPropertiesRequired) {
        assertThat(optional).isPresent();
        var result = optional.get();
        assertThat(result).isInstanceOf(projectionType);
        verifyQueryResult(result, projectionType, allPropertiesRequired);
        return result;
    }

    protected void verifyQueryResult(S source) {
        verifyQueryResult(source, true);
    }

    protected void verifyQueryResult(S source, boolean allPropertiesRequired) {
        verifyQueryResult(source, domainType,allPropertiesRequired);
    }

    protected void verifyQueryResult(Iterable<S> source) {
        verifyQueryResult(source, true);
    }

    protected <P> void verifyQueryResult(Iterable<S> source, Class<P> projectionType) {
        verifyQueryResult(source, projectionType,true);
    }

    protected void verifyQueryResult(Iterable<S> source, boolean allPropertiesRequired) {
        Object object = source;
        verifyQueryResult(object, domainType, allPropertiesRequired);
    }

    protected <P> void verifyQueryResult(Iterable<P> source, Class<P> projectionType, boolean allPropertiesRequired) {
        Object object = source;
        verifyQueryResult(object, projectionType, allPropertiesRequired);
    }

    protected void verifyQueryResult(Object source, Class<?> projectionType) {
        verifyQueryResult(source, projectionType, true);
    }

    protected



    }
