package com.old.silence.data.mybatis.test.fixture.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.old.silence.data.mybatis.test.fixture.entity.Role;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper for TestRole — ensures TableInfo for TestRole is initialized by MyBatis-Plus.
 */
@Mapper
public interface RoleMapper extends BaseMapper<Role> {
}
