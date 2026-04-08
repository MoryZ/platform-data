package com.old.silence.data.mybatis.projection;

import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;

/**
 * Centralized TableInfo lookup with lazy initialization fallback.
 */
final class ProjectionTableInfoSupport {

    private ProjectionTableInfoSupport() {
    }

    static TableInfo getTableInfo(Configuration configuration, Class<?> entityType) {
        if (entityType == null || entityType.isInterface() || entityType.isPrimitive()) {
            return null;
        }

        TableInfo tableInfo = TableInfoHelper.getTableInfo(entityType);
        if (tableInfo != null || configuration == null) {
            return tableInfo;
        }

        try {
            MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, entityType.getName());
            assistant.setCurrentNamespace(entityType.getName());
            return TableInfoHelper.initTableInfo(assistant, entityType);
        } catch (Exception ex) {
            return TableInfoHelper.getTableInfo(entityType);
        }
    }
}
