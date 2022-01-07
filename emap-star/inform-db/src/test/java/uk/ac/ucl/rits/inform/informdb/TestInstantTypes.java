package uk.ac.ucl.rits.inform.informdb;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.reflections.Reflections;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Ensure Instant types are annotated correctly.
 */
class TestInstantTypes {
    private void fieldIsTimestampWithTimeZone(Field field) {
        List<String> columnDefinition = Arrays.stream(field.getAnnotationsByType(Column.class))
                .map(Column::columnDefinition)
                .collect(Collectors.toList());
        assertEquals(1, columnDefinition.size(),
                String.format("field '%s' needs exactly one @Column annotation with a columnDefinition specified", field.getName()));
        assertEquals("timestamp with time zone", columnDefinition.get(0));
    }

    /**
     * Find all classes that contain fields that will end up in the DB.
     * Ie. those annotated with @Entity or @MappedSuperclass (or both)
     */
    public static Stream<Class<?>> findAllEntities() {
        Reflections reflections = new Reflections("uk.ac.ucl.rits.inform.informdb");

        // since @MappedSuperclass is inherited, these two sets overlap
        Set<Class<?>> entities = reflections.getTypesAnnotatedWith(Entity.class);
        Set<Class<?>> mappedSuperClasses = reflections.getTypesAnnotatedWith(MappedSuperclass.class);
        entities.addAll(mappedSuperClasses);
        return entities.stream().sorted(Comparator.comparing(Class::getName));
    }

    /*
     * Ensure every Instant field is of a timezone aware type.
     */
    @ParameterizedTest
    @MethodSource("findAllEntities")
    void testEntityInstantFields(Class<?> entityClass) {
        Arrays.stream(entityClass.getDeclaredFields())
                .filter(field -> field.getType().equals(Instant.class))
                .forEach(this::fieldIsTimestampWithTimeZone);
    }
}
