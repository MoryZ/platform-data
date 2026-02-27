package com.old.silence.data.mybatis.projection;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

/**
 * Invalid mapper contract: findByQuery first argument must be query object, not Page.
 */
public interface BadProjectionFindByQueryOrderSignatureTest {

    List<TestUserProjection> findByQuery(Page<?> page, Class<TestUserProjection> projectionType);
}
