package uk.ac.ucl.rits.inform.informdb;


import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Field;
import java.util.Arrays;
import javax.persistence.JoinColumn;

import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Ensure that all entities use the convention that a foreign key to another entity is <entityClass>Id.
 * @author Tom Young
 */
class TestForeignKeyNaming {

    boolean isAForeignKey(Field f) {
        return Arrays.stream(f.getDeclaredAnnotations()).anyMatch(a -> a.annotationType().equals(JoinColumn.class));
    }

    void assertHasIdSuffix(Field f) {
        String fieldName = f.toString();
        assertTrue(fieldName.length() > 2
                && fieldName.charAt(0) == fieldName.toLowerCase().charAt(0)
                && fieldName.endsWith("Id"));
    }

    /**
     * Ensure that every foreign key has the correct naming convention.
     */
    @ParameterizedTest
    @MethodSource("uk.ac.ucl.rits.inform.informdb.DBTestUtils#findAllEntities")
    void testForeignKeyNaming(Class<?> entityClass) {
        Arrays.stream(entityClass.getDeclaredFields())
                .filter(this::isAForeignKey)
                .forEach(this::assertHasIdSuffix);
    }
}
