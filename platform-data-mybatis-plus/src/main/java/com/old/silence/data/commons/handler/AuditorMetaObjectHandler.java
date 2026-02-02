package com.old.silence.data.commons.handler;

import java.time.Instant;

import org.apache.ibatis.reflection.MetaObject;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.old.silence.core.security.UserContextAware;

/**
 * @author moryzang
 */
public class AuditorMetaObjectHandler implements MetaObjectHandler {

    private final UserContextAware<String> userContextAware;

    // 通过构造函数注入
    public AuditorMetaObjectHandler(UserContextAware<String> userContextAware) {
        this.userContextAware = userContextAware;
    }

    // 插入时自动填充
    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createdDate", Instant::now, Instant.class);
        this.strictInsertFill(metaObject, "updatedDate", Instant::now, Instant.class);
        this.strictInsertFill(metaObject, "createdBy", this::getCurrentAuditor, String.class);
        this.strictInsertFill(metaObject, "updatedBy", this::getCurrentAuditor, String.class);

    }

    // 更新时自动填充
    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updatedDate", Instant::now, Instant.class);
        this.strictUpdateFill(metaObject, "updatedBy", this::getCurrentAuditor, String.class);
    }

    private String getCurrentAuditor() {
        return userContextAware.getCurrentAuditor().orElse("SYSTEM");
    }
}
