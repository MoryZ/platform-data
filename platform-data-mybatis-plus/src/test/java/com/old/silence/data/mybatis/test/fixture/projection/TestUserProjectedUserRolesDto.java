package com.old.silence.data.mybatis.test.fixture.projection;

import java.util.List;

/**
 * Projection for backfilling explicit join-entity rows as projection elements.
 */
public class TestUserProjectedUserRolesDto {

    private Long id;

    private String username;

    private List<TestUserRoleDto> userRoles;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public List<TestUserRoleDto> getUserRoles() {
        return userRoles;
    }

    public void setUserRoles(List<TestUserRoleDto> userRoles) {
        this.userRoles = userRoles;
    }
}