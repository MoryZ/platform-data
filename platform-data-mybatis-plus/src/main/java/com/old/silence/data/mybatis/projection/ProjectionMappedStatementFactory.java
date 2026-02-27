package com.old.silence.data.mybatis.projection;

import com.baomidou.mybatisplus.core.toolkit.Constants;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.scripting.LanguageDriver;

import java.util.Collections;
import java.util.Map;

/**
 * Build and register MappedStatement for projection queries.
 */
class ProjectionMappedStatementFactory {

    private static final String STATEMENT_SUFFIX = ".selectProjection";
    private static final String MAP_STATEMENT_SUFFIX = ".selectProjectionMap";
    private static final String COUNT_STATEMENT_SUFFIX = ".countProjection";
    private static final String COUNT_RESULT_MAP_SUFFIX = ".countProjectionResultMap";
    private static final String MAP_RESULT_MAP_SUFFIX = ".mapProjectionResultMap";

    private final ProjectionResultMapRegistry resultMapRegistry;

    ProjectionMappedStatementFactory(ProjectionResultMapRegistry resultMapRegistry) {
        this.resultMapRegistry = resultMapRegistry;
    }

    String ensureStatement(Configuration configuration, ProjectionMetadata metadata) {
        String statementId = buildStatementId(metadata);
        if (configuration.hasStatement(statementId)) {
            return statementId;
        }

        String columnList = metadata.getColumnList();
        String sql = "<script>SELECT " + columnList + " FROM " + metadata.getTableName() +
                " ${" + Constants.WRAPPER + ".customSqlSegment}</script>";

        LanguageDriver languageDriver = configuration.getDefaultScriptingLanguageInstance();
        SqlSource sqlSource = languageDriver.createSqlSource(configuration, sql, Map.class);

        String resultMapId = resultMapRegistry.registerIfAbsent(configuration, metadata);

        MappedStatement.Builder builder = new MappedStatement.Builder(configuration, statementId, sqlSource,
            SqlCommandType.SELECT);
        builder.resultMaps(Collections.singletonList(configuration.getResultMap(resultMapId)));

        configuration.addMappedStatement(builder.build());
        return statementId;
    }

    String ensureCountStatement(Configuration configuration, ProjectionMetadata metadata) {
        String statementId = buildCountStatementId(metadata);
        if (configuration.hasStatement(statementId)) {
            return statementId;
        }

        String sql = "<script>SELECT COUNT(1) FROM " + metadata.getTableName() +
                " ${" + Constants.WRAPPER + ".customSqlSegment}</script>";

        LanguageDriver languageDriver = configuration.getDefaultScriptingLanguageInstance();
        SqlSource sqlSource = languageDriver.createSqlSource(configuration, sql, Map.class);

        String countResultMapId = metadata.getEntityType().getName() + COUNT_RESULT_MAP_SUFFIX;
        if (!configuration.hasResultMap(countResultMapId)) {
            ResultMap countResultMap = new ResultMap.Builder(configuration, countResultMapId, Long.class,
                    Collections.emptyList()).build();
            configuration.addResultMap(countResultMap);
        }

        MappedStatement.Builder builder = new MappedStatement.Builder(configuration, statementId, sqlSource,
                SqlCommandType.SELECT);
        builder.resultMaps(Collections.singletonList(configuration.getResultMap(countResultMapId)));

        configuration.addMappedStatement(builder.build());
        return statementId;
    }

    String ensureMapStatement(Configuration configuration, ProjectionMetadata metadata) {
        String statementId = buildMapStatementId(metadata);
        if (configuration.hasStatement(statementId)) {
            return statementId;
        }

        String columnList = metadata.getColumnList();
        String sql = "<script>SELECT " + columnList + " FROM " + metadata.getTableName() +
                " ${" + Constants.WRAPPER + ".customSqlSegment}</script>";

        LanguageDriver languageDriver = configuration.getDefaultScriptingLanguageInstance();
        SqlSource sqlSource = languageDriver.createSqlSource(configuration, sql, Map.class);

        String resultMapId = metadata.getEntityType().getName() + MAP_RESULT_MAP_SUFFIX;
        if (!configuration.hasResultMap(resultMapId)) {
            ResultMap mapResultMap = new ResultMap.Builder(configuration, resultMapId, java.util.HashMap.class,
                Collections.emptyList()).build();
            configuration.addResultMap(mapResultMap);
        }

        MappedStatement.Builder builder = new MappedStatement.Builder(configuration, statementId, sqlSource,
                SqlCommandType.SELECT);
        builder.resultMaps(Collections.singletonList(configuration.getResultMap(resultMapId)));

        configuration.addMappedStatement(builder.build());
        return statementId;
    }

    private String buildStatementId(ProjectionMetadata metadata) {
        return metadata.getEntityType().getName() + STATEMENT_SUFFIX + ":" + metadata.getProjectionType().getName()
                + ":" + metadata.getSelectionKey();
    }

    private String buildCountStatementId(ProjectionMetadata metadata) {
        return metadata.getEntityType().getName() + COUNT_STATEMENT_SUFFIX + ":" + metadata.getProjectionType().getName()
                + ":" + metadata.getSelectionKey();
    }

    private String buildMapStatementId(ProjectionMetadata metadata) {
        return metadata.getEntityType().getName() + MAP_STATEMENT_SUFFIX + ":" + metadata.getProjectionType().getName()
                + ":" + metadata.getSelectionKey();
    }
}
