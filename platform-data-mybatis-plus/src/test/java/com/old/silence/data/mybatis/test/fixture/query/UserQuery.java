package com.old.silence.data.mybatis.test.fixture.query;

import com.old.silence.data.commons.annotation.RelationalQueryProperty;
import com.old.silence.data.commons.converter.Part;

/**
 * Query DTO for projection integration tests.
 */
public class UserQuery {

    @RelationalQueryProperty(type = Part.Type.CONTAINING, name = "username")
    private String usernameLike;

    @RelationalQueryProperty(type = Part.Type.EQUAL)
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
