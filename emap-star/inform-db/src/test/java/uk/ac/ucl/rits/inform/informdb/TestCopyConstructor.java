package uk.ac.ucl.rits.inform.informdb;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.reflections.Reflections;

import javax.persistence.Entity;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;


public class TestCopyConstructor {

    private static final List<String> STRINGS = Arrays.asList("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K");

    private static final List<Integer> INTEGERS = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);

    boolean isCopyConstructorOf(Constructor<?> constructor, Class<?> entity){
        return constructor.getParameterCount() == 1
                && constructor.getParameterTypes()[0].getName().equals(entity.getTypeName());
    }


    /**
     * Create a new instance of a class given a name of it 
     *
     * @param className Name of a class
     * @return Instance of the class
     */
    Object newInstance(String className) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {

        Random random = new Random();

        // Match primitive types
        switch (className){
            case "java.time.Instant":
                return Instant.now();
            case "java.lang.String":
                return STRINGS.get(random.nextInt(STRINGS.size()));
            case "long":
                return (long) INTEGERS.get(random.nextInt(INTEGERS.size()));
        }

        Class<?> entity = Class.forName(className);
        Object instance = entity.getConstructor().newInstance();

        var setMethods = Arrays.stream(entity.getMethods())
                .filter(m -> m.getName().startsWith("set")).collect(Collectors.toList());

        // Set attributes by invoking all the setter methods
        for (Method method : setMethods){
            Object[] params = Arrays.stream(method.getParameterTypes()).map(p ->
                    {
                        try {
                            return newInstance(p.getName());
                        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
                            e.printStackTrace();
                        }
                        throw new RuntimeException("Failed to generate an instance of " + p.getSimpleName());
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

    @Test
    void test() throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {

        Reflections reflections = new Reflections("uk.ac.ucl.rits.inform.informdb.conditions");
        var entities = reflections.getTypesAnnotatedWith(Entity.class)
                .stream()
                .sorted(Comparator.comparing(Class::getName))
                .collect(Collectors.toList());

        var a = newInstance(entities.get(0).getName());
        System.out.println(a);

    }
}
