package com.old.silence.data.mybatis.test.fixture.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.old.silence.data.mybatis.test.fixture.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * Test mapper for projection integration tests.
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
