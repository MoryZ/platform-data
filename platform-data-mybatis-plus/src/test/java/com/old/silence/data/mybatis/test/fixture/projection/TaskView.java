package com.old.silence.data.mybatis.test.fixture.projection;

/**
 * Task projection with nested ProjectView interface.
 */
public interface TaskView {
    Long getId();
    String getTaskName();
    ProjectView getProject();
}
