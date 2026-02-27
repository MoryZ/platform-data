package com.old.silence.data.mybatis.test;

import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;

import javax.sql.DataSource;
import java.sql.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Provider for column metadata from database
 * Adapted for MyBatis Plus
 * 
 * @author moryzang
 */
class ColumnMetaDataProvider {
    
    private static final ConcurrentMap<Class<?>, Map<String, ColumnMetaData>> cache = new ConcurrentHashMap<>();

    /**
     * Get column metadata for specified entity type and column name
     * 
     * @param dataSource data source
     * @param entityType entity type
     * @param columnName column name (database column name)
     * @return column metadata
     */
    static ColumnMetaData getColumnMetaData(DataSource dataSource, Class<?> entityType, String columnName) {
        Map<String, ColumnMetaData> columnMetaDatas = getColumnMetaDatas(dataSource, entityType);
        return columnMetaDatas.get(columnName);
    }

    private static Map<String, ColumnMetaData> getColumnMetaDatas(DataSource dataSource, Class<?> entityType) {
        return cache.computeIfAbsent(entityType, type -> loadColumnMetaDatas(dataSource, type));
    }

    private static Map<String, ColumnMetaData> loadColumnMetaDatas(DataSource dataSource, Class<?> entityType) {
        TableInfo tableInfo = TableInfoHelper.getTableInfo(entityType);
        if (tableInfo == null) {
            throw new IllegalArgumentException("No TableInfo found for entity type: " + entityType);
        }

        String tableName = tableInfo.getTableName();
        String sql = "SELECT * FROM " + tableName + " WHERE 1 <> 1";

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            Map<String, ColumnMetaData> metaDataMap = new HashMap<>();
            ResultSetMetaData metaData = resultSet.getMetaData();

            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                String columnName = metaData.getColumnName(i);
                ColumnMetaData columnMetaData = new ColumnMetaData(
                    columnName,
                    metaData.getColumnType(i),
                    metaData.getPrecision(i),
                    metaData.getScale(i),
                    metaData.isNullable(i) == ResultSetMetaData.columnNullable,
                    metaData.isSigned(i)
                );
                
                metaDataMap.put(columnName, columnMetaData);
            }

            return Collections.unmodifiableMap(metaDataMap);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to load column metadata for table: " + tableName, e);
        }
    }
}
