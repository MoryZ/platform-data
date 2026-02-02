package com.old.silence.data.commons.handler;

import org.apache.ibatis.reflection.MetaObject;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * @author moryzang
 */
public record CompositeMetaObjectHandler(List<MetaObjectHandler> handlers) implements MetaObjectHandler {

    public CompositeMetaObjectHandler(List<MetaObjectHandler> handlers) {
        this.handlers = handlers != null ? handlers : new ArrayList<>();
    }

    @Override
    public void insertFill(MetaObject metaObject) {
        for (MetaObjectHandler handler : handlers) {
            handler.insertFill(metaObject);
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        for (MetaObjectHandler handler : handlers) {
            handler.updateFill(metaObject);
        }
    }
}
