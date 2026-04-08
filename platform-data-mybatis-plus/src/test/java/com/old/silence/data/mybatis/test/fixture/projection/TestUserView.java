package com.old.silence.data.mybatis.test.fixture.projection;

import com.old.silence.data.mybatis.test.fixture.enmus.UserStatus;

/**
 * Interface projection view for TestUser.
 */
public interface TestUserView {

    Long getId();

    String getUsername();

    Boolean getEnabled();

    UserStatus getStatus();
}
