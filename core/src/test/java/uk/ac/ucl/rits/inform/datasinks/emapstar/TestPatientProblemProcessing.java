package uk.ac.ucl.rits.inform.datasinks.emapstar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.*;
import uk.ac.ucl.rits.inform.informdb.conditions.PatientCondition;
import uk.ac.ucl.rits.inform.informdb.conditions.PatientConditionAudit;
import uk.ac.ucl.rits.inform.interchange.PatientProblem;
import uk.ac.ucl.rits.inform.interchange.ConditionAction;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;


import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
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
public class TestPatientProblemProcessing extends MessageProcessingBase {
    @Autowired
    PatientConditionRepository patientConditionRepository;
    @Autowired
    PatientConditionAuditRepository patientConditionAuditRepository;
    @Autowired
    ConditionTypeRepository conditionTypeRepository;
    @Autowired
    HospitalVisitRepository hospitalVisitRepository;
    @Autowired
    ConditionVisitLinkRepository conditionVisitLinkRepository;

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
    private static final String MYELOMA_ADDED_TIME = "2019-06-01";
    private static final String PATIENT_MRN = "8DcEwvqa8Q3";
    private static final String MYELOMA_ONSET_DATE = "2019-05-31";
    private static final String VISIT_NUMBER = "123412341234";

    @BeforeEach
    private void setUp() throws IOException {
        hooverAddThenDeleteMessages = messageFactory.getPatientProblems("clarity/add_then_delete.yaml");
        hooverDeleteMessages =  messageFactory.getPatientProblems("clarity/deleted_only.yaml");
        hl7MyelomaInpatient = messageFactory.getPatientProblems("hl7/minimal_myeloma_inpatient.yaml").get(0);
        hl7OtherProblemInpatient =  messageFactory.getPatientProblems("hl7/minimal_other_problem_inpatient.yaml").get(0);
        hl7MyelomaOutpatient = messageFactory.getPatientProblems("hl7/minimal_myeloma_outpatient.yaml").get(0);
        hl7MyelomaAdd = messageFactory.getPatientProblems("hl7/myeloma_add.yaml").get(0);
        hooverMyelomaAdd = messageFactory.getPatientProblems("clarity_add.yaml").get(0);
        hooverMyelomaDeleteThenAdd = messageFactory.getPatientProblems("clarity/delete_then_add.yaml");
    }

    /**
     * Given that no problem list exists for outpatient
     * When a minimal problem list message arrives
     * Then a new problem list is generated for this patient (which has an encounter)
     * with the correct fields
     */
    @Test
    void testCreateProblemOutpatient() throws EmapOperationMessageProcessingException {

        processSingleMessage(hl7MyelomaOutpatient);
        PatientCondition condition = patientConditionRepository.findByMrnIdMrn(PATIENT_MRN).orElseThrow();

        assertNotNull(condition.getHospitalVisitId());
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
        assertEquals(LocalDate.parse(MYELOMA_ADDED_TIME), condition.getAddedDate());
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
        assertNull(condition.getResolutionDatetime());

        assertEquals(0, getAllEntities(patientConditionAuditRepository).size());

        hl7MyelomaInpatient.setUpdatedDateTime(hl7MyelomaInpatient.getUpdatedDateTime().plus(1, ChronoUnit.SECONDS));
        hl7MyelomaInpatient.setResolvedDate(LocalDate.now());
        processSingleMessage(hl7MyelomaInpatient);

        PatientCondition resolvedCondition = patientConditionRepository.findByMrnIdMrn(PATIENT_MRN).orElseThrow();
        assertNotNull(resolvedCondition.getResolutionDate());
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
        assertEquals(hooverAddThenDeleteMessages.get(0).getAddedDate(), audit.getAddedDate());
    }

    PatientProblem messageWithNewNameAndUpdatedTimeChanged(Instant time) throws IOException {

        PatientProblem msg =  messageFactory.getPatientProblems("hl7/minimal_myeloma_inpatient.yaml").get(0);
        msg.setConditionName(InterchangeValue.buildFromHl7("Myeloma"));
        msg.setUpdatedDateTime(time);

        return msg;
    }

    /**
     * Given that a problem list exists for a patient
     * When a new problem list arrives with the same code but a different name
     * Then a new problem is not added and only the problem name is updated
     */
    @Test
    void testProblemNameUpdate() throws EmapOperationMessageProcessingException, IOException {

        processSingleMessage(hl7MyelomaInpatient);

        Instant newTime = hl7MyelomaInpatient.getUpdatedDateTime().plus(1, ChronoUnit.MINUTES);
        PatientProblem msg = messageWithNewNameAndUpdatedTimeChanged(newTime);

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

        Instant newTime = hl7MyelomaInpatient.getUpdatedDateTime().minus(1, ChronoUnit.MINUTES);
        PatientProblem msg = messageWithNewNameAndUpdatedTimeChanged(newTime);

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

        // The action is only used for processing so set the comment so this condition can be found from the repo
        String comment = action.toString();
        hl7MyelomaInpatient.setComment(InterchangeValue.buildFromHl7(comment));

        processSingleMessage(hl7MyelomaInpatient);

        return patientConditionRepository.findByMrnIdMrnAndComment(PATIENT_MRN, comment).orElseThrow();
    }

