package uk.ac.ucl.rits.inform.informdb;


import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Field;
import java.util.Arrays;
import javax.persistence.JoinColumn;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static uk.ac.ucl.rits.inform.informdb.DBTestUtils.lowerCaseFirstCharacter;


/**
 * Ensure that all entities use the convention that a foreign key to another entity is <entityClass>Id.
 * @author Tom Young
 */
class TestForeignKeyNaming {

    boolean isAForeignKey(Field f) {
        return Arrays.stream(f.getDeclaredAnnotations()).anyMatch(a -> a.annotationType().equals(JoinColumn.class));
    }

    /**
     * Assert that a field has the name entityClassId where EntityClass is the simple class name.
     * For example: A ConditionType should have a conditionTypeId name.
     * @param field  Class field
     */
    void assertHasIdSuffix(Field field) {

        String className = lowerCaseFirstCharacter(field.getType().getSimpleName());
        String fieldName = field.getName();

        if (fieldName.equals("liveMrnId")){
            // allow this specific exception to the rule
            return;
        }

        assertEquals(className + "Id", fieldName);
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
