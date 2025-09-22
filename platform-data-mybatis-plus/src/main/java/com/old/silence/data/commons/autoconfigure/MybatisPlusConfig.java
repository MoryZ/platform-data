package com.old.silence.data.commons.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.core.injector.AbstractSqlInjector;
import com.baomidou.mybatisplus.core.injector.DefaultSqlInjector;
import com.baomidou.mybatisplus.core.injector.ISqlInjector;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.old.silence.core.security.UserContextAware;
import com.old.silence.data.commons.handler.AuditorMetaObjectHandler;
import com.old.silence.data.commons.injecter.CustomSqlInjector;

/**
 * @author moryzang
 */
@AutoConfiguration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 添加分页插件（必须配置）
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    // 自定义审计处理器
    @Bean
    @ConditionalOnBean(UserContextAware.class)
    public MetaObjectHandler metaObjectHandler(UserContextAware<String> userContextAware) {
        return new AuditorMetaObjectHandler(userContextAware);
    }


    @Bean
    @ConditionalOnMissingBean({DefaultSqlInjector.class, AbstractSqlInjector.class, ISqlInjector.class})
    public CustomSqlInjector customSqlInjector() {
        return new CustomSqlInjector();
    }

}