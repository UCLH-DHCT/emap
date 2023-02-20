package uk.ac.ucl.rits.inform.informdb;


import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Field;
import java.util.Arrays;
import javax.persistence.JoinColumn;
import javax.persistence.Id;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static uk.ac.ucl.rits.inform.informdb.DBTestUtils.lowerCaseFirstCharacter;


/**
 * Ensure that all entities use the convention that a foreign key to another entity is <entityClass>Id.
 * @author Tom Young
 */
class TestKeyNaming {

    boolean isAForeignKey(Field f) {
        return Arrays.stream(f.getDeclaredAnnotations()).anyMatch(a -> a.annotationType().equals(JoinColumn.class));
    }

    boolean isAPrimaryKey(Field f) {
        return Arrays.stream(f.getDeclaredAnnotations()).anyMatch(a -> a.annotationType().equals(Id.class));
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
     * Assert that a field has the same name as the class it belongs to, plus the "Id" suffix
     * For example: A field in the ConditionType class should be called conditionTypeId.
     * @param field  Class field
     */
    void assertHasClassNameAsPrefix(Field field) {

        String declaringClass = field.getDeclaringClass().toString();
        String fieldName = field.getName();
        String capitalizedFieldName = fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
        assertTrue((declaringClass + "Id").endsWith(capitalizedFieldName));
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

    @ParameterizedTest
    @MethodSource("uk.ac.ucl.rits.inform.informdb.DBTestUtils#findAllEntities")
    void testPrimaryKeyNaming(Class<?> entityClass) {
        Arrays.stream(entityClass.getDeclaredFields())
                .filter(this::isAPrimaryKey)
                .forEach(this::assertHasClassNameAsPrefix);
    }
}
