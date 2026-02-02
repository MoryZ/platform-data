package com.old.silence.core.security;

import java.util.Optional;

/**
 * 多租户上下文感知接口，由业务方实现以提供当前请求的租户 ID。
 *
 * @author moryzang
 */
public interface TenantContextAware<ID> {

    /**
     * 返回当前请求上下文的租户 ID。
     *
     * @return Optional 包装的租户 ID；为空表示当前线程没有租户上下文
     */
    Optional<ID> getCurrentTenantId();
}


