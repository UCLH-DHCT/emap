package uk.ac.ucl.rits.inform.datasinks.emapstar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.ConditionTypeRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.HospitalVisitRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.PatientConditionAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.PatientConditionRepository;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.PatientProblem;

import java.io.IOException;
import java.util.List;

/**
 * Test cases to ensure that processing of patient infection messages is working correctly.
 * @author Anika Cawthorn
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
        System.out.println(hl7MyelomaInpatient);
    }

    /**
     * Given that no problem list exists for inpatient
     * When a minimal problem list message arrives
     * Then a new problem list is generated for this patient and it is linked to a hospital stay
     */

    /**
     * Given that a problem list exists for a patient
     * When a new minimal problem list message arrives that concerns the same patient
     * Then the message is added as a condition for the patient
     */

    /**
     * Given that a problem list exists for patient
     * When a minimal problem list message arrives that's older but concerning the same patient
     * Then nothing is changed
     */

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
}
