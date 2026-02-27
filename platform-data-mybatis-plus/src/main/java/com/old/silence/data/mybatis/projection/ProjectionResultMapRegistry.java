package com.old.silence.data.mybatis.projection;

import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.mapping.ResultMapping.Builder;
import org.apache.ibatis.mapping.ResultFlag;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.TypeHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Registers ResultMap for projection types at runtime.
 */
public class ProjectionResultMapRegistry {

    private static final String RESULT_MAP_SUFFIX = ".ProjectionResultMap";
    private static final String MAP_RESULT_MAP_SUFFIX = ".ProjectionMapResultMap";

    public String registerIfAbsent(Configuration configuration, ProjectionMetadata metadata) {
        String resultMapId = metadata.getProjectionType().getName() + RESULT_MAP_SUFFIX + ":" + metadata.getSelectionKey();
        if (configuration.hasResultMap(resultMapId)) {
            return resultMapId;
        }

        List<ResultMapping> mappings = new ArrayList<>();
        for (ProjectionField field : metadata.getFields()) {
            Builder builder = new ResultMapping.Builder(configuration, field.getPropertyName(), field.getColumnName(),
                    field.getJavaType());

            if (field.isIdField()) {
                builder.flags(Collections.singletonList(ResultFlag.ID));
            }

            Class<? extends TypeHandler<?>> typeHandler = field.getTypeHandler();
            if (typeHandler != null) {
                TypeHandler<?> handler = instantiateTypeHandler(typeHandler, field.getJavaType(), configuration);
                if (handler != null) {
                    builder.typeHandler(handler);
                }
            }

            mappings.add(builder.build());
        }

        ResultMap resultMap = new ResultMap.Builder(configuration, resultMapId,
            metadata.getProjectionType(), mappings).build();

        configuration.addResultMap(resultMap);
        return resultMapId;
    }

    public String registerMapIfAbsent(Configuration configuration, ProjectionMetadata metadata) {
        String resultMapId = metadata.getProjectionType().getName() + MAP_RESULT_MAP_SUFFIX + ":" + metadata.getSelectionKey();
        if (configuration.hasResultMap(resultMapId)) {
            return resultMapId;
        }

        List<ResultMapping> mappings = new ArrayList<>();
        for (ProjectionField field : metadata.getFields()) {
            Builder builder = new ResultMapping.Builder(configuration, field.getPropertyName(), field.getColumnName(),
                    field.getJavaType());

            Class<? extends TypeHandler<?>> typeHandler = field.getTypeHandler();
            if (typeHandler != null) {
                TypeHandler<?> handler = instantiateTypeHandler(typeHandler, field.getJavaType(), configuration);
                if (handler != null) {
                    builder.typeHandler(handler);
                }
            }

            mappings.add(builder.build());
        }

        ResultMap resultMap = new ResultMap.Builder(configuration, resultMapId,
                java.util.Map.class, mappings).build();

        configuration.addResultMap(resultMap);
        return resultMapId;
    }

    private TypeHandler<?> instantiateTypeHandler(Class<? extends TypeHandler<?>> handlerType,
                                                  Class<?> javaType,
                                                  Configuration configuration) {
        try {
            // Prefer constructor(Class) for enum handlers
            try {
                return handlerType.getConstructor(Class.class).newInstance(javaType);
            } catch (NoSuchMethodException ignored) {
                // Fall back to no-arg constructor
                return handlerType.getDeclaredConstructor().newInstance();
            }
        } catch (Exception ex) {
            // Fallback to registry default handler for javaType
            return configuration.getTypeHandlerRegistry().getTypeHandler(javaType);
        }
    }
}
