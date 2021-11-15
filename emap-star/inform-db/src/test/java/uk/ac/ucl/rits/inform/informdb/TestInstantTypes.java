package uk.ac.ucl.rits.inform.informdb;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.reflections.Reflections;
import uk.ac.ucl.rits.inform.informdb.consults.ConsultationRequest;
import uk.ac.ucl.rits.inform.informdb.consults.ConsultationRequestAudit;
import uk.ac.ucl.rits.inform.informdb.decisions.AdvanceDecision;
import uk.ac.ucl.rits.inform.informdb.decisions.AdvanceDecisionAudit;
import uk.ac.ucl.rits.inform.informdb.visit_recordings.VisitObservationType;
import uk.ac.ucl.rits.inform.informdb.visit_recordings.VisitObservationTypeAudit;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Ensure Instant types are annotated correctly.
 */
class TestInstantTypes {
    /**
     * Instant fields which are allowed to have a non timezone aware DB type.
     */
    final HashSet<Field> ignoreFields = new HashSet<>();

    TestInstantTypes() throws NoSuchFieldException {
        /*
         * At time of writing, these are the fields that would cause the tests to fail if not excluded.
         * Very likely these fields should be fixed and then removed from this exclusion list.
         */
        ignoreFields.add(ConsultationRequest.class.getDeclaredField("statusChangeTime"));
        ignoreFields.add(ConsultationRequest.class.getDeclaredField("scheduledDatetime"));
        ignoreFields.add(ConsultationRequestAudit.class.getDeclaredField("statusChangeTime"));
        ignoreFields.add(ConsultationRequestAudit.class.getDeclaredField("scheduledDatetime"));
        ignoreFields.add(AdvanceDecision.class.getDeclaredField("statusChangeDatetime"));
        ignoreFields.add(AdvanceDecision.class.getDeclaredField("requestedDatetime"));
        ignoreFields.add(AdvanceDecisionAudit.class.getDeclaredField("statusChangeDatetime"));
        ignoreFields.add(AdvanceDecisionAudit.class.getDeclaredField("requestedDatetime"));
        ignoreFields.add(VisitObservationType.class.getDeclaredField("creationTime"));
        ignoreFields.add(VisitObservationTypeAudit.class.getDeclaredField("creationTime"));
    }

    private void fieldIsTimestampWithTimeZone(Field f) {
        List<String> columnDefinition = Arrays.stream(f.getAnnotationsByType(Column.class))
                .map(Column::columnDefinition)
                .collect(Collectors.toList());
        assertEquals(1, columnDefinition.size(),
                String.format("field %s needs exactly one @Column annotation with a columnDefinition specified", f.getName()));
        assertEquals("timestamp with time zone", columnDefinition.get(0));
    }

    /**
     * Find all classes that contain fields that will end up in the DB.
     * Ie. those annotated with @Entity or @MappedSuperclass (or both)
     */
    public static Stream<Class<?>> findAllEntities() {
        Reflections reflections = new Reflections("uk.ac.ucl.rits.inform.informdb" );

        // since @MappedSuperclass is inherited, these two sets overlap
        Set<Class<?>> entities = reflections.getTypesAnnotatedWith(Entity.class);
        Set<Class<?>> mappedSuperClasses = reflections.getTypesAnnotatedWith(MappedSuperclass.class);
        entities.addAll(mappedSuperClasses);
        return entities.stream().sorted((e1, e2) -> e1.getName().compareTo(e2.getName()));
    }

    /*
     * Ensure every Instant field is of a timezone aware type.
     */
    @ParameterizedTest
    @MethodSource("findAllEntities")
    void testEntityInstantFields(Class<?> entityClass) {
        List<Field> allInstantFields = Arrays.stream(entityClass.getDeclaredFields())
                .filter(f -> f.getType().equals(Instant.class))
                .collect(Collectors.toList());

        for (var f : allInstantFields) {
            if (!ignoreFields.contains(f)) {
                fieldIsTimestampWithTimeZone(f);
            }
        }
    }
}
