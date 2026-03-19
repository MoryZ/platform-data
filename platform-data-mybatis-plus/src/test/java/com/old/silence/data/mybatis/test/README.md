# MyBatis Plus 测试框架使用指南

## 概述

这是一个专为 MyBatis Plus 设计的测试框架，提供了完整的 CRUD 操作和投影查询测试支持。

## 关联建模约定（Projection 测试）

- 推荐主路径：`User -> UserRole -> Role`（显式中间实体，适配联合主键中间表）
- 兼容路径：`@ManyToMany + @JoinTable`（保留兼容性覆盖，不作为新用例首选）
- 预期告警：`TestUserRole` 无单一 `@TableId` 时，MyBatis-Plus 会提示 `xxById` 方法不可用；该告警在联合主键中间表测试中是预期行为。

在新增关联投影或条件过滤测试时，优先使用显式中间实体路径。

## 核心特性

✅ **自动化 Mock 数据生成** - 基于数据库元数据自动生成测试数据  
✅ **投影查询支持** - 支持只查询部分字段的投影 DTO  
✅ **类型安全** - 完整的泛型支持和类型检查  
✅ **SQL 性能优化** - 投影查询在 SQL 层面只查询需要的字段  
✅ **Spring Boot 测试集成** - 支持事务回滚、自动配置等

## 快速开始

### 1. 定义实体和 Mapper

```java
@TableName("users")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String email;
    private Integer age;
    private LocalDateTime createTime;
    // getters and setters...
}

public interface UserMapper extends BaseMapper<User> {
}
```

### 2. 创建测试类

```java
@DataMyBatisTest
class UserMapperTest extends MyBatisMapperTests<UserMapper, User, Long> {
    
    @Test
    void testInsert() {
        // 验证插入操作
        verifyInsert();
    }
    
    @Test
    void testSelectById() {
        // 先插入一条数据
        verifyInsert();
        
        // 查询并验证
        User user = verifySelectById(1L);
        assertThat(user.getUsername()).isNotNull();
    }
    
    @Test
    void testUpdate() {
        verifyInsert();
        
        // 更新并验证
        verifyUpdateById(1L, entity -> {
            entity.setAge(30);  // 自定义修改
        });
    }
}
```

### 3. 投影查询测试

```java
// 定义投影 DTO
public class UserDTO {
    private Long id;
    private String username;
    private String email;
    // getters and setters...
}

@Test
void testSelectWithProjection() {
    verifyInsert();
    
    // 使用投影查询（只查询 id, username, email 字段）
    UserDTO dto = verifySelectById(1L, UserDTO.class);
    
    assertThat(dto.getId()).isNotNull();
    assertThat(dto.getUsername()).isNotNull();
    // age 字段不会被查询
}

@Test
void testSelectListWithProjection() {
    verifyInsertBatch(5);
    
    // 批量查询投影
    List<UserDTO> dtos = verifySelectList(UserDTO.class);
    assertThat(dtos).hasSize(5);
}
```

### 4. QueryWrapper 条件查询

```java
@Test
void testSelectByWrapper() {
    verifyInsertBatch(10);
    
    // 使用 QueryWrapper 查询
    QueryWrapper<User> wrapper = new QueryWrapper<>();
    wrapper.gt("age", 25);  // age > 25
    
    List<User> users = verifySelectByWrapper(wrapper);
    assertThat(users).isNotEmpty();
}

@Test
void testSelectByWrapperWithProjection() {
    verifyInsertBatch(10);
    
    QueryWrapper<User> wrapper = new QueryWrapper<>();
    wrapper.like("username", "Test");
    
    // 条件查询 + 投影
    List<UserDTO> dtos = verifySelectByWrapper(wrapper, UserDTO.class);
    assertThat(dtos).isNotEmpty();
}
```

### 5. 分页查询

```java
@Test
void testSelectPage() {
    verifyInsertBatch(20);
    
    // 分页查询：第1页，每页10条
    Page<User> page = new Page<>(1, 10);
    IPage<User> result = verifySelectPage(page, 20, 10);
    
    assertThat(result.getTotal()).isEqualTo(20);
    assertThat(result.getRecords()).hasSize(10);
}

@Test
void testSelectPageWithProjection() {
    verifyInsertBatch(20);
    
    Page<Map<String, Object>> page = new Page<>(1, 10);
    IPage<Map<String, Object>> result = verifySelectPageMaps(page, UserDTO.class, 20, 10);
    
    // 转换为投影对象
    List<UserDTO> dtos = convertPageToProjections(result, UserDTO.class);
    assertThat(dtos).hasSize(10);
}
```

### 6. 删除操作

```java
@Test
void testDelete() {
    verifyInsert();
    
    // 删除并验证
    verifyDeleteById(1L);
    verifyCount(0);
}

@Test
void testDeleteBatch() {
    verifyInsertBatch(5);
    
    // 批量删除
    verifyDeleteBatchIds(List.of(1L, 2L, 3L), 3);
    verifyCount(2);
}
```

## 可用的验证方法

### 查询方法
- `verifySelectById(ID id)` - 根据 ID 查询
- `verifySelectById(ID id, Class<P> projectionType)` - 根据 ID 查询投影
- `verifySelectList()` - 查询所有
- `verifySelectList(Class<P> projectionType)` - 查询所有（投影）
- `verifySelectBatchIds(Collection<ID> ids)` - 批量查询
- `verifySelectByWrapper(QueryWrapper<T> wrapper)` - 条件查询
- `verifySelectPage(Page<T> page, long expectedTotal, long expectedSize)` - 分页查询

