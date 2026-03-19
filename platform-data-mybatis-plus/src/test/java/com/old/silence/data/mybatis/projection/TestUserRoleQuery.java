package com.old.silence.data.mybatis.projection;

import com.old.silence.data.commons.annotation.RelationalQueryProperty;
import com.old.silence.data.commons.converter.Part;

/**
 * Query DTO for filtering explicit join-entity rows by associated role name.
 */
public class TestUserRoleQuery {

    @RelationalQueryProperty(type = Part.Type.EQUAL, name = "role.roleName")
    private String roleName;

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }
}