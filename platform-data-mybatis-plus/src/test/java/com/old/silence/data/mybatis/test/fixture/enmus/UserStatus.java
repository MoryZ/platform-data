package com.old.silence.data.mybatis.test.fixture.enmus;

import com.old.silence.core.enums.EnumValue;

/**
 * Test enum for projection integration tests.
 */
public enum UserStatus implements EnumValue<Integer> {
    ACTIVE(1),
    DISABLED(2);

    private final Integer value;

    UserStatus(Integer value) {
        this.value = value;
    }

    @Override
    public Integer getValue() {
        return value;
    }
}
