package uk.ac.ucl.rits.inform.informdb;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import javax.persistence.Column;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
     * Ensure every Instant field is of a timezone aware type.
     */
    @ParameterizedTest
    @MethodSource("uk.ac.ucl.rits.inform.informdb.DBTestUtils#findAllEntities")
    void testEntityInstantFields(Class<?> entityClass) {

        Arrays.stream(entityClass.getDeclaredFields())
                .filter(field -> field.getType().equals(Instant.class))
                .forEach(this::fieldIsTimestampWithTimeZone);
    }
}
