package com.old.silence.data.mybatis.test.fixture.projection;

import com.old.silence.data.mybatis.test.fixture.entity.UserRole;
import java.util.List;

/**
 * Projection for explicit join-entity collection backfill on TestUser.
 */
public class TestUserUserRolesDto {

    private Long id;

    private String username;

    private List<UserRole> userRoles;

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

    public List<UserRole> getUserRoles() {
        return userRoles;
    }

    public void setUserRoles(List<UserRole> userRoles) {
        this.userRoles = userRoles;
    }
}