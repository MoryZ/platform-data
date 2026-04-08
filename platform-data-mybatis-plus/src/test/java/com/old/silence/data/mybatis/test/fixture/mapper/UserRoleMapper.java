package com.old.silence.data.mybatis.test.fixture.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.old.silence.data.mybatis.test.fixture.entity.UserRole;
import org.apache.ibatis.annotations.Mapper;

/**
 * Test mapper for the explicit join entity.
 */
@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {
}