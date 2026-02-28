package com.old.silence.data.mybatis.projection;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.old.silence.data.mybatis.test.DataMyBatisTest;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for projection query support.
 */
@DataMyBatisTest
@MapperScan(basePackages = "com.old.silence.data.mybatis.projection")
@Import(ProjectionIntegrationTest.TestConfig.class)
@ActiveProfiles("test")
@ContextConfiguration(classes = ProjectionIntegrationTest.TestApp.class)
@ImportAutoConfiguration(MybatisPlusAutoConfiguration.class)
class ProjectionIntegrationTest {

    @Autowired
    private ProjectionRepositoryFactory repositoryFactory;

    @Autowired
    private ProjectionMapperByteBuddyFactory mapperByteBuddyFactory;

    @Autowired
    private ProjectionQueryOperations projectionQueryOperations;

    @Autowired
    private SimpleProjectionQueryRepositoryFactory simpleProjectionQueryRepositoryFactory;

    @Test
    void shouldResolveTableFieldMappingAndEnumHandler() {
        ProjectionRepository<TestUser, Long> repository = repositoryFactory.create(TestUser.class);

        QueryWrapper<TestUser> queryWrapper = createEnabledUserQueryWrapper();

        List<TestUserProjection> result = repository.findByQuery(queryWrapper, TestUserProjection.class);

        assertThat(result).isNotEmpty();
        TestUserProjection projection = result.getFirst();

        // Validate @TableField("is_enabled") mapping
        assertThat(projection.getEnabled()).isTrue();

        // Validate enum type handler mapping
        assertThat(projection.getStatus()).isEqualTo(TestUserStatus.ACTIVE);
    }

