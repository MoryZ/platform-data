package com.old.silence.data.mybatis.test.projection;

import com.old.silence.data.mybatis.projection.ProjectionField;
import com.old.silence.data.mybatis.projection.ProjectionMetadata;
import com.old.silence.data.mybatis.projection.ProjectionValueConverter;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectionValueConverterTest {

    public interface TestProjectView {
        String getProjectName();
        String getProjectCode();
    }

    public interface TestTaskView {
        Long getId();
        String getTaskName();
        TestProjectView getProject();
    }

    @Test
    void shouldNormalizeNestedInterfaceProjection() {
        ProjectionMetadata metadata = createTestMetadata();

        Map<String, Object> sourceRow = new LinkedHashMap<>();
        sourceRow.put("id", 1L);
        sourceRow.put("task_name", "Task 1");
        sourceRow.put("project_project_name", "Project A");
        sourceRow.put("project_project_code", "PROJ-A");

        Map<String, Object> result = ProjectionValueConverter.normalizeRow(metadata, sourceRow);

        assertThat(result).containsKeys("id", "taskName", "project");
        assertThat(result.get("id")).isEqualTo(1L);
        assertThat(result.get("taskName")).isEqualTo("Task 1");

        Object projectObj = result.get("project");
        assertThat(projectObj).isNotNull();
        assertThat(projectObj).isInstanceOf(TestProjectView.class);
        TestProjectView projectView = (TestProjectView) projectObj;
        assertThat(projectView.getProjectName()).isEqualTo("Project A");
        assertThat(projectView.getProjectCode()).isEqualTo("PROJ-A");
    }

    @Test
    void shouldHandleNullNestedInterface() {
        ProjectionMetadata metadata = createTestMetadata();

        Map<String, Object> sourceRow = new LinkedHashMap<>();
        sourceRow.put("id", 2L);
        sourceRow.put("task_name", "Task 2");
        sourceRow.put("project_project_name", null);
        sourceRow.put("project_project_code", null);

        Map<String, Object> result = ProjectionValueConverter.normalizeRow(metadata, sourceRow);

        Object projectObj = result.get("project");
        assertThat(projectObj).isNotNull();
        assertThat(projectObj).isInstanceOf(TestProjectView.class);
        TestProjectView projectView = (TestProjectView) projectObj;
        assertThat(projectView.getProjectName()).isNull();
        assertThat(projectView.getProjectCode()).isNull();
    }

    @Test
    void shouldNormalizeMultipleRows() {
        ProjectionMetadata metadata = createTestMetadata();

        List<Map<String, Object>> sourceList = new ArrayList<>();

        Map<String, Object> row1 = new LinkedHashMap<>();
        row1.put("id", 1L);
        row1.put("task_name", "Task 1");
        row1.put("project_project_name", "Project A");
        row1.put("project_project_code", "PROJ-A");
        sourceList.add(row1);

        Map<String, Object> row2 = new LinkedHashMap<>();
        row2.put("id", 2L);
        row2.put("task_name", "Task 2");
        row2.put("project_project_name", "Project B");
        row2.put("project_project_code", "PROJ-B");
        sourceList.add(row2);

        List<Map<String, Object>> results = ProjectionValueConverter.normalizeRows(metadata, sourceList);

        assertThat(results).hasSize(2);

        TestProjectView project1 = (TestProjectView) results.get(0).get("project");
        assertThat(project1.getProjectName()).isEqualTo("Project A");

        TestProjectView project2 = (TestProjectView) results.get(1).get("project");
        assertThat(project2.getProjectName()).isEqualTo("Project B");
    }

    private ProjectionMetadata createTestMetadata() {
        List<ProjectionField> fields = new ArrayList<>();

        fields.add(new ProjectionField("id", "id", "id", Long.class, null, true));
        fields.add(new ProjectionField("taskName", "task_name", "taskName", String.class, null, true));
        fields.add(new ProjectionField("project", "project", "project", TestProjectView.class, null, true));
        fields.add(new ProjectionField("project_projectName", "project.project_name AS project_project_name", "project_projectName", String.class, null, true));
        fields.add(new ProjectionField("project_projectCode", "project.project_code AS project_project_code", "project_projectCode", String.class, null, true));

        return new ProjectionMetadata(TestTaskView.class, Object.class, "task",
                "task", fields, List.of(), "key", false, List.of());
    }
}
