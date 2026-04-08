package com.old.silence.data.mybatis.test.fixture.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.old.silence.data.mybatis.test.fixture.entity.User;
import com.old.silence.data.mybatis.test.fixture.projection.UserDto;

import java.util.List;

/**
 * Invalid mapper contract: PAGE mode requires the second argument to be Page.
 */
public interface BadProjectionPageParamSignatureContract {

    IPage<UserDto> findByQuery(Wrapper<User> queryWrapper,
                               List<OrderItem> orderItems,
                               Page<?> page,
                               Class<UserDto> projectionType);
}
