package com.old.silence.data.mybatis.test.fixture.projection;

import com.old.silence.data.mybatis.test.fixture.enmus.UserStatus;

/**
 * Projection DTO for TestUser.
 */
public class UserDto {

    private Long id;
    private String username;
    private Boolean enabled;
    private UserStatus status;

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

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }
}
