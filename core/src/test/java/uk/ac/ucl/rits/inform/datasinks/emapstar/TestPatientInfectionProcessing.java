package uk.ac.ucl.rits.inform.datasinks.emapstar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.HospitalVisitRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.PatientStateRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.PatientStateTypeRepository;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.informdb.state.PatientState;
import uk.ac.ucl.rits.inform.informdb.state.PatientStateType;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.PatientInfection;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TestPatientInfectionProcessing extends MessageProcessingBase {
    @Autowired
    HospitalVisitRepository hospitalVisitRepository;
    @Autowired
    PatientStateRepository patientStateRepository;
    @Autowired
    PatientStateTypeRepository patientStateTypeRepository;

    private List<PatientInfection> hooverMessages;
    private PatientInfection hl7Mumps;
    private PatientInfection hooverMumps;
    private static String MUMPS_MRN = "8DcEwvqa8Q3";
    private static Instant MUMPS_ADD_TIME = Instant.parse("2019-03-07T11:31:05Z");
    private static String MUMPS_INFECTION = "Mumps";
    private static String PATIENT_INFECTION = "PATIENT_INFECTION";
    private static Instant HL7_UPDATE_TIME = Instant.parse("2019-03-07T11:32:00Z");

    @BeforeEach
    private void setUp() throws IOException {
        hooverMessages = messageFactory.getPatientInfections("2019-04.yaml");
        hl7Mumps = messageFactory.getPatientInfections("hl7/minimal_mumps.yaml").get(0);
        hooverMumps = hooverMessages.get(0);
    }

    /**
     * Processing an unknown MRN should create a minimal MRN.
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    void testCreatesMrns() throws EmapOperationMessageProcessingException {
        for (PatientInfection msg : hooverMessages) {
            processSingleMessage(msg);
        }

        List<Mrn> mrns = getAllMrns();

        assertEquals(4, mrns.size());
        assertEquals("hoover", mrns.get(0).getSourceSystem());
    }

    /**
     * Ensure that the type of state is created.
     * Fairly minimal information can be added from a patient infection.
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    void testPatientInfectionTypeCreated() throws EmapOperationMessageProcessingException {
        processSingleMessage(hl7Mumps);

        PatientStateType type = patientStateTypeRepository.findByDataTypeAndName(PATIENT_INFECTION, MUMPS_INFECTION).orElseThrow();

        assertEquals(HL7_UPDATE_TIME, type.getValidFrom());
        assertNotNull(type.getValidFrom());
        assertNull(type.getStandardisedCode());
        assertNull(type.getStandardisedVocabulary());
    }

    /**
     * Patient infection with only added time should create an entity that has no resolution time.
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    void testMinimalPatientInfection() throws EmapOperationMessageProcessingException {
        processSingleMessage(hl7Mumps);

        PatientState infection = patientStateRepository
                .findByMrnIdMrnAndPatientStateTypeIdNameAndAddedDateTime(MUMPS_MRN, MUMPS_INFECTION, MUMPS_ADD_TIME)
                .orElseThrow();

        assertEquals(HL7_UPDATE_TIME, infection.getValidFrom());
        assertNotNull(infection.getValidFrom());
        assertNull(infection.getResolutionDateTime());
    }

    /**
     * Patient infection resolution time should be saved.
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    void testPatientInfectionWithResolveTime () throws EmapOperationMessageProcessingException {
        Instant resolveTime = MUMPS_ADD_TIME.plus(21, ChronoUnit.DAYS);
        hl7Mumps.setInfectionResolved(InterchangeValue.buildFromHl7(resolveTime));
        processSingleMessage(hl7Mumps);

        PatientState infection = patientStateRepository
                .findByMrnIdMrnAndPatientStateTypeIdNameAndAddedDateTime(MUMPS_MRN, MUMPS_INFECTION, MUMPS_ADD_TIME)
                .orElseThrow();
        assertEquals(resolveTime, infection.getResolutionDateTime());
    }


}
