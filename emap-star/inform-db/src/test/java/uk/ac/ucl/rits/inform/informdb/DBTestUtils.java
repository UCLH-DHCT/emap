package uk.ac.ucl.rits.inform.informdb;

import org.reflections.Reflections;

import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Stream;

/**
 * General utilities for operations on InformDB that may be useful in tests.
 */
public class DBTestUtils {

    /**
     * Find all classes that contain fields that will end up in the DB.
     * I.e. those annotated with @Entity or @MappedSuperclass (or both)
     */
    public static Stream<Class<?>> findAllEntities() {
        Reflections reflections = new Reflections("uk.ac.ucl.rits.inform.informdb");

        // since @MappedSuperclass is inherited, these two sets overlap
        Set<Class<?>> entities = reflections.getTypesAnnotatedWith(Entity.class);
        Set<Class<?>> mappedSuperClasses = reflections.getTypesAnnotatedWith(MappedSuperclass.class);
        entities.addAll(mappedSuperClasses);
        return entities.stream().sorted(Comparator.comparing(Class::getName));
    }

    /**
     * Lower case the first character of a string
     */
    public static String lowerCaseFirstCharacter(String string){

        if (string.length() == 0) {
            throw new RuntimeException("Cannot convert the first character of a string with length 0");
        }
        return string.substring(0, 1).toLowerCase() + string.substring(1);
    }

}
