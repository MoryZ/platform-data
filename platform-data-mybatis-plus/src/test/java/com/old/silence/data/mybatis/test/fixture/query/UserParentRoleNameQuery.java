package com.old.silence.data.mybatis.test.fixture.query;

import com.old.silence.data.commons.annotation.RelationalQueryProperty;
import com.old.silence.data.commons.converter.Part;

/**
 * Query DTO for filtering users by deep nested role hierarchy path.
 */
public class UserParentRoleNameQuery {

    @RelationalQueryProperty(type = Part.Type.EQUAL, name = "userRoles.role.parentRole.roleName")
    private String parentRoleName;

    public String getParentRoleName() {
        return parentRoleName;
    }

    public void setParentRoleName(String parentRoleName) {
        this.parentRoleName = parentRoleName;
    }
}