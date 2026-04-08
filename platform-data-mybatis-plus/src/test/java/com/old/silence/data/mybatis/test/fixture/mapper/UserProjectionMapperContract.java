package com.old.silence.data.mybatis.test.fixture.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.old.silence.data.mybatis.test.fixture.entity.User;
import com.old.silence.data.mybatis.test.fixture.projection.TestUserView;

import java.util.List;

/**
 * Test mapper contract for unified findByQuery projection mapping.
 */
public interface UserProjectionMapperContract {

    List<TestUserView> findByQuery(Wrapper<User> queryWrapper,
                                             Class<TestUserView> projectionType);

    IPage<TestUserView> findByQuery(Wrapper<User> queryWrapper,
                                              Page<?> page,
                                              Class<TestUserView> projectionType);
}
