package com.old.silence.data.mybatis.test.fixture.projection;

import java.util.Map;


/**
 * Interface projection view for TestUser.
 */
public interface MenuView {

    Long getId();

    String getMenuName();

    Map<String, Object> getMeta();
}
