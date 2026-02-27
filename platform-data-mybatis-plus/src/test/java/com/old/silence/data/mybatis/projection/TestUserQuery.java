package com.old.silence.data.mybatis.projection;

import com.old.silence.data.commons.annotation.RelationalQueryProperty;
import com.old.silence.data.commons.converter.Part;

/**
 * Query DTO for projection integration tests.
 */
public class TestUserQuery {

    @RelationalQueryProperty(type = Part.Type.CONTAINING, name = "username")
    private String usernameLike;

    @RelationalQueryProperty(type = Part.Type.EQUAL, name = "enabled")
    private Boolean enabled;

    public String getUsernameLike() {
        return usernameLike;
    }

    public void setUsernameLike(String usernameLike) {
        this.usernameLike = usernameLike;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
