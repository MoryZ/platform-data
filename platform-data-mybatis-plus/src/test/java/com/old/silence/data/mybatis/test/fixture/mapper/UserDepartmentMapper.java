package com.old.silence.data.mybatis.test.fixture.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.old.silence.data.mybatis.test.fixture.entity.UserDepartment;
import org.apache.ibatis.annotations.Mapper;

/**
 * Test mapper for the explicit join entity between user and department.
 */
@Mapper
public interface UserDepartmentMapper extends BaseMapper<UserDepartment> {
}
