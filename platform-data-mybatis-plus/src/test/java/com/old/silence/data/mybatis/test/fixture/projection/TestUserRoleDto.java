package com.old.silence.data.mybatis.test.fixture.projection;

import com.old.silence.data.commons.annotation.RelationalQueryProperty;
import com.old.silence.data.commons.converter.Part;

/**
 * Projection for explicit join-entity traversal tests.
 */
public class TestUserRoleDto {

    private Long userId;

    private Long roleId;

    @RelationalQueryProperty(type = Part.Type.SIMPLE_PROPERTY, name = "user.username")
    private String username;

    @RelationalQueryProperty(type = Part.Type.SIMPLE_PROPERTY, name = "role.roleName")
    private String roleName;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }
}