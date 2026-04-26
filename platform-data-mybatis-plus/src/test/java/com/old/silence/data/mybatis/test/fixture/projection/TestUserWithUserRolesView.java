package com.old.silence.data.mybatis.test.fixture.projection;

import java.util.List;

/**
 * Interface projection with a one-to-many collection.
 */
public interface TestUserWithUserRolesView {

    Long getId();

    String getUsername();

    List<TestUserRoleDto> getUserRoles();
}
