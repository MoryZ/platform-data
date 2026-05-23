package com.old.silence.data.mybatis.test.fixture.mapper;

import com.old.silence.data.mybatis.projection.ProjectionMapperRepository;
import com.old.silence.data.mybatis.test.fixture.entity.Task;

public interface TaskMapper extends ProjectionMapperRepository<Task, Long> {
}
