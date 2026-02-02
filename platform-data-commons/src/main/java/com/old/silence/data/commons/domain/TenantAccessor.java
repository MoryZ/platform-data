package com.old.silence.data.commons.domain;

import org.springframework.lang.Nullable;

/**
 * @author moryzang
 */
public interface TenantAccessor<ID> {

    @Nullable
    ID getNamespaceId();
}
