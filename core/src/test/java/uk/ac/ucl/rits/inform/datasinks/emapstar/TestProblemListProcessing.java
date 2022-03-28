package uk.ac.ucl.rits.inform.datasinks.emapstar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.ConditionTypeRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.HospitalVisitRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.PatientConditionAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.PatientConditionRepository;
import uk.ac.ucl.rits.inform.informdb.conditions.PatientCondition;
import uk.ac.ucl.rits.inform.informdb.conditions.PatientConditionAudit;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.PatientProblem;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;



/**
 * Test cases to ensure that processing of patient problem messages is working correctly. Note that a problem is also
 * known as a problem list, thus the terms may be used interchangeably.
 * @author Anika Cawthorn
 * @author Tom Young
 */
public class TestProblemListProcessing extends MessageProcessingBase {
    @Autowired
    PatientConditionRepository patientConditionRepository;
    @Autowired
    PatientConditionAuditRepository patientConditionAuditRepository;
    @Autowired
    ConditionTypeRepository conditionTypeRepository;
    @Autowired
    HospitalVisitRepository hospitalVisitRepository;

    private List<PatientProblem> hooverDelteMessages;

    private PatientProblem hooverUpdateMessage;
    private PatientProblem hooverQueryOrderingMessage;
    private PatientProblem hl7MyelomaInpatient;
    private PatientProblem hl7OtherProblemInpatient;
    private PatientProblem hl7MyelomaOutpatient;

    private static final String SAMPLE_COMMENT = "a comment";
    private static final String UPDATED_PROBLEM_NAME = "new problem";
    private static final String MYELOMA_PROBLEM_NAME = "Multiple Myeloma";
    private static final String MYELOMA_PROBLEM_CODE = "C90.0";
    private static final String BACKACHE_PROBLEM_CODE = "M54.9";
    private static final String BACKACHE_PROBLEM_NAME = "Backache";
    private static final String PATIENT_MRN = "8DcEwvqa8Q3";


    @BeforeEach
    private void setUp() throws IOException {
        hooverUpdateMessage = messageFactory.getPatientProblems("updated_only.yaml").get(0);
        hooverDelteMessages =  messageFactory.getPatientProblems("deleted_only.yaml");
        hooverQueryOrderingMessage = messageFactory.getPatientProblems("query_ordering_with_nulls.yaml").get(0);
        hl7MyelomaInpatient = messageFactory.getPatientProblems("hl7/minimal_myeloma_inpatient.yaml").get(0);
        hl7OtherProblemInpatient =  messageFactory.getPatientProblems("hl7/minimal_other_problem_inpatient.yaml").get(0);
        hl7MyelomaOutpatient = messageFactory.getPatientProblems("hl7/minimal_myeloma_outpatient.yaml").get(0);
    }


    /**
     * Given that no problem list exists for outpatient
     * When a minimal problem list message arrives
     * Then a new problem list is generated for this patient (not linked to a hospital stay)
     * with the correct fields
     */
    @Test
    void testCreateProblemOutpatient() throws EmapOperationMessageProcessingException {

        assertEquals(Instant.parse("2019-06-07T11:32:00Z"), hl7MyelomaOutpatient.getUpdatedDateTime());

        processSingleMessage(hl7MyelomaOutpatient);
        List<PatientCondition> entities = getAllEntities(patientConditionRepository);

        assertEquals(1, entities.size());

        PatientCondition entity = entities.get(0);

        assertNull(entity.getHospitalVisitId());
        assertEquals(PATIENT_MRN, entity.getMrnId().getMrn());
        assertEquals(Instant.parse("2019-06-02T10:31:05Z"), entity.getAddedDateTime());
        assertEquals("ACTIVE", entity.getStatus());
        assertEquals(MYELOMA_PROBLEM_NAME, entity.getConditionTypeId().getName());
        assertEquals(MYELOMA_PROBLEM_CODE, entity.getConditionTypeId().getInternalCode());
    }


