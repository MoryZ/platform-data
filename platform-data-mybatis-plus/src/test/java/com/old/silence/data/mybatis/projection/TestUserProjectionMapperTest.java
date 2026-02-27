package com.old.silence.data.mybatis.projection;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

/**
 * Test mapper contract for unified findByQuery projection mapping.
 */
public interface TestUserProjectionMapperTest {

    List<TestUserProjectionView> findByQuery(TestUserQuery query,
                                             Class<TestUserProjectionView> projectionType);

    IPage<TestUserProjectionView> findByQuery(TestUserQuery query,
                                              Page<?> page,
                                              List<OrderItem> orderItems,
                                              Class<TestUserProjectionView> projectionType);

    List<TestUserProjectionView> findByQuery(TestUserQuery query,
                                             Class<TestUserProjectionView> projectionType,
                                             String fields);

    List<TestUserProjectionView> findByQuery(TestUserQuery query,
                                             Class<TestUserProjectionView> projectionType,
                                             String[] fields);

    IPage<TestUserProjectionView> findByQuery(TestUserQuery query,
                                              Page<?> page,
                                              List<OrderItem> orderItems,
                                              Class<TestUserProjectionView> projectionType,
                                              String fields);

    IPage<TestUserProjectionView> findByQuery(TestUserQuery query,
                                              Page<?> page,
                                              List<OrderItem> orderItems,
                                              Class<TestUserProjectionView> projectionType,
                                              String[] fields);
}
