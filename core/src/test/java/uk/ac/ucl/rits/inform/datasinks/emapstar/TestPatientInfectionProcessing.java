package uk.ac.ucl.rits.inform.datasinks.emapstar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.RequiredDataMissingException;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.ConditionTypeRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.PatientConditionAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.PatientConditionRepository;
import uk.ac.ucl.rits.inform.informdb.conditions.ConditionType;
import uk.ac.ucl.rits.inform.informdb.conditions.PatientCondition;
import uk.ac.ucl.rits.inform.informdb.conditions.PatientConditionAudit;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.PatientInfection;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test cases to ensure that processing of patient infection messages is working correctly.
 * @author Stef Piatek
 * @author Anika Cawthorn
 */
public class TestPatientInfectionProcessing extends MessageProcessingBase {
    @Autowired
    PatientConditionRepository patientConditionRepository;
    @Autowired
    PatientConditionAuditRepository patientConditionAuditRepository;
    @Autowired
    ConditionTypeRepository conditionTypeRepository;

    private List<PatientInfection> hooverMessages;
    private PatientInfection hl7Mumps;
    private PatientInfection hooverMumps;
    private static String MUMPS_MRN = "8DcEwvqa8Q3";
    private static Instant MUMPS_ADD_TIME = Instant.parse("2019-06-02T10:31:05Z");
    private static String MUMPS_INFECTION = "Mumps";
    private static String PATIENT_INFECTION = "PATIENT_INFECTION";
    private static Instant HL7_UPDATE_TIME = Instant.parse("2019-06-07T11:32:00Z");

    @BeforeEach
    private void setUp() throws IOException {
        hooverMessages = messageFactory.getPatientInfections("updated_only.yaml");
        hl7Mumps = messageFactory.getPatientInfections("hl7/minimal_mumps.yaml").get(0);
        hooverMumps = hooverMessages.get(0);
    }

    private void assertHooverMumpsTimes(PatientCondition infection) {
        assertEquals(Instant.parse("2019-06-08T14:22:01Z"), infection.getResolutionDateTime());
        assertEquals(LocalDate.parse("2019-03-05"), infection.getOnsetDate());
    }


    private void processOlderDateTimeMessage() throws EmapOperationMessageProcessingException {
        Instant olderResolvedTime = MUMPS_ADD_TIME.plusSeconds(60);
        LocalDate olderOnsetDate = LocalDate.parse("2019-01-01");

        PatientInfection olderMessage = new PatientInfection();
        olderMessage.setInfectionAdded(hooverMumps.getInfectionAdded());
        olderMessage.setEpicInfectionId(InterchangeValue.buildFromHl7(1L));
        olderMessage.setSourceSystem(hooverMumps.getSourceSystem());
        olderMessage.setMrn(MUMPS_MRN);
        olderMessage.setInfectionCode(MUMPS_INFECTION);
        olderMessage.setInfectionName(InterchangeValue.buildFromHl7(MUMPS_INFECTION));
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

        assertEquals(2, mrns.size());
        assertEquals("clarity", mrns.get(0).getSourceSystem());
    }

    /**
     * Ensure that the type of condition is created.
     * Fairly minimal information can be added from a patient infection.
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    void testPatientInfectionTypeCreated() throws EmapOperationMessageProcessingException {
        processSingleMessage(hl7Mumps);

        ConditionType type = conditionTypeRepository.findByDataTypeAndInternalCode(PATIENT_INFECTION, MUMPS_INFECTION).orElseThrow();

        assertEquals(HL7_UPDATE_TIME, type.getValidFrom());
        assertNotNull(type.getValidFrom());
        assertNull(type.getStandardisedCode());
        assertNull(type.getStandardisedVocabulary());
        assertNull(type.getName());
    }

    @Test
    void testPatientInfectionTypeNameUpdated() throws EmapOperationMessageProcessingException {
        processSingleMessage(hl7Mumps);
        processSingleMessage(hooverMumps);
        ConditionType type = conditionTypeRepository.findByDataTypeAndInternalCode(PATIENT_INFECTION, MUMPS_INFECTION).orElseThrow();

        assertEquals(MUMPS_INFECTION, type.getName());
    }


    /**
     * Patient infection with only added time should create an entity that has no resolution time.
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    void testMinimalPatientInfection() throws EmapOperationMessageProcessingException {
        processSingleMessage(hl7Mumps);

        PatientCondition infection = patientConditionRepository
                .findByMrnIdMrnAndConditionTypeIdInternalCodeAndAddedDateTime(MUMPS_MRN, MUMPS_INFECTION, MUMPS_ADD_TIME)
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

        PatientCondition infection = patientConditionRepository
                .findByMrnIdMrnAndConditionTypeIdInternalCodeAndAddedDateTime(MUMPS_MRN, MUMPS_INFECTION, MUMPS_ADD_TIME)
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

        PatientCondition infection = patientConditionRepository
                .findByMrnIdMrnAndConditionTypeIdInternalCodeAndAddedDateTime(MUMPS_MRN, MUMPS_INFECTION, MUMPS_ADD_TIME)
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

        PatientCondition infection = patientConditionRepository
                .findByMrnIdMrnAndConditionTypeIdInternalCodeAndAddedDateTime(MUMPS_MRN, MUMPS_INFECTION, MUMPS_ADD_TIME)
                .orElseThrow();

        assertEquals(comment, infection.getComment());
        assertEquals("Active", infection.getStatus());
    }

    /**
     * The when a hoover row is processed, the internal id should be saved to the database.
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    void testInternalIdAddedFromHoover() throws EmapOperationMessageProcessingException {
        processSingleMessage(hooverMumps);
        PatientCondition infection = patientConditionRepository
                .findByMrnIdMrnAndConditionTypeIdInternalCodeAndAddedDateTime(MUMPS_MRN, MUMPS_INFECTION, MUMPS_ADD_TIME)
                .orElseThrow();
        assertEquals(1, infection.getInternalId());
    }

    /**
     * A hoover infection with an unknown condition Id should throw an exception upon processing.
     */
    @Test
    void testMissingInternalIdFromHooverThrows() {
        hooverMumps.setEpicInfectionId(InterchangeValue.unknown());
        assertThrows(RequiredDataMissingException.class, () -> processSingleMessage(hooverMumps));
    }

