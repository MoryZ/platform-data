package com.old.silence.data.mybatis.projection;

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

    @Test
    void shouldResolveTableFieldMappingAndEnumHandler() {
        ProjectionRepository<TestUser> repository = repositoryFactory.create(TestUser.class);

        TestUserQuery query = new TestUserQuery();
        query.setUsernameLike("user");
        query.setEnabled(true);

        List<TestUserProjection> result = repository.findByQuery(query, TestUserProjection.class);

        assertThat(result).isNotEmpty();
        TestUserProjection projection = result.getFirst();

        // Validate @TableField("is_enabled") mapping
        assertThat(projection.getEnabled()).isTrue();

        // Validate enum type handler mapping
        assertThat(projection.getStatus()).isEqualTo(TestUserStatus.ACTIVE);
    }

    @Test
    void shouldSupportPaginationAndOrdering() {
        ProjectionRepository<TestUser> repository = repositoryFactory.create(TestUser.class);

        TestUserQuery query = new TestUserQuery();
        query.setUsernameLike("user");
        query.setEnabled(true);

        Page<?> page = new Page<>(1, 1);
        List<OrderItem> orderItems = List.of(OrderItem.desc("id"));

        IPage<TestUserProjection> result = repository.findByQuery(query, page, orderItems, TestUserProjection.class);

        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getTotal()).isEqualTo(2);
    }

    @Test
    void shouldFailFastWhenPageOffsetExceedsRowBoundsRange() {
        ProjectionRepository<TestUser> repository = repositoryFactory.create(TestUser.class);

        TestUserQuery query = new TestUserQuery();
        query.setUsernameLike("user");
        query.setEnabled(true);

        Page<?> page = new Page<>(2_147_483_648L, 2L);

        assertThatThrownBy(() -> repository.findByQuery(query, page, TestUserProjection.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Page offset exceeds RowBounds integer range");
    }

    @Test
    void shouldSupportByteBuddyProjectionMapper() {
        TestUserProjectionMapperTest mapper = mapperByteBuddyFactory.create(TestUserProjectionMapperTest.class, TestUser.class);

        TestUserQuery query = new TestUserQuery();
        query.setUsernameLike("user");
        query.setEnabled(true);

        List<TestUserProjectionView> list = mapper.findByQuery(query, TestUserProjectionView.class);
        assertThat(list).isNotEmpty();

        IPage<TestUserProjectionView> page = mapper.findByQuery(query, new Page<>(1, 1), List.of(OrderItem.desc("id")),
            TestUserProjectionView.class);
        assertThat(page.getRecords()).hasSize(1);
    }

    @Test
    void shouldSupportProjectionFieldsMode() {
        TestUserProjectionMapperTest mapper = mapperByteBuddyFactory.create(TestUserProjectionMapperTest.class, TestUser.class);

        TestUserQuery query = new TestUserQuery();
        query.setUsernameLike("user");
        query.setEnabled(true);

        List<TestUserProjectionView> list = mapper.findByQuery(query, TestUserProjectionView.class, "username,enabled");
        assertThat(list).isNotEmpty();
        TestUserProjectionView projection = list.getFirst();
        assertThat(projection.getUsername()).isNotBlank();
        assertThat(projection.getEnabled()).isNotNull();
        assertThat(projection.getStatus()).isNull();

        IPage<TestUserProjectionView> page = mapper.findByQuery(query, new Page<>(1, 1), List.of(OrderItem.desc("id")),
            TestUserProjectionView.class, "username,enabled");
        assertThat(page.getRecords()).hasSize(1);
        assertThat(page.getRecords().getFirst().getStatus()).isNull();
    }

    @Test
    void shouldSupportProjectionFieldsArrayMode() {
        TestUserProjectionMapperTest mapper = mapperByteBuddyFactory.create(TestUserProjectionMapperTest.class, TestUser.class);

        TestUserQuery query = new TestUserQuery();
        query.setUsernameLike("user");
        query.setEnabled(true);

        List<TestUserProjectionView> list = mapper.findByQuery(query, TestUserProjectionView.class,
                new String[]{"username", "enabled"});
        assertThat(list).isNotEmpty();

        TestUserProjectionView projection = list.getFirst();
        assertThat(projection.getUsername()).isNotBlank();
        assertThat(projection.getEnabled()).isNotNull();
        assertThat(projection.getStatus()).isNull();

        IPage<TestUserProjectionView> page = mapper.findByQuery(query, new Page<>(1, 1),
                List.of(OrderItem.desc("id")), TestUserProjectionView.class,
                new String[]{"username", "enabled"});
        assertThat(page.getRecords()).hasSize(1);
        assertThat(page.getRecords().getFirst().getStatus()).isNull();
    }

    @Test
    void shouldSupportInterfaceProjection() {
        ProjectionRepository<TestUser> repository = repositoryFactory.create(TestUser.class);

        TestUserQuery query = new TestUserQuery();
        query.setUsernameLike("user");
        query.setEnabled(true);

        List<TestUserProjectionView> result = repository.findByQuery(query, TestUserProjectionView.class);
        assertThat(result).isNotEmpty();

        TestUserProjectionView projection = result.getFirst();
        assertThat(projection.getUsername()).isNotBlank();
        assertThat(projection.getEnabled()).isTrue();
        assertThat(projection.getStatus()).isEqualTo(TestUserStatus.ACTIVE);
    }

    @Test
    void shouldSupportInterfaceProjectionFieldsMode() {
        ProjectionRepository<TestUser> repository = repositoryFactory.create(TestUser.class);

        TestUserQuery query = new TestUserQuery();
        query.setUsernameLike("user");
        query.setEnabled(true);

        List<TestUserProjectionView> result = repository.findByQuery(query, TestUserProjectionView.class,
                List.of("username", "enabled"));
        assertThat(result).isNotEmpty();

        TestUserProjectionView projection = result.getFirst();
        assertThat(projection.getUsername()).isNotBlank();
        assertThat(projection.getEnabled()).isNotNull();
        assertThat(projection.getStatus()).isNull();
    }

    @Test
    void shouldThrowWhenProjectionTypeMissingInByteBuddyMapper() {
        assertThatThrownBy(() -> mapperByteBuddyFactory.create(BadProjectionMapperMissingProjectionTypeTest.class, TestUser.class))
                .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("parameter count must be 2 to 5");
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
                .hasMessageContaining("(query, Page, Class)");
    }

    @Test
    void shouldValidatePageSecondArgumentAtCreateStage() {
        assertThatThrownBy(() -> mapperByteBuddyFactory.create(BadProjectionPageParamSignatureTest.class, TestUser.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("for 4 parameters");
    }

    @Test
    void shouldValidateFindByQuerySignatureAtCreateStage() {
        assertThatThrownBy(() -> mapperByteBuddyFactory.create(BadProjectionFindByQuerySignatureTest.class, TestUser.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("(query, Class)");
    }

    @Test
    void shouldValidateFindByQueryParameterOrderAtCreateStage() {
        assertThatThrownBy(() -> mapperByteBuddyFactory.create(BadProjectionFindByQueryOrderSignatureTest.class, TestUser.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("first argument must be query object");
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
        ProjectionMapperByteBuddyFactory projectionMapperByteBuddyFactory(ProjectionRepositoryFactory factory) {
            return new ProjectionMapperByteBuddyFactory(factory);
        }
    }

    @SpringBootConfiguration
    static class TestApp {
    }
}
