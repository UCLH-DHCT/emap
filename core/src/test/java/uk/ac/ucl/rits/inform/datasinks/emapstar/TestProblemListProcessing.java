package uk.ac.ucl.rits.inform.datasinks.emapstar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.ConditionTypeRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.HospitalVisitRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.PatientConditionAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.PatientConditionRepository;
import uk.ac.ucl.rits.inform.informdb.conditions.PatientCondition;
import uk.ac.ucl.rits.inform.informdb.conditions.PatientConditionAudit;
import uk.ac.ucl.rits.inform.interchange.PatientProblem;
import uk.ac.ucl.rits.inform.interchange.ConditionStatus;
import uk.ac.ucl.rits.inform.interchange.ConditionAction;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;


import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;


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
    private List<PatientProblem> hooverMyelomaDeleteThenAdd;

    private PatientProblem hl7MyelomaInpatient;
    private PatientProblem hl7OtherProblemInpatient;
    private PatientProblem hl7MyelomaOutpatient;
    private PatientProblem hl7MyelomaAdd;
    private PatientProblem hooverMyelomaAdd;


    private static final String MYELOMA_PROBLEM_NAME = "Multiple Myeloma";
    private static final String MYELOMA_PROBLEM_CODE = "C90.0";
    private static final String MYELOMA_ADDED_TIME = "2019-06-02T10:31:05Z";
    private static final String PATIENT_MRN = "8DcEwvqa8Q3";
    private static final String MYELOMA_ONSET_DATE = "2019-03-05";

    @BeforeEach
    private void setUp() throws IOException {
        hooverAddThenDeleteMessages = messageFactory.getPatientProblems("add_then_delete.yaml");
        hooverDeleteMessages =  messageFactory.getPatientProblems("deleted_only.yaml");
        hl7MyelomaInpatient = messageFactory.getPatientProblems("hl7/minimal_myeloma_inpatient.yaml").get(0);
        hl7OtherProblemInpatient =  messageFactory.getPatientProblems("hl7/minimal_other_problem_inpatient.yaml").get(0);
        hl7MyelomaOutpatient = messageFactory.getPatientProblems("hl7/minimal_myeloma_outpatient.yaml").get(0);
        hl7MyelomaAdd = messageFactory.getPatientProblems("hl7/myeloma_add.yaml").get(0);
        hooverMyelomaAdd = messageFactory.getPatientProblems("clarity_add.yaml").get(0);
        hooverMyelomaDeleteThenAdd = messageFactory.getPatientProblems("delete_then_add.yaml");
    }

    /**
     * Given that no problem list exists for outpatient
     * When a minimal problem list message arrives
     * Then a new problem list is generated for this patient (not linked to a hospital stay)
     * with the correct fields
     */
    @Test
    void testCreateProblemOutpatient() throws EmapOperationMessageProcessingException {

        processSingleMessage(hl7MyelomaOutpatient);
        PatientCondition condition = patientConditionRepository.findByMrnIdMrn(PATIENT_MRN).orElseThrow();

        assertNull(condition.getHospitalVisitId());
    }

    /**
     * Given that a problem list does not exist for a patient
     * When one arrives from either EPIC (hl7) or clarity (hoover)
     * Then the patient does have an associated problem list, with the correct fields
     */
    @ParameterizedTest
    @ValueSource(strings = {"EPIC", "clarity"})
    void testProblemAddition(String input) throws EmapOperationMessageProcessingException{

        for (PatientProblem msg : new PatientProblem[]{hl7MyelomaAdd, hooverMyelomaAdd}){
            if (msg.getSourceSystem().equals(input)){
                processSingleMessage(msg);
            }
        }

        PatientCondition condition = patientConditionRepository.findByMrnIdMrn(PATIENT_MRN).orElseThrow();

        assertEquals(PATIENT_MRN, condition.getMrnId().getMrn());
        assertEquals(MYELOMA_PROBLEM_CODE, condition.getConditionTypeId().getInternalCode());
        assertEquals(MYELOMA_PROBLEM_NAME, condition.getConditionTypeId().getName());
        assertEquals(Instant.parse(MYELOMA_ADDED_TIME), condition.getAddedDateTime());
        assertEquals(LocalDate.parse(MYELOMA_ONSET_DATE), condition.getOnsetDate());
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
    void testAddProblemListSamePatient() throws EmapOperationMessageProcessingException {

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

        hl7MyelomaInpatient.setConditionName(InterchangeValue.buildFromHl7("the current problem"));
        processSingleMessage(hl7MyelomaInpatient);

        Instant originalTime = hl7MyelomaInpatient.getUpdatedDateTime();
        Instant olderTime = originalTime.minus(1, ChronoUnit.SECONDS);
        hl7MyelomaInpatient.setUpdatedDateTime(olderTime);
        hl7MyelomaInpatient.setConditionName(InterchangeValue.buildFromHl7("an older problem"));

        processSingleMessage(hl7MyelomaInpatient);

        PatientCondition condition = patientConditionRepository.findByMrnIdMrn(PATIENT_MRN).orElseThrow();
        assertEquals(originalTime, condition.getValidFrom());
        assertEquals("the current problem", condition.getConditionTypeId().getName());
    }

    /**
     * Given that a problem list exist for patient
     * When a problem list message arrive that's newer, concerns the same patient and has a resolved time
     * Then the existing problem list is updated accordingly with a resolution time
     */
    @Test
    void testProcessingNewerMessage() throws EmapOperationMessageProcessingException {

        processSingleMessage(hl7MyelomaInpatient);

        PatientCondition condition = patientConditionRepository.findByMrnIdMrn(PATIENT_MRN).orElseThrow();
        assertNull(condition.getResolutionDateTime());

        assertEquals(0, getAllEntities(patientConditionAuditRepository).size());

        hl7MyelomaInpatient.setUpdatedDateTime(hl7MyelomaInpatient.getUpdatedDateTime().plus(1, ChronoUnit.SECONDS));
        hl7MyelomaInpatient.setResolvedTime(InterchangeValue.buildFromHl7(Instant.now()));
        processSingleMessage(hl7MyelomaInpatient);

        PatientCondition resolvedCondition = patientConditionRepository.findByMrnIdMrn(PATIENT_MRN).orElseThrow();
        assertNotNull(resolvedCondition.getResolutionDateTime());
        assertEquals(1, getAllEntities(patientConditionAuditRepository).size());
    }

    boolean patientConditionIsDeleted(){
        return patientConditionRepository.findByMrnIdMrn(PATIENT_MRN).orElseThrow().getIsDeleted();
    }

    /**
     * Given that a problem list exist for a patient
     * When a problem list message for deleting an existing problem arrives with an active status
     * Then this problem list is deleted for the patient and an audit taken
     */
    @Test
    void testDeletingAProblem() throws EmapOperationMessageProcessingException{

        for (PatientProblem message : hooverAddThenDeleteMessages){
            processSingleMessage(message);
        }

        assertTrue(patientConditionIsDeleted());

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

        PatientCondition condition = patientConditionRepository.findByMrnIdMrn(PATIENT_MRN).orElseThrow();
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

        PatientCondition condition = patientConditionRepository.findByMrnIdMrn(PATIENT_MRN).orElseThrow();
        assertEquals(hl7MyelomaInpatient.getConditionName().get(), condition.getConditionTypeId().getName());
    }

    /**
     * Given that a problem list does not exist for a patient
     * When one arrives with a delete action
     * Then an exception should not be thrown and no problem list should be added
     */
    @Test
    void testClarityProblemDeletion() throws EmapOperationMessageProcessingException {

        processSingleMessage(hooverDeleteMessages.get(0));
        assertTrue(patientConditionIsDeleted());
    }

    PatientCondition conditionMyelomaInpatientWithAction(ConditionAction action) throws EmapOperationMessageProcessingException {

        hl7MyelomaInpatient.setAction(action);
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

        PatientCondition conditionFromUp = conditionMyelomaInpatientWithAction(ConditionAction.UP);
        PatientCondition conditionFromAd = conditionMyelomaInpatientWithAction(ConditionAction.AD);

        assertEquals(conditionFromAd.getInternalId(), conditionFromUp.getInternalId());
        assertEquals(conditionFromAd.getAddedDateTime(), conditionFromUp.getAddedDateTime());
        assertEquals(conditionFromAd.getResolutionDateTime(), conditionFromUp.getResolutionDateTime());
        assertEquals(conditionFromAd.getStatus(), conditionFromUp.getStatus());
        assertEquals(conditionFromAd.getComment(), conditionFromUp.getComment());
        assertEquals(conditionFromAd.getPriority(), conditionFromUp.getPriority());
    }

    /**
     * Given that no problem lists exist
     * When one arrives that has a delete action but a 'Deleted' or 'Resolved' status
     * Then a condition should not be deleted
     */
    @ParameterizedTest
    @ValueSource(strings = {"DELETED", "RESOLVED"})
    void testDeleteActionWithDeleteOrResolvedStatus(String input) throws EmapOperationMessageProcessingException {

        hl7MyelomaInpatient.setAction(ConditionAction.DE);
        hl7MyelomaInpatient.setStatus(ConditionStatus.valueOf(input));

        processSingleMessage(hl7MyelomaInpatient);

        assertFalse(patientConditionIsDeleted());
    }

    /**
     * Given that no problem lists exist for a patient
     * When two arrive out of order such that a delete message comes before an add message
     * Then no active conditions exist for the patient
     */
    @Test
    void testDeleteThenAdd() throws EmapOperationMessageProcessingException {

        for (PatientProblem msg : hooverMyelomaDeleteThenAdd){
            processSingleMessage(msg);
        }

        PatientCondition condition = patientConditionRepository.findByMrnIdMrn(PATIENT_MRN).orElseThrow();
        assertTrue(condition.getIsDeleted());
    }

}
