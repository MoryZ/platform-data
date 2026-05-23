package com.old.silence.data.mybatis.test.projection;

import org.junit.jupiter.api.Test;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.old.silence.core.util.CollectionUtils;
import com.old.silence.data.mybatis.test.MyBatisProjectionRepositoryTests;
import com.old.silence.data.mybatis.test.fixture.entity.Menu;
import com.old.silence.data.mybatis.test.fixture.entity.Project;
import com.old.silence.data.mybatis.test.fixture.mapper.MenuPlainProjectionRepository;
import com.old.silence.data.mybatis.test.fixture.mapper.ProjectMapper;
import com.old.silence.data.mybatis.test.fixture.projection.MenuView;
import com.old.silence.data.mybatis.test.fixture.projection.ProjectView;

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
class ProjectRepositoryTest extends MyBatisProjectionRepositoryTests<ProjectMapper, Project, Long> {

    @Test
    void testFindByQuery() {
        var viewList = repository.findByQuery(new QueryWrapper<>(), ProjectView.class);
        assertThat(viewList).isNotEmpty();

        assertThat(CollectionUtils.firstElement(viewList)).isPresent();
    }

    @Test
    void testFindAll() {
        var viewList = repository.findAll();
        assertThat(viewList).isNotEmpty();
    }
}
