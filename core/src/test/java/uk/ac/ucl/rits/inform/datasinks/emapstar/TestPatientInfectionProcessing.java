package uk.ac.ucl.rits.inform.datasinks.emapstar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Test cases to ensure that processing of patient infection messages is working correctly.
 *
 * @author Stef Piatek
 * @author Anika Cawthorn
 */
public class TestPatientInfectionProcessing extends MessageProcessingBase {
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

    private void assertHooverMumpsTimes(PatientState infection) {
        assertEquals(Instant.parse("2019-03-21T15:22:01Z"), infection.getResolutionDateTime());
        assertEquals(LocalDate.parse("2019-03-05"), infection.getOnsetDate());
    }


    private void processOlderDateTimeMessage() throws EmapOperationMessageProcessingException {
        Instant olderResolvedTime = MUMPS_ADD_TIME.plusSeconds(60);
        LocalDate olderOnsetDate = LocalDate.parse("2019-01-01");

        PatientInfection olderMessage = new PatientInfection();
        olderMessage.setInfectionAdded(hooverMumps.getInfectionAdded());
        olderMessage.setSourceSystem(hooverMumps.getSourceSystem());
        olderMessage.setMrn(MUMPS_MRN);
        olderMessage.setInfection(MUMPS_INFECTION);
        olderMessage.setUpdatedDateTime(HL7_UPDATE_TIME.minus(20, ChronoUnit.DAYS));
        olderMessage.setInfectionResolved(InterchangeValue.buildFromHl7(olderResolvedTime));
        olderMessage.setInfectionOnset(InterchangeValue.buildFromHl7(olderOnsetDate));
        processSingleMessage(olderMessage);
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

        PatientStateType type = patientStateTypeRepository.findByDataTypeAndName(PATIENT_INFECTION,
                MUMPS_INFECTION).orElseThrow();

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
    void testPatientInfectionWithResolveTime() throws EmapOperationMessageProcessingException {
        Instant resolveTime = MUMPS_ADD_TIME.plus(21, ChronoUnit.DAYS);
        hl7Mumps.setInfectionResolved(InterchangeValue.buildFromHl7(resolveTime));
        processSingleMessage(hl7Mumps);

        PatientState infection = patientStateRepository
                .findByMrnIdMrnAndPatientStateTypeIdNameAndAddedDateTime(MUMPS_MRN, MUMPS_INFECTION, MUMPS_ADD_TIME)
                .orElseThrow();
        assertEquals(resolveTime, infection.getResolutionDateTime());
    }

    /**
     * Patient infection containing resolution datetime and onset date should be parsed.
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    void testAllDates() throws EmapOperationMessageProcessingException {
        processSingleMessage(hooverMumps);

        PatientState infection = patientStateRepository
                .findByMrnIdMrnAndPatientStateTypeIdNameAndAddedDateTime(MUMPS_MRN, MUMPS_INFECTION, MUMPS_ADD_TIME)
                .orElseThrow();

        assertHooverMumpsTimes(infection);
        assertNull(infection.getComment());
    }

    /**
     * Patient infection comment and status should be parsed.
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    void testCommentAndStatus() throws EmapOperationMessageProcessingException {
        String comment = "great comment";
        hooverMumps.setComment(InterchangeValue.buildFromHl7(comment));
        processSingleMessage(hooverMumps);

        PatientState infection = patientStateRepository
                .findByMrnIdMrnAndPatientStateTypeIdNameAndAddedDateTime(MUMPS_MRN, MUMPS_INFECTION, MUMPS_ADD_TIME)
                .orElseThrow();

        assertEquals(comment, infection.getComment());
        assertEquals("Active", infection.getStatus());
    }

    /**
     * hl7 message was updated before the hoover message, so should be updated
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    void testHl7UpdatedByLaterMessage() throws EmapOperationMessageProcessingException {
        processSingleMessage(hl7Mumps);
        processSingleMessage(hooverMumps);

        PatientState infection = patientStateRepository
                .findByMrnIdMrnAndPatientStateTypeIdNameAndAddedDateTime(MUMPS_MRN, MUMPS_INFECTION, MUMPS_ADD_TIME)
                .orElseThrow();
        assertHooverMumpsTimes(infection);
    }


    /**
     * First message has a lot of unknown data.
     * Second message is older than first message, but at processing, every field which is null in the database
     * should have the new information added to it.
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    void testNullInfectionDataAlwaysAdded() throws EmapOperationMessageProcessingException {
        // original minimal message has comment, resolution datetime, onset date and comment as unknown
        processSingleMessage(hl7Mumps);

        // process message that is older than current database, but has information which was missing in first message
        hooverMumps.setUpdatedDateTime(HL7_UPDATE_TIME.minus(20, ChronoUnit.DAYS));
        String comment = "great comment";
        hooverMumps.setComment(InterchangeValue.buildFromHl7(comment));
        processSingleMessage(hooverMumps);

        PatientState infection = patientStateRepository
                .findByMrnIdMrnAndPatientStateTypeIdNameAndAddedDateTime(MUMPS_MRN, MUMPS_INFECTION, MUMPS_ADD_TIME)
                .orElseThrow();

        // extra data should be added
        assertHooverMumpsTimes(infection);
        assertEquals(comment, infection.getComment());
    }

    /**
     * Database fields shouldn't update from an older message if they aren't null.
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    void olderMessageDoesntUpdateExistingData() throws EmapOperationMessageProcessingException {
        // process original message with date information
        processSingleMessage(hooverMumps);

        System.out.println(hooverMumps);

        // process message that is older than current database with different data
        processOlderDateTimeMessage();

        PatientState infection = patientStateRepository
                .findByMrnIdMrnAndPatientStateTypeIdNameAndAddedDateTime(MUMPS_MRN, MUMPS_INFECTION, MUMPS_ADD_TIME)
                .orElseThrow();
        System.out.println(infection);

        assertHooverMumpsTimes(infection);
    }

    @Test
    void newerMessageUpdatesExistingDate() throws EmapOperationMessageProcessingException {
        // process message that is older than current database with different data
        processOlderDateTimeMessage();

        // process newer message with different date information
        processSingleMessage(hooverMumps);

        PatientState infection = patientStateRepository
                .findByMrnIdMrnAndPatientStateTypeIdNameAndAddedDateTime(MUMPS_MRN, MUMPS_INFECTION, MUMPS_ADD_TIME)
                .orElseThrow();

        assertHooverMumpsTimes(infection);
    }

}
