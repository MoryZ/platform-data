package com.old.silence.data.mybatis.projection;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

/**
 * Mapper to verify MyBatis mapped method has priority over ProjectionRepository fallback.
 */
@Mapper
public interface TestUserHybridCreateMapper extends ProjectionRepository<TestUser, Long> {

    @Insert("INSERT INTO test_user (username, is_enabled, status) VALUES (#{username}, #{enabled}, 2)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int create(TestUser entity);
}
