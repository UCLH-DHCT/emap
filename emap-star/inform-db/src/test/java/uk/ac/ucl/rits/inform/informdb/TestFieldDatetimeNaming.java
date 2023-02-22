package uk.ac.ucl.rits.inform.informdb;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;


public class TestFieldDatetimeNaming {

    /**
     * Does this field have an Instant type?
     */
    boolean isAnInstant(Field field){
        return field.getType().getName().equals("java.time.Instant");
    }

    /**
     * Is a field excluded from this rule?
     */
    boolean isNotExcluded(Field field){

        String fieldName = field.getName();
        List<String> exclusions = Arrays.asList("validUntil", "validFrom", "storedFrom", "storedUntil",
                "datetimeOfBirth", "datetimeOfDeath");

        return !exclusions.contains(fieldName);
    }

    /**
     * Assert that this field does have a Datetime suffix
     */
    void assertHasDatetimeSuffix(Field field){
        assertTrue(field.getName().endsWith("Datetime"), field.getName() + " has no Datetime suffix");
    }


    @ParameterizedTest
    @MethodSource("uk.ac.ucl.rits.inform.informdb.DBTestUtils#findAllEntities")
    void testInstantAttributesHaveDatetimeSuffix(Class<?> entityClass) {
        Arrays.stream(entityClass.getDeclaredFields())
                .filter(this::isAnInstant)
                .filter(this::isNotExcluded)
                .forEach(this::assertHasDatetimeSuffix);
    }

}
