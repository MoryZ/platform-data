package com.old.silence.data.mybatis.test.fixture.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.old.silence.data.mybatis.test.fixture.entity.User;
import com.old.silence.data.mybatis.test.fixture.projection.UserDto;

/**
 * Invalid mapper contract: findByQuery without projection Class parameter.
 */
public interface BadProjectionFindByQuerySignatureContract {

    IPage<UserDto> findByQuery(Wrapper<User> queryWrapper, Page<?> page);
}
