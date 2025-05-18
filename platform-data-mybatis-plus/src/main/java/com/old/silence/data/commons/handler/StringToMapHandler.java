package com.old.silence.data.commons.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;

/**
 * @author moryzang
 */
@MappedTypes(Map.class)    // 定义处理的Java类型
@MappedJdbcTypes(JdbcType.VARCHAR) // 定义处理的JDBC类型
public class StringToMapHandler extends BaseTypeHandler<Map<String, Object>> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // 序列化Map到数据库
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i,
                                    Map<String, Object> parameter,
                                    JdbcType jdbcType) throws SQLException {
        try {
            String json = OBJECT_MAPPER.writeValueAsString(parameter);
            ps.setString(i, json);
        } catch (Exception e) {
            throw new SQLException("Error converting Map to JSON string", e);
        }
    }

    // 以下三个方法用于从结果集反序列化
    @Override
    public Map<String, Object> getNullableResult(ResultSet rs, String columnName)
            throws SQLException {
        return parseJson(rs.getString(columnName));
    }

    @Override
    public Map<String, Object> getNullableResult(ResultSet rs, int columnIndex)
            throws SQLException {
        return parseJson(rs.getString(columnIndex));
    }

    @Override
    public Map<String, Object> getNullableResult(CallableStatement cs, int columnIndex)
            throws SQLException {
        var json = cs.getString(columnIndex);
        return parseJson(json);
    }

    private Map<String, Object> parseJson(String json) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            return OBJECT_MAPPER.readValue(
                    json,
                    new TypeReference<>() {
                    }
            );
        } catch (Exception e) {
            throw new RuntimeException("Error parsing JSON to Map", e);
        }
    }
}

