package com.old.silence.data.commons.autoconfigure;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusPropertiesCustomizer;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.handlers.AnnotationHandler;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.core.injector.AbstractSqlInjector;
import com.baomidou.mybatisplus.core.injector.DefaultSqlInjector;
import com.baomidou.mybatisplus.core.injector.ISqlInjector;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.old.silence.core.security.TenantContextAware;
import com.old.silence.core.security.UserContextAware;
import com.old.silence.data.commons.handler.AuditorMetaObjectHandler;
import com.old.silence.data.commons.handler.CompositeMetaObjectHandler;
import com.old.silence.data.commons.handler.CustomTenantHandler;
import com.old.silence.data.commons.handler.DefaultMetaObjectHandler;
import com.old.silence.data.commons.handler.AssociationFieldIgnoreAnnotationHandler;
import com.old.silence.data.commons.handler.TenantMetaObjectHandler;
import com.old.silence.data.commons.injecter.CustomSqlInjector;
import com.old.silence.data.commons.tenant.TenantTableRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * @author moryzang
 */
@AutoConfiguration
public class MybatisPlusConfig {

    @Bean
    @ConditionalOnMissingBean(AnnotationHandler.class)
    public AnnotationHandler annotationHandler() {
        return new AssociationFieldIgnoreAnnotationHandler();
    }

    @Bean
    public MybatisPlusPropertiesCustomizer mybatisPlusPropertiesCustomizer(AnnotationHandler annotationHandler) {
        return properties -> {
            GlobalConfig globalConfig = properties.getGlobalConfig();
            if (globalConfig == null) {
                globalConfig = new GlobalConfig();
            }
            globalConfig.setAnnotationHandler(annotationHandler);
            properties.setGlobalConfig(globalConfig);
        };
    }

    @Bean
    public TenantTableRegistry tenantTableRegistry() {
        return new TenantTableRegistry();
    }

    @Bean
    @ConditionalOnBean(TenantContextAware.class)
    public TenantLineHandler customTenantHandler(TenantContextAware<String> tenantContextAware,
                                                 TenantTableRegistry tenantTableRegistry) {
        return new CustomTenantHandler(tenantContextAware, tenantTableRegistry);
    }

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(ObjectProvider<TenantLineHandler> tenantLineHandlerProvider) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 租户插件必须先添加，确保先执行租户条件，再进行分页
        tenantLineHandlerProvider.ifAvailable(handler -> {
            TenantLineInnerInterceptor tenantInterceptor = new TenantLineInnerInterceptor();
            tenantInterceptor.setTenantLineHandler(handler);
            interceptor.addInnerInterceptor(tenantInterceptor);
        });

        // 分页插件后添加，在租户条件之后执行
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));

        return interceptor;
    }

    // 使用 ObjectProvider 解决多个 MetaObjectHandler 的问题
    @Bean
    public MetaObjectHandler metaObjectHandler(
            ObjectProvider<UserContextAware<String>> userContextAwareProvider,
            ObjectProvider<TenantContextAware<String>> tenantContextAwareProvider) {

        List<MetaObjectHandler> handlers = new ArrayList<>();

        // 动态添加可用的处理器
        userContextAwareProvider.ifAvailable(userContextAware ->
                handlers.add(new AuditorMetaObjectHandler(userContextAware)));

        tenantContextAwareProvider.ifAvailable(tenantContextAware ->
                handlers.add(new TenantMetaObjectHandler(tenantContextAware)));

        // 返回适当的处理器
        if (handlers.isEmpty()) {
            return new DefaultMetaObjectHandler();
        } else if (handlers.size() == 1) {
            return handlers.getFirst();
        } else {
            return new CompositeMetaObjectHandler(handlers);
        }
    }


    @Bean
    @ConditionalOnMissingBean({DefaultSqlInjector.class, AbstractSqlInjector.class, ISqlInjector.class})
    public CustomSqlInjector customSqlInjector() {
        return new CustomSqlInjector();
    }

}