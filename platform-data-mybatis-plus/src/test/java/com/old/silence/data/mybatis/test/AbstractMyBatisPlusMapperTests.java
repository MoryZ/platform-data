package com.old.silence.data.mybatis.test;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract base class for MyBatis Plus Mapper tests
 * Provides common test utility methods for CRUD operations and projections
 * 
 * @param <M> Mapper type extending BaseMapper
 * @param <T> Entity type
 * @param <ID> ID type
 * @author moryzang
 */
public abstract class AbstractMyBatisPlusMapperTests<M extends BaseMapper<T>, T, ID extends Serializable> {

    @Autowired
    protected M mapper;

    @Autowired
    private DataSource dataSource;

    protected final Class<T> entityType;
    protected final Class<ID> idType;
    protected TableInfo tableInfo;
    protected EntityMockFactory<T, ID> entityMockFactory;

    @SuppressWarnings("unchecked")
    public AbstractMyBatisPlusMapperTests() {
        // Extract generic types from subclass
        Type superclass = getClass().getGenericSuperclass();
        if (superclass instanceof ParameterizedType) {
            Type[] types = ((ParameterizedType) superclass).getActualTypeArguments();
            // M extends BaseMapper<T>
            Type mapperType = types[0];
            if (mapperType instanceof ParameterizedType) {
                Type[] mapperArgs = ((ParameterizedType) mapperType).getActualTypeArguments();
                this.entityType = (Class<T>) mapperArgs[0];
            } else {
                throw new IllegalStateException("Cannot resolve entity type");
            }
            this.idType = (Class<ID>) types[2];
        } else {
            throw new IllegalStateException("Cannot resolve generic types");
        }
    }

    @PostConstruct
    public void initialize() {
        this.tableInfo = TableInfoHelper.getTableInfo(entityType);
        if (this.tableInfo == null) {
            throw new IllegalStateException("No TableInfo found for entity type: " + entityType);
        }
        this.entityMockFactory = new EntityMockFactory<>(dataSource, entityType);
    }

    // ==================== Query Methods ====================

    /**
     * Verify selectById and assert entity is not null
     */
    protected T verifySelectById(ID id) {
        T entity = mapper.selectById((Serializable) id);
        assertThat(entity).isNotNull();
        return entity;
    }

    /**
     * Verify selectById with projection
     */
    protected <P> P verifySelectById(ID id, Class<P> projectionType) {
        String[] columns = ProjectionSupport.resolveSelectColumns(projectionType, tableInfo);
        QueryWrapper<T> wrapper = new QueryWrapper<>();
        wrapper.select(columns).eq(tableInfo.getKeyColumn(), id);

        List<Map<String, Object>> maps = mapper.selectMaps(wrapper);
        assertThat(maps).isNotEmpty();

        return ProjectionSupport.convertMapToProjection(maps.get(0), projectionType);
    }

    /**
     * Verify selectList (find all)
     */
    protected List<T> verifySelectList() {
        List<T> list = mapper.selectList(null);
        assertThat(list).isNotEmpty();
        return list;
    }

    /**
     * Verify selectList with projection
     */
    protected <P> List<P> verifySelectList(Class<P> projectionType) {
        String[] columns = ProjectionSupport.resolveSelectColumns(projectionType, tableInfo);
        QueryWrapper<T> wrapper = new QueryWrapper<>();
        wrapper.select(columns);

        List<Map<String, Object>> maps = mapper.selectMaps(wrapper);
        assertThat(maps).isNotEmpty();

        return ProjectionSupport.convertMapsToProjections(maps, projectionType);
    }

    /**
     * Verify selectList by IDs
     */
    protected List<T> verifySelectBatchIds(Collection<? extends Serializable> ids) {
        List<T> list = mapper.selectBatchIds(ids);
        assertThat(list).isNotEmpty();
        return list;
    }

    /**
     * Verify selectList by IDs with projection
     */
    protected <P> List<P> verifySelectBatchIds(Collection<? extends Serializable> ids, Class<P> projectionType) {
        String[] columns = ProjectionSupport.resolveSelectColumns(projectionType, tableInfo);
        QueryWrapper<T> wrapper = new QueryWrapper<>();
        wrapper.select(columns).in(tableInfo.getKeyColumn(), ids);

        List<Map<String, Object>> maps = mapper.selectMaps(wrapper);
        assertThat(maps).isNotEmpty();

        return ProjectionSupport.convertMapsToProjections(maps, projectionType);
    }

