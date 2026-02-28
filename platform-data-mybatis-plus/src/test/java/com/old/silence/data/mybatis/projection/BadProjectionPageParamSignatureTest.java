package com.old.silence.data.mybatis.projection;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

/**
 * Invalid mapper contract: PAGE mode requires the second argument to be Page.
 */
public interface BadProjectionPageParamSignatureTest {

    IPage<TestUserProjection> findByQuery(Wrapper<TestUser> queryWrapper,
                                          List<OrderItem> orderItems,
                                          Page<?> page,
                                          Class<TestUserProjection> projectionType);
}
