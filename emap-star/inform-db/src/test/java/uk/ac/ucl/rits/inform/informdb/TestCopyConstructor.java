package uk.ac.ucl.rits.inform.informdb;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;


public class TestCopyConstructor {

    private static final List<String> STRINGS = Arrays.asList("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K");
    private static final List<Integer> INTEGERS = Arrays.asList(0,  1,   2,   3,   4,   5,   6,   7,   8,   9,   10);

    private Integer index = 0;

    /**
     * Create a new instance of a class given a name of it
     *
     * @param className Name of a class
     * @return Instance of the class
     */
    Object newInstance(String className) throws ClassNotFoundException, NoSuchMethodException,
            InvocationTargetException, InstantiationException, IllegalAccessException {

        index = (index + 1) % Collections.min(Arrays.asList(STRINGS.size(), INTEGERS.size()));

        if (className.equals("long") | className.equals("java.lang.Long")){
            return (long) INTEGERS.get(index);
        }

        // Match primitive types
        switch (className){
            case "java.lang.Boolean":
                return true;
            case "java.time.Instant":
                return Instant.now();
            case "java.lang.Double":
                return (double) INTEGERS.get(index);
            case "java.lang.String":
                return STRINGS.get(index);
            case "uk.ac.ucl.rits.inform.informdb.TemporalFrom":
                return new TemporalFrom(Instant.now(), Instant.now().plus(1, ChronoUnit.SECONDS));
            case "java.time.LocalDate":
                return LocalDate.now();
            case "java.util.List":
                return List.of();
        }

        Class<?> entity = Class.forName(className);
        Object instance = entity.getConstructor().newInstance();

        var setMethods = Arrays.stream(entity.getMethods())
                .filter(m -> m.getName().startsWith("set")).collect(Collectors.toList());

        // Set attributes by invoking all the setter methods
        for (Method method : setMethods){

            if (method.getName().equals("setValueAsBytes")){
                // No corresponding get method, so skip
                continue;
            }

            // System.out.println("Params of "+ method.getName()+" are");
            // Arrays.stream(method.getParameters()).forEach(p -> System.out.println(p));

            Object[] params = Arrays.stream(method.getParameterTypes()).map(p ->
                    {
                        try {
                            // System.out.println("Set method required: "+ p.getName());
                            return newInstance(p.getName());
                        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException
                                | InstantiationException | IllegalAccessException e) {
                            e.printStackTrace();
                        }
                        throw new RuntimeException("Failed to generate an instance of " + p.getName());
                    }
            ).toArray();

            try {
                method.invoke(instance, params);
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }

        return instance;
    }

    boolean isCopyConstructorOf(Constructor<?> constructor, Class<?> entity) {
        return constructor.getParameterCount() == 1
                && constructor.getParameterTypes()[0].getName().equals(entity.getTypeName());
    }

    boolean hasCopyConstructor(Class<?> entity) {
        return Arrays.stream(entity.getDeclaredConstructors()).anyMatch(c -> isCopyConstructorOf(c, entity));
    }

    void assertCorrectCopyConstructor(Class<?> entity){

        try {
            System.out.println("Creating instance of "+ entity.getName());
            Object instance = newInstance(entity.getName());

            Object instanceCopy = Arrays.stream(entity.getMethods())
                                    .filter(m -> m.getName().equals("copy"))
                                    .findFirst().orElseThrow(() -> new RuntimeException("No copy method"))
                                    .invoke(instance);

            List<Method> getMethods = Arrays.stream(entity.getMethods())
                    .filter(m -> m.getName().startsWith("get"))
                    .collect(Collectors.toList());

            for (Method getMethod: getMethods){
                if (getMethod.getName().equals("get" + entity.getSimpleName() + "Id")){
                    // Don't want to check that the primary keys are identical. For example, for a ConditionType class
                    // entity then the conditionTypeId can be different
                    continue;
                }

                assertEquals(getMethod.invoke(instance), getMethod.invoke(instanceCopy));
            }

        } catch (ClassNotFoundException | IllegalAccessException | NoSuchMethodException
                | InvocationTargetException | InstantiationException e) {
            e.printStackTrace();
            fail();
        }
    }

    @ParameterizedTest
    @MethodSource("uk.ac.ucl.rits.inform.informdb.DBTestUtils#findAllEntities")
    void testClassHasCorrectCopyConstructor(Class<?> entityClass) {

        if (hasCopyConstructor(entityClass) && !entityClass.getName().endsWith("Core")){
            assertCorrectCopyConstructor(entityClass);
        }
    }
}