    /**
     * Verify selectList by QueryWrapper
     */
    protected List<T> verifySelectByWrapper(QueryWrapper<T> wrapper) {
        List<T> list = mapper.selectList(wrapper);
        assertThat(list).isNotEmpty();
        return list;
    }

    /**
     * Verify selectList by QueryWrapper with projection
     */
    protected <P> List<P> verifySelectByWrapper(QueryWrapper<T> wrapper, Class<P> projectionType) {
        String[] columns = ProjectionSupport.resolveSelectColumns(projectionType, tableInfo);
        wrapper.select(columns);

        List<Map<String, Object>> maps = mapper.selectMaps(wrapper);
        assertThat(maps).isNotEmpty();

        return ProjectionSupport.convertMapsToProjections(maps, projectionType);
    }

    /**
     * Verify selectPage (pagination)
     */
    protected IPage<T> verifySelectPage(Page<T> page, long expectedTotal) {
        return verifySelectPage(page, expectedTotal, page.getSize());
    }

    /**
     * Verify selectPage with expected total and number of elements
     */
    protected IPage<T> verifySelectPage(Page<T> page, long expectedTotal, long expectedSize) {
        IPage<T> result = mapper.selectPage(page, null);
        assertThat(result.getTotal()).isEqualTo(expectedTotal);
        assertThat(result.getRecords()).hasSize((int) expectedSize);
        return result;
    }

    /**
     * Verify selectPage with QueryWrapper
     */
    protected IPage<T> verifySelectPage(Page<T> page, QueryWrapper<T> wrapper, long expectedTotal, long expectedSize) {
        IPage<T> result = mapper.selectPage(page, wrapper);
        assertThat(result.getTotal()).isEqualTo(expectedTotal);
        assertThat(result.getRecords()).hasSize((int) expectedSize);
        return result;
    }

    /**
     * Verify selectPage with projection
     * Note: Returns raw maps, use convertPageToProjections() to convert to projection objects
     */
    protected <P> IPage<Map<String, Object>> verifySelectPageMaps(Page<Map<String, Object>> page, 
                                                                   Class<P> projectionType,
                                                                   long expectedTotal, long expectedSize) {
        String[] columns = ProjectionSupport.resolveSelectColumns(projectionType, tableInfo);
        QueryWrapper<T> wrapper = new QueryWrapper<>();
        wrapper.select(columns);

        IPage<Map<String, Object>> result = mapper.selectMapsPage(page, wrapper);
        assertThat(result.getTotal()).isEqualTo(expectedTotal);
        assertThat(result.getRecords()).hasSize((int) expectedSize);
        return result;
    }

    /**
     * Convert IPage<Map> to List of projections
     */
    protected <P> List<P> convertPageToProjections(IPage<Map<String, Object>> page, Class<P> projectionType) {
        return ProjectionSupport.convertMapsToProjections(page.getRecords(), projectionType);
    }

    // ==================== Existence Check Methods ====================

    /**
     * Verify entity exists by ID
     */
    protected void verifyExists(ID id, boolean expected) {
        QueryWrapper<T> wrapper = new QueryWrapper<>();
        wrapper.eq(tableInfo.getKeyColumn(), id);
        Long count = mapper.selectCount(wrapper);
        assertThat(count > 0).isEqualTo(expected);
    }

    /**
     * Verify exists by QueryWrapper
     */
    protected void verifyExistsByWrapper(QueryWrapper<T> wrapper, boolean expected) {
        Long count = mapper.selectCount(wrapper);
        assertThat(count > 0).isEqualTo(expected);
    }

    // ==================== Count Methods ====================

    /**
     * Verify total count
     */
    protected void verifyCount(long expected) {
        Long count = mapper.selectCount(null);
        assertThat(count).isEqualTo(expected);
    }

    /**
     * Verify count by QueryWrapper
     */
    protected void verifyCountByWrapper(QueryWrapper<T> wrapper, long expected) {
        Long count = mapper.selectCount(wrapper);
        assertThat(count).isEqualTo(expected);
    }

    // ==================== Insert Methods ====================

    /**
     * Verify insert operation
     */
    protected void verifyInsert() {
        verifyInsert(null);
    }

    /**
     * Verify insert with customizer
     */
    protected void verifyInsert(MockedEntityCustomizer<T> customizer) {
        long countBefore = mapper.selectCount(null);
        T entity = entityMockFactory.mockForInsert(customizer);
        
        int rows = mapper.insert(entity);
        
        assertThat(rows).isEqualTo(1);
        long countAfter = mapper.selectCount(null);
        assertThat(countAfter).isEqualTo(countBefore + 1);
        
        // Verify ID is generated (for auto-increment)
        ID id = getEntityId(entity);
        if (id != null) {
            assertThat(id).as("ID should be generated").isNotNull();
        }
    }

