package com.old.silence.data.mybatis.test.fixture.mapper;

import com.old.silence.data.mybatis.projection.ProjectionMapperRepository;
import com.old.silence.data.mybatis.test.fixture.entity.User;

/**
 * Plain projection repository with no MyBatis mapped statements.
 * Extends ProjectionMapperRepository to ensure TableInfo is initialized via BaseMapper.
 */
public interface UserPlainProjectionRepository extends ProjectionMapperRepository<User, Long> {
}
