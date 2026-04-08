package com.old.silence.data.mybatis.test.fixture.projection;

import com.old.silence.data.mybatis.test.fixture.entity.User;
import java.util.List;

/**
 * Projection for one-to-many association loading tests.
 */
public class DepartmentUsersDto {

    private Long id;

    private String name;

    private List<User> users;

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

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }
}
