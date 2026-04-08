package com.old.silence.data.mybatis.test.fixture.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.old.silence.data.mybatis.test.fixture.entity.Menu;
import com.old.silence.data.mybatis.test.fixture.projection.MenuView;

/**
 * Test mapper contract for unified findByQuery projection mapping.
 */
public interface MenuProjectionMapperContract {

    List<MenuView> findByQuery(Wrapper<Menu> queryWrapper,
                               Class<MenuView> projectionType);

    IPage<MenuView> findByQuery(Wrapper<Menu> queryWrapper,
                                Page<?> page,
                                Class<MenuView> projectionType);
}
