package com.old.silence.data.mybatis.projection;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * Test mapper for projection integration tests.
 */
@Mapper
public interface TestUserMapper extends BaseMapper<TestUser> {
}
