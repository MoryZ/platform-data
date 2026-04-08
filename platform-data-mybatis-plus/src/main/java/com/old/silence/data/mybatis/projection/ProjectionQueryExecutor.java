package com.old.silence.data.mybatis.projection;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Execute projection queries using dynamically registered MappedStatements.
 */
public class ProjectionQueryExecutor {

    private final SqlSessionFactory sqlSessionFactory;
    private final ProjectionMappedStatementFactory statementFactory;

    public ProjectionQueryExecutor(SqlSessionFactory sqlSessionFactory,
                                   ProjectionResultMapRegistry resultMapRegistry) {
        this.sqlSessionFactory = sqlSessionFactory;
        this.statementFactory = new ProjectionMappedStatementFactory(resultMapRegistry);
    }

    public <T, P> List<P> select(Wrapper<T> wrapper, ProjectionMetadata metadata) {
        String statementId = statementFactory.ensureStatement(sqlSessionFactory.getConfiguration(), metadata);
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.WRAPPER, wrapper);

        try (SqlSession session = sqlSessionFactory.openSession()) {
            return session.selectList(statementId, params);
        }
    }

    public <T, P> IPage<P> selectPage(Page<?> page,
                                      Wrapper<T> wrapper,
                                      Wrapper<T> countWrapper,
                                      ProjectionMetadata metadata) {
        String statementId = statementFactory.ensureStatement(sqlSessionFactory.getConfiguration(), metadata);
        String countStatementId = statementFactory.ensureCountStatement(sqlSessionFactory.getConfiguration(), metadata);

        Map<String, Object> params = new HashMap<>();
        params.put(Constants.WRAPPER, wrapper);

        Map<String, Object> countParams = new HashMap<>();
        countParams.put(Constants.WRAPPER, countWrapper);

        long current = page.getCurrent() <= 0 ? 1 : page.getCurrent();
        long size = page.getSize() <= 0 ? 10 : page.getSize();
        int offset = toRowBoundsOffset(current, size);
        int limit = toRowBoundsLimit(size);

        try (SqlSession session = sqlSessionFactory.openSession()) {
            Long total = session.selectOne(countStatementId, countParams);
            List<P> records = session.selectList(statementId, params, new RowBounds(offset, limit));

            @SuppressWarnings("unchecked")
            IPage<P> result = (IPage<P>) page;
            result.setRecords(records);
            result.setTotal(total == null ? 0 : total);
            return result;
        }
    }

    public <T> long selectCount(Wrapper<T> wrapper, Class<T> entityType) {
        TableInfo tableInfo = getRequiredTableInfo(entityType);

        ProjectionMetadata countMetadata = new ProjectionMetadata(entityType,
                entityType,
                tableInfo.getTableName(),
                List.of(),
                "ALL");
        String countStatementId = statementFactory.ensureCountStatement(sqlSessionFactory.getConfiguration(), countMetadata);
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.WRAPPER, wrapper);

        try (SqlSession session = sqlSessionFactory.openSession()) {
            Long total = session.selectOne(countStatementId, params);
            return total == null ? 0L : total;
        }
    }

    public <T> int insert(T entity, Class<T> entityType) {
        Objects.requireNonNull(entity, "Entity must not be null");

        TableInfo tableInfo = getRequiredTableInfo(entityType);
        String statementId = statementFactory.ensureInsertStatement(sqlSessionFactory.getConfiguration(), entityType, tableInfo);

        try (SqlSession session = sqlSessionFactory.openSession()) {
            return session.insert(statementId, entity);
        }
    }

    public <T> int updateById(T entity, Class<T> entityType) {
        Objects.requireNonNull(entity, "Entity must not be null");

        TableInfo tableInfo = getRequiredTableInfo(entityType);
        org.springframework.beans.BeanWrapperImpl beanWrapper = new org.springframework.beans.BeanWrapperImpl(entity);
        Object keyValue = beanWrapper.getPropertyValue(tableInfo.getKeyProperty());
        if (keyValue == null) {
            throw new IllegalArgumentException("Entity id must not be null for updateById");
        }

        boolean hasAnyFieldToUpdate = tableInfo.getFieldList().stream()
                .filter(fieldInfo -> ProjectionMappedStatementFactory.isPersistableField(entityType, fieldInfo))
                .map(TableFieldInfo::getProperty)
                .anyMatch(property -> beanWrapper.getPropertyValue(property) != null);
        if (!hasAnyFieldToUpdate) {
            return 0;
        }

        String statementId = statementFactory.ensureUpdateByIdStatement(sqlSessionFactory.getConfiguration(), entityType, tableInfo);

        try (SqlSession session = sqlSessionFactory.openSession()) {
            return session.update(statementId, entity);
        }
    }

    public <T> int updateByIdAllowNull(T entity, Class<T> entityType) {
        Objects.requireNonNull(entity, "Entity must not be null");

        TableInfo tableInfo = getRequiredTableInfo(entityType);
        org.springframework.beans.BeanWrapperImpl beanWrapper = new org.springframework.beans.BeanWrapperImpl(entity);
        Object keyValue = beanWrapper.getPropertyValue(tableInfo.getKeyProperty());
        if (keyValue == null) {
            throw new IllegalArgumentException("Entity id must not be null for update");
        }

        String statementId = statementFactory.ensureUpdateAllByIdStatement(sqlSessionFactory.getConfiguration(),
                entityType,
                tableInfo);

        try (SqlSession session = sqlSessionFactory.openSession()) {
            return session.update(statementId, entity);
        }
    }

    public <T, ID extends Serializable> int deleteById(ID id, Class<T> entityType) {
        Objects.requireNonNull(id, "Id must not be null");

        TableInfo tableInfo = getRequiredTableInfo(entityType);
        String statementId = statementFactory.ensureDeleteByIdStatement(sqlSessionFactory.getConfiguration(), entityType, tableInfo);

        Map<String, Object> params = new HashMap<>();
        params.put(tableInfo.getKeyProperty(), id);
        try (SqlSession session = sqlSessionFactory.openSession()) {
            return session.delete(statementId, params);
        }
    }

    public <T> int deleteByQuery(Wrapper<T> wrapper, Class<T> entityType) {
        Objects.requireNonNull(wrapper, "Query wrapper must not be null");

        String sqlSegment = wrapper.getCustomSqlSegment();
        if (sqlSegment == null || sqlSegment.isBlank()) {
            throw new IllegalArgumentException("deleteByQuery requires non-empty query conditions");
        }

        TableInfo tableInfo = getRequiredTableInfo(entityType);
        String statementId = statementFactory.ensureDeleteByQueryStatement(sqlSessionFactory.getConfiguration(), entityType, tableInfo);

        Map<String, Object> params = new HashMap<>();
        params.put(Constants.WRAPPER, wrapper);
        try (SqlSession session = sqlSessionFactory.openSession()) {
            return session.delete(statementId, params);
        }
    }

    public <T> int deleteAll(Class<T> entityType) {
        TableInfo tableInfo = getRequiredTableInfo(entityType);
        String statementId = statementFactory.ensureDeleteAllStatement(sqlSessionFactory.getConfiguration(), entityType, tableInfo);

        try (SqlSession session = sqlSessionFactory.openSession()) {
            return session.delete(statementId);
        }
    }

    private <T> TableInfo getRequiredTableInfo(Class<T> entityType) {
        TableInfo tableInfo = ProjectionTableInfoSupport.getTableInfo(sqlSessionFactory.getConfiguration(), entityType);
        if (tableInfo == null) {
            throw new IllegalArgumentException("No TableInfo found for entity type: " + entityType.getName());
        }
        return tableInfo;
    }

    <T> TableInfo requireTableInfo(Class<T> entityType) {
        return getRequiredTableInfo(entityType);
    }

    public List<Map<String, Object>> selectJoinTablePairs(String joinTableName, String sourceJoinCol,
                                                          String targetJoinCol, java.util.Collection<?> sourceIds) {
        String statementId = statementFactory.ensureJoinTableStatement(
                sqlSessionFactory.getConfiguration(), joinTableName, sourceJoinCol, targetJoinCol);
        Map<String, Object> params = new HashMap<>();
        params.put("sourceIds", sourceIds);
        try (SqlSession session = sqlSessionFactory.openSession()) {
            return session.selectList(statementId, params);
        }
    }

    public <T> List<Map<String, Object>> selectMaps(Wrapper<T> wrapper, ProjectionMetadata metadata) {
        String statementId = statementFactory.ensureMapStatement(sqlSessionFactory.getConfiguration(), metadata);
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.WRAPPER, wrapper);

        try (SqlSession session = sqlSessionFactory.openSession()) {
            return session.selectList(statementId, params);
        }
    }

    public <T> IPage<Map<String, Object>> selectPageMaps(Page<?> page,
                                                          Wrapper<T> wrapper,
                                                          Wrapper<T> countWrapper,
                                                          ProjectionMetadata metadata) {
        String statementId = statementFactory.ensureMapStatement(sqlSessionFactory.getConfiguration(), metadata);
        String countStatementId = statementFactory.ensureCountStatement(sqlSessionFactory.getConfiguration(), metadata);

        Map<String, Object> params = new HashMap<>();
        params.put(Constants.WRAPPER, wrapper);

        Map<String, Object> countParams = new HashMap<>();
        countParams.put(Constants.WRAPPER, countWrapper);

        long current = page.getCurrent() <= 0 ? 1 : page.getCurrent();
        long size = page.getSize() <= 0 ? 10 : page.getSize();
        int offset = toRowBoundsOffset(current, size);
        int limit = toRowBoundsLimit(size);

        try (SqlSession session = sqlSessionFactory.openSession()) {
            Long total = session.selectOne(countStatementId, countParams);
            List<Map<String, Object>> records = session.selectList(statementId, params, new RowBounds(offset, limit));

            @SuppressWarnings("unchecked")
            IPage<Map<String, Object>> result = (IPage<Map<String, Object>>) page;
            result.setRecords(records);
            result.setTotal(total == null ? 0 : total);
            return result;
        }
    }

    private int toRowBoundsOffset(long current, long size) {
        try {
            long offset = Math.multiplyExact(current - 1, size);
            if (offset > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("Page offset exceeds RowBounds integer range: " + offset);
            }
            return (int) offset;
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException("Page offset overflow for current=" + current + ", size=" + size, ex);
        }
    }

    private int toRowBoundsLimit(long size) {
        if (size > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Page size exceeds RowBounds integer range: " + size);
        }
        return (int) size;
    }
}
