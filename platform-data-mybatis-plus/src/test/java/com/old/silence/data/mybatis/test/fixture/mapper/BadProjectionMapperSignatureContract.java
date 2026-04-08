package com.old.silence.data.mybatis.test.fixture.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.old.silence.data.mybatis.test.fixture.entity.User;
import com.old.silence.data.mybatis.test.fixture.projection.UserDto;

import java.util.List;

/**
 * Invalid mapper signature contract for create-time validation test.
 */
public interface BadProjectionMapperSignatureContract {

    List<UserDto> findByQuery(Wrapper<User> queryWrapper, Page<?> page, Class<UserDto> projectionType);
}
