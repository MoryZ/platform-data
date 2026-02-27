package com.old.silence.data.mybatis.projection;

import com.old.silence.core.enums.EnumValue;

/**
 * Test enum for projection integration tests.
 */
public enum TestUserStatus implements EnumValue<Integer> {
    ACTIVE(1),
    DISABLED(2);

    private final Integer value;

    TestUserStatus(Integer value) {
        this.value = value;
    }

    @Override
    public Integer getValue() {
        return value;
    }
}
