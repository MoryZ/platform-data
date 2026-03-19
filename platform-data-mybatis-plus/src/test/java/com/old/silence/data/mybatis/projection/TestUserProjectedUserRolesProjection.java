package com.old.silence.data.mybatis.projection;

import java.util.List;

/**
 * Projection for backfilling explicit join-entity rows as projection elements.
 */
public class TestUserProjectedUserRolesProjection {

    private Long id;

    private String username;

    private List<TestUserRoleProjection> userRoles;

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

    public List<TestUserRoleProjection> getUserRoles() {
        return userRoles;
    }

    public void setUserRoles(List<TestUserRoleProjection> userRoles) {
        this.userRoles = userRoles;
    }
}