package com.old.silence.data.commons.handler;

import org.apache.ibatis.reflection.MetaObject;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;

/**
 * @author moryzang
 */
public class DefaultMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        // 默认不进行任何字段填充
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        // 默认不进行任何字段填充
    }
}