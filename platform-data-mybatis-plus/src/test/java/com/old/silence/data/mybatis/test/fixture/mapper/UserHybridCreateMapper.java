package com.old.silence.data.mybatis.test.fixture.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import com.old.silence.data.mybatis.projection.ProjectionRepository;
import com.old.silence.data.mybatis.test.fixture.entity.User;

/**
 * Mapper to verify MyBatis mapped method has priority over ProjectionRepository fallback.
 */
@Mapper
public interface UserHybridCreateMapper extends ProjectionRepository<User, Long> {

    @Insert("INSERT INTO t_user (username, is_enabled, status) VALUES (#{username}, #{enabled}, 2)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int create(User entity);
}