### 插入方法
- `verifyInsert()` - 插入单条记录
- `verifyInsert(MockedEntityCustomizer<T> customizer)` - 插入并自定义字段
- `verifyInsertBatch(int count)` - 批量插入

### 更新方法
- `verifyUpdateById(ID id)` - 更新
- `verifyUpdateById(ID id, MockedEntityCustomizer<T> customizer)` - 更新并自定义

### 删除方法
- `verifyDeleteById(ID id)` - 删除
- `verifyDeleteBatchIds(Collection<ID> ids, long expectedDeleted)` - 批量删除
- `verifyDeleteByWrapper(QueryWrapper<T> wrapper, long expectedDeleted)` - 条件删除

### 统计方法
- `verifyCount(long expected)` - 验证总数
- `verifyExists(ID id, boolean expected)` - 验证是否存在

## Mock 数据自定义

```java
@Test
void testInsertWithCustomData() {
    verifyInsert(user -> {
        user.setUsername("customName");
        user.setEmail("custom@example.com");
        // 其他字段会自动生成
    });
}
```

## 投影查询性能说明

投影查询会在 **SQL 层面** 只查询需要的字段，例如：

```java
// 投影类
class UserDTO {
    Long id;
    String username;
}

// 执行投影查询
verifySelectById(1L, UserDTO.class);
```

**生成的 SQL**：
```sql
SELECT id, username FROM users WHERE id = 1  -- 只查询 id 和 username
```

而不是：
```sql
SELECT * FROM users WHERE id = 1  -- 查询所有字段后过滤
```

这确保了真正的性能优化！

## ByteBuddy Mapper 扩展用法

除了直接使用 `ProjectionRepository`，也支持通过 ByteBuddy 在运行时生成 Mapper 接口实现。

```java
public interface UserProjectionMapperTest {

    List<UserView> findByQuery(Wrapper<User> queryWrapper, Class<UserView> projectionType);

    IPage<UserView> findByQuery(Wrapper<User> queryWrapper,
                                Page<?> page,
                                Class<UserView> projectionType);
}

@Autowired
private ProjectionMapperByteBuddyFactory mapperFactory;

@Test
void testByteBuddyMapper() {
    UserProjectionMapperTest mapper = mapperFactory.create(UserProjectionMapperTest.class, User.class);
    QueryWrapper<User> queryWrapper = new QueryWrapper<User>()
            .like("username", "tom")
            .eq("is_enabled", true);
    List<UserView> list = mapper.findByQuery(queryWrapper, UserView.class);
    assertThat(list).isNotNull();
}
```

说明：

1. Mapper 接口建议按测试规范以 `*Test` 结尾。
2. 统一方法名为 `findByQuery`，通过参数形态区分列表查询与分页查询。
3. SDK 调用通过 `Class<?> projectionType` 传入投影类型，字段集合由投影定义决定。
4. 实际执行链路仍复用 `ProjectionRepository`，保持排序/分页与字段映射规则一致。

## SimpleJdbcRepository 风格用法（无 ByteBuddy）

如果你希望像 `SimpleJdbcRepository` 一样，在业务层按 `domainType` 直接拿仓储再调用，可使用 `SimpleProjectionQueryRepositoryFactory`：

```java
@Service
public class UserProjectionService {

    private final SimpleProjectionQueryRepository<User> repository;

    public UserProjectionService(SimpleProjectionQueryRepositoryFactory factory) {
        this.repository = factory.create(User.class);
    }

    public List<UserView> listEnabledUsers() {
        QueryWrapper<User> wrapper = new QueryWrapper<User>()
                .like("username", "tom")
                .eq("is_enabled", true);
        return repository.findByQuery(wrapper, UserView.class);
    }
}
```

说明：

1. 不需要定义 Mapper 接口，也不依赖 ByteBuddy 运行时生成。
2. 调用协议与 `ProjectionRepository` 保持一致（`Wrapper + projectionType + page`）。
3. 适合在 Service 层显式编排查询条件、分页与字段裁剪。

## 注意事项

1. **主键类型**：ID 类型必须实现 `Serializable`
2. **事务回滚**：测试默认启用事务并自动回滚，不会影响数据库数据
3. **自增主键**：插入后会自动回填 ID 到实体对象
4. **审计字段**：`createdBy`、`createTime` 等字段会被跳过，由审计机制自动填充

## 文件结构

```
src/test/java/com/old/silence/data/mybatis/test/
├── AbstractMyBatisPlusMapperTests.java   # 抽象基类（核心测试方法）
├── MyBatisMapperTests.java               # 具体测试基类（继承使用）
├── EntityMockFactory.java                 # Mock 数据工厂
├── ProjectionSupport.java                 # 投影支持工具
├── PropertyValueGenerator.java            # 属性值生成器
├── ColumnMetaDataProvider.java            # 列元数据提供者
├── ColumnMetaData.java                    # 列元数据
├── MockedEntityCustomizer.java            # 自定义接口
├── DataMyBatisTest.java                   # 测试注解
└── autoconfigure/
    └── AutoConfigureDataMyBatis.java      # 自动配置
```

## 技术实现

- **元数据获取**：通过 MyBatis Plus 的 `TableInfoHelper` 获取表信息
- **字段解析**：自动解析投影类的字段并映射到数据库列
- **SQL 优化**：使用 `QueryWrapper.select()` 指定查询字段
- **类型转换**：支持下划线命名和驼峰命名的自动转换
- **随机数据**：基于数据库列元数据生成符合约束的随机值
