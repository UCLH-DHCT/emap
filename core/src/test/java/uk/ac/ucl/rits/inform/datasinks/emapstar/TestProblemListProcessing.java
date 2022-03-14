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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


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
    private PatientProblem hl7MyelomaInpatient;
    private PatientProblem hl7MyelomaOutpatient;

    @BeforeEach
    private void setUp() throws IOException {
        hooverMessages = messageFactory.getPatientProblems("updated_only.yaml");
        hl7MyelomaInpatient = messageFactory.getPatientProblems("hl7/minimal_myeloma_inpatient.yaml").get(0);
        hl7MyelomaOutpatient = messageFactory.getPatientProblems("hl7/minimal_myeloma_outpatient.yaml").get(0);
    }


    /**
     * Given that no problem list exists for outpatient
     * When a minimal problem list message arrives
     * Then a new problem list is generated for this patient (not linked to a hospital stay)
     */
    @Test
    void testCreateProblemListOutpatient() throws EmapOperationMessageProcessingException {

        processSingleMessage(hl7MyelomaOutpatient);
        List<PatientCondition> entities = getAllEntities(patientConditionRepository);

        assertEquals(1, entities.size());

        PatientCondition entity = entities.get(0);
        assertNull(entity.getHospitalVisitId());
    }

    /**
     * Given that no problem list exists for inpatient
     * When a minimal problem list message arrives
     * Then a new problem list is generated for this patient and it is linked to a hospital stay
     */
    @Test
    void testCreateProblemListInpatient() throws EmapOperationMessageProcessingException {

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
    void testProcessingOlderMessage() throws EmapOperationMessageProcessingException, IOException {

        PatientProblem currentMessage = messageFactory.getPatientProblems("hl7/minimal_myeloma_inpatient.yaml").get(0);
        currentMessage.setComment(InterchangeValue.buildFromHl7("the current message"));

        processSingleMessage(currentMessage);

        PatientProblem message = sampleMessage();
        message.setComment(InterchangeValue.buildFromHl7("an older problem"));
        message.setUpdatedDateTime(Instant.now().minus(1, ChronoUnit.SECONDS));

        List<PatientCondition> entities = getAllEntities(patientConditionRepository);
        assertEquals(1, entities.size());

        assertEquals("the current message", entities.get(0).getComment());
    }

    /**
     * Given that no problem list for a patient exists
     * When a problem list message list with notes arrives
     * Then the problem list is added with comments
     */

    /**
     * Given that a problem list exist for patient
     * When a problem list message arrive that's newer, concerns the same patient and contains updated fields
     * Then the existing problem list is updated accordingly
     * (e.g. notes are added in for a specific problem)
     */

    /**
     * Given that a problem list exist for a patient
     * When a problem list message for deleting an existing problem arrives
     * Then this problem list is deleted for the patient
     * (e.g diagnosis is entered as "working hypothesis" and then deleted as tests come back)
     */

    // Create a sample messaged with default fields
    PatientProblem sampleMessage(){

        PatientProblem message = new PatientProblem();
        message.setProblemAdded(Instant.now());
        message.setEpicConditionId(InterchangeValue.buildFromHl7(1L));
        message.setSourceSystem(hl7MyelomaInpatient.getSourceSystem());
        message.setMrn("0");
        message.setConditionCode("XX");
        message.setConditionName(InterchangeValue.buildFromHl7("YY"));

        message.setUpdatedDateTime(Instant.now());
        message.setProblemResolved(InterchangeValue.buildFromHl7(Instant.now()));
        message.setProblemOnset(InterchangeValue.buildFromHl7(LocalDate.now()));

        return message;
    }
}
