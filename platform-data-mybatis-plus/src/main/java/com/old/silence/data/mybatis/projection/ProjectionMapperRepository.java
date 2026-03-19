package com.old.silence.data.mybatis.projection;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.io.Serializable;

/**
 * Combined mapper/repository contract for users who want one interface that includes
 * both MyBatis-Plus BaseMapper and projection query capabilities.
 */
@ProjectionNoRepositoryBean
public interface ProjectionMapperRepository<T, ID extends Serializable>
        extends BaseMapper<T>, ProjectionRepository<T, ID> {
}
