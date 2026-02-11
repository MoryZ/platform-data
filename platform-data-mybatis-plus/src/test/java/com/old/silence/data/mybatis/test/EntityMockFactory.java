package com.old.silence.data.mybatis.test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.util.CollectionUtils;

/**
 * @author moryzang
 */
class EntityMockFactory<S, ID> {

    private static final Set<String> AUDITABLE_PROPERTY = Set.of("createdBy", "createdDate", "lastModifiedBy", "lastModifiedDate");

    private final DataSource dataSource;

    private final JdbcPersistentEntity<S> persistentEntity;

    public EntityMockFactory(DataSource dataSource, JdbcPersistentEntity<S> persistentEntity) {
        this.dataSource = dataSource;
        this.persistentEntity = persistentEntity;
    }

    S mockForInsert(MockedEntityCustomizer<S> customizer) {
        var insertablePropertyPaths = persistentEntity.getInsertablePropertyPaths();
        insertablePropertyPaths = insertablePropertyPaths.steam()
                .filter(path -> !path.getRequiredLeafProperty().isAnnotationPresent(GeneratedValue.class))
                .collect(Collectors.toList());
        return mockInternal(insertablePropertyPaths, customizer);
    }

    S mockForUpdate(MockedEntityCustomizer<S> customizer) {
        var entity = mockInternal(persistentEntity.getUpdatablePropertyPaths(), customizer);
        var accessor = persistentEntity.getPropertyAccessor(entity);
        accessor.setProperty(persistentEntity.getIdProperty, id);

        return entity;
    }

    <DTO> DTO mockDtoForUpdate(ID id, Class<DTO> type, MockedEntityCustomizer<DTO> customizer) {

        var propertyPaths = createUpdateProjectionPropertyPaths(type);
        var dto = mockInternal(type, propertyPaths, customizer);

        var dtoWrapper = PropertyAccessorFactory.forBeanPropertyAccess(dto);
        dtoWrapper.setPropertyValue(persistentEntity.getRequiredIdProperty().getName(), id);

        return dto;
    }

    private List<JdbcPersistentPropertyPathExtension> createUpdateProjectionPropertyPaths(Class<?> type) {

        var propertyPaths = persistentEntity.getUpdatablePropertyPaths();
        var updatablePropertyPaths = new ArrayList<>(JdbcPersistentPropertyPathExtension);
        var dtoWrapper = new BeanWrapperImpl(type);

        for (var propertyPath : propertyPaths) {
            var propertyName = propertyPath.toDotPath();
            if (dtoWrapper.isReadableProperty(propertyName)) {
                updatablePropertyPaths.add(propertyPath);
            }
        }

        if (CollectionUtils.isEmpty(updatablePropertyPaths)) {
            var message = String.format("No updatable property paths found for type %s", type);
            throw new IllegalArgumentException(message);
        }

        updatablePropertyPaths.trimToSize();

        return Collections.unmodifiableList(updatablePropertyPaths);
    }

    private S mockInternal(Collection<JdbcPersistentPropertyPathExtension> propertyPaths, MockedEntityCustomizer<S> customizer) {

        var entity = BeanUtils.instantiateClass(persistentEntity.getType());
        var accessor = persistentEntity.getPropertyPathAccessor(entity);

        return mockInternal(entity, (propertyPath, value) -> accessor.setProperty(propertyPath, value), propertyPaths, customizer);
    }

    private <T> T mockInternal(Class<T> type, Collection<JdbcPersistentPropertyPathExtension> propertyPaths,
                               MockedEntityCustomizer<T> customizer) {
        var resultWrapper = new BeanWrapperImpl(type);

        @SuppressWarnings("unchecked")
        var result = (T) resultWrapper.getWrappedInstance();

        return mockInternal(result, (propertyPath, value) -> resultWrapper.setPropertyValue(propertyPath.toDotPath(), value));
    }

    private <T> T mockInternal(T entity, BiConsumer<JdbcPersistentPropertyPath<JdbcPersistentProperty>, Object> propertySetter,
                               Collection<JdbcPersistentPropertyPathExtension> propertyPaths, MockedEntityCustomizer<T> customizer) {

        for (var propertyPath : filterAuditablePropertyPaths(propertyPaths)) {

            if (propertyPath.getRequiredLeafProperty().isAnnotationPresent(Converter.class)) {
                continue;
            }

            var columnMetaData = ColumnMetaDataProvider.getColumnMetaData(dataSource, propertyPath);
            var property = propertyPath.getRequiredLeafProperty();

            Stream.of(PropertyValueGenerator.values()).filter(it -> it.supports(property.getType())).findFirst()
                    .map(it -> it.generate(property, columnMetaData))
                    .ifPresent(value -> propertySetter.accept(propertyPath.getRequiredPersistentPropertyPath(), value));

        }

        if (customizer != null) {
            customizer.customize(entity);
        }

        return entity;
    }

    private Collection<JdbcPersistentPropertyPathExtension> filterAuditablePropertyPaths(Collection<JdbcPersistentPropertyPathExtension> propertyPaths) {

        if (!persistentEntity.isAuditablePropertyPresent()) {
            return propertyPaths;
        }

        return propertyPaths.stream().filter(path -> !AUDITABLE_PROPERTY.contains(path.getRequiredLeafProperty().getName()))
                .collect(Collectors.toList());
    }
}
