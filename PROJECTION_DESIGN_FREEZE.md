# Projection Materialization Design Freeze (Step 0)

# 投影物化设计冻结（第 0 步） / Projection Materialization Design Freeze (Step 0)

Date / 日期: 2026-03-30
Status / 状态: Frozen before implementation / 实施前冻结
Scope / 范围: `platform-data-mybatis-plus` projection pipeline only / 仅限 `platform-data-mybatis-plus` 投影管线

## 1. Goals / 目标

1. A single `Class<P> projectionType` path must support interface projections, mutable DTO class projections, and record/final DTO projections.
1. 单一 `Class<P> projectionType` 路径必须同时支持 interface 投影、可变 DTO class 投影、record/final DTO 投影。

2. Repository methods return the target projection directly, without service-layer conversion.
2. Repository 方法直接返回目标投影，不允许依赖 service 层二次转换。

3. Behavior should align with Spring Data JDBC style: uniform repository signature and internal strategy dispatch by `projectionType`.
3. 行为风格对齐 Spring Data JDBC：仓储签名统一，内部按 `projectionType` 做策略分发。

4. Mixed mode must remain stable: MyBatis mapped methods keep priority and projection methods keep fallback semantics.
4. 混合模式必须保持稳定：MyBatis 映射方法优先，Projection 标准方法保持回退语义。

## 2. Non-goals / 非目标

1. No API signature expansion in Step 1.
1. Step 1 不扩展对外 API 签名。

2. No unrelated SQL generation refactor.
2. 不做无关的 SQL 生成重构。

3. No bytecode optimization first; correctness first.
3. 不优先做字节码优化，先保证正确性。

## 3. Current Risk Profile / 当前风险画像

1. Interface projection currently uses row maps and getter-time conversion; query can succeed while getter call fails later.
1. 当前 interface 投影走 row map，并在 getter 调用时才做类型转换；因此查询可能成功，但 getter 后续失败。

2. High-risk conversion scenarios are DB `String` -> Java `Map`, and DB scalar -> enum (`name`, `ordinal`, custom value).
2. 高风险转换场景主要是数据库 `String -> Java Map`，以及数据库标量 -> 枚举（`name`、`ordinal`、自定义 value）。

3. This is not primarily a type-erasure problem; the main issue is conversion timing and converter consistency.
3. 这不是主要由类型擦除导致的问题；根因是转换时机和转换器一致性。

## 4. Frozen Technical Decisions / 已冻结技术决策

1. Use one unified materialization pipeline: `metadata resolve -> row read -> pre-convert values -> materialize target`.
1. 统一采用一条物化管线：`metadata resolve -> row read -> pre-convert values -> materialize target`。

2. Conversion must happen before the target object is exposed.
2. 类型转换必须发生在目标对象暴露之前。

3. Conversion precedence is frozen as: TypeHandler-derived conversion -> explicit projection converter -> default conversion service -> fail fast.
3. 转换优先级冻结为：TypeHandler 派生转换 -> 显式 projection converter -> 默认 conversion service -> 快速失败。

4. Materializer strategy matrix is frozen as: interface -> interface materializer; mutable class -> bean/property materializer; record/final class -> constructor materializer.
4. 物化策略矩阵冻结为：interface -> interface materializer；mutable class -> bean/property materializer；record/final class -> constructor materializer。

5. Error contract is frozen: fail during materialization, and include projection type, property name, source runtime type, target type, and safe raw-value preview.
5. 错误契约冻结：必须在物化阶段失败，且错误信息必须包含 projection type、property 名、源运行时类型、目标类型、原始值安全摘要。

## 5. Execution Plan / 执行计划

1. Step 0: freeze design and acceptance gates.
1. Step 0：冻结设计决议和验收门槛。

2. Step 1: stabilize interface path with pre-conversion and remove getter-time conversion failure.
2. Step 1：先稳定 interface 路径，引入前置转换并消除 getter 时失败。

3. Step 2: add constructor-based materialization for record/final DTO.
3. Step 2：为 record/final DTO 增加基于构造器的物化能力。

4. Step 3: align list/page behavior under one conversion/materialization policy.
4. Step 3：统一 list/page 路径下的转换与物化行为。

5. Step 4: add regression matrix and complete test coverage.
5. Step 4：补齐回归矩阵和完整测试覆盖。

## 6. Acceptance Gates / 验收门槛

1. Step 1 gate: interface projection no longer fails on getter access due to type conversion, and `String -> Map` plus enum conversion variants are stable.
1. Step 1 验收：interface 投影不再因 getter 触发类型转换失败，且 `String -> Map` 与枚举多形态转换稳定。

2. Step 2 gate: record/final DTO projections can be materialized without service conversion.
2. Step 2 验收：record/final DTO 投影可在仓储层直接物化，不依赖 service 转换。

3. Step 3 gate: list and page pathways produce equivalent conversion semantics.
3. Step 3 验收：list/page 两条路径产出一致的转换语义。

4. Step 4 gate: regression tests cover mixed-mode priority and conversion edge cases.
4. Step 4 验收：回归测试覆盖 mixed mode 优先级与转换边界场景。

## 7. Tracking Rules / 记录规则

1. `PROGRESS.txt` tracks implementation decisions, validation commands, and risk notes.
1. `PROGRESS.txt` 记录实现决策、验证命令与风险说明。

2. `CHANGELOG.md` keeps externally visible behavior and compatibility notes only.
2. `CHANGELOG.md` 仅记录外部可见行为变化与兼容性说明。

3. Each implementation step must include scope, changed files, and verification command plus result.
3. 每个实施步骤都必须记录范围、变更文件、验证命令和验证结果。