    /**
     * Given that no problem lists exist
     * When one arrives that has either an AD or UP action associated
     * Then the same state is reached
     */
    @Test
    void testSameStateObtainedForUpAndAdActions() throws EmapOperationMessageProcessingException {

        PatientCondition conditionFromUp = conditionMyelomaInpatientWithAction(ConditionAction.UPDATE);
        PatientCondition conditionFromAd = conditionMyelomaInpatientWithAction(ConditionAction.ADD);

        assertEquals(conditionFromAd.getInternalId(), conditionFromUp.getInternalId());
        assertEquals(conditionFromAd.getAddedDatetime(), conditionFromUp.getAddedDatetime());
        assertEquals(conditionFromAd.getResolutionDatetime(), conditionFromUp.getResolutionDatetime());
        assertEquals(conditionFromAd.getStatus(), conditionFromUp.getStatus());
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

        hl7MyelomaInpatient.setAction(ConditionAction.DELETE);
        hl7MyelomaInpatient.setStatus(InterchangeValue.buildFromHl7(input));

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

    /**
     * Given a problem list message
     * When it is processed
     * Then a single entry in the patient condition <-> hospital visit linker table should exist
     * @throws EmapOperationMessageProcessingException should not happen
     */
    @Test
    void testLinkerTablePopulateWithSingleMessage() throws EmapOperationMessageProcessingException{

        processSingleMessage(hl7MyelomaOutpatient);
        var condition = patientConditionRepository.findByMrnIdMrn(PATIENT_MRN).orElseThrow();
        var visit = hospitalVisitRepository.findByEncounter(VISIT_NUMBER).orElseThrow();
        var link = conditionVisitLinkRepository.findByPatientConditionIdAndHospitalVisitId(condition, visit);

        assertTrue(link.isPresent());
    }

    /**
     * Given two almost identical problem list messages with different hospital visit numbers (CSNs)
     * When they are processed
     * Then the most recent CSN should be associated with the condition but the linker table contain two entries
     * @throws EmapOperationMessageProcessingException should not happen
     */
    @Test
    void testLinkerTablePopulateWithMultipleVisits() throws EmapOperationMessageProcessingException{

        processSingleMessage(hl7MyelomaOutpatient);

        var oldVisitNumber = hl7MyelomaOutpatient.getVisitNumber().get();
        var newVisitNumber = "987654321";
        assertNotEquals(oldVisitNumber, newVisitNumber);

        hl7MyelomaOutpatient.setVisitNumber(InterchangeValue.buildFromHl7(newVisitNumber));

        processSingleMessage(hl7MyelomaOutpatient);

        var condition = patientConditionRepository.findByMrnIdMrn(PATIENT_MRN).orElseThrow();
        var visit = hospitalVisitRepository.findByEncounter(newVisitNumber).orElseThrow();

        assertEquals(condition.getHospitalVisitId().getEncounter(), visit.getEncounter());

        var links = conditionVisitLinkRepository.findAll();
        assertEquals(2, links.size());
    }

    /**
     * Given a deletion message has been processed for a patient
     * When a new update message arrives concerning the same patient
     * Then the problem is un-deleted
     * @throws EmapOperationMessageProcessingException should not happen
     */
    @Test
    void testProblemUpdateOnDeletedProblemUndeletes() throws EmapOperationMessageProcessingException{

        var message = hl7MyelomaInpatient;
        processSingleMessage(message);

        // Set the status to active and action to delete and process the updated message
        assertEquals(message.getStatus().get(), "ACTIVE");
        message.setAction(ConditionAction.DELETE);
        message.setUpdatedDateTime(message.getUpdatedDateTime().plus(1, ChronoUnit.SECONDS));
        processSingleMessage(message);

        // Then set the status to update and processes the updated message
        message.setAction(ConditionAction.UPDATE);
        message.setUpdatedDateTime(message.getUpdatedDateTime().plus(2, ChronoUnit.SECONDS));
        processSingleMessage(message);

        PatientCondition condition = patientConditionRepository.findByMrnIdMrn(PATIENT_MRN).orElseThrow();
        assertFalse(condition.getIsDeleted());
    }

    /**
     * Given a problem list add message has been processed
     * When a delete message arrives at an identical time
     * Then the add message takes presidince and the delete is discarded
     * @throws EmapOperationMessageProcessingException should not happen
     */
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testDeleteMessageIsDiscardedWhenIdenticalUpdatedTime(boolean forwards) throws EmapOperationMessageProcessingException{

        ConditionAction[] actions = {ConditionAction.ADD, ConditionAction.DELETE};

        if (!forwards){
            Collections.reverse(Arrays.asList(actions));
        }

        var message = hl7MyelomaInpatient;
        message.setAction(actions[0]);
        processSingleMessage(message);

        message.setAction(actions[1]);
        processSingleMessage(message);

        PatientCondition condition = patientConditionRepository.findByMrnIdMrn(PATIENT_MRN).orElseThrow();
        assertFalse(condition.getIsDeleted());
    }
}
