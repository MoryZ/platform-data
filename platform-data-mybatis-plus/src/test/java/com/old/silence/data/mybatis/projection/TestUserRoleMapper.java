package com.old.silence.data.mybatis.projection;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * Test mapper for the explicit join entity.
 */
@Mapper
public interface TestUserRoleMapper extends BaseMapper<TestUserRole> {
}