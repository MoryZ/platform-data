package com.old.silence.data.mybatis.test.fixture.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.old.silence.data.mybatis.test.fixture.entity.User;
import com.old.silence.data.mybatis.test.fixture.projection.UserDto;

import java.util.List;

/**
 * Invalid mapper contract: LIST mode without projectionType and without Class parameter.
 */
public interface BadProjectionMapperMissingProjectionTypeContract {

    List<UserDto> findByQuery(Wrapper<User> queryWrapper);
}
