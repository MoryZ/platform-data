# Changelog

变更日志 / Changelog

All notable changes to this repository will be documented in this file.

本仓库的重要变更都会记录在这里。

## [Unreleased]

## [Unreleased] / 未发布

### Added
- Added phase-0 design freeze for projection materialization roadmap: unified `projectionType` support for interface, DTO class, and record/final DTO.
- 新增投影物化路线的第 0 阶段设计冻结，统一 `projectionType` 对 interface、DTO class、record/final DTO 的支持目标。
- Added execution plan and acceptance gates to avoid iterative redesign before implementation.
- 新增执行步骤与验收门槛，避免实现阶段反复改方案。
- Added `ProjectionValueConverter` to normalize interface projection row values before proxy creation.
- 新增 `ProjectionValueConverter`，在 interface 投影创建代理前规范化行数据。
- Added regression tests for interface projection fail-fast conversion and enum or JSON map coercion.
- 新增 interface 投影的快速失败回归测试，覆盖枚举与 JSON Map 转换。
- Added constructor-based projection materialization for record and final DTO projections.
- 新增基于构造器的投影物化能力，支持 record 与 final DTO 投影。
- Added `ProjectionResultMaterializer` to unify map-backed list/page projection materialization.
- 新增 `ProjectionResultMaterializer`，统一 map-backed 的 list/page 投影物化入口。
- Added a generic ByteBuddy projection mapper test contract and regression scenarios for record/final DTO coverage across repository styles.
- 新增泛型 ByteBuddy 投影 mapper 测试契约，并补齐 record/final DTO 在多种仓储风格下的回归场景。
- Added a module-level `platform-data-mybatis-plus/README.md` documenting support matrix, entrypoints, materialization rules, and limitations.
- 新增模块级 `platform-data-mybatis-plus/README.md`，说明支持矩阵、查询入口、物化规则与当前限制。
- Added delivery-oriented module README examples for direct repository, ByteBuddy mapper, and SimpleJdbc-style usage.
- 新增面向交付的模块 README 接入示例，覆盖直接仓储、ByteBuddy mapper 与 SimpleJdbc 风格用法。
- Added projection update operations: `updateProjection` and `updateAllProjection` in `SimpleProjectionRepository` with id-enforced patch semantics.
- 新增投影更新能力：在 `SimpleProjectionRepository` 中实现 `updateProjection` 与 `updateAllProjection`，并采用带 id 约束的补丁式更新语义。
- Added a projection-focused test base `MyBatisProjectionMapperTests` for projection-repository compatibility scenarios.
- 新增 projection 专用测试基类 `MyBatisProjectionMapperTests`，用于 projection-repository 兼容场景验证。

### Changed
- Changed interface projection materialization to pre-convert row-map values before exposing the proxy.
- 调整 interface 投影物化流程，在暴露代理对象前完成 row-map 值转换。
- Changed projection query routing so record/final DTOs use constructor materialization while mutable DTOs keep existing bean mapping path.
- 调整投影查询分流：record/final DTO 走构造器物化，mutable DTO 继续沿用现有 bean 映射路径。
- Changed `SimpleProjectionRepository` to route interface and constructor-based projections through one shared materialization helper.
- 调整 `SimpleProjectionRepository`，让 interface 与构造器投影统一经过共享物化 helper。
- Updated projection usage documentation to reflect interface/mutable DTO/record/final DTO support and mixed-mode guidance.
- 更新投影使用文档，补齐 interface/mutable DTO/record/final DTO 支持说明以及 mixed mode 使用指引。
- Deduplicated projection property key resolution and primitive-wrapper helper logic through `ProjectionPropertyAccessSupport`.
- 通过 `ProjectionPropertyAccessSupport` 收敛投影属性键解析与基础类型辅助逻辑的重复实现。
- Expanded module troubleshooting documentation and cleaned minor unused code warnings.
- 扩充模块排障文档，并清理少量未使用代码告警。
- Reorganized projection tests: moved real tests to `...test.projection` and split fixtures by role into `...test.fixture.{entity,mapper,projection,query}`.
- 调整投影测试结构：真实测试统一迁移到 `...test.projection`，夹具按职责拆分到 `...test.fixture.{entity,mapper,projection,query}`。
- Standardized fixture naming: non-JUnit contracts use `*Contract`; projection target types use `*View` for interfaces and `*Dto` for class/record/final DTOs.
- 统一测试夹具命名：非 JUnit 契约类使用 `*Contract`；投影目标类型采用 interface=`*View`、class/record/final=`*Dto`。
- Updated projection-update verification in base tests to execute when mapper supports projection repository and to fail clearly for BaseMapper-only mappers.
- 更新基类中的投影更新校验逻辑：mapper 支持 projection repository 时执行校验，BaseMapper-only 场景给出明确失败提示。

### Planned
- Normalize projection materialization to fail fast during object creation instead of deferred getter-time conversion.
- 继续将投影物化统一为创建阶段快速失败，而不是延迟到 getter 调用时失败。
- Add stable conversion strategy for `String -> Map` and enum value coercion (`name`/`ordinal`/`value`) in mixed repository mode.
- 继续完善混合模式下 `String -> Map` 与枚举值（`name`/`ordinal`/`value`）的稳定转换策略。
