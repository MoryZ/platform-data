package com.old.silence.data.mybatis.test.fixture.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/**
 * Explicit join entity for User -> UserRole -> Role tests.
 * The backing table uses a composite primary key (user_id, role_id), so this entity intentionally has no @TableId.
 */
@TableName("user_department")
public class UserDepartment {

    @TableField("user_id")
    private Long userId;

    @TableField("dept_id")
    private Long deptId;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "dept_id")
    private Department department;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getDepartmentId() {
        return  deptId;
    }

    public void setDepartmentId(Long deptId) {
        this.deptId = deptId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }
}