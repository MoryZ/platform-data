package com.old.silence.data.mybatis.projection;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;
import java.util.Map;

final class ProjectionResultMaterializer {

    private ProjectionResultMaterializer() {
    }

    static boolean requiresMapQuery(Class<?> projectionType) {
        return projectionType.isInterface()
                || ProjectionConstructorMaterializer.requiresConstructorMaterialization(projectionType);
    }

    static void validateCollectionCompatibility(Class<?> projectionType, ProjectionMetadata metadata) {
        if (!metadata.getCollectionAssociations().isEmpty()) {
            throw new IllegalArgumentException("Collection association projection for map-backed materialization is not supported yet: "
                    + projectionType.getName());
        }
    }

    static <P> List<P> materializeList(Class<P> projectionType,
                                       ProjectionMetadata metadata,
                                       List<Map<String, Object>> rows) {
        if (projectionType.isInterface()) {
            return InterfaceProjectionFactory.createList(projectionType, metadata, rows);
        }
        if (ProjectionConstructorMaterializer.requiresConstructorMaterialization(projectionType)) {
            return ProjectionConstructorMaterializer.createList(projectionType, metadata, rows);
        }
        throw new IllegalArgumentException("Map-backed materialization is not supported for projection type: "
                + projectionType.getName());
    }

    static <P> IPage<P> materializePage(Page<?> page,
                                        IPage<Map<String, Object>> mapPage,
                                        Class<P> projectionType,
                                        ProjectionMetadata metadata) {
        List<P> records = materializeList(projectionType, metadata, mapPage.getRecords());

        @SuppressWarnings("unchecked")
        IPage<P> result = (IPage<P>) page;
        result.setRecords(records);
        result.setTotal(mapPage.getTotal());
        return result;
    }
}