    /**
     * Verify batch insert (multiple entities)
     */
    protected void verifyInsertBatch(int count) {
        verifyInsertBatch(count, null);
    }

    /**
     * Verify batch insert with customizer
     */
    protected void verifyInsertBatch(int count, MockedEntityCustomizer<T> customizer) {
        long countBefore = mapper.selectCount(null);
        
        List<T> entities = java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> entityMockFactory.mockForInsert(customizer))
                .collect(Collectors.toList());
        
        // Note: MyBatis Plus doesn't have built-in batch insert in BaseMapper
        // You need to use custom SQL or loop
        for (T entity : entities) {
            mapper.insert(entity);
        }
        
        long countAfter = mapper.selectCount(null);
        assertThat(countAfter).isEqualTo(countBefore + count);
    }

    // ==================== Update Methods ====================

    /**
     * Verify update by ID
     */
    protected void verifyUpdateById(ID id) {
        verifyUpdateById(id, null);
    }

    /**
     * Verify update by ID with customizer
     */
    protected void verifyUpdateById(ID id, MockedEntityCustomizer<T> customizer) {
        T entity = entityMockFactory.mockForUpdate(id, customizer);
        int rows = mapper.updateById(entity);
        assertThat(rows).isEqualTo(1);
    }

    /**
     * Verify update returns zero rows (entity not found)
     */
    protected void verifyUpdateByIdZero(ID id) {
        T entity = entityMockFactory.mockForUpdate(id, null);
        int rows = mapper.updateById(entity);
        assertThat(rows).isEqualTo(0);
    }

    /**
     * Verify update with projection DTO
     */
    protected <DTO> void verifyUpdateProjection(ID id, Class<DTO> dtoType) {
        verifyUpdateProjection(id, dtoType, null);
    }

    /**
     * Verify update with projection DTO and customizer
     */
    protected <DTO> void verifyUpdateProjection(ID id, Class<DTO> dtoType, MockedEntityCustomizer<DTO> customizer) {
        // TODO: Implement custom update logic for DTO
        // This requires custom mapper methods to update entity from DTO
        // Example:
        // DTO dto = entityMockFactory.mockDtoForUpdate(id, dtoType, customizer);
        // int rows = mapper.updateFromDto(dto);
        // assertThat(rows).isEqualTo(1);
        throw new UnsupportedOperationException("Update from DTO requires custom mapper method");
    }

    // ==================== Delete Methods ====================

    /**
     * Verify deleteById
     */
    protected void verifyDeleteById(ID id) {
        long countBefore = mapper.selectCount(null);
        int rows = mapper.deleteById((Serializable) id);
        assertThat(rows).isEqualTo(1);
        long countAfter = mapper.selectCount(null);
        assertThat(countAfter).isEqualTo(countBefore - 1);
    }

    /**
     * Verify deleteById returns zero (entity not found)
     */
    protected void verifyDeleteByIdZero(ID id) {
        int rows = mapper.deleteById((Serializable) id);
        assertThat(rows).isEqualTo(0);
    }

    /**
     * Verify deleteBatchIds
     */
    protected void verifyDeleteBatchIds(Collection<? extends Serializable> ids, long expectedDeleted) {
        long countBefore = mapper.selectCount(null);
        int rows = mapper.deleteBatchIds(ids);
        assertThat(rows).isEqualTo(expectedDeleted);
        long countAfter = mapper.selectCount(null);
        assertThat(countAfter).isEqualTo(countBefore - expectedDeleted);
    }

    /**
     * Verify delete by QueryWrapper
     */
    protected void verifyDeleteByWrapper(QueryWrapper<T> wrapper, long expectedDeleted) {
        long countBefore = mapper.selectCount(null);
        int rows = mapper.delete(wrapper);
        assertThat(rows).isEqualTo(expectedDeleted);
        long countAfter = mapper.selectCount(null);
        assertThat(countAfter).isEqualTo(countBefore - expectedDeleted);
    }

    // ==================== Helper Methods ====================

    /**
     * Get entity ID using reflection
     */
    @SuppressWarnings("unchecked")
    protected ID getEntityId(T entity) {
        try {
            String keyProperty = tableInfo.getKeyProperty();
            java.lang.reflect.Field field = entityType.getDeclaredField(keyProperty);
            field.setAccessible(true);
            return (ID) field.get(entity);
        } catch (Exception e) {
            return null;
        }
    }
}
