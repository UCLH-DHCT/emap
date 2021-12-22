package uk.ac.ucl.rits.inform.datasinks.emapstar;

import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.ConditionTypeRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.HospitalVisitRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.PatientConditionAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.PatientConditionRepository;

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

//    private PatientInfection hl7Mumps;
//    private PatientInfection hooverMumps;
//    private static String MUMPS_MRN = "8DcEwvqa8Q3";
//    private static Instant MUMPS_ADD_TIME = Instant.parse("2019-06-02T10:31:05Z");
//    private static String MUMPS_INFECTION = "Mumps";
//    private static String PATIENT_INFECTION = "PATIENT_INFECTION";
//    private static Instant HL7_UPDATE_TIME = Instant.parse("2019-06-07T11:32:00Z");


    /**
     * Given that no problem list exists for outpatient
     * When a problem list message arrives
     * Then a new problem list is generated for this patient (not linked to a hospital stay)
     */

    /**
     * Given that no problem list exists for inpatient
     * When a problem list message arrives
     * Then a new problem list is generated for this patient and it is linked to a hospital stay
     * (actually the hospital stay as such might be the problem list as there is no problem typically attached to it)
     */

    /**
     * Given that a problem list exist for patient
     * When a problem list message arrives that's older but concerning the same patient
     * Then nothing is changed
     */

    /**
     * Given that a problem list exist for patient
     * When a problem list message arrive that's newer, concerns the same patient and contains updated fields
     * Then the existing problem list is updated accordingly
     * (We might not do this if we only expect updates ?)
     */
}
