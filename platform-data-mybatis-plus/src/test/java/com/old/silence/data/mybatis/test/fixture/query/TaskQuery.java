package com.old.silence.data.mybatis.test.fixture.query;

import com.old.silence.data.commons.annotation.RelationalQueryProperty;
import com.old.silence.data.commons.converter.Part;

import java.time.Instant;

/**
 * Query DTO for projection integration tests.
 */
public class TaskQuery {

    @RelationalQueryProperty(name = "createdDate", type = Part.Type.GREATER_THAN_EQUAL)
    private Instant createdDateStart;

    @RelationalQueryProperty(name = "createdDate", type = Part.Type.LESS_THAN_EQUAL)
    private Instant createdDateEnd;

    public Instant getCreatedDateStart() {
        return createdDateStart;
    }

    public void setCreatedDateStart(Instant createdDateStart) {
        this.createdDateStart = createdDateStart;
    }

    public Instant getCreatedDateEnd() {
        return createdDateEnd;
    }

    public void setCreatedDateEnd(Instant createdDateEnd) {
        this.createdDateEnd = createdDateEnd;
    }
}
