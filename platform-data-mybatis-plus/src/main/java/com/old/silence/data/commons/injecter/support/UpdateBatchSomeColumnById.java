package com.old.silence.data.commons.injecter.support;

import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;
import com.baomidou.mybatisplus.core.injector.AbstractMethod;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * @author moryzang
 */
public class UpdateBatchSomeColumnById extends AbstractMethod {

    private static final String METHOD_NAME = "updateAll";

    private Predicate<TableFieldInfo> predicate;

    public UpdateBatchSomeColumnById() {
        super("updateAll");
    }

    public UpdateBatchSomeColumnById(Predicate<TableFieldInfo> predicate) {
        super("updateAll");
        this.predicate = predicate;
    }

    public UpdateBatchSomeColumnById(String name, Predicate<TableFieldInfo> predicate) {
        super(name);
        this.predicate = predicate;
    }

    @Override
    public MappedStatement injectMappedStatement(Class<?> mapperClass, Class<?> modelClass, TableInfo tableInfo) {
        String sql = "<script>\n" +
                "<foreach collection=\"list\" item=\"item\" separator=\";\">\n" +
                "UPDATE %s %s WHERE %s=#{%s} %s\n" +
                "</foreach>\n" +
                "</script>";
        String tableName = tableInfo.getTableName();

        // 使用自定义的 setSql
        String setSql = buildCustomSetSql(tableInfo);

        String keyProperty = tableInfo.getKeyProperty();
        String keyColumn = tableInfo.getKeyColumn();
        String where = "";
        if (tableInfo.isWithLogicDelete()) {
            where = tableInfo.getLogicDeleteSql(true, true);
        }
        String sqlResult = String.format(sql, tableName, setSql, keyColumn, "item." + keyProperty, where);
        SqlSource sqlSource = languageDriver.createSqlSource(configuration, sqlResult, modelClass);
        return this.addUpdateMappedStatement(mapperClass, modelClass, METHOD_NAME, sqlSource);
    }

    /**
     * 构建自定义的 SET SQL
     */
    private String buildCustomSetSql(TableInfo tableInfo) {
        List<String> setClauses = new ArrayList<>();

        // 1. 根据 Predicate 过滤普通字段
        tableInfo.getFieldList().stream()
                .filter(predicate)
                .forEach(field -> {
                    // 排除主键字段
                    if (!tableInfo.getKeyProperty().equals(field.getProperty())) {
                        setClauses.add(field.getColumn() + "=#{item." + field.getProperty() + "}");
                    }
                });

        // 2. 特殊处理乐观锁字段（总是自动+1，不受 Predicate 影响）
        if (tableInfo.getVersionFieldInfo() != null) {
            setClauses.add(tableInfo.getVersionFieldInfo().getColumn() + "=" +
                    tableInfo.getVersionFieldInfo().getColumn() + "+1");
        }

        // 3. 处理逻辑删除字段（如果 Predicate 允许的话）
        if (tableInfo.getLogicDeleteFieldInfo() != null &&
                predicate.test(tableInfo.getLogicDeleteFieldInfo())) {
            setClauses.add(tableInfo.getLogicDeleteFieldInfo().getColumn() +
                    "=#{item." + tableInfo.getLogicDeleteFieldInfo().getProperty() + "}");
        }

        if (setClauses.isEmpty()) {
            // 如果没有字段需要更新，至少更新一个字段避免语法错误
            setClauses.add(tableInfo.getKeyColumn() + "=" + tableInfo.getKeyColumn());
        }

        return "SET " + String.join(", ", setClauses);
    }

}
