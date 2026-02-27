package com.old.silence.data.mybatis.test;

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.io.Serializable;

/**
 * Base test class for MyBatis Plus Mappers
 * Provides common test infrastructure for CRUD operations and projections
 * 
 * Usage example:
 * <pre>
 * &#64;DataMyBatisTest
 * class UserMapperTest extends MyBatisMapperTests&lt;UserMapper, User, Long&gt; {
 *     
 *     &#64;Test
 *     void testInsert() {
 *         verifyInsert();
 *     }
 *     
 *     &#64;Test
 *     void testSelectById() {
 *         Long id = 1L;
 *         verifySelectById(id);
 *     }
 *     
 *     &#64;Test
 *     void testSelectWithProjection() {
 *         Long id = 1L;
 *         UserDTO dto = verifySelectById(id, UserDTO.class);
 *         assertThat(dto.getName()).isNotNull();
 *     }
 * }
 * </pre>
 * 
 * @param <M> Mapper type extending BaseMapper
 * @param <T> Entity type
 * @param <ID> ID type (must be Serializable)
 * @author moryzang
 */
@DataMyBatisTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
public abstract class MyBatisMapperTests<M extends BaseMapper<T>, T, ID extends Serializable> 
        extends AbstractMyBatisPlusMapperTests<M, T, ID> {
}
