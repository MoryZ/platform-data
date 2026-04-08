package com.old.silence.data.mybatis.test;

import com.old.silence.data.mybatis.projection.ProjectionMapperRepository;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;

import java.io.Serializable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base test class for ProjectionMapperRepository-based mappers.
 *
 * @param <M> Mapper type extending ProjectionMapperRepository
 * @param <T> Entity type
 * @param <ID> ID type
 */
@DataMyBatisTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
public abstract class MyBatisProjectionMapperTests<M extends ProjectionMapperRepository<T, ID>, T, ID extends Serializable>
        extends AbstractMyBatisPlusMapperTests<M, T, ID> {

    @Override
    protected <DTO> void verifyUpdateProjection(ID id, Class<DTO> dtoType, MockedEntityCustomizer<DTO> customizer) {
        DTO dto = entityMockFactory.mockDtoForUpdate(id, dtoType, customizer);
        int rows = mapper.updateProjection(dto);
        assertThat(rows).isEqualTo(1);
    }
}
