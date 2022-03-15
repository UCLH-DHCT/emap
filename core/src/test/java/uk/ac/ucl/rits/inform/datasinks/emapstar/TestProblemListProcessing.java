package uk.ac.ucl.rits.inform.datasinks.emapstar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.ConditionTypeRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.HospitalVisitRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.PatientConditionAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.PatientConditionRepository;
import uk.ac.ucl.rits.inform.informdb.conditions.PatientCondition;
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
 * Test cases to ensure that processing of patient problem messages is working correctly.
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

    private List<PatientProblem> hooverMessages;
    private List<PatientProblem> hooverDelteMessages;

    private PatientProblem hl7MyelomaInpatient;
    private PatientProblem hl7MyelomaOutpatient;

    private static final String SAMPLE_COMMENT = "a comment";
    private static final String UPDATED_PROBLEM_NAME = "new problem";
    private static final String MYELOMA_PROBLEM_NAME = "Multiple Myeloma";
    private static final String MYELOMA_PROBLEM_CODE = "C90.0";
    private static final String MYELOMA_PATIENT_MRN = "8DcEwvqa8Q3";


    @BeforeEach
    private void setUp() throws IOException {
        hooverMessages = messageFactory.getPatientProblems("updated_only.yaml");
        hooverDelteMessages =  messageFactory.getPatientProblems("deleted_only.yaml");
        hl7MyelomaInpatient = messageFactory.getPatientProblems("hl7/minimal_myeloma_inpatient.yaml").get(0);
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
        assertEquals(MYELOMA_PATIENT_MRN, entity.getMrnId().getMrn());
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

        assertEquals(MYELOMA_PATIENT_MRN, entity.getMrnId().getMrn());
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

        PatientProblem message = sampleMessage();
        message.setMrn(hl7MyelomaInpatient.getMrn());
        processSingleMessage(message);

        List<PatientCondition> entities = getAllEntities(patientConditionRepository);
        assertEquals(2, entities.size());

        assertEquals(entities.get(0).getMrnId(), entities.get(1).getMrnId());
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
    @Test
    void testProcessingWithYamlComment() throws EmapOperationMessageProcessingException {

        processSingleMessage(hooverMessages.get(1));

        PatientCondition condition = getAllEntities(patientConditionRepository).get(0);
        assertEquals("Investigation ongoing.", condition.getComment());
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
        processSingleMessage(hl7MyelomaInpatient);

        assertEquals(getAllEntities(patientConditionRepository).size(), 0);
    }
    @Test
    void testDeletingAProblemClarity() throws EmapOperationMessageProcessingException{

        Instant newestUpdatedTime = hooverMessages.get(0).getUpdatedDateTime();

        // Adding then deleting a problem for the same patient should leave the condition repository empty
        PatientProblem addMessage = hooverMessages.get(0);
        processSingleMessage(addMessage);

        PatientProblem deleteMessage = hooverDelteMessages.get(0);
        deleteMessage.setUpdatedDateTime(newestUpdatedTime.plus(1, ChronoUnit.SECONDS));

        assertEquals(addMessage.getMrn(), deleteMessage.getMrn());
        assertEquals(addMessage.getAddedTime(), deleteMessage.getAddedTime());

        processSingleMessage(deleteMessage);

        assertEquals(0, getAllEntities(patientConditionRepository).size());
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

        processSingleMessage(hooverMessages.get(0));

        List<PatientCondition> entities = getAllEntities(patientConditionRepository);
        assertEquals(1, entities.size());

        PatientCondition entity = entities.get(0);
        assertEquals(MYELOMA_PATIENT_MRN, entity.getMrnId().getMrn());
        assertEquals(MYELOMA_PROBLEM_CODE, entity.getConditionTypeId().getInternalCode());
        assertEquals(MYELOMA_PROBLEM_NAME, entity.getConditionTypeId().getName());
        assertEquals(1, entity.getInternalId());
        assertEquals(Instant.parse("2019-06-02T10:31:05Z"), entity.getAddedDateTime());
        assertEquals(Instant.parse("2019-06-08T14:22:01Z"), entity.getResolutionDateTime());
        assertEquals(LocalDate.parse("2019-03-05"), entity.getOnsetDate());
    }


    /**
     * Given that a patient list does not exist for a patient
     * When one arrives with a delete action
     * Then nothing should be thrown
     */
    @Test
    void testClarityProblemDeletion() throws EmapOperationMessageProcessingException {

        PatientProblem message = hooverDelteMessages.get(0);

        processSingleMessage(message);

        assertDoesNotThrow(() -> processSingleMessage(message));
    }

    /**
     * Given that no problem lists exist
     * When two update messages are received that correspond to different patients with the same condition
     * Then two problem conditions are present
     */
    @Test
    void testMultipleClarityProblemAdd() throws EmapOperationMessageProcessingException{

        assertEquals(2, hooverMessages.size());
        assertNotEquals(hooverMessages.get(0).getMrn(), hooverMessages.get(1).getMrn());

        for (PatientProblem message : hooverMessages){
            processSingleMessage(message);
        }

        List<PatientCondition> problems = getAllEntities(patientConditionRepository);
        assertEquals(2, problems.size());
    }

    /**
     * Given that no problem lists exist
     * When two are added and two deleted, with the second two corresponding to the same patient
     * Then only a single patient condition remains
     */
    @Test
    void testMultipleClarityProblemDelete() throws EmapOperationMessageProcessingException{

        assertEquals(2, hooverMessages.size());
        assertNotEquals(hooverMessages.get(0).getMrn(), hooverMessages.get(1).getMrn());
        assertEquals(hooverDelteMessages.get(0).getMrn(), hooverDelteMessages.get(1).getMrn());

        for (PatientProblem message : hooverMessages){
            processSingleMessage(message);
        }
        for (PatientProblem message : hooverDelteMessages){
            processSingleMessage(message);
        }

        assertEquals(1, getAllEntities(patientConditionRepository).size());
    }


    // Create a sample messaged with default fields
    PatientProblem sampleMessage(){

        PatientProblem message = new PatientProblem();
        message.setProblemAdded(Instant.now());
        message.setEpicProblemId(InterchangeValue.buildFromHl7(1L));
        message.setSourceSystem(hl7MyelomaInpatient.getSourceSystem());
        message.setMrn("0");
        message.setProblemCode("XX");
        message.setProblemName(InterchangeValue.buildFromHl7("YY"));

        message.setUpdatedDateTime(Instant.now());
        message.setProblemResolved(InterchangeValue.buildFromHl7(Instant.now()));
        message.setProblemOnset(InterchangeValue.buildFromHl7(LocalDate.now()));

        return message;
    }
}
