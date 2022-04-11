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

import static org.junit.jupiter.api.Assertions.assertTrue;
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

    private List<PatientProblem> hooverDeleteMessages;
    private List<PatientProblem> hooverAddThenDeleteMessages;

    private PatientProblem hooverUpdateMessage;
    private PatientProblem hl7MyelomaInpatient;
    private PatientProblem hl7OtherProblemInpatient;
    private PatientProblem hl7MyelomaOutpatient;

    private static final String SAMPLE_COMMENT = "a comment";
    private static final String MYELOMA_PROBLEM_NAME = "Multiple Myeloma";
    private static final String MYELOMA_PROBLEM_CODE = "C90.0";
    private static final String BACKACHE_PROBLEM_CODE = "M54.9";
    private static final String BACKACHE_PROBLEM_NAME = "Backache";
    private static final String MYELOMA_ADDED_TIME = "2019-06-02T10:31:05Z";
    private static final String PATIENT_MRN = "8DcEwvqa8Q3";
    private static final String MYELOMA_RESOLVED_TIME = "2019-06-08T14:22:01Z";
    private static final String MYELOMA_ONSET_DATE = "2019-03-05";

    @BeforeEach
    private void setUp() throws IOException {
        hooverUpdateMessage = messageFactory.getPatientProblems("updated_only.yaml").get(0);
        hooverAddThenDeleteMessages = messageFactory.getPatientProblems("add_then_delete.yaml");
        hooverDeleteMessages =  messageFactory.getPatientProblems("deleted_only.yaml");
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

        PatientCondition condition = patientConditionRepository.findByMrnIdMrn(PATIENT_MRN).get();
        assertEquals("the current problem", condition.getComment());
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

        PatientCondition condition = patientConditionRepository.findByMrnIdMrn(PATIENT_MRN).get();
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

        PatientCondition condition = patientConditionRepository.findByMrnIdMrn(PATIENT_MRN).get();
        assertNull(condition.getResolutionDateTime());

        assertEquals(0, getAllEntities(patientConditionAuditRepository).size());

        hl7MyelomaInpatient.setUpdatedDateTime(hl7MyelomaInpatient.getUpdatedDateTime().plus(1, ChronoUnit.SECONDS));
        hl7MyelomaInpatient.setResolvedTime(InterchangeValue.buildFromHl7(Instant.now()));
        processSingleMessage(hl7MyelomaInpatient);

        PatientCondition resolvedCondition = patientConditionRepository.findByMrnIdMrn(PATIENT_MRN).get();
        assertNotNull(resolvedCondition.getResolutionDateTime());
        assertEquals(1, getAllEntities(patientConditionAuditRepository).size());
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
        assertEquals("ACTIVE", hl7MyelomaInpatient.getStatus());
        processSingleMessage(hl7MyelomaInpatient);

        assertEquals(getAllEntities(patientConditionRepository).size(), 0);
        assertEquals(1, getAllEntities(patientConditionAuditRepository).size());
    }

    /**
     * Given that a problem list exist for a patient
     * When a problem list message for deleting an existing problem arrives with an active status
     * Then this problem list is deleted for the patient and an audit taken
     */
    @Test
    void testDeletingAProblemClarity() throws EmapOperationMessageProcessingException{

        for (PatientProblem message : hooverAddThenDeleteMessages){
            processSingleMessage(message);
        }

        assertEquals(0, getAllEntities(patientConditionRepository).size());

        // should have an audit log of the condition that was deleted
        PatientConditionAudit audit = getAllEntities(patientConditionAuditRepository).get(0);
        assertEquals(hooverAddThenDeleteMessages.get(0).getAddedTime(), audit.getAddedDateTime());
    }

    /**
     * Given that a problem list exists for a patient
     * When a new problem list arrives with the same code but a different name
     * Then a new problem is not added and only the problem name is updated
     */
    @Test
    void testProblemNameUpdate() throws EmapOperationMessageProcessingException, IOException {

        processSingleMessage(hl7MyelomaInpatient);

        PatientProblem msg = messageFactory.getPatientProblems("hl7/minimal_myeloma_inpatient.yaml").get(1);
        assertTrue(msg.getUpdatedDateTime().isAfter(hl7MyelomaInpatient.getUpdatedDateTime()));
        processSingleMessage(msg);

        List<PatientCondition> entities = getAllEntities(patientConditionRepository);
        assertEquals(1, entities.size());

        PatientCondition condition = entities.get(0);
        assertEquals(msg.getConditionName().get(), condition.getConditionTypeId().getName());
        assertEquals(msg.getUpdatedDateTime(), condition.getValidFrom());
    }

    /**
     * Given that a problem list exists for a patient
     * When a new problem list arrives with the same code but a different name
     * Then a new problem is not added and only the problem name is updated
     */
    @Test
    void testProblemNameNoUpdateOlderMessage() throws EmapOperationMessageProcessingException, IOException {

        processSingleMessage(hl7MyelomaInpatient);

        PatientProblem msg = messageFactory.getPatientProblems("hl7/minimal_myeloma_inpatient.yaml").get(1);
        msg.setUpdatedDateTime(hl7MyelomaInpatient.getUpdatedDateTime().minus(1, ChronoUnit.MINUTES));
        processSingleMessage(msg);

        PatientCondition condition = patientConditionRepository.findByMrnIdMrn(PATIENT_MRN).get();
        assertEquals(hl7MyelomaInpatient.getConditionName().get(), condition.getConditionTypeId().getName());
    }

    /**
     * Given that a problem list does not exist for a patient
     * When one arrives from clarity
     * Then the patient does have an associated problem list, with the correct fields
     */
    @Test
    void testClarityProblemAddition() throws EmapOperationMessageProcessingException{

        processSingleMessage(hooverUpdateMessage);

        PatientCondition condition = patientConditionRepository.findByMrnIdMrn(PATIENT_MRN).get();

        assertEquals(PATIENT_MRN, condition.getMrnId().getMrn());
        assertEquals(BACKACHE_PROBLEM_CODE, condition.getConditionTypeId().getInternalCode());
        assertEquals(BACKACHE_PROBLEM_NAME, condition.getConditionTypeId().getName());
        assertEquals(1, condition.getInternalId());
        assertEquals(Instant.parse(MYELOMA_ADDED_TIME), condition.getAddedDateTime());
        assertEquals(Instant.parse(MYELOMA_RESOLVED_TIME), condition.getResolutionDateTime());
        assertEquals(LocalDate.parse(MYELOMA_ONSET_DATE), condition.getOnsetDate());
    }

    /**
     * Given that a problem list does not exist for a patient
     * When one arrives with a delete action
     * Then an exception should not be thrown and no problem list should be added
     */
    @Test
    void testClarityProblemDeletion() throws EmapOperationMessageProcessingException {

        processSingleMessage(hooverDeleteMessages.get(0));
        assertEquals(0, getAllEntities(patientConditionRepository).size());
    }

    PatientCondition conditionMyelomaInpatientWithSatus(String status) throws EmapOperationMessageProcessingException {

        hl7MyelomaInpatient.setAction(status);
        processSingleMessage(hl7MyelomaInpatient);

        PatientCondition condition = getAllEntities(patientConditionRepository).get(0);
        patientConditionRepository.deleteAll();
        conditionTypeRepository.deleteAll();
        return condition;
    }

    /**
     * Given that no problem lists exist
     * When one arrives that has either an AD or UP action associated
     * Then the same state is reached
     */
    @Test
    void testSameStateObtainedForUpAndAdActions() throws EmapOperationMessageProcessingException {

        PatientCondition conditionFromUp = conditionMyelomaInpatientWithSatus("UP");
        PatientCondition conditionFromAd = conditionMyelomaInpatientWithSatus("AD");

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
        hl7MyelomaInpatient.setStatus("ACTIVE");
        processSingleMessage(hl7MyelomaInpatient);

        assertEquals(0, getAllEntities(patientConditionRepository).size());

        hl7MyelomaInpatient.setStatus("Deleted");
        processSingleMessage(hl7MyelomaInpatient);

        assertTrue(patientConditionRepository.findByMrnIdMrn(PATIENT_MRN).isPresent());

        patientConditionRepository.deleteAll();

        hl7MyelomaInpatient.setStatus("Resolved");
        processSingleMessage(hl7MyelomaInpatient);
        assertTrue(patientConditionRepository.findByMrnIdMrn(PATIENT_MRN).isPresent());
    }
}
