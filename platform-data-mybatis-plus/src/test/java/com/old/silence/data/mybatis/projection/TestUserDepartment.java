package com.old.silence.data.mybatis.projection;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/**
 * Explicit join entity for User -> UserRole -> Role tests.
 * The backing table uses a composite primary key (user_id, role_id), so this entity intentionally has no @TableId.
 */
@TableName("test_user_department")
public class TestUserDepartment {

    @TableField("user_id")
    private Long userId;

    @TableField("dept_id")
    private Long deptId;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private TestUser user;

    @ManyToOne
    @JoinColumn(name = "dept_id")
    private TestDepartment department;

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

    public TestUser getUser() {
        return user;
    }

    public void setUser(TestUser user) {
        this.user = user;
    }

    public TestDepartment getDepartment() {
        return department;
    }

    public void setDepartment(TestDepartment department) {
        this.department = department;
    }
}