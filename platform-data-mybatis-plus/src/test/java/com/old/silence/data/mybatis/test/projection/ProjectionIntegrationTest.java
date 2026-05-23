package com.old.silence.data.mybatis.test.projection;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.old.silence.data.mybatis.projection.ProjectionMapperByteBuddyFactory;
import com.old.silence.data.mybatis.projection.ProjectionMetadataResolver;
import com.old.silence.data.mybatis.projection.ProjectionQueryExecutor;
import com.old.silence.data.mybatis.projection.ProjectionQueryOperations;
import com.old.silence.data.mybatis.projection.ProjectionRepository;
import com.old.silence.data.mybatis.projection.ProjectionRepositoryFactory;
import com.old.silence.data.mybatis.projection.ProjectionRepositoryProxyFactory;
import com.old.silence.data.mybatis.projection.ProjectionResultMapRegistry;
import com.old.silence.data.mybatis.projection.SimpleProjectionQueryRepository;
import com.old.silence.data.mybatis.projection.SimpleProjectionQueryRepositoryFactory;
import com.old.silence.data.mybatis.test.DataMyBatisTest;
import com.old.silence.data.mybatis.test.fixture.entity.User;
import com.old.silence.data.mybatis.test.fixture.enmus.UserStatus;
import com.old.silence.data.mybatis.test.fixture.mapper.BadProjectionFindByQueryOrderSignatureContract;
import com.old.silence.data.mybatis.test.fixture.mapper.BadProjectionFindByQuerySignatureContract;
import com.old.silence.data.mybatis.test.fixture.mapper.BadProjectionMapperMissingProjectionTypeContract;
import com.old.silence.data.mybatis.test.fixture.mapper.BadProjectionMapperSignatureContract;
import com.old.silence.data.mybatis.test.fixture.mapper.BadProjectionPageParamSignatureContract;
import com.old.silence.data.mybatis.test.fixture.mapper.UserAnnotatedProjectionRepository;
import com.old.silence.data.mybatis.test.fixture.mapper.UserGenericProjectionMapperContract;
import com.old.silence.data.mybatis.test.fixture.mapper.UserHybridCreateMapper;
import com.old.silence.data.mybatis.test.fixture.mapper.UserPlainProjectionRepository;
import com.old.silence.data.mybatis.test.fixture.mapper.UserProjectionMapperContract;
import com.old.silence.data.mybatis.test.fixture.projection.UserFinalDto;
import com.old.silence.data.mybatis.test.fixture.projection.UserDto;
import com.old.silence.data.mybatis.test.fixture.projection.TestUserView;
import com.old.silence.data.mybatis.test.fixture.projection.TestUserWithUserRolesView;
import com.old.silence.data.mybatis.test.fixture.projection.TestUserRecordDto;
import com.old.silence.data.mybatis.test.fixture.projection.TaskView;
import com.old.silence.data.mybatis.test.fixture.entity.Task;
import com.old.silence.data.mybatis.test.fixture.entity.Project;
import com.old.silence.data.mybatis.test.fixture.projection.ProjectView;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
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
 *
 * Association models in this suite:
 * - User -> UserRole -> Role (explicit join entity, composite-key table)
 * - User -> UserDepartment -> Department (explicit join entity, many-to-many through intermediate table)
 */
