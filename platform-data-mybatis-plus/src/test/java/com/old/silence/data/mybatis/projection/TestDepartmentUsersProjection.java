package com.old.silence.data.mybatis.projection;

import java.util.List;

/**
 * Projection for one-to-many association loading tests.
 */
public class TestDepartmentUsersProjection {

    private Long id;

    private String name;

    private List<TestUser> users;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<TestUser> getUsers() {
        return users;
    }

    public void setUsers(List<TestUser> users) {
        this.users = users;
    }
}
