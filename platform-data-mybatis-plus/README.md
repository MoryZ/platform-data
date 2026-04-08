# platform-data-mybatis-plus

MyBatis-Plus data support with projection query capabilities.

MyBatis-Plus 数据支持模块，包含投影查询能力。

## Overview / 概览

This module provides projection-oriented query support on top of MyBatis-Plus.
It allows one repository query contract to return multiple projection target types.

该模块在 MyBatis-Plus 之上提供面向 projection 的查询支持。
它允许同一套 repository 查询协议返回多种投影目标类型。

## Supported Projection Types / 支持的投影类型

1. Interface projection
1. interface 投影

2. Mutable DTO class
2. 可变 DTO class

3. Record DTO
3. record DTO

4. Final immutable DTO
4. final 不可变 DTO

## Query Entrypoints / 查询入口

1. Direct repository usage via `ProjectionRepository<T, ID>`
1. 通过 `ProjectionRepository<T, ID>` 直接查询

2. Runtime-generated mapper usage via `ProjectionMapperByteBuddyFactory`
2. 通过 `ProjectionMapperByteBuddyFactory` 使用运行时生成的 mapper

3. SimpleJdbc-style usage via `SimpleProjectionQueryRepository<T>` or its factory
3. 通过 `SimpleProjectionQueryRepository<T>` 或其 factory 使用 SimpleJdbc 风格查询

## Core Contract / 核心调用协议

The main query method shape is:

核心查询协议为：

```java
<P> List<P> findByQuery(Wrapper<T> queryWrapper, Class<P> projectionType);
<P> IPage<P> findByQuery(Wrapper<T> queryWrapper, Page<?> page, Class<P> projectionType);
```

## Minimal Examples / 最小接入示例

### 1. Direct Repository / 直接仓储用法

```java
public interface UserRepository extends ProjectionMapperRepository<UserEntity, Long> {
}

public interface UserView {
	Long getId();
	String getUsername();
}

public record UserRecordDto(Long id, String username, Boolean enabled) {
}

public final class UserFinalDto {

	private final Long id;
	private final String username;

	@ConstructorProperties({"id", "username"})
	public UserFinalDto(Long id, String username) {
		this.id = id;
		this.username = username;
	}

	public Long getId() {
		return id;
	}

	public String getUsername() {
		return username;
	}
}

@Service
public class UserQueryService {

	private final UserRepository userRepository;

	public UserQueryService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public List<UserView> listViews() {
		QueryWrapper<UserEntity> wrapper = new QueryWrapper<UserEntity>().eq("is_enabled", true);
		return userRepository.findByQuery(wrapper, UserView.class);
	}

	public List<UserRecordDto> listRecords() {
		QueryWrapper<UserEntity> wrapper = new QueryWrapper<UserEntity>().eq("is_enabled", true);
		return userRepository.findByQuery(wrapper, UserRecordDto.class);
	}

	public IPage<UserFinalDto> pageFinalDtos() {
		QueryWrapper<UserEntity> wrapper = new QueryWrapper<UserEntity>().eq("is_enabled", true);
		Page<?> page = new Page<>(1, 20);
		return userRepository.findByQuery(wrapper, page, UserFinalDto.class);
	}
}
```

### 2. ByteBuddy Mapper / ByteBuddy 运行时 Mapper

```java
public interface UserProjectionMapper {

	<P> List<P> findByQuery(Wrapper<UserEntity> queryWrapper, Class<P> projectionType);

	<P> IPage<P> findByQuery(Wrapper<UserEntity> queryWrapper,
							 Page<?> page,
							 Class<P> projectionType);
}

@Service
public class UserMapperFacade {

	private final UserProjectionMapper mapper;

	public UserMapperFacade(ProjectionMapperByteBuddyFactory factory) {
		this.mapper = factory.create(UserProjectionMapper.class, UserEntity.class);
	}

	public List<UserRecordDto> listRecords() {
		QueryWrapper<UserEntity> wrapper = new QueryWrapper<UserEntity>().like("username", "tom");
		return mapper.findByQuery(wrapper, UserRecordDto.class);
	}
}
```

### 3. SimpleJdbc-style / SimpleJdbc 风格

