package com.old.silence.data.mybatis.test.fixture.projection;

import java.beans.ConstructorProperties;

import com.old.silence.data.mybatis.test.fixture.enmus.UserStatus;

public final class UserFinalDto {

    private final Long id;
    private final String username;
    private final Boolean enabled;
    private final UserStatus status;

    @ConstructorProperties({"id", "username", "enabled", "status"})
    public UserFinalDto(Long id, String username, Boolean enabled, UserStatus status) {
        this.id = id;
        this.username = username;
        this.enabled = enabled;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public UserStatus getStatus() {
        return status;
    }
}