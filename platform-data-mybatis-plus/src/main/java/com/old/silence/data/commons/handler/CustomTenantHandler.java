package com.old.silence.data.commons.handler;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.old.silence.core.security.TenantContextAware;
import com.old.silence.data.commons.tenant.TenantTableRegistry;


/**
 * @author moryzang
 */
public class CustomTenantHandler implements TenantLineHandler {

    private static final String NO_TENANT = "NO_TENANT";
    private final TenantContextAware<String> tenantContextAware;
    private final TenantTableRegistry tenantTableRegistry;

    public CustomTenantHandler(TenantContextAware<String> tenantContextAware,
                               TenantTableRegistry tenantTableRegistry) {
        this.tenantContextAware = tenantContextAware;
        this.tenantTableRegistry = tenantTableRegistry;
    }

    @Override
    public Expression getTenantId() {
        return tenantContextAware.getCurrentTenantId()
                .map(StringValue::new)
                .orElse(new StringValue(NO_TENANT)); // 使用一个特殊的租户值
    }


    @Override
    public String getTenantIdColumn() {
        return "namespace_id";
    }

    @Override
    public boolean ignoreTable(String tableName) {
        String tenantId = tenantContextAware.getCurrentTenantId().orElse(NO_TENANT);
        if (NO_TENANT.equals(tenantId)) {
            return true; // 特殊租户值时忽略租户过滤
        }

        return !tenantTableRegistry.isTenantTable(tableName);
    }


}