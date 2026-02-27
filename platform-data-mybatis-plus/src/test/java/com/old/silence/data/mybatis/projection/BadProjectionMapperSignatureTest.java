package com.old.silence.data.mybatis.projection;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

/**
 * Invalid mapper signature contract for create-time validation test.
 */
public interface BadProjectionMapperSignatureTest {

    List<TestUserProjection> findByQuery(TestUserQuery query, Page<?> page, Class<TestUserProjection> projectionType);
}
