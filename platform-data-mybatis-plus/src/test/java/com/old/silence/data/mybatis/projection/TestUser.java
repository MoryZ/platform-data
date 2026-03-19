package com.old.silence.data.mybatis.projection;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.old.silence.data.commons.handler.GenericEnumTypeHandler;
import jakarta.persistence.OneToMany;

import java.util.List;

/**
 * Test entity for projection integration tests.
 *
 * Role-association model: User -> UserRole -> Role via {@link #userRoles} (explicit join entity).
 * Department-association model: User -> UserDepartment -> Department via {@link #userDepartments} (explicit join entity).
 */
@TableName("test_user")
public class TestUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    @TableField("is_enabled")
    private Boolean enabled;

    @TableField(value = "status", typeHandler = GenericEnumTypeHandler.class)
    private TestUserStatus status;

    @OneToMany(mappedBy = "user")
    private List<TestUserRole> userRoles;

    @OneToMany(mappedBy = "user")
    private List<TestUserDepartment> userDepartments;

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

    public List<TestUserRole> getUserRoles() {
        return userRoles;
    }

    public void setUserRoles(List<TestUserRole> userRoles) {
        this.userRoles = userRoles;
    }

    public List<TestUserDepartment> getUserDepartments() {
        return userDepartments;
    }

    public void setUserDepartments(List<TestUserDepartment> userDepartments) {
        this.userDepartments = userDepartments;
    }
}
