package com.old.silence.data.mybatis.test.fixture.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.old.silence.data.mybatis.test.fixture.entity.Department;
import org.apache.ibatis.annotations.Mapper;

/**
 * Test mapper for association target entity.
 */
@Mapper
public interface DepartmentMapper extends BaseMapper<Department> {
}
