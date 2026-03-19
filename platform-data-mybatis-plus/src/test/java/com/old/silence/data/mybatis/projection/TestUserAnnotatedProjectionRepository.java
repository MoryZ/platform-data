package com.old.silence.data.mybatis.projection;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;

/**
 * Projection repository with MyBatis annotation statements but without @Mapper.
 * Provides default implementations for ProjectionRepository methods.
 */
@ProjectionNoRepositoryBean
public interface TestUserAnnotatedProjectionRepository extends ProjectionRepository<TestUser, Long> {

    @Insert("INSERT INTO test_user (username, is_enabled, status) VALUES (#{username}, #{enabled}, 2)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int createAnnotated(TestUser entity);
}
