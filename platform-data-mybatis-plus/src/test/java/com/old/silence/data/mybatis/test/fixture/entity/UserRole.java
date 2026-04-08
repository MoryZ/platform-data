package com.old.silence.data.mybatis.test.fixture.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/**
 * Explicit join entity for User -> UserRole -> Role tests.
 * The backing table uses a composite primary key (user_id, role_id), so this entity intentionally has no @TableId.
 */
@TableName("user_role")
public class UserRole {

    @TableField("user_id")
    private Long userId;

    @TableField("role_id")
    private Long roleId;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "role_id")
    private Role role;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}