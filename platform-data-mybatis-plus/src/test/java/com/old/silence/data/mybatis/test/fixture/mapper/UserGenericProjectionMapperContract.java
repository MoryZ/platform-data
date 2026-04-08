package com.old.silence.data.mybatis.test.fixture.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.old.silence.data.mybatis.test.fixture.entity.User;

import java.util.List;

/**
 * Generic mapper contract used to verify ByteBuddy projection routing across multiple projection target types.
 */
public interface UserGenericProjectionMapperContract {

    <P> List<P> findByQuery(Wrapper<User> queryWrapper, Class<P> projectionType);

    <P> IPage<P> findByQuery(Wrapper<User> queryWrapper, Page<?> page, Class<P> projectionType);
}