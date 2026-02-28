package com.old.silence.data.mybatis.projection;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

/**
 * Test mapper contract for unified findByQuery projection mapping.
 */
public interface TestUserProjectionMapperTest {

    List<TestUserProjectionView> findByQuery(Wrapper<TestUser> queryWrapper,
                                             Class<TestUserProjectionView> projectionType);

    IPage<TestUserProjectionView> findByQuery(Wrapper<TestUser> queryWrapper,
                                              Page<?> page,
                                              Class<TestUserProjectionView> projectionType);
}
