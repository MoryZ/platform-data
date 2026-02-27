package com.old.silence.data.mybatis.projection;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public <T, P> List<P> select(QueryWrapper<T> wrapper, ProjectionMetadata metadata) {
        String statementId = statementFactory.ensureStatement(sqlSessionFactory.getConfiguration(), metadata);
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.WRAPPER, wrapper);

        try (SqlSession session = sqlSessionFactory.openSession()) {
            return session.selectList(statementId, params);
        }
    }

    public <T, P> IPage<P> selectPage(Page<?> page,
                                      QueryWrapper<T> wrapper,
                                      QueryWrapper<T> countWrapper,
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

    public <T> List<Map<String, Object>> selectMaps(QueryWrapper<T> wrapper, ProjectionMetadata metadata) {
        String statementId = statementFactory.ensureMapStatement(sqlSessionFactory.getConfiguration(), metadata);
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.WRAPPER, wrapper);

        try (SqlSession session = sqlSessionFactory.openSession()) {
            return session.selectList(statementId, params);
        }
    }

    public <T> IPage<Map<String, Object>> selectPageMaps(Page<?> page,
                                                          QueryWrapper<T> wrapper,
                                                          QueryWrapper<T> countWrapper,
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
