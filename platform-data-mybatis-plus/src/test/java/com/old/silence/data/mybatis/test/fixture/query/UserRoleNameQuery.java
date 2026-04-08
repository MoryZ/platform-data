package com.old.silence.data.mybatis.test.fixture.query;

import com.old.silence.data.commons.annotation.RelationalQueryProperty;
import com.old.silence.data.commons.converter.Part;

/**
 * Query DTO for filtering users by explicit join-entity role path.
 */
public class UserRoleNameQuery {

    @RelationalQueryProperty(type = Part.Type.EQUAL, name = "userRoles.role.roleName")
    private String roleName;

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }
}