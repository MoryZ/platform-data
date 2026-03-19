package com.old.silence.data.mybatis.projection;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * Test mapper for association target entity.
 */
@Mapper
public interface TestDepartmentMapper extends BaseMapper<TestDepartment> {
}
