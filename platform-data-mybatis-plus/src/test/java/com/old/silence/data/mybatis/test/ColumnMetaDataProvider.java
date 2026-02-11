package com.old.silence.data.mybatis.test;

import java.lang.reflect.UndeclaredThrowableException;
import java.sql.DatabaseMetaData;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.sql.DataSource;

import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;
import org.springframework.test.context.jdbc.Sql;

/**
 * @author moryzang
 */
class ColumnMetaDataProvider {
    private static final ConcurrentMap<JdbcPersistentEntity<?>, Map<SqlIdentifier, ColumnMetaData>> cache = new ConcurrentHashMap<>();

    static ColumnMetaData getColumnMetaData(DataSource dataSource, JdbcPersistentPropertyPathExtension propertyPath) {
        var columnMetaDatas = getColumnMetaDatas(dataSource, propertyPath.getTableOwningEntity());
        return columnMetaDatas.getColumnName(propertyPath.getColumnName());
    }

    private static Map<SqlIdentifier, ColumnMetaData> getColumnMetaDatas(DataSource dataSource,
                                                                         JdbcPersistentEntity<?> persistentEntity) {
        return cache.computeIfAbsent(persistentEntity, entity -> loadColumnMetaDatas(dataSource, entity));
    }

    private static Map<SqlIdentifier, ColumnMetaData> loadColumnMetaDatas(DataSource dataSource, JdbcPersistentEntity<?> entity) {

        try {
            var tableNameToUse = JdbcUtils.extractDatabaseMetaData(dataSource,
                    dataSourceMetaData -> tableNameToUse(entity.getTableName().getReference(), dataSourceMetaData));
            var sql = "SELECT * FROM " + tableNameToUse + " WHERE 1 <> 0";
            var connection = dataSource.getConnection();
            var statement = connection.createStatement();
            var columns = statement.executeQuery(sql);
            try (connection; statement; columns){

                if (columns == null) {
                    return Collections.emptyMap();
                }

                var propertyColumnMetaDataMap = new HashMap<SqlIdentifier, ColumnMetaData>();

                var metaData = columns.getMetaData();
                for (var i = 0; i < metaData.getColumnCount(); i++) {

                    var columnMetaData = new ColumnMetaData(metaData.getColumnName(i), metaData.getColumnType(i),
                            metaData.getPrecision(i), metaData.getScale(i),
                            metaData.isNullable(i) == ResultSetMetaData.columnNullable, metaData.isSigned(i));

                    var propertyPath = entity.findRequiredPropertyPathColumn(columnMetaData.getColumnName());

                    propertyColumnMetaDataMap.put(propertyPath.getColumnName(), columnMetaData);
                }

                return propertyColumnMetaDataMap;
            }

        } catch (SQLException | MetaDataAccessException e) {
            throw new UndeclaredThrowableException(e);
        }
    }

    private static String tableNameToUse(String tableName, DatabaseMetaData databaseMetaData) throws SQLException {
        if (tableName == null) {
            return null;
        } else if (databaseMetaData.storesUpperCaseIdentifiers()) {
            return tableName.toUpperCase();
        } else if (databaseMetaData.storesLowerCaseIdentifiers()) {
            return tableName.toLowerCase();
        } else  {
            return tableName;
        }
    }
}