@DataMyBatisTest
@MapperScan(basePackages = "com.old.silence.data.mybatis.test.fixture.mapper")
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

    @Autowired
    private ProjectionRepositoryProxyFactory projectionRepositoryProxyFactory;

    @Autowired
    private UserHybridCreateMapper mapperScannedHybridMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldResolveTableFieldMappingAndEnumHandler() {
        ProjectionRepository<User, Long> repository = repositoryFactory.create(User.class);

        QueryWrapper<User> queryWrapper = createEnabledUserQueryWrapper();

        List<UserDto> result = repository.findByQuery(queryWrapper, UserDto.class);

        assertThat(result).isNotEmpty();
        UserDto projection = result.getFirst();
        assertThat(projection.getEnabled()).isTrue();
        assertThat(projection.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void shouldSupportPaginationAndOrdering() {
        ProjectionRepository<User, Long> repository = repositoryFactory.create(User.class);

        QueryWrapper<User> queryWrapper = createEnabledUserQueryWrapper();

        Page<?> page = new Page<>(1, 1);
        page.addOrder(OrderItem.desc("id"));

        IPage<UserDto> result = repository.findByQuery(queryWrapper, page, UserDto.class);

        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getTotal()).isEqualTo(2);
    }

    @Test
    void shouldFailFastWhenPageOffsetExceedsRowBoundsRange() {
        ProjectionRepository<User, Long> repository = repositoryFactory.create(User.class);

        QueryWrapper<User> queryWrapper = createEnabledUserQueryWrapper();

        Page<?> page = new Page<>(2_147_483_648L, 2L);

        assertThatThrownBy(() -> repository.findByQuery(queryWrapper, page, UserDto.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Page offset exceeds RowBounds integer range");
    }

    @Test
    void shouldSupportByteBuddyProjectionMapper() {
        UserProjectionMapperContract mapper = mapperByteBuddyFactory.create(UserProjectionMapperContract.class, User.class);

        QueryWrapper<User> queryWrapper = createEnabledUserQueryWrapper();

        List<TestUserView> list = mapper.findByQuery(queryWrapper, TestUserView.class);
        assertThat(list).isNotEmpty();

        Page<?> pageRequest = new Page<>(1, 1);
        pageRequest.addOrder(OrderItem.desc("id"));
        IPage<TestUserView> page = mapper.findByQuery(queryWrapper, pageRequest, TestUserView.class);
        assertThat(page.getRecords()).hasSize(1);
    }

    @Test
    void shouldSupportSimpleJdbcStyleProjectionRepository() {
        SimpleProjectionQueryRepository<User> repository =
                new SimpleProjectionQueryRepository<>(projectionQueryOperations, User.class);

        QueryWrapper<User> queryWrapper = createEnabledUserQueryWrapper();

        List<TestUserView> list = repository.findByQuery(queryWrapper, TestUserView.class);
        assertThat(list).isNotEmpty();
        assertThat(list.getFirst().getUsername()).isNotBlank();

        Page<?> pageRequest = new Page<>(1, 1);
        pageRequest.addOrder(OrderItem.desc("id"));
        IPage<TestUserView> page = repository.findByQuery(queryWrapper, pageRequest, TestUserView.class);

        assertThat(page.getRecords()).hasSize(1);
        assertThat(page.getRecords().getFirst().getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void shouldSupportInterfaceProjectionWithOneToManyCollection() {
        ProjectionRepository<User, Long> repository = repositoryFactory.create(User.class);

        QueryWrapper<User> queryWrapper = createEnabledUserQueryWrapper();
        List<TestUserWithUserRolesView> list = repository.findByQuery(queryWrapper, TestUserWithUserRolesView.class);

        assertThat(list).hasSize(2);
        TestUserWithUserRolesView userA = list.stream()
                .filter(user -> "user_a".equals(user.getUsername()))
                .findFirst()
                .orElseThrow();
        TestUserWithUserRolesView userB = list.stream()
                .filter(user -> "user_b".equals(user.getUsername()))
                .findFirst()
                .orElseThrow();

        assertThat(userA.getUserRoles()).hasSize(2);
        assertThat(userA.getUserRoles()).extracting("roleId").containsExactlyInAnyOrder(1L, 2L);
        assertThat(userB.getUserRoles()).hasSize(1);
        assertThat(userB.getUserRoles()).extracting("roleId").containsExactly(2L);
    }

    @Test
    void shouldSupportInterfaceProjectionWithOneToManyCollectionInPageQuery() {
        ProjectionRepository<User, Long> repository = repositoryFactory.create(User.class);

        QueryWrapper<User> queryWrapper = createEnabledUserQueryWrapper();
        Page<?> page = new Page<>(1, 1);
        page.addOrder(OrderItem.asc("id"));

        IPage<TestUserWithUserRolesView> result = repository.findByQuery(queryWrapper, page, TestUserWithUserRolesView.class);

        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().getFirst().getUsername()).isEqualTo("user_a");
        assertThat(result.getRecords().getFirst().getUserRoles()).hasSize(2);
    }

    @Test
    void shouldSupportSimpleJdbcStyleProjectionRepositoryFactory() {
        SimpleProjectionQueryRepository<User> repository =
                simpleProjectionQueryRepositoryFactory.create(User.class);

        QueryWrapper<User> queryWrapper = createEnabledUserQueryWrapper();
        List<TestUserView> list = repository.findByQuery(queryWrapper, TestUserView.class);

        assertThat(list).isNotEmpty();
        assertThat(list.getFirst().getUsername()).isNotBlank();
    }

    @Test
    void shouldSupportNestedInterfaceProjection() {
        ProjectionRepository<Task, Long> repository = repositoryFactory.create(Task.class);

        QueryWrapper<Task> queryWrapper = new QueryWrapper<>();
        List<TaskView> list = repository.findByQuery(queryWrapper, TaskView.class);

        assertThat(list).hasSize(3);

        TaskView task1 = list.stream()
                .filter(t -> "Task 1".equals(t.getTaskName()))
                .findFirst()
                .orElseThrow();

        assertThat(task1.getProject()).isNotNull();
        assertThat(task1.getProject().getProjectName()).isEqualTo("Alpha Project");
        assertThat(task1.getProject().getProjectCode()).isEqualTo("ALPHA");
    }

    @Test
    void shouldSupportNestedInterfaceProjectionWithPagination() {
        ProjectionRepository<Task, Long> repository = repositoryFactory.create(Task.class);

        Page<?> page = new Page<>(1, 2);
        IPage<TaskView> result = repository.findByQuery(new QueryWrapper<Task>(), page, TaskView.class);

        assertThat(result.getRecords()).hasSize(2);
        assertThat(result.getRecords().getFirst().getProject()).isNotNull();
        assertThat(result.getRecords().getFirst().getProject().getProjectName()).isNotBlank();
    }

    @Test
    void shouldSupportRecordProjectionMaterialization() {
        ProjectionRepository<User, Long> repository = repositoryFactory.create(User.class);

        QueryWrapper<User> queryWrapper = createEnabledUserQueryWrapper();
        List<TestUserRecordDto> list = repository.findByQuery(queryWrapper, TestUserRecordDto.class);

        assertThat(list).isNotEmpty();
        assertThat(list.getFirst().username()).isNotBlank();
        assertThat(list.getFirst().status()).isEqualTo(UserStatus.ACTIVE);

        Page<?> page = new Page<>(1, 1);
        IPage<TestUserRecordDto> paged = repository.findByQuery(queryWrapper, page, TestUserRecordDto.class);
        assertThat(paged.getRecords()).hasSize(1);
        assertThat(paged.getRecords().getFirst().status()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void shouldSupportFinalDtoProjectionMaterialization() {
        ProjectionRepository<User, Long> repository = repositoryFactory.create(User.class);

        QueryWrapper<User> queryWrapper = createEnabledUserQueryWrapper();
        List<UserFinalDto> list = repository.findByQuery(queryWrapper, UserFinalDto.class);

        assertThat(list).isNotEmpty();
        assertThat(list.getFirst().getUsername()).isNotBlank();
        assertThat(list.getFirst().getStatus()).isEqualTo(UserStatus.ACTIVE);

        Page<?> page = new Page<>(1, 1);
        IPage<UserFinalDto> paged = repository.findByQuery(queryWrapper, page, UserFinalDto.class);
        assertThat(paged.getRecords()).hasSize(1);
        assertThat(paged.getRecords().getFirst().getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void shouldSupportRecordAndFinalProjectionFindById() {
        ProjectionRepository<User, Long> repository = repositoryFactory.create(User.class);

        var recordProjection = repository.findById(1L, TestUserRecordDto.class);
        var finalProjection = repository.findById(1L, UserFinalDto.class);

        assertThat(recordProjection).isPresent();
        assertThat(recordProjection.orElseThrow().status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(finalProjection).isPresent();
        assertThat(finalProjection.orElseThrow().getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void shouldSupportSimpleJdbcStyleRecordAndFinalProjectionMaterialization() {
        SimpleProjectionQueryRepository<User> repository =
                simpleProjectionQueryRepositoryFactory.create(User.class);

        QueryWrapper<User> queryWrapper = createEnabledUserQueryWrapper();

        List<TestUserRecordDto> recordList = repository.findByQuery(queryWrapper, TestUserRecordDto.class);
        List<UserFinalDto> finalList = repository.findByQuery(queryWrapper, UserFinalDto.class);

        assertThat(recordList).isNotEmpty();
        assertThat(recordList.getFirst().status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(finalList).isNotEmpty();
        assertThat(finalList.getFirst().getStatus()).isEqualTo(UserStatus.ACTIVE);

        Page<?> page = new Page<>(1, 1);
        IPage<TestUserRecordDto> recordPage = repository.findByQuery(queryWrapper, page, TestUserRecordDto.class);
        assertThat(recordPage.getRecords()).hasSize(1);
        assertThat(recordPage.getRecords().getFirst().status()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void shouldSupportGenericByteBuddyProjectionMapperAcrossProjectionTypes() {
        UserGenericProjectionMapperContract mapper =
                mapperByteBuddyFactory.create(UserGenericProjectionMapperContract.class, User.class);

        QueryWrapper<User> queryWrapper = createEnabledUserQueryWrapper();

        List<TestUserView> viewList = mapper.findByQuery(queryWrapper, TestUserView.class);
        List<TestUserRecordDto> recordList = mapper.findByQuery(queryWrapper, TestUserRecordDto.class);
        List<UserFinalDto> finalList = mapper.findByQuery(queryWrapper, UserFinalDto.class);

        assertThat(viewList).isNotEmpty();
        assertThat(viewList.getFirst().getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(recordList).isNotEmpty();
        assertThat(recordList.getFirst().status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(finalList).isNotEmpty();
        assertThat(finalList.getFirst().getStatus()).isEqualTo(UserStatus.ACTIVE);

        Page<?> page = new Page<>(1, 1);
        IPage<UserFinalDto> finalPage = mapper.findByQuery(queryWrapper, page, UserFinalDto.class);
        assertThat(finalPage.getRecords()).hasSize(1);
        assertThat(finalPage.getRecords().getFirst().getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void shouldSupportCountAndExistsByQuery() {
        ProjectionRepository<User, Long> repository = repositoryFactory.create(User.class);
        QueryWrapper<User> enabledUsers = createEnabledUserQueryWrapper();

        long count = repository.countByQuery(enabledUsers);
        boolean exists = repository.existsByQuery(enabledUsers);

        assertThat(count).isEqualTo(2L);
        assertThat(exists).isTrue();

        QueryWrapper<User> notExistsQuery = new QueryWrapper<User>()
                .like("username", "not-exists-user");
        assertThat(repository.countByQuery(notExistsQuery)).isZero();
        assertThat(repository.existsByQuery(notExistsQuery)).isFalse();
    }

    @Test
    void shouldSupportSimpleJdbcStyleCountAndExistsByQuery() {
        SimpleProjectionQueryRepository<User> repository =
                simpleProjectionQueryRepositoryFactory.create(User.class);
        QueryWrapper<User> enabledUsers = createEnabledUserQueryWrapper();

        assertThat(repository.countByQuery(enabledUsers)).isEqualTo(2L);
        assertThat(repository.existsByQuery(enabledUsers)).isTrue();
    }

    @Test
    void shouldSupportFindById() {
        ProjectionRepository<User, Long> repository = repositoryFactory.create(User.class);

        java.util.Optional<User> found = repository.findById(1L);
        assertThat(found).isPresent();
        assertThat(found.orElseThrow().getId()).isEqualTo(1L);

        java.util.Optional<User> notFound = repository.findById(999999L);
        assertThat(notFound).isEmpty();

        java.util.Optional<TestUserView> projection = repository.findById(1L, TestUserView.class);
        assertThat(projection).isPresent();
        assertThat(projection.orElseThrow().getUsername()).isNotBlank();

        java.util.Optional<TestUserView> projectionNotFound = repository.findById(999999L, TestUserView.class);
        assertThat(projectionNotFound).isEmpty();
    }

    @Test
    void shouldSupportSimpleJdbcStyleFindById() {
        SimpleProjectionQueryRepository<User> repository =
                simpleProjectionQueryRepositoryFactory.create(User.class);

        java.util.Optional<User> found = repository.findById(1L);
        assertThat(found).isPresent();
        assertThat(found.orElseThrow().getId()).isEqualTo(1L);

        java.util.Optional<User> notFound = repository.findById(999999L);
        assertThat(notFound).isEmpty();

        java.util.Optional<TestUserView> projection = repository.findById(1L, TestUserView.class);
        assertThat(projection).isPresent();
        assertThat(projection.orElseThrow().getUsername()).isNotBlank();

        java.util.Optional<TestUserView> projectionNotFound = repository.findById(999999L, TestUserView.class);
        assertThat(projectionNotFound).isEmpty();
    }

    @Test
    void shouldSupportInsertUpdateDeleteByRepository() {
        ProjectionRepository<User, Long> repository = repositoryFactory.create(User.class);

        User entity = new User();
        entity.setUsername("user_crud_repo");
        entity.setEnabled(true);
        entity.setStatus(UserStatus.ACTIVE);

        int created = repository.insert(entity);
        assertThat(created).isEqualTo(1);
        assertThat(entity.getId()).isNotNull();

        entity.setUsername("user_crud_repo_updated");
        int updated = repository.updateNonNull(entity);
        assertThat(updated).isEqualTo(1);

        QueryWrapper<User> byId = new QueryWrapper<User>().eq("id", entity.getId());
        List<TestUserView> records = repository.findByQuery(byId, TestUserView.class);
        assertThat(records).hasSize(1);
        assertThat(records.getFirst().getUsername()).isEqualTo("user_crud_repo_updated");

        int deleted = repository.deleteById(entity.getId());
        assertThat(deleted).isEqualTo(1);
        assertThat(repository.existsByQuery(byId)).isFalse();
    }

    @Test
    void shouldSupportInsertUpdateDeleteBySimpleJdbcStyleRepository() {
        SimpleProjectionQueryRepository<User> repository =
                simpleProjectionQueryRepositoryFactory.create(User.class);

        User entity = new User();
        entity.setUsername("user_crud_simple");
        entity.setEnabled(false);
        entity.setStatus(UserStatus.DISABLED);

        assertThat(repository.insert(entity)).isEqualTo(1);
        assertThat(entity.getId()).isNotNull();

        entity.setEnabled(true);
        entity.setStatus(UserStatus.ACTIVE);
        assertThat(repository.updateNonNull(entity)).isEqualTo(1);

        QueryWrapper<User> byId = new QueryWrapper<User>().eq("id", entity.getId());
        assertThat(repository.existsByQuery(byId)).isTrue();

        assertThat(repository.deleteByQuery(byId)).isEqualTo(1);
        assertThat(repository.existsByQuery(byId)).isFalse();
    }

    @Test
    void shouldPrioritizeMyBatisMappedCreateMethodOverProjectionRepositoryCreate() {
        UserHybridCreateMapper repository = projectionRepositoryProxyFactory.create(UserHybridCreateMapper.class);

        User entity = new User();
        entity.setUsername("user_hybrid_mybatis_priority");
        entity.setEnabled(true);
        entity.setStatus(UserStatus.ACTIVE);

        int created = repository.create(entity);
        assertThat(created).isEqualTo(1);
        assertThat(entity.getId()).isNotNull();

        Integer persistedStatus = jdbcTemplate.queryForObject(
                "select status from t_user where id = ?",
                Integer.class,
                entity.getId()
        );
        assertThat(persistedStatus).isEqualTo(2);
    }

    @Test
    void shouldUseMapperScannedBeanWithoutProjectionFactoryConflict() {
        User entity = new User();
        entity.setUsername("user_mapper_scanned_no_conflict");
        entity.setEnabled(true);
        entity.setStatus(UserStatus.ACTIVE);

        int created = mapperScannedHybridMapper.create(entity);
        assertThat(created).isEqualTo(1);
        assertThat(entity.getId()).isNotNull();
    }

    @Test
    void shouldFallbackToProjectionRepositoryInsertWhenNoMyBatisMappedStatement() {
        UserPlainProjectionRepository repository =
                projectionRepositoryProxyFactory.create(UserPlainProjectionRepository.class);

        User entity = new User();
        entity.setUsername("user_hybrid_projection_fallback");
        entity.setEnabled(true);
        entity.setStatus(UserStatus.ACTIVE);

        int created = repository.insert(entity);
        assertThat(created).isEqualTo(1);
        assertThat(entity.getId()).isNotNull();

        Integer persistedStatus = jdbcTemplate.queryForObject(
                "select status from t_user where id = ?",
                Integer.class,
                entity.getId()
        );
        assertThat(persistedStatus).isEqualTo(1);
    }

    @Test
    void shouldSupportAnnotatedMethodAndProjectionMethodsWithoutMapperAnnotation() {
        UserAnnotatedProjectionRepository localAnnotatedRepo =
                projectionRepositoryProxyFactory.create(UserAnnotatedProjectionRepository.class);

        User entity = new User();
        entity.setUsername("user_annotation_projection_combo");
        entity.setEnabled(true);
        entity.setStatus(UserStatus.ACTIVE);

        assertThat(localAnnotatedRepo.createAnnotated(entity)).isEqualTo(1);
        assertThat(entity.getId()).isNotNull();

        Integer customMethodStatus = jdbcTemplate.queryForObject(
                "select status from t_user where id = ?",
                Integer.class,
                entity.getId()
        );
        assertThat(customMethodStatus).isEqualTo(2);

        User fallbackEntity = new User();
        fallbackEntity.setUsername("user_annotation_projection_fallback");
        fallbackEntity.setEnabled(true);
        fallbackEntity.setStatus(UserStatus.ACTIVE);

        assertThat(localAnnotatedRepo.insert(fallbackEntity)).isEqualTo(1);
        assertThat(fallbackEntity.getId()).isNotNull();

        Integer projectionFallbackStatus = jdbcTemplate.queryForObject(
                "select status from t_user where id = ?",
                Integer.class,
                fallbackEntity.getId()
        );
        assertThat(projectionFallbackStatus).isEqualTo(1);
    }

    @Test
    void shouldSupportSaveAndUpdateSemantics() {
        ProjectionRepository<User, Long> repository = repositoryFactory.create(User.class);

        User entity = new User();
        entity.setUsername("user_save_insert");
        entity.setEnabled(true);
        entity.setStatus(UserStatus.ACTIVE);

        assertThat(repository.save(entity)).isEqualTo(1);
        assertThat(entity.getId()).isNotNull();
        assertThat(repository.existsById(entity.getId())).isTrue();

        entity.setUsername("user_save_update");
        assertThat(repository.save(entity)).isEqualTo(1);

        User refreshed = repository.findRequiredById(entity.getId());
        assertThat(refreshed.getUsername()).isEqualTo("user_save_update");

        entity.setEnabled(null);
        assertThatThrownBy(() -> repository.update(entity))
                .isInstanceOf(org.apache.ibatis.exceptions.PersistenceException.class)
                .hasMessageContaining("NULL not allowed for column");
    }

    @Test
    void shouldSupportFindAllAndDeleteAllApis() {
        ProjectionRepository<User, Long> repository = repositoryFactory.create(User.class);

        List<User> initial = repository.findAll();
        assertThat(initial).isNotEmpty();
        assertThat(repository.count()).isEqualTo(initial.size());

        List<Long> ids = initial.stream().map(User::getId).limit(2).toList();
        List<TestUserView> subset = repository.findAllById(ids, TestUserView.class);
        assertThat(subset).hasSize(ids.size());

        assertThat(repository.deleteAllById(ids)).isEqualTo(ids.size());
        assertThat(repository.findAllById(ids)).isEmpty();

        long remaining = repository.count();
        assertThat(remaining).isGreaterThanOrEqualTo(1);
        assertThat(repository.deleteAll()).isEqualTo((int) remaining);
        assertThat(repository.count()).isZero();
    }

    @Test
    void shouldSupportInterfaceProjection() {
        ProjectionRepository<User, Long> repository = repositoryFactory.create(User.class);

        QueryWrapper<User> queryWrapper = createEnabledUserQueryWrapper();

        List<TestUserView> result = repository.findByQuery(queryWrapper, TestUserView.class);
        assertThat(result).isNotEmpty();

        TestUserView projection = result.getFirst();
        assertThat(projection.getUsername()).isNotBlank();
        assertThat(projection.getEnabled()).isTrue();
        assertThat(projection.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void shouldThrowWhenProjectionTypeMissingInByteBuddyMapper() {
        assertThatThrownBy(() -> mapperByteBuddyFactory.create(BadProjectionMapperMissingProjectionTypeContract.class, User.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("parameter count must be 2 or 3");
    }

    @Test
    void shouldReuseCachedByteBuddyMapperInstance() {
        UserProjectionMapperContract mapper1 = mapperByteBuddyFactory.create(UserProjectionMapperContract.class, User.class);
        UserProjectionMapperContract mapper2 = mapperByteBuddyFactory.create(UserProjectionMapperContract.class, User.class);

        assertThat(mapper1).isSameAs(mapper2);
    }

    @Test
    void shouldReuseCachedMapperUnderConcurrentCreateCalls() throws Exception {
        int threadCount = 12;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);

        try {
            List<Future<UserProjectionMapperContract>> futures = new ArrayList<>(threadCount);
            for (int i = 0; i < threadCount; i++) {
                futures.add(executorService.submit(() -> {
                    ready.countDown();
                    start.await(5, TimeUnit.SECONDS);
                    return mapperByteBuddyFactory.create(UserProjectionMapperContract.class, User.class);
                }));
            }

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<UserProjectionMapperContract> instances = new ArrayList<>(threadCount);
            for (Future<UserProjectionMapperContract> future : futures) {
                instances.add(future.get(5, TimeUnit.SECONDS));
            }

            assertThat(instances).hasSize(threadCount);
            UserProjectionMapperContract first = instances.getFirst();
            assertThat(instances).allSatisfy(instance -> assertThat(instance).isSameAs(first));
        } finally {
            executorService.shutdownNow();
        }
    }

    @Test
    void shouldValidateMapperSignatureAtCreateStage() {
        assertThatThrownBy(() -> mapperByteBuddyFactory.create(BadProjectionMapperSignatureContract.class, User.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("(queryWrapper, Page, Class)");
    }

    @Test
    void shouldValidatePageSecondArgumentAtCreateStage() {
        assertThatThrownBy(() -> mapperByteBuddyFactory.create(BadProjectionPageParamSignatureContract.class, User.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("parameter count must be 2 or 3");
    }

    @Test
    void shouldValidateFindByQuerySignatureAtCreateStage() {
        assertThatThrownBy(() -> mapperByteBuddyFactory.create(BadProjectionFindByQuerySignatureContract.class, User.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("(queryWrapper, Class)");
    }

    @Test
    void shouldValidateFindByQueryParameterOrderAtCreateStage() {
        assertThatThrownBy(() -> mapperByteBuddyFactory.create(BadProjectionFindByQueryOrderSignatureContract.class, User.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("first argument must be QueryWrapper/LambdaQueryWrapper");
    }

    private QueryWrapper<User> createEnabledUserQueryWrapper() {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.like("username", "user");
        queryWrapper.eq("is_enabled", true);
        return queryWrapper;
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        ProjectionMetadataResolver projectionMetadataResolver(SqlSessionFactory sqlSessionFactory) {
            ProjectionMetadataResolver resolver = new ProjectionMetadataResolver();
            resolver.setConfiguration(sqlSessionFactory.getConfiguration());
            return resolver;
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

        @Bean
        ProjectionRepositoryProxyFactory projectionRepositoryProxyFactory(
                ProjectionRepositoryFactory projectionRepositoryFactory,
                ObjectProvider<SqlSessionFactory> sqlSessionFactoryProvider,
                ObjectProvider<SqlSessionTemplate> sqlSessionTemplateProvider) {
            return new ProjectionRepositoryProxyFactory(
                    projectionRepositoryFactory,
                    sqlSessionFactoryProvider.getIfAvailable(),
                    sqlSessionTemplateProvider.getIfAvailable()
            );
        }
    }

    @SpringBootConfiguration
    static class TestApp {
    }
}