    /**
     * Given that no problem list exists for inpatient
     * When a minimal problem list message arrives
     * Then a new problem list is generated for this patient and it is linked to a hospital stay
     */
    @Test
    void testCreateProblemInpatient() throws EmapOperationMessageProcessingException {

        processSingleMessage(hl7MyelomaInpatient);
        List<PatientCondition> entities = getAllEntities(patientConditionRepository);
        assertEquals(1, entities.size());

        PatientCondition entity = entities.get(0);
        assertNotNull(entity.getHospitalVisitId());

        // assertEquals(11144333L, entity.getHospitalVisitId().getHospitalVisitId());

        assertEquals(PATIENT_MRN, entity.getMrnId().getMrn());
        assertEquals(Instant.parse("2019-06-02T10:31:05Z"), entity.getAddedDateTime());
        assertEquals("ACTIVE", entity.getStatus());
        assertEquals(MYELOMA_PROBLEM_NAME, entity.getConditionTypeId().getName());
        assertEquals(MYELOMA_PROBLEM_CODE, entity.getConditionTypeId().getInternalCode());
    }


    /**
     * Given that a problem list exists for a patient
     * When a new minimal problem list message arrives that concerns the same patient
     * Then the message is added as a condition for the patient
     */
    @Test
    void testAddProblemList() throws EmapOperationMessageProcessingException {

        processSingleMessage(hl7MyelomaInpatient);
        processSingleMessage(hl7OtherProblemInpatient);

        List<PatientCondition> entities = getAllEntities(patientConditionRepository);
        assertEquals(2, entities.size());
        PatientCondition firstProblem = entities.get(0);
        PatientCondition secondProblem = entities.get(1);

        // Conditions should refer to the same patient, but have different types and problem names
        assertEquals(firstProblem.getMrnId(), secondProblem.getMrnId());
        assertNotEquals(firstProblem.getConditionTypeId(), secondProblem.getConditionTypeId());
        assertNotEquals(firstProblem.getConditionTypeId().getName(), secondProblem.getConditionTypeId().getName());
    }


    /**
     * Given that a problem list exists for patient
     * When a minimal problem list message arrives that's older but concerning the same patient
     * Then nothing is changed
     */
    @Test
    void testProcessingOlderMessage() throws EmapOperationMessageProcessingException {

        hl7MyelomaInpatient.setComment(InterchangeValue.buildFromHl7("the current problem"));

        processSingleMessage(hl7MyelomaInpatient);

        Instant older_time = hl7MyelomaInpatient.getUpdatedDateTime().minus(1, ChronoUnit.SECONDS);
        hl7MyelomaInpatient.setUpdatedDateTime(older_time);
        hl7MyelomaInpatient.setComment(InterchangeValue.buildFromHl7("an older problem"));

        processSingleMessage(hl7MyelomaInpatient);

        List<PatientCondition> entities = getAllEntities(patientConditionRepository);
        assertEquals(1, entities.size());

        assertEquals("the current problem", entities.get(0).getComment());
    }


    /**
     * Given that no problem list for a patient exists
     * When a problem list message list with notes arrives
     * Then the problem list is added with comments
     */
    @Test
    void testProcessingWithSetComment() throws EmapOperationMessageProcessingException {
        hl7MyelomaInpatient.setComment(InterchangeValue.buildFromHl7(SAMPLE_COMMENT));

        processSingleMessage(hl7MyelomaInpatient);

        PatientCondition condition = getAllEntities(patientConditionRepository).get(0);
        assertEquals(SAMPLE_COMMENT, condition.getComment());
    }


    /**
     * Given that a problem list exist for patient
     * When a problem list message arrive that's newer, concerns the same patient and contains updated fields
     * Then the existing problem list is updated accordingly
     * (e.g. notes are added in for a specific problem)
     */
    @Test
    void testProcessingNewerMessage() throws EmapOperationMessageProcessingException {

        processSingleMessage(hl7MyelomaInpatient);

        assertNull(getAllEntities(patientConditionRepository).get(0).getResolutionDateTime());

        hl7MyelomaInpatient.setUpdatedDateTime(hl7MyelomaInpatient.getUpdatedDateTime().plus(1, ChronoUnit.SECONDS));
        hl7MyelomaInpatient.setResolvedTime(InterchangeValue.buildFromHl7(Instant.now()));
        processSingleMessage(hl7MyelomaInpatient);

        assertNotNull(getAllEntities(patientConditionRepository).get(0).getResolutionDateTime());
    }


