package com.old.silence.data.mybatis.projection;

import java.util.List;

/**
 * Projection for explicit join-entity collection backfill on TestUser.
 */
public class TestUserUserRolesProjection {

    private Long id;

    private String username;

    private List<TestUserRole> userRoles;

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

    public List<TestUserRole> getUserRoles() {
        return userRoles;
    }

    public void setUserRoles(List<TestUserRole> userRoles) {
        this.userRoles = userRoles;
    }
}