    /**
     * hl7 message was updated before the hoover message, so should be updated
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    void testHl7UpdatedByLaterMessage() throws EmapOperationMessageProcessingException {
        processSingleMessage(hl7Mumps);
        processSingleMessage(hooverMumps);

        PatientCondition infection = patientConditionRepository
                .findByMrnIdMrnAndConditionTypeIdInternalCodeAndAddedDateTime(MUMPS_MRN, MUMPS_INFECTION, MUMPS_ADD_TIME)
                .orElseThrow();
        assertHooverMumpsTimes(infection);
    }


    /**
     * First message from hl7 (no epic infection id)
     * Second message has the same data, but the incremental update date will mean it's after the current information
     * should have the new information added to it.
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    void testHl7InfectionDeletedAndNewInformationAdded() throws EmapOperationMessageProcessingException {
        // original minimal message has comment, resolution datetime, onset date and comment as unknown
        processSingleMessage(hl7Mumps);
        PatientCondition hl7Infection = patientConditionRepository
                .findByMrnIdMrnAndConditionTypeIdInternalCodeAndAddedDateTime(MUMPS_MRN, MUMPS_INFECTION, MUMPS_ADD_TIME)
                .orElseThrow();

        String comment = "great comment";
        hooverMumps.setComment(InterchangeValue.buildFromHl7(comment));
        processSingleMessage(hooverMumps);

        PatientCondition infection = patientConditionRepository
                .findByMrnIdMrnAndConditionTypeIdInternalCodeAndAddedDateTime(MUMPS_MRN, MUMPS_INFECTION, MUMPS_ADD_TIME)
                .orElseThrow();

        // id shouldn't be the same as the hl7 infection because that should be deleted
        assertNotEquals(hl7Infection.getPatientConditionId(), infection.getPatientConditionId());

        // extra data should be added
        assertHooverMumpsTimes(infection);
        assertEquals(comment, infection.getComment());
        assertEquals(1, infection.getInternalId());
    }

    /**
     * Given Hl7 message for a different infection has been processed
     * When a later hoover message is parsed of another infection
     * Then an audit log should be made for the hl7 message as it was deleted
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    void testUnrelatedHl7InfectionDeletedAndAudited() throws EmapOperationMessageProcessingException {
        String anotherInfection = "COVID";
        hl7Mumps.setInfectionCode(anotherInfection);
        hl7Mumps.setInfectionName(InterchangeValue.buildFromHl7(anotherInfection));
        processSingleMessage(hl7Mumps);
        PatientCondition hl7Infection = patientConditionRepository
                .findByMrnIdMrnAndConditionTypeIdInternalCodeAndAddedDateTime(MUMPS_MRN, anotherInfection, MUMPS_ADD_TIME)
                .orElseThrow();

        processSingleMessage(hooverMumps);

        // should be deleted
        Optional<PatientCondition> deletedInfection = patientConditionRepository
                .findByMrnIdMrnAndConditionTypeIdInternalCodeAndAddedDateTime(MUMPS_MRN, anotherInfection, MUMPS_ADD_TIME);
        assertTrue(deletedInfection.isEmpty());

        // should have an audit log of the previous infection id
        PatientConditionAudit audit = patientConditionAuditRepository.findByPatientConditionId(hl7Infection.getPatientConditionId()).orElseThrow();
        assertEquals(MUMPS_ADD_TIME, audit.getAddedDateTime());
    }

    /**
     * Database fields shouldn't update from an older message if they aren't null.
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    void olderMessageDoesntUpdateExistingData() throws EmapOperationMessageProcessingException {
        // process original message with date information
        processSingleMessage(hooverMumps);

        // process message that is older than current database with different data
        processOlderDateTimeMessage();

        PatientCondition infection = patientConditionRepository
                .findByMrnIdMrnAndConditionTypeIdInternalCodeAndAddedDateTime(MUMPS_MRN, MUMPS_INFECTION, MUMPS_ADD_TIME)
                .orElseThrow();

        assertHooverMumpsTimes(infection);
    }

    /**
     * Tests whether newer message overwrites information of older in the relevant date fields.
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    void newerMessageUpdatesExistingDate() throws EmapOperationMessageProcessingException {
        // process message that is older than current database with different data
        processOlderDateTimeMessage();

        // process newer message with different date information
        processSingleMessage(hooverMumps);

        PatientCondition infection = patientConditionRepository
                .findByMrnIdMrnAndConditionTypeIdInternalCodeAndAddedDateTime(MUMPS_MRN, MUMPS_INFECTION, MUMPS_ADD_TIME)
                .orElseThrow();

        assertHooverMumpsTimes(infection);
    }

    /**
     * Given patient infection with no added datetime should be processed
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    void testInfectionWithNoAddedDateTimeIsParsed() throws EmapOperationMessageProcessingException {
        hl7Mumps.setInfectionAdded(null);
        processSingleMessage(hl7Mumps);

        PatientCondition infection = patientConditionRepository
                .findByMrnIdMrnAndConditionTypeIdInternalCodeAndAddedDateTime(MUMPS_MRN, MUMPS_INFECTION, null)
                .orElseThrow();

        assertNull(infection.getAddedDateTime());
    }
}