    /**
     * Given that a problem list exist for a patient
     * When a problem list message for deleting an existing problem arrives
     * Then this problem list is deleted for the patient
     * (e.g diagnosis is entered as "working hypothesis" and then deleted as tests come back)
     */
    @Test
    void testDeletingAProblem() throws EmapOperationMessageProcessingException{

        processSingleMessage(hl7MyelomaInpatient);

        hl7MyelomaInpatient.setAction("DE");
        assertEquals("ACTIVE", hl7MyelomaInpatient.getStatus().get());
        processSingleMessage(hl7MyelomaInpatient);

        assertEquals(getAllEntities(patientConditionRepository).size(), 0);
    }
    @Test
    void testDeletingAProblemClarity() throws EmapOperationMessageProcessingException{

        Instant newestUpdatedTime = hooverUpdateMessage.getUpdatedDateTime();

        // Adding then deleting a problem for the same patient should leave the condition repository empty
        PatientProblem addMessage = hooverUpdateMessage;
        processSingleMessage(addMessage);

        PatientCondition condition = getAllEntities(patientConditionRepository).get(0);

        PatientProblem deleteMessage = hooverUpdateMessage;
        deleteMessage.setAction("DE");
        deleteMessage.setStatus(InterchangeValue.buildFromHl7("ACTIVE"));
        deleteMessage.setUpdatedDateTime(newestUpdatedTime.plus(1, ChronoUnit.SECONDS));

        // message needs to refer to the same patient
        assertEquals(addMessage.getMrn(), deleteMessage.getMrn());
        assertEquals(addMessage.getAddedTime(), deleteMessage.getAddedTime());

        processSingleMessage(deleteMessage);

        assertEquals(0, getAllEntities(patientConditionRepository).size());

        // should have an audit log of the condition that was deleted
        // TODO: Should there be two entries in the audit log?
        PatientConditionAudit audit = getAllEntities(patientConditionAuditRepository).get(1);
        assertEquals(hooverUpdateMessage.getAddedTime(), audit.getAddedDateTime());
    }


    /**
     * Given that a problem list exists for a patient
     * When a new problem list arrives with the same code but a different name
     * Then a new problem is not added and only the problem name is updated
     */
    @Test
    void testProblemNameUpdate() throws EmapOperationMessageProcessingException{

        processSingleMessage(hl7MyelomaInpatient);

        hl7MyelomaInpatient.setProblemName(InterchangeValue.buildFromHl7(UPDATED_PROBLEM_NAME));
        hl7MyelomaInpatient.setUpdatedDateTime(hl7MyelomaInpatient.getUpdatedDateTime().plus(1, ChronoUnit.SECONDS));

        processSingleMessage(hl7MyelomaInpatient);

        List<PatientCondition> entities = getAllEntities(patientConditionRepository);
        assertEquals(1, entities.size());
        assertEquals(UPDATED_PROBLEM_NAME, entities.get(0).getConditionTypeId().getName());
    }


    /**
     * Given that a problem list does not exist for a patient
     * When one arrives from clarity
     * Then the patient does have an associated problem list, with the correct fields
     */
    @Test
    void testClarityProblemAddition() throws EmapOperationMessageProcessingException{

        processSingleMessage(hooverUpdateMessage);

        List<PatientCondition> entities = getAllEntities(patientConditionRepository);
        assertEquals(1, entities.size());

        PatientCondition entity = entities.get(0);
        assertEquals(PATIENT_MRN, entity.getMrnId().getMrn());
        assertEquals(BACKACHE_PROBLEM_CODE, entity.getConditionTypeId().getInternalCode());
        assertEquals(BACKACHE_PROBLEM_NAME, entity.getConditionTypeId().getName());
        assertEquals(1, entity.getInternalId());
        assertEquals(Instant.parse("2019-06-02T10:31:05Z"), entity.getAddedDateTime());
        assertEquals(Instant.parse("2019-06-08T14:22:01Z"), entity.getResolutionDateTime());
        assertEquals(LocalDate.parse("2019-03-05"), entity.getOnsetDate());
    }


