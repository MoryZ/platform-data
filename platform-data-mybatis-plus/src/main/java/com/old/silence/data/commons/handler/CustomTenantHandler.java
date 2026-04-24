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
                .orElse(null); // 没有租户上下文时不添加租户条件
    }


    @Override
    public String getTenantIdColumn() {
        return "namespace_id";
    }

    @Override
    public boolean ignoreTable(String tableName) {
        // 没有租户上下文时，忽略所有表的租户过滤
        if (tenantContextAware.getCurrentTenantId().isEmpty()) {
            return true;
        }
        return !tenantTableRegistry.isTenantTable(tableName);
    }


}