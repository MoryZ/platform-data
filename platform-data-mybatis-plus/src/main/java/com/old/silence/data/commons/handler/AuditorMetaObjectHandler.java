package com.old.silence.data.commons.handler;

import org.apache.ibatis.reflection.MetaObject;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;

import java.time.Instant;

/**
 * @author moryzang
 */
public class AuditorMetaObjectHandler implements MetaObjectHandler {

    // 插入时自动填充
    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createdDate", Instant::now, Instant.class);
        this.strictInsertFill(metaObject, "updatedDate", Instant::now, Instant.class);
        this.strictInsertFill(metaObject, "createdBy", this::getCurrentUsername, String.class);
        this.strictInsertFill(metaObject, "updatedBy", this::getCurrentUsername, String.class);
    }

    // 更新时自动填充
    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updatedDate", Instant::now, Instant.class);
        this.strictUpdateFill(metaObject, "updatedBy", this::getCurrentUsername, String.class);
    }

    // 获取当前用户（需结合安全框架）
    private String getCurrentUsername() {
       /* return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(Authentication::getName)
                .orElse("SYSTEM");*/
        return "SYSTEM";
    }
}
