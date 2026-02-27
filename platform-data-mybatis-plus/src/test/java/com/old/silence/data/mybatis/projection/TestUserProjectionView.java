package com.old.silence.data.mybatis.projection;

/**
 * Interface projection view for TestUser.
 */
public interface TestUserProjectionView {

    Long getId();

    String getUsername();

    Boolean getEnabled();

    TestUserStatus getStatus();
}
