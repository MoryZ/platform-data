package com.old.silence.data.mybatis.test.projection;

import com.old.silence.data.commons.handler.GenericEnumTypeHandler;
import com.old.silence.data.commons.handler.StringToMapHandler;
import com.old.silence.data.mybatis.projection.ProjectionField;
import com.old.silence.data.mybatis.projection.ProjectionMetadata;
import com.old.silence.data.mybatis.test.fixture.entity.User;
import com.old.silence.data.mybatis.test.fixture.enmus.UserStatus;
import org.apache.ibatis.type.TypeHandler;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InterfaceProjectionFactoryTest {

    @Test
    void shouldPreConvertMapAndEnumBeforeGetterAccess() {
        ProjectionMetadata metadata = new ProjectionMetadata(
                SampleProjection.class,
                User.class,
                "user",
                List.of(
                        new ProjectionField("metadata", "metadata", Map.class, StringToMapHandler.class, false),
                        new ProjectionField("status", "status", UserStatus.class, genericEnumHandler(), false),
                        new ProjectionField("enabled", "is_enabled", Boolean.class, null, false)
                ),
                "ALL"
        );

        SampleProjection projection = invokeCreate(
                SampleProjection.class,
                metadata,
                Map.of(
                        "metadata", "{\"tier\":\"gold\",\"score\":7}",
                        "status", 1,
                        "is_enabled", true
                )
        );

        assertThat(projection.getMetadata()).containsEntry("tier", "gold");
        assertThat(projection.getMetadata()).containsEntry("score", 7);
        assertThat(projection.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(projection.isEnabled()).isTrue();
    }

    @Test
    void shouldFailFastWhenMapJsonCannotBeConverted() {
        ProjectionMetadata metadata = new ProjectionMetadata(
                SampleProjection.class,
                User.class,
                "user",
                List.of(new ProjectionField("metadata", "metadata", Map.class, StringToMapHandler.class, false)),
                "ALL"
        );

        assertThatThrownBy(() -> invokeCreate(
                SampleProjection.class,
                metadata,
                Map.of("metadata", "{bad-json")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Failed to materialize projection field 'metadata'");
    }

    @Test
    void shouldFailFastWhenEnumValueCannotBeConverted() {
        ProjectionMetadata metadata = new ProjectionMetadata(
                SampleProjection.class,
                User.class,
                "user",
                List.of(new ProjectionField("status", "status", UserStatus.class, genericEnumHandler(), false)),
                "ALL"
        );

        assertThatThrownBy(() -> invokeCreate(
                SampleProjection.class,
                metadata,
                Map.of("status", 999)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Failed to materialize projection field 'status'");
    }

    interface SampleProjection {

        Map<String, Object> getMetadata();

        UserStatus getStatus();

        boolean isEnabled();
    }

    @SuppressWarnings("unchecked")
    private static <P> P invokeCreate(Class<P> projectionType, ProjectionMetadata metadata, Map<String, Object> source) {
        try {
            Class<?> factoryClass = Class.forName("com.old.silence.data.mybatis.projection.InterfaceProjectionFactory");
            Method method = factoryClass.getDeclaredMethod("create", Class.class, ProjectionMetadata.class, Map.class);
            method.setAccessible(true);
            return (P) method.invoke(null, projectionType, metadata, source);
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