    /**
     * Given that a problem list does not exist for a patient
     * When one arrives with a delete action
     * Then an exception should not be thrown and no problem list should be added
     */
    @Test
    void testClarityProblemDeletion() throws EmapOperationMessageProcessingException {

        PatientProblem message = hooverDelteMessages.get(0);

        processSingleMessage(message);

        assertDoesNotThrow(() -> processSingleMessage(message));
        assertEquals(0, getAllEntities(patientConditionRepository).size());
    }


    /**
     * Given ??
     * When ??
     * Then ??
     */
    @Test
    void testQueryOrderingMessageProblem() throws EmapOperationMessageProcessingException{

        processSingleMessage(hooverQueryOrderingMessage);
        PatientCondition condition = getAllEntities(patientConditionRepository).get(0);

        assertEquals(PATIENT_MRN, condition.getMrnId().getMrn());
        assertEquals(BACKACHE_PROBLEM_NAME, condition.getConditionTypeId().getName());
        assertEquals(BACKACHE_PROBLEM_CODE, condition.getConditionTypeId().getInternalCode());
        assertEquals(1, condition.getInternalId());
        assertEquals("1234", condition.getHospitalVisitId().getEncounter());
        assertEquals("Resolved", condition.getStatus());
        assertEquals(Instant.parse("2019-06-08T14:22:01Z"), condition.getResolutionDateTime());
        assertEquals(LocalDate.parse("2019-03-05"), condition.getOnsetDate());

        // TODO: Assert something else
    }


    /**
     * Given that no problem lists exist
     * When one arrives that has either an AD or UP action associated
     * Then the same state is reached
     */
    @Test
    void testSameStateObtainedForUpAndAdActions() throws EmapOperationMessageProcessingException {

        hl7MyelomaInpatient.setAction("UP");
        processSingleMessage(hl7MyelomaInpatient);

        PatientCondition conditionFromUp = getAllEntities(patientConditionRepository).get(0);

        patientConditionRepository.deleteAll();
        conditionTypeRepository.deleteAll();

        hl7MyelomaInpatient.setAction("AD");
        processSingleMessage(hl7MyelomaInpatient);

        PatientCondition conditionFromAd = getAllEntities(patientConditionRepository).get(0);

        assertEquals(conditionFromAd.getInternalId(), conditionFromUp.getInternalId());
        assertEquals(conditionFromAd.getAddedDateTime(), conditionFromUp.getAddedDateTime());
        assertEquals(conditionFromAd.getResolutionDateTime(), conditionFromUp.getResolutionDateTime());
        assertEquals(conditionFromAd.getStatus(), conditionFromUp.getStatus());
        assertEquals(conditionFromAd.getComment(), conditionFromUp.getComment());
        assertEquals(conditionFromAd.getPriority(), conditionFromUp.getPriority());
    }


    /**
     * Given that no problem lists exist
     * When one arrives that has a delete action but a 'Delete' or 'Resolved' status
     * Then a condition should be added into the table
     */
    @Test
    void testDeleteActionWithDeleteOrResolvedStatus() throws EmapOperationMessageProcessingException {

        hl7MyelomaInpatient.setAction("DE");
        hl7MyelomaInpatient.setStatus(InterchangeValue.buildFromHl7("ACTIVE"));
        processSingleMessage(hl7MyelomaInpatient);

        assertEquals(0, getAllEntities(patientConditionRepository).size());

        hl7MyelomaInpatient.setStatus(InterchangeValue.buildFromHl7("Deleted"));
        processSingleMessage(hl7MyelomaInpatient);
        assertEquals(1, getAllEntities(patientConditionRepository).size());

        patientConditionRepository.deleteAll();

        hl7MyelomaInpatient.setStatus(InterchangeValue.buildFromHl7("Resolved"));
        processSingleMessage(hl7MyelomaInpatient);
        assertEquals(1, getAllEntities(patientConditionRepository).size());
    }
}
