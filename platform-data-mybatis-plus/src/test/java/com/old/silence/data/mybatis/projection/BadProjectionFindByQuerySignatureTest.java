package com.old.silence.data.mybatis.projection;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * Invalid mapper contract: findByQuery without projection Class parameter.
 */
public interface BadProjectionFindByQuerySignatureTest {

    IPage<TestUserProjection> findByQuery(TestUserQuery query, Page<?> page);
}