```java
@Service
public class UserSimpleProjectionService {

	private final SimpleProjectionQueryRepository<UserEntity> repository;

	public UserSimpleProjectionService(SimpleProjectionQueryRepositoryFactory factory) {
		this.repository = factory.create(UserEntity.class);
	}

	public Optional<UserFinalDto> findOne(Long id) {
		return repository.findById(id, UserFinalDto.class);
	}

	public List<UserView> listViews() {
		QueryWrapper<UserEntity> wrapper = new QueryWrapper<UserEntity>().eq("is_enabled", true);
		return repository.findByQuery(wrapper, UserView.class);
	}
}
```

### 4. Choosing Projection Type / 如何选择 projectionType

1. Use interface projection for lightweight read-only views.
1. 只读轻量视图优先用 interface projection。

2. Use mutable DTO when you already have setter-based transport objects.
2. 如果已有 setter 风格传输对象，优先用 mutable DTO。

3. Use record DTO when you want the simplest immutable projection contract.
3. 如果想要最简洁的不可变投影契约，优先用 record DTO。

4. Use final DTO when you need immutable objects plus custom constructor semantics.
4. 如果需要不可变对象且要保留自定义构造器语义，使用 final DTO。

## Materialization Rules / 物化规则

1. Interface projections use map-backed materialization with pre-conversion.
1. interface 投影使用基于 map 的物化，并在创建前完成类型转换。

2. Mutable DTO classes use bean/property mapping.
2. 可变 DTO 使用 bean/property 映射。

3. Record DTOs use canonical constructor materialization.
3. record DTO 使用 canonical constructor 物化。

4. Final DTOs use a single constructor or a constructor marked with `@ConstructorProperties`.
4. final DTO 使用单构造器，或使用 `@ConstructorProperties` 标记的构造器。

## Conversion Behavior / 转换行为

1. `String -> Map<String, Object>` is supported through JSON parsing during materialization.
1. 支持在物化阶段通过 JSON 解析完成 `String -> Map<String, Object>`。

2. Enum conversion supports `EnumValue.value`, `Enum.name()`, and `ordinal`.
2. 枚举转换支持 `EnumValue.value`、`Enum.name()` 和 `ordinal`。

3. List and page queries share the same fail-fast conversion semantics.
3. list 与 page 查询共享同一套快速失败转换语义。

## Mixed Mode Notes / mixed mode 说明

If a repository interface contains both MyBatis mapped methods and projection methods,
prefer the framework proxy path so MyBatis-specific methods can take priority while projection methods fall back correctly.

如果仓储接口同时包含 MyBatis 映射方法和 projection 方法，
优先使用框架代理路径，这样 MyBatis 特定方法可以优先执行，而 projection 标准方法能正确回退。

## Current Limitation / 当前限制

Collection association projection is still not supported for the map-backed materialization path
(interface / record / final DTO).

当前 map-backed 物化路径（interface / record / final DTO）仍不支持 collection association 投影。

## Troubleshooting / 常见排障

1. `BindingException: Invalid bound statement`
1. `BindingException: Invalid bound statement`

This usually means the interface was taken over as a plain MyBatis mapper instead of going through the framework projection proxy path.

这通常表示接口被当成了纯 MyBatis mapper，而没有走框架的 projection proxy 路径。

2. `ConflictingBeanDefinitionException`
2. `ConflictingBeanDefinitionException`

This usually means the same repository interface is being registered by both `@MapperScan` and the projection repository registrar.

这通常表示同一个 repository 接口同时被 `@MapperScan` 和 projection repository 注册器注册了。

3. `Constructor parameter names are required for projection ...`
3. `Constructor parameter names are required for projection ...`

For final DTO constructor materialization, keep stable constructor parameter names or use `@ConstructorProperties` explicitly.

对于 final DTO 的构造器物化，请确保构造器参数名稳定可获取，或显式使用 `@ConstructorProperties`。

4. `Collection association projection ... is not supported yet`
4. `Collection association projection ... is not supported yet`

Map-backed projection materialization currently does not support collection association fields. Use mutable DTO bean mapping or split the query.

当前 map-backed 投影物化不支持 collection association 字段。此时请改用 mutable DTO bean 映射，或拆分查询。

5. `Failed to materialize projection field ...`
5. `Failed to materialize projection field ...`

Check the raw database value against the target projection field type, especially for JSON-to-Map conversion and enum value coercion.

重点检查数据库原始值和目标投影字段类型是否匹配，尤其是 JSON 转 Map 和枚举值转换场景。

## Verification / 验证

The module test suite can be run with:

模块测试可通过以下命令执行：

```bash
mvn -pl platform-data-mybatis-plus test
```
