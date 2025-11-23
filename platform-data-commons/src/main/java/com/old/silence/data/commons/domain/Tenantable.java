package com.old.silence.data.commons.domain;

/**
 * @author moryzang
 */
public interface Tenantable<ID> extends TenantAccessor<ID> {

    void setNamespaceId(ID tenantId);
}
