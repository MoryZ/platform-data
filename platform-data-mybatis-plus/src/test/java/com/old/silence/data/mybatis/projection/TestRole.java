package com.old.silence.data.mybatis.projection;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/**
 * Test role entity for ManyToMany join-table projection tests.
 */
@TableName("test_role")
public class TestRole {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String roleName;

    @ManyToOne
    @JoinColumn(name = "parent_role_id")
    private TestRole parentRole;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public TestRole getParentRole() {
        return parentRole;
    }

    public void setParentRole(TestRole parentRole) {
        this.parentRole = parentRole;
    }
}
