package com.old.silence.data.mybatis.test.projection;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.old.silence.data.commons.handler.GenericEnumTypeHandler;
import com.old.silence.data.commons.handler.StringToMapHandler;
import com.old.silence.data.mybatis.projection.ProjectionField;
import com.old.silence.data.mybatis.projection.ProjectionMetadata;
import com.old.silence.data.mybatis.test.fixture.entity.User;
import com.old.silence.data.mybatis.test.fixture.enmus.UserStatus;
import org.apache.ibatis.type.TypeHandler;
import org.junit.jupiter.api.Test;

import java.beans.ConstructorProperties;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProjectionResultMaterializerTest {

    @Test
    void shouldMaterializeInterfaceAndConstructorProjectionsFromSameEntryPoint() {
        ProjectionMetadata interfaceMetadata = metadata(SampleView.class,
                new ProjectionField("metadata", "metadata", Map.class, StringToMapHandler.class, false),
                new ProjectionField("status", "status", UserStatus.class, genericEnumHandler(), false));
        ProjectionMetadata finalMetadata = metadata(SampleFinalDto.class,
                new ProjectionField("metadata", "metadata", Map.class, StringToMapHandler.class, false),
                new ProjectionField("status", "status", UserStatus.class, genericEnumHandler(), false));

        List<Map<String, Object>> rows = List.of(Map.of(
                "metadata", "{\"tier\":\"gold\"}",
                "status", 1
        ));

        List<SampleView> views = invokeMaterializeList(SampleView.class, interfaceMetadata, rows);
        List<SampleFinalDto> finals = invokeMaterializeList(SampleFinalDto.class, finalMetadata, rows);

        assertThat(views.getFirst().getMetadata()).containsEntry("tier", "gold");
        assertThat(views.getFirst().getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(finals.getFirst().getMetadata()).containsEntry("tier", "gold");
        assertThat(finals.getFirst().getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void shouldFailFastConsistentlyForListAndPageMaterialization() {
        ProjectionMetadata metadata = metadata(SampleView.class,
                new ProjectionField("status", "status", UserStatus.class, genericEnumHandler(), false));
        List<Map<String, Object>> rows = List.of(Map.of("status", 999));

        assertThatThrownBy(() -> invokeMaterializeList(SampleView.class, metadata, rows))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Failed to materialize projection field 'status'");

        Page<Map<String, Object>> rawPage = new Page<>(1, 10);
        rawPage.setRecords(rows);
        rawPage.setTotal(1);

        assertThatThrownBy(() -> invokeMaterializePage(new Page<>(1, 10), rawPage, SampleView.class, metadata))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Failed to materialize projection field 'status'");
    }

    private static ProjectionMetadata metadata(Class<?> projectionType, ProjectionField... fields) {
        return new ProjectionMetadata(projectionType, User.class, "user", List.of(fields), "ALL");
    }

    interface SampleView {

        Map<String, Object> getMetadata();

        UserStatus getStatus();
    }

    static final class SampleFinalDto {

        private final Map<String, Object> metadata;
        private final UserStatus status;

        @ConstructorProperties({"metadata", "status"})
        SampleFinalDto(Map<String, Object> metadata, UserStatus status) {
            this.metadata = metadata;
            this.status = status;
        }

        Map<String, Object> getMetadata() {
            return metadata;
        }

        UserStatus getStatus() {
            return status;
        }
    }

    @SuppressWarnings("unchecked")
    private static <P> List<P> invokeMaterializeList(Class<P> projectionType,
                                                     ProjectionMetadata metadata,
                                                     List<Map<String, Object>> rows) {
        try {
            Class<?> materializerClass = Class.forName("com.old.silence.data.mybatis.projection.ProjectionResultMaterializer");
            Method method = materializerClass.getDeclaredMethod("materializeList", Class.class, ProjectionMetadata.class, List.class);
            method.setAccessible(true);
            return (List<P>) method.invoke(null, projectionType, metadata, rows);
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new RuntimeException(cause);
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }

    @SuppressWarnings("unchecked")
    private static <P> IPage<P> invokeMaterializePage(Page<?> page,
                                                      IPage<Map<String, Object>> rawPage,
                                                      Class<P> projectionType,
                                                      ProjectionMetadata metadata) {
        try {
            Class<?> materializerClass = Class.forName("com.old.silence.data.mybatis.projection.ProjectionResultMaterializer");
            Method method = materializerClass.getDeclaredMethod("materializePage", Page.class, IPage.class, Class.class, ProjectionMetadata.class);
            method.setAccessible(true);
            return (IPage<P>) method.invoke(null, page, rawPage, projectionType, metadata);
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new RuntimeException(cause);
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends TypeHandler<?>> genericEnumHandler() {
        return (Class<? extends TypeHandler<?>>) (Class<?>) GenericEnumTypeHandler.class;
    }
}
