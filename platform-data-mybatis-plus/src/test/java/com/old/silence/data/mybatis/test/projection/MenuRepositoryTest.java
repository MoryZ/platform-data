package com.old.silence.data.mybatis.test.projection;

import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.junit.jupiter.api.Test;

import com.old.silence.core.util.CollectionUtils;
import com.old.silence.data.mybatis.test.MyBatisProjectionRepositoryTests;
import com.old.silence.data.mybatis.test.fixture.entity.Menu;
import com.old.silence.data.mybatis.test.fixture.mapper.MenuPlainProjectionRepository;
import com.old.silence.data.mybatis.test.fixture.projection.MenuView;

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
class MenuRepositoryTest extends MyBatisProjectionRepositoryTests<MenuPlainProjectionRepository, Menu, Long> {

    @Test
    void testFindByQuery() {
        var viewList = repository.findByQuery(new QueryWrapper<>(), MenuView.class);
        assertThat(viewList).isNotEmpty();

        assertThat(CollectionUtils.firstElement(viewList)).isPresent();
        assertThat(CollectionUtils.firstElement(viewList).map(MenuView::getMeta)).isPresent();
        assertThat(CollectionUtils.firstElement(viewList).map(MenuView::getMeta).get()).isNotNull();
    }

    @Test
    void testFindAll() {
        var viewList = repository.findAll();
        assertThat(viewList).isNotEmpty();

        assertThat(CollectionUtils.firstElement(viewList)).isPresent();
        assertThat(CollectionUtils.firstElement(viewList).map(Menu::getMeta)).isPresent();
        assertThat(CollectionUtils.firstElement(viewList).map(Menu::getMeta).get()).isNotNull();
    }
}
