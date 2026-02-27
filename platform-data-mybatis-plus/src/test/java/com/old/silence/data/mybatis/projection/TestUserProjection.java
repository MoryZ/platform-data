package com.old.silence.data.mybatis.projection;

/**
 * Projection DTO for TestUser.
 */
public class TestUserProjection {

    private Long id;
    private String username;
    private Boolean enabled;
    private TestUserStatus status;

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

    public TestUserStatus getStatus() {
        return status;
    }

    public void setStatus(TestUserStatus status) {
        this.status = status;
    }
}
