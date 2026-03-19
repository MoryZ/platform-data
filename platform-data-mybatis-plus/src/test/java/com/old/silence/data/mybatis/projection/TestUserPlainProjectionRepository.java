package com.old.silence.data.mybatis.projection;

/**
 * Plain projection repository with no MyBatis mapped statements.
 * Extends ProjectionMapperRepository to ensure TableInfo is initialized via BaseMapper.
 */
public interface TestUserPlainProjectionRepository extends ProjectionMapperRepository<TestUser, Long> {
}
