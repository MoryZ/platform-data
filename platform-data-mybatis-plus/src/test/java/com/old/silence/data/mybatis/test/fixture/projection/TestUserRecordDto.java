package com.old.silence.data.mybatis.test.fixture.projection;

import com.old.silence.data.mybatis.test.fixture.enmus.UserStatus;

public record TestUserRecordDto(Long id,
                                       String username,
                                       Boolean enabled,
                                       UserStatus status) {
}