    @Test
    void shouldSupportPaginationAndOrdering() {
        ProjectionRepository<TestUser, Long> repository = repositoryFactory.create(TestUser.class);

        QueryWrapper<TestUser> queryWrapper = createEnabledUserQueryWrapper();

        Page<?> page = new Page<>(1, 1);
        page.addOrder(OrderItem.desc("id"));

        IPage<TestUserProjection> result = repository.findByQuery(queryWrapper, page, TestUserProjection.class);

        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getTotal()).isEqualTo(2);
    }

    @Test
    void shouldFailFastWhenPageOffsetExceedsRowBoundsRange() {
        ProjectionRepository<TestUser, Long> repository = repositoryFactory.create(TestUser.class);

        QueryWrapper<TestUser> queryWrapper = createEnabledUserQueryWrapper();

        Page<?> page = new Page<>(2_147_483_648L, 2L);

        assertThatThrownBy(() -> repository.findByQuery(queryWrapper, page, TestUserProjection.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Page offset exceeds RowBounds integer range");
    }

    @Test
    void shouldSupportByteBuddyProjectionMapper() {
        TestUserProjectionMapperTest mapper = mapperByteBuddyFactory.create(TestUserProjectionMapperTest.class, TestUser.class);

        QueryWrapper<TestUser> queryWrapper = createEnabledUserQueryWrapper();

        List<TestUserProjectionView> list = mapper.findByQuery(queryWrapper, TestUserProjectionView.class);
        assertThat(list).isNotEmpty();

        Page<?> pageRequest = new Page<>(1, 1);
        pageRequest.addOrder(OrderItem.desc("id"));
        IPage<TestUserProjectionView> page = mapper.findByQuery(queryWrapper, pageRequest, TestUserProjectionView.class);
        assertThat(page.getRecords()).hasSize(1);
    }

    @Test
    void shouldSupportSimpleJdbcStyleProjectionRepository() {
        SimpleProjectionQueryRepository<TestUser> repository =
            new SimpleProjectionQueryRepository<>(projectionQueryOperations, TestUser.class);

        QueryWrapper<TestUser> queryWrapper = createEnabledUserQueryWrapper();

        List<TestUserProjectionView> list = repository.findByQuery(queryWrapper, TestUserProjectionView.class);
        assertThat(list).isNotEmpty();
        assertThat(list.getFirst().getUsername()).isNotBlank();

        Page<?> pageRequest = new Page<>(1, 1);
        pageRequest.addOrder(OrderItem.desc("id"));
        IPage<TestUserProjectionView> page = repository.findByQuery(queryWrapper,
            pageRequest,
            TestUserProjectionView.class);

        assertThat(page.getRecords()).hasSize(1);
        assertThat(page.getRecords().getFirst().getStatus()).isEqualTo(TestUserStatus.ACTIVE);
    }

    @Test
    void shouldSupportSimpleJdbcStyleProjectionRepositoryFactory() {
        SimpleProjectionQueryRepository<TestUser> repository =
                simpleProjectionQueryRepositoryFactory.create(TestUser.class);

        QueryWrapper<TestUser> queryWrapper = createEnabledUserQueryWrapper();
        List<TestUserProjectionView> list = repository.findByQuery(queryWrapper,
            TestUserProjectionView.class);

        assertThat(list).isNotEmpty();
        assertThat(list.getFirst().getUsername()).isNotBlank();
    }

    @Test
    void shouldSupportCountAndExistsByQuery() {
        ProjectionRepository<TestUser, Long> repository = repositoryFactory.create(TestUser.class);
        QueryWrapper<TestUser> enabledUsers = createEnabledUserQueryWrapper();

        long count = repository.countByQuery(enabledUsers);
        boolean exists = repository.existsByQuery(enabledUsers);

        assertThat(count).isEqualTo(2L);
        assertThat(exists).isTrue();

        QueryWrapper<TestUser> notExistsQuery = new QueryWrapper<TestUser>()
                .like("username", "not-exists-user");
        assertThat(repository.countByQuery(notExistsQuery)).isZero();
        assertThat(repository.existsByQuery(notExistsQuery)).isFalse();
    }

    @Test
    void shouldSupportSimpleJdbcStyleCountAndExistsByQuery() {
        SimpleProjectionQueryRepository<TestUser> repository =
                simpleProjectionQueryRepositoryFactory.create(TestUser.class);
        QueryWrapper<TestUser> enabledUsers = createEnabledUserQueryWrapper();

        assertThat(repository.countByQuery(enabledUsers)).isEqualTo(2L);
        assertThat(repository.existsByQuery(enabledUsers)).isTrue();
    }

    @Test
    void shouldSupportFindById() {
        ProjectionRepository<TestUser, Long> repository = repositoryFactory.create(TestUser.class);

        java.util.Optional<TestUser> found = repository.findById(1L);
        assertThat(found).isPresent();
        assertThat(found.orElseThrow().getId()).isEqualTo(1L);

        java.util.Optional<TestUser> notFound = repository.findById(999999L);
        assertThat(notFound).isEmpty();

        java.util.Optional<TestUserProjectionView> projection = repository.findById(1L, TestUserProjectionView.class);
        assertThat(projection).isPresent();
        assertThat(projection.orElseThrow().getUsername()).isNotBlank();

        java.util.Optional<TestUserProjectionView> projectionNotFound = repository.findById(999999L, TestUserProjectionView.class);
        assertThat(projectionNotFound).isEmpty();
    }

    @Test
    void shouldSupportSimpleJdbcStyleFindById() {
        SimpleProjectionQueryRepository<TestUser> repository =
                simpleProjectionQueryRepositoryFactory.create(TestUser.class);

        java.util.Optional<TestUser> found = repository.findById(1L);
        assertThat(found).isPresent();
        assertThat(found.orElseThrow().getId()).isEqualTo(1L);

        java.util.Optional<TestUser> notFound = repository.findById(999999L);
        assertThat(notFound).isEmpty();

        java.util.Optional<TestUserProjectionView> projection = repository.findById(1L, TestUserProjectionView.class);
        assertThat(projection).isPresent();
        assertThat(projection.orElseThrow().getUsername()).isNotBlank();

        java.util.Optional<TestUserProjectionView> projectionNotFound = repository.findById(999999L, TestUserProjectionView.class);
        assertThat(projectionNotFound).isEmpty();
    }

    @Test
    void shouldSupportCreateUpdateDeleteByRepository() {
        ProjectionRepository<TestUser, Long> repository = repositoryFactory.create(TestUser.class);

        TestUser entity = new TestUser();
        entity.setUsername("user_crud_repo");
        entity.setEnabled(true);
        entity.setStatus(TestUserStatus.ACTIVE);

        int created = repository.create(entity);
        assertThat(created).isEqualTo(1);
        assertThat(entity.getId()).isNotNull();

        entity.setUsername("user_crud_repo_updated");
        int updated = repository.updateById(entity);
        assertThat(updated).isEqualTo(1);

        QueryWrapper<TestUser> byId = new QueryWrapper<TestUser>().eq("id", entity.getId());
        List<TestUserProjectionView> records = repository.findByQuery(byId, TestUserProjectionView.class);
        assertThat(records).hasSize(1);
        assertThat(records.getFirst().getUsername()).isEqualTo("user_crud_repo_updated");

        int deleted = repository.deleteById(entity.getId());
        assertThat(deleted).isEqualTo(1);
        assertThat(repository.existsByQuery(byId)).isFalse();
    }

    @Test
    void shouldSupportCreateUpdateDeleteBySimpleJdbcStyleRepository() {
        SimpleProjectionQueryRepository<TestUser> repository =
                simpleProjectionQueryRepositoryFactory.create(TestUser.class);

        TestUser entity = new TestUser();
        entity.setUsername("user_crud_simple");
        entity.setEnabled(false);
        entity.setStatus(TestUserStatus.DISABLED);

        assertThat(repository.create(entity)).isEqualTo(1);
        assertThat(entity.getId()).isNotNull();

        entity.setEnabled(true);
        entity.setStatus(TestUserStatus.ACTIVE);
        assertThat(repository.updateById(entity)).isEqualTo(1);

        QueryWrapper<TestUser> byId = new QueryWrapper<TestUser>().eq("id", entity.getId());
        assertThat(repository.existsByQuery(byId)).isTrue();

        assertThat(repository.deleteByQuery(byId)).isEqualTo(1);
        assertThat(repository.existsByQuery(byId)).isFalse();
    }

    @Test
    void shouldSupportInterfaceProjection() {
        ProjectionRepository<TestUser, Long> repository = repositoryFactory.create(TestUser.class);

        QueryWrapper<TestUser> queryWrapper = createEnabledUserQueryWrapper();

        List<TestUserProjectionView> result = repository.findByQuery(queryWrapper, TestUserProjectionView.class);
        assertThat(result).isNotEmpty();

        TestUserProjectionView projection = result.getFirst();
        assertThat(projection.getUsername()).isNotBlank();
        assertThat(projection.getEnabled()).isTrue();
        assertThat(projection.getStatus()).isEqualTo(TestUserStatus.ACTIVE);
    }

    @Test
    void shouldThrowWhenProjectionTypeMissingInByteBuddyMapper() {
        assertThatThrownBy(() -> mapperByteBuddyFactory.create(BadProjectionMapperMissingProjectionTypeTest.class, TestUser.class))
                .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("parameter count must be 2 or 3");
    }

    @Test
    void shouldReuseCachedByteBuddyMapperInstance() {
        TestUserProjectionMapperTest mapper1 = mapperByteBuddyFactory.create(TestUserProjectionMapperTest.class, TestUser.class);
        TestUserProjectionMapperTest mapper2 = mapperByteBuddyFactory.create(TestUserProjectionMapperTest.class, TestUser.class);

        assertThat(mapper1).isSameAs(mapper2);
    }

    @Test
    void shouldReuseCachedMapperUnderConcurrentCreateCalls() throws Exception {
        int threadCount = 12;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);

        try {
            List<Future<TestUserProjectionMapperTest>> futures = new ArrayList<>(threadCount);
            for (int i = 0; i < threadCount; i++) {
                futures.add(executorService.submit(() -> {
                    ready.countDown();
                    start.await(5, TimeUnit.SECONDS);
                    return mapperByteBuddyFactory.create(TestUserProjectionMapperTest.class, TestUser.class);
                }));
            }

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<TestUserProjectionMapperTest> instances = new ArrayList<>(threadCount);
            for (Future<TestUserProjectionMapperTest> future : futures) {
                instances.add(future.get(5, TimeUnit.SECONDS));
            }

            assertThat(instances).hasSize(threadCount);
            TestUserProjectionMapperTest first = instances.getFirst();
            assertThat(instances).allSatisfy(instance -> assertThat(instance).isSameAs(first));
        } finally {
            executorService.shutdownNow();
        }
    }

    @Test
    void shouldValidateMapperSignatureAtCreateStage() {
        assertThatThrownBy(() -> mapperByteBuddyFactory.create(BadProjectionMapperSignatureTest.class, TestUser.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("(queryWrapper, Page, Class)");
    }

    @Test
    void shouldValidatePageSecondArgumentAtCreateStage() {
        assertThatThrownBy(() -> mapperByteBuddyFactory.create(BadProjectionPageParamSignatureTest.class, TestUser.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("parameter count must be 2 or 3");
    }

    @Test
    void shouldValidateFindByQuerySignatureAtCreateStage() {
        assertThatThrownBy(() -> mapperByteBuddyFactory.create(BadProjectionFindByQuerySignatureTest.class, TestUser.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("(queryWrapper, Class)");
    }

    @Test
    void shouldValidateFindByQueryParameterOrderAtCreateStage() {
        assertThatThrownBy(() -> mapperByteBuddyFactory.create(BadProjectionFindByQueryOrderSignatureTest.class, TestUser.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("first argument must be QueryWrapper/LambdaQueryWrapper");
    }

    private QueryWrapper<TestUser> createEnabledUserQueryWrapper() {
        QueryWrapper<TestUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.like("username", "user");
        queryWrapper.eq("is_enabled", true);
        return queryWrapper;
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        ProjectionMetadataResolver projectionMetadataResolver() {
            return new ProjectionMetadataResolver();
        }

        @Bean
        ProjectionResultMapRegistry projectionResultMapRegistry() {
            return new ProjectionResultMapRegistry();
        }

        @Bean
        ProjectionQueryExecutor projectionQueryExecutor(SqlSessionFactory sqlSessionFactory,
                                                        ProjectionResultMapRegistry resultMapRegistry) {
            return new ProjectionQueryExecutor(sqlSessionFactory, resultMapRegistry);
        }

        @Bean
        ProjectionRepositoryFactory projectionRepositoryFactory(ProjectionMetadataResolver resolver,
                                                               ProjectionQueryExecutor executor) {
            return new ProjectionRepositoryFactory(resolver, executor);
        }

        @Bean
        ProjectionQueryOperations projectionQueryOperations(ProjectionRepositoryFactory projectionRepositoryFactory) {
            return new ProjectionQueryOperations(projectionRepositoryFactory);
        }

        @Bean
        SimpleProjectionQueryRepositoryFactory simpleProjectionQueryRepositoryFactory(
                ProjectionQueryOperations projectionQueryOperations) {
            return new SimpleProjectionQueryRepositoryFactory(projectionQueryOperations);
        }

        @Bean
        ProjectionMapperByteBuddyFactory projectionMapperByteBuddyFactory(ProjectionRepositoryFactory factory) {
            return new ProjectionMapperByteBuddyFactory(factory);
        }
    }

    @SpringBootConfiguration
    static class TestApp {
    }
}
