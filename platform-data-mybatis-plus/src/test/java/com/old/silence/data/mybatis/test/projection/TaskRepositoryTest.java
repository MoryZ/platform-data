package com.old.silence.data.mybatis.test.projection;

import org.junit.jupiter.api.Test;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.old.silence.core.util.CollectionUtils;
import com.old.silence.data.commons.converter.QueryWrapperConverter;
import com.old.silence.data.mybatis.test.MyBatisProjectionRepositoryTests;
import com.old.silence.data.mybatis.test.fixture.entity.Project;
import com.old.silence.data.mybatis.test.fixture.entity.Task;
import com.old.silence.data.mybatis.test.fixture.mapper.ProjectMapper;
import com.old.silence.data.mybatis.test.fixture.mapper.TaskMapper;
import com.old.silence.data.mybatis.test.fixture.projection.ProjectView;
import com.old.silence.data.mybatis.test.fixture.projection.TaskView;
import com.old.silence.data.mybatis.test.fixture.query.TaskQuery;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Business-style repository test.
 *
 * By extending MyBatisProjectionRepositoryTests,
 * the test automatically:
 * - Scans repositories in the same package as MenuPlainProjectionRepository
 * - Loads Liquibase from classpath:/db/changelogs/changelog-master.xml
 * - Initializes H2 database with Liquibase
 *
 * No explicit repository package or changelog path configuration needed.
 *
 * @author moryzang
 */
class TaskRepositoryTest extends MyBatisProjectionRepositoryTests<TaskMapper, Task, Long> {



    @Test
    void testFindByQueryParams() {
        var taskQuery = new TaskQuery();
        taskQuery.setCreatedDateStart(Instant.EPOCH);
        var queryWrapper = QueryWrapperConverter.convert(taskQuery, Task.class);
        var viewList = repository.findByQuery(queryWrapper, TaskView.class);
        assertThat(viewList).isNotEmpty();

        assertThat(CollectionUtils.firstElement(viewList)).isPresent();
    }
    @Test
    void testFindByQueryPage() {
        var viewList = repository.findByQuery(new QueryWrapper<>(), TaskView.class);
        assertThat(viewList).isNotEmpty();

        assertThat(CollectionUtils.firstElement(viewList)).isPresent();
    }

    @Test
    void testFindAll() {
        var viewList = repository.findAll();
        assertThat(viewList).isNotEmpty();

        assertThat(CollectionUtils.firstElement(viewList)).isPresent();
    }
}
