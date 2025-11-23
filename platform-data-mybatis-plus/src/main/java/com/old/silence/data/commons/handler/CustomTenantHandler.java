package com.old.silence.data.commons.handler;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.old.silence.core.security.TenantContextAware;
import com.old.silence.data.commons.tenant.TenantTableRegistry;

import java.util.Optional;

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
        Optional<String> tenantId = tenantContextAware.getCurrentTenantId();
        if (tenantId.isEmpty()) {
            // 返回一个永远为true的表达式：1=1
            return new EqualsTo(new LongValue(1), new LongValue(1));
        }
        return new StringValue(tenantId.get());
    }


    @Override
    public String getTenantIdColumn() {
        return "namespace_id";
    }

    @Override
    public boolean ignoreTable(String tableName) {
        return !tenantTableRegistry.isTenantTable(tableName);
    }


}