package com.old.silence.data.mybatis.projection;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * Test mapper for the explicit join entity between user and department.
 */
@Mapper
public interface TestUserDepartmentMapper extends BaseMapper<TestUserDepartment> {
}
