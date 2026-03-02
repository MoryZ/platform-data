package com.old.silence.data.mybatis.projection;

import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator;
import org.apache.ibatis.executor.keygen.NoKeyGenerator;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.scripting.LanguageDriver;

import java.lang.reflect.Method;
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
    private static final String INSERT_STATEMENT_SUFFIX = ".insertEntity";
    private static final String UPDATE_BY_ID_STATEMENT_SUFFIX = ".updateEntityById";
    private static final String UPDATE_ALL_BY_ID_STATEMENT_SUFFIX = ".updateAllEntityById";
    private static final String DELETE_BY_ID_STATEMENT_SUFFIX = ".deleteEntityById";
    private static final String DELETE_BY_QUERY_STATEMENT_SUFFIX = ".deleteEntityByQuery";
    private static final String DELETE_ALL_STATEMENT_SUFFIX = ".deleteAllEntity";

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

    String ensureInsertStatement(Configuration configuration, Class<?> entityType, TableInfo tableInfo) {
        String statementId = entityType.getName() + INSERT_STATEMENT_SUFFIX;
        if (configuration.hasStatement(statementId)) {
            return statementId;
        }

        StringBuilder columns = new StringBuilder();
        StringBuilder values = new StringBuilder();

        String keyProperty = tableInfo.getKeyProperty();
        String keyColumn = tableInfo.getKeyColumn();
        if (keyProperty != null && !keyProperty.isBlank() && keyColumn != null && !keyColumn.isBlank()) {
            columns.append("<if test='").append(keyProperty).append(" != null'>")
                    .append(keyColumn).append(",</if>");
            values.append("<if test='").append(keyProperty).append(" != null'>#{")
                    .append(keyProperty).append("},</if>");
        }

        for (TableFieldInfo fieldInfo : tableInfo.getFieldList()) {
            columns.append(fieldInfo.getColumn()).append(",");
            values.append("#{").append(buildParameterExpression(fieldInfo)).append("},");
        }

        String sql = "<script>INSERT INTO " + tableInfo.getTableName()
                + " <trim prefix='(' suffix=')' suffixOverrides=','>" + columns + "</trim>"
                + " VALUES <trim prefix='(' suffix=')' suffixOverrides=','>" + values + "</trim>"
                + "</script>";

        LanguageDriver languageDriver = configuration.getDefaultScriptingLanguageInstance();
        SqlSource sqlSource = languageDriver.createSqlSource(configuration, sql, entityType);

        MappedStatement.Builder builder = new MappedStatement.Builder(configuration,
                statementId,
                sqlSource,
                SqlCommandType.INSERT);

        if (keyProperty != null && !keyProperty.isBlank()) {
            builder.keyProperty(keyProperty);
            if (keyColumn != null && !keyColumn.isBlank()) {
                builder.keyColumn(keyColumn);
            }
            builder.keyGenerator(Jdbc3KeyGenerator.INSTANCE);
        } else {
            builder.keyGenerator(NoKeyGenerator.INSTANCE);
        }

        configuration.addMappedStatement(builder.build());
        return statementId;
    }

    String ensureUpdateByIdStatement(Configuration configuration, Class<?> entityType, TableInfo tableInfo) {
        String statementId = entityType.getName() + UPDATE_BY_ID_STATEMENT_SUFFIX;
        if (configuration.hasStatement(statementId)) {
            return statementId;
        }

        String keyProperty = tableInfo.getKeyProperty();
        String keyColumn = tableInfo.getKeyColumn();
        if (keyProperty == null || keyProperty.isBlank() || keyColumn == null || keyColumn.isBlank()) {
            throw new IllegalArgumentException("No @TableId found for entity type: " + entityType.getName());
        }

        StringBuilder setClause = new StringBuilder();
        for (TableFieldInfo fieldInfo : tableInfo.getFieldList()) {
            setClause.append("<if test='").append(fieldInfo.getProperty()).append(" != null'>")
                    .append(fieldInfo.getColumn()).append("=#{").append(buildParameterExpression(fieldInfo)).append("},</if>");
        }

        String sql = "<script>UPDATE " + tableInfo.getTableName()
                + " <set>" + setClause + "</set>"
                + " WHERE " + keyColumn + "=#{" + keyProperty + "}"
                + "</script>";

        LanguageDriver languageDriver = configuration.getDefaultScriptingLanguageInstance();
        SqlSource sqlSource = languageDriver.createSqlSource(configuration, sql, entityType);

        MappedStatement.Builder builder = new MappedStatement.Builder(configuration,
                statementId,
                sqlSource,
                SqlCommandType.UPDATE);
        builder.keyGenerator(NoKeyGenerator.INSTANCE);

        configuration.addMappedStatement(builder.build());
        return statementId;
    }

    String ensureUpdateAllByIdStatement(Configuration configuration, Class<?> entityType, TableInfo tableInfo) {
        String statementId = entityType.getName() + UPDATE_ALL_BY_ID_STATEMENT_SUFFIX;
        if (configuration.hasStatement(statementId)) {
            return statementId;
        }

        String keyProperty = tableInfo.getKeyProperty();
        String keyColumn = tableInfo.getKeyColumn();
        if (keyProperty == null || keyProperty.isBlank() || keyColumn == null || keyColumn.isBlank()) {
            throw new IllegalArgumentException("No @TableId found for entity type: " + entityType.getName());
        }

        StringBuilder setClause = new StringBuilder();
        for (TableFieldInfo fieldInfo : tableInfo.getFieldList()) {
            setClause.append(fieldInfo.getColumn())
                    .append("=#{")
                    .append(buildParameterExpression(fieldInfo))
                    .append("},");
        }

        String sql = "<script>UPDATE " + tableInfo.getTableName()
                + " <set>" + setClause + "</set>"
                + " WHERE " + keyColumn + "=#{" + keyProperty + "}"
                + "</script>";

        LanguageDriver languageDriver = configuration.getDefaultScriptingLanguageInstance();
        SqlSource sqlSource = languageDriver.createSqlSource(configuration, sql, entityType);

        MappedStatement.Builder builder = new MappedStatement.Builder(configuration,
                statementId,
                sqlSource,
                SqlCommandType.UPDATE);
        builder.keyGenerator(NoKeyGenerator.INSTANCE);

        configuration.addMappedStatement(builder.build());
        return statementId;
    }

    private String buildParameterExpression(TableFieldInfo fieldInfo) {
        String property = fieldInfo.getProperty();
        Class<?> typeHandler = resolveTypeHandler(fieldInfo);
        if (typeHandler == null || typeHandler == org.apache.ibatis.type.UnknownTypeHandler.class) {
            return property;
        }
        return property + ",typeHandler=" + typeHandler.getName();
    }

    private Class<?> resolveTypeHandler(TableFieldInfo fieldInfo) {
        try {
            Method getter = TableFieldInfo.class.getMethod("getTypeHandler");
            Object value = getter.invoke(fieldInfo);
            if (value instanceof Class<?> handlerClass) {
                return handlerClass;
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }

    String ensureDeleteByIdStatement(Configuration configuration, Class<?> entityType, TableInfo tableInfo) {
        String statementId = entityType.getName() + DELETE_BY_ID_STATEMENT_SUFFIX;
        if (configuration.hasStatement(statementId)) {
            return statementId;
        }

        String keyProperty = tableInfo.getKeyProperty();
        String keyColumn = tableInfo.getKeyColumn();
        if (keyProperty == null || keyProperty.isBlank() || keyColumn == null || keyColumn.isBlank()) {
            throw new IllegalArgumentException("No @TableId found for entity type: " + entityType.getName());
        }

        Class<?> keyType = tableInfo.getKeyType();
        String keyParameter = keyProperty;
        if (keyType != null) {
            keyParameter = keyParameter + ",javaType=" + keyType.getName();
        }

        String sql = "<script>DELETE FROM " + tableInfo.getTableName() + " WHERE " + keyColumn
            + "=#{" + keyParameter + "}</script>";

        LanguageDriver languageDriver = configuration.getDefaultScriptingLanguageInstance();
        SqlSource sqlSource = languageDriver.createSqlSource(configuration, sql, java.util.Map.class);

        MappedStatement.Builder builder = new MappedStatement.Builder(configuration,
                statementId,
                sqlSource,
                SqlCommandType.DELETE);
        builder.keyGenerator(NoKeyGenerator.INSTANCE);

        configuration.addMappedStatement(builder.build());
        return statementId;
    }

    String ensureDeleteByQueryStatement(Configuration configuration, Class<?> entityType, TableInfo tableInfo) {
        String statementId = entityType.getName() + DELETE_BY_QUERY_STATEMENT_SUFFIX;
        if (configuration.hasStatement(statementId)) {
            return statementId;
        }

        String sql = "<script>DELETE FROM " + tableInfo.getTableName()
                + " ${" + Constants.WRAPPER + ".customSqlSegment}</script>";

        LanguageDriver languageDriver = configuration.getDefaultScriptingLanguageInstance();
        SqlSource sqlSource = languageDriver.createSqlSource(configuration, sql, java.util.Map.class);

        MappedStatement.Builder builder = new MappedStatement.Builder(configuration,
                statementId,
                sqlSource,
                SqlCommandType.DELETE);
        builder.keyGenerator(NoKeyGenerator.INSTANCE);

        configuration.addMappedStatement(builder.build());
        return statementId;
    }

    String ensureDeleteAllStatement(Configuration configuration, Class<?> entityType, TableInfo tableInfo) {
        String statementId = entityType.getName() + DELETE_ALL_STATEMENT_SUFFIX;
        if (configuration.hasStatement(statementId)) {
            return statementId;
        }

        String sql = "<script>DELETE FROM " + tableInfo.getTableName() + "</script>";

        LanguageDriver languageDriver = configuration.getDefaultScriptingLanguageInstance();
        SqlSource sqlSource = languageDriver.createSqlSource(configuration, sql, java.util.Map.class);

        MappedStatement.Builder builder = new MappedStatement.Builder(configuration,
                statementId,
                sqlSource,
                SqlCommandType.DELETE);
        builder.keyGenerator(NoKeyGenerator.INSTANCE);

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
