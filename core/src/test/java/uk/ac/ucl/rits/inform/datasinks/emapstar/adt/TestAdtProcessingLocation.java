package uk.ac.ucl.rits.inform.datasinks.emapstar.adt;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import uk.ac.ucl.rits.inform.datasinks.emapstar.MessageProcessingBase;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.RequiredDataMissingException;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.HospitalVisitRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LocationRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LocationVisitAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LocationVisitRepository;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.movement.LocationVisit;
import uk.ac.ucl.rits.inform.informdb.movement.LocationVisitAudit;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.adt.AdmitPatient;
import uk.ac.ucl.rits.inform.interchange.adt.CancelAdmitPatient;
import uk.ac.ucl.rits.inform.interchange.adt.CancelDischargePatient;
import uk.ac.ucl.rits.inform.interchange.adt.CancelTransferPatient;
import uk.ac.ucl.rits.inform.interchange.adt.DeletePersonInformation;
import uk.ac.ucl.rits.inform.interchange.adt.DischargePatient;
import uk.ac.ucl.rits.inform.interchange.adt.SwapLocations;
import uk.ac.ucl.rits.inform.interchange.adt.TransferPatient;
import uk.ac.ucl.rits.inform.interchange.adt.UpdatePatientInfo;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestAdtProcessingLocation extends MessageProcessingBase {
    @Autowired
    private HospitalVisitRepository hospitalVisitRepository;
    @Autowired
    private LocationRepository locationRepository;
    @Autowired
    private LocationVisitRepository locationVisitRepository;
    @Autowired
    private LocationVisitAuditRepository locationVisitAuditRepository;

    private final String originalLocation = "T42E^T42E BY03^BY03-17";
    private final long defaultHospitalVisitId = 4001;

    /**
     * No locations or location-visit in database.
     * Should create a new location and location-visit, but no audit location visit
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    void testCreateNewLocationVisit() throws EmapOperationMessageProcessingException {
        AdmitPatient msg = messageFactory.getAdtMessage("generic/A01.yaml");
        dbOps.processMessage(msg);

        assertEquals(1L, getAllEntities(locationRepository).size());
        assertEquals(1L, getAllEntities(locationVisitRepository).size());
        assertEquals(0L, getAllEntities(locationVisitAuditRepository).size());
    }

    /**
     * No locations or location-visit in database.
     * Two admits for the same time and location should only create a single entry
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    void testDuplicateAdmit() throws EmapOperationMessageProcessingException {
        AdmitPatient msg = messageFactory.getAdtMessage("generic/A01.yaml");

        dbOps.processMessage(msg);
        msg.setEventOccurredDateTime(Instant.now());
        dbOps.processMessage(msg);

        assertEquals(1L, getAllEntities(locationVisitRepository).size());
        assertEquals(0L, getAllEntities(locationVisitAuditRepository).size());
    }


    /**
     * Visit and location visit already exist in the database, new location given.
     * Should discharge the original location visit, audit log the original state and create a new location.
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    @Sql("/populate_db.sql")
    void testMoveCurrentVisitLocation() throws EmapOperationMessageProcessingException {
        TransferPatient msg = messageFactory.getAdtMessage("generic/A02.yaml");
        dbOps.processMessage(msg);

        // original location visit is discharged
        LocationVisit dischargedVisit = locationVisitRepository.findByLocationIdLocationString(originalLocation).orElseThrow(NullPointerException::new);
        assertNotNull(dischargedVisit.getDischargeTime());

        // current location visit is different
        LocationVisit currentVisit = locationVisitRepository
                .findByDischargeTimeIsNullAndHospitalVisitIdHospitalVisitId(defaultHospitalVisitId)
                .orElseThrow(NullPointerException::new);
        assertNotEquals(originalLocation, currentVisit.getLocationId().getLocationString());

        // audit row for location when it had no discharge time
        LocationVisitAudit audit = locationVisitAuditRepository.findByLocationIdLocationString(originalLocation).orElseThrow(NullPointerException::new);
        assertNull(audit.getDischargeTime());
    }

    /**
     * Visit and location visit already exist in the database.
     * DischargePatient message: discharge the original location visit, audit log the original state.
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    @Sql("/populate_db.sql")
    void testDischargeMessage() throws EmapOperationMessageProcessingException {
        DischargePatient msg = messageFactory.getAdtMessage("generic/A03.yaml");
        msg.setFullLocationString(InterchangeValue.buildFromHl7(originalLocation));
        dbOps.processMessage(msg);

        // original location visit is discharged
        List<LocationVisit> dischargedVisits = locationVisitRepository
                .findAllByLocationIdLocationStringAndHospitalVisitIdEncounter(originalLocation, defaultEncounter);
        assertEquals(1, dischargedVisits.size());
        dischargedVisits.forEach(visit -> assertNotNull(visit.getDischargeTime()));

        // audit row for location when it had no discharge time
        LocationVisitAudit audit = locationVisitAuditRepository.findByLocationIdLocationString(originalLocation).orElseThrow(NullPointerException::new);
        assertNull(audit.getDischargeTime());
    }

    /**
     * Visit and location visit already exist in the database.
     * Duplicate discharge should have no effect
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    @Sql("/populate_db.sql")
    void testDuplicateDischargeMessage() throws EmapOperationMessageProcessingException {
        DischargePatient msg = messageFactory.getAdtMessage("generic/A03.yaml");
        msg.setFullLocationString(InterchangeValue.buildFromHl7(originalLocation));
        // first discharge
        dbOps.processMessage(msg);
        // duplicate discharge message
        dbOps.processMessage(msg);

        // original location visit is discharged
        List<LocationVisit> dischargedVisits = locationVisitRepository
                .findAllByLocationIdLocationStringAndHospitalVisitIdEncounter(originalLocation, defaultEncounter);
        assertEquals(1, dischargedVisits.size());
        dischargedVisits.forEach(visit -> assertNotNull(visit.getDischargeTime()));

        // single audit row for location when it had no discharge time
        LocationVisitAudit audit = locationVisitAuditRepository.findByLocationIdLocationString(originalLocation).orElseThrow(NullPointerException::new);
        assertNull(audit.getDischargeTime());
    }

    /**
     * Visit and location visit already exist in the database, old message given.
     * Should do nothing to location
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    @Sql("/populate_db.sql")
    void testOldMessageDoesNotMoveLocation() throws EmapOperationMessageProcessingException {
        TransferPatient msg = messageFactory.getAdtMessage("generic/A02.yaml");
        msg.setEventOccurredDateTime(past);
        dbOps.processMessage(msg);

        // current db location visit has not been discharged
        LocationVisit visit = locationVisitRepository.findByLocationIdLocationString(originalLocation).orElseThrow(NullPointerException::new);
        assertNull(visit.getDischargeTime());

        // audit row for location when it had no discharge time
        Optional<LocationVisitAudit> audit = locationVisitAuditRepository.findByLocationIdLocationString(originalLocation);
        assertTrue(audit.isEmpty());
    }

    /**
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    @Sql("/populate_db.sql")
    void testDeletePersonInformation() throws EmapOperationMessageProcessingException {
        DeletePersonInformation msg = messageFactory.getAdtMessage("generic/A29.yaml");
        // process message
        dbOps.processMessage(msg);

        // original location does not exist
        LocationVisit locationVisit = locationVisitRepository.findByLocationIdLocationString(originalLocation).orElse(null);
        assertNull(locationVisit);

        // audit row for the existing location
        LocationVisitAudit audit = locationVisitAuditRepository.findByLocationIdLocationString(originalLocation).orElse(null);
        assertNotNull(audit);
    }

    /**
     * Message is older than database, so no deletes should take place.
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    @Sql("/populate_db.sql")
    void testOldDeleteMessageHasNoEffect() throws EmapOperationMessageProcessingException {
        DeletePersonInformation msg = messageFactory.getAdtMessage("generic/A29.yaml");
        msg.setEventOccurredDateTime(Instant.parse("2000-01-01T00:00:00Z"));
        // process message
        dbOps.processMessage(msg);

        // original location does still exist
        LocationVisit locationVisit = locationVisitRepository.findByLocationIdLocationString(originalLocation).orElse(null);
        assertNotNull(locationVisit);

        // No audit rows
        assertEquals(0L, getAllEntities(locationVisitAuditRepository).size());
    }

    /**
     * No locations or location-visit in database.
     * Cancel admit patient should not add a new location visit.
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    void testCancelAdmitDoesntExist() throws EmapOperationMessageProcessingException {
        CancelAdmitPatient msg = messageFactory.getAdtMessage("generic/A11.yaml");
        dbOps.processMessage(msg);

        assertEquals(0L, getAllEntities(locationVisitRepository).size());
    }

    /**
     * Location visit exists in the database
     * Cancel admit should remove the existing location visit
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    @Sql("/populate_db.sql")
    void testCancelAdmit() throws EmapOperationMessageProcessingException {
        CancelAdmitPatient msg = messageFactory.getAdtMessage("generic/A11.yaml");
        dbOps.processMessage(msg);

        // original location visit is deleted
        Optional<LocationVisit> deletedVisit = locationVisitRepository.findByLocationIdLocationString(originalLocation);
        assertTrue(deletedVisit.isEmpty());
    }

    /**
     * In validation, a lot of single admissions that are then cancelled but with no cancellation times in message
     * If only one location exists for a visit, and we don't have a cancellation time, cancel the the single location
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    @Sql("/populate_db.sql")
    void testMalformedCancelAdmitWithSingleLocation() throws EmapOperationMessageProcessingException {
        CancelAdmitPatient msg = messageFactory.getAdtMessage("generic/A11.yaml");
        msg.setCancelledDateTime(null);
        setDataForHospitalVisitId4002(msg);
        String location = "T06C^T06C SR41^SR41-41";
        msg.setFullLocationString(InterchangeValue.buildFromHl7(location));

        dbOps.processMessage(msg);
        // original location visit is deleted
        Optional<LocationVisit> deletedVisit = locationVisitRepository.findByLocationIdLocationString(location);
        assertTrue(deletedVisit.isEmpty());
    }

    /**
     * If there's a cancellation message with no cancellation time and there's multiple locations, shouldn't delete the message.
     */
    @Test
    @Sql("/populate_db.sql")
    void testCancelAdmitMalformedWithMultipleLocations() {
        CancelAdmitPatient msg = messageFactory.getAdtMessage("generic/A11.yaml");
        msg.setCancelledDateTime(null);
        setDataForHospitalVisitId4002(msg);
        assertThrows(RequiredDataMissingException.class, () -> dbOps.processMessage(msg));
    }

    /**
     * If there's a cancellation message with no cancellation time a single location but it doesn't match the cancel message.
     * shouldn't delete the message
     */
    @Test
    @Sql("/populate_db.sql")
    void testCancelAdmitMalformedWithSingleMismatchedLocation() {
        CancelAdmitPatient msg = messageFactory.getAdtMessage("generic/A11.yaml");
        msg.setCancelledDateTime(null);
        msg.setFullLocationString(InterchangeValue.buildFromHl7("I^don't^exist"));
        assertThrows(RequiredDataMissingException.class, () -> dbOps.processMessage(msg));
    }


    /**
     * No locations or location-visit in database.
     * Cancel transfer patient should not create the correct location
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    void testCancelTransferDoesntExist() throws EmapOperationMessageProcessingException {
        CancelTransferPatient msg = messageFactory.getAdtMessage("generic/A12.yaml");
        dbOps.processMessage(msg);

        assertEquals(0L, getAllEntities(locationVisitRepository).size());
    }

    /**
     * location-visit exists in database.
     * Cancel transfer patient should delete the current open visit location and undischarge the previous location.
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    @Sql("/populate_db.sql")
    void testCancelTransfer() throws EmapOperationMessageProcessingException {
        CancelTransferPatient msg = messageFactory.getAdtMessage("generic/A12.yaml");
        msg.setCancelledLocation(originalLocation);
        String correctLocation = "T11E^T11E BY02^BY02-25";

        dbOps.processMessage(msg);

        // original location visit is deleted
        Optional<LocationVisit> deletedVisit = locationVisitRepository.findByLocationIdLocationString(originalLocation);
        assertTrue(deletedVisit.isEmpty());

        // correct location is reopened
        LocationVisit reopenedVisit = locationVisitRepository.findByLocationIdLocationString(correctLocation).orElseThrow();
        assertNull(reopenedVisit.getDischargeTime());
    }

    /**
     * No locations or location-visit in database.
     * Cancel discharge should do nothing
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    void testCancelDischargeDoesntExist() throws EmapOperationMessageProcessingException {
        CancelDischargePatient msg = messageFactory.getAdtMessage("generic/A13.yaml");
        dbOps.processMessage(msg);

        assertEquals(0L, getAllEntities(locationVisitRepository).size());
    }

    /**
     * Encounter has discharged location visit
     * Cancel discharge message should remove the discharged date time from the original visit
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    @Sql("/populate_db.sql")
    void testCancelDischarge() throws EmapOperationMessageProcessingException {
        CancelDischargePatient msg = messageFactory.getAdtMessage("generic/A13.yaml");
        String correctLocation = "T06C^T06C SR41^SR41-41";
        msg.setFullLocationString(InterchangeValue.buildFromHl7(correctLocation));
        msg = setDataForHospitalVisitId4002(msg);
        dbOps.processMessage(msg);

        // correct location is reopened
        LocationVisit reopenedVisit = locationVisitRepository.findByLocationIdLocationString(correctLocation).orElseThrow();
        assertNull(reopenedVisit.getDischargeTime());
    }

    /**
     * Can get an A08 before and after a cancel discharge.
     * The newly opened visit from the A08 should be removed, and the discharged visit should be reopened instead
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    @Sql("/populate_db.sql")
    void testUpdateInfoBeforeAndAfterCancelDischarge() throws EmapOperationMessageProcessingException {
        String correctLocation = "T06C^T06C SR41^SR41-41";
        Instant messageDateTime = Instant.parse("2020-01-01T05:00:00Z");

        // update patient info for discharged location
        UpdatePatientInfo updatePatientInfo = messageFactory.getAdtMessage("generic/A08_v1.yaml");
        updatePatientInfo.setFullLocationString(InterchangeValue.buildFromHl7(correctLocation));
        setDataForHospitalVisitId4002(updatePatientInfo);
        updatePatientInfo.setRecordedDateTime(messageDateTime);
        // cancel discharge
        CancelDischargePatient cancelDischarge = messageFactory.getAdtMessage("generic/A13.yaml");
        cancelDischarge.setFullLocationString(InterchangeValue.buildFromHl7(correctLocation));
        setDataForHospitalVisitId4002(cancelDischarge);
        cancelDischarge.setRecordedDateTime(messageDateTime);
        // A08s will have a later message date time than the cancellation event occurred time
        cancelDischarge.setEventOccurredDateTime(messageDateTime.minus(1, ChronoUnit.HOURS));

        dbOps.processMessage(updatePatientInfo);
        dbOps.processMessage(cancelDischarge);

        // correct location is reopened and there are no duplicate results
        LocationVisit reopenedVisit = locationVisitRepository.findByLocationIdLocationString(correctLocation).orElseThrow();
        assertNull(reopenedVisit.getDischargeTime());

        // processing a further message should not come into an error of more than one open location
        dbOps.processMessage(updatePatientInfo);
    }

    /**
     * Swap locations of two open locations.
     */
    @Test
    @Sql("/populate_db.sql")
    void testSwapLocations() throws EmapOperationMessageProcessingException {
        SwapLocations msg = messageFactory.getAdtMessage("generic/A17.yaml");
        String locationA = "T11E^T11E BY02^BY02-17";
        String visitNumberA = "123412341234";
        msg.setFullLocationString(InterchangeValue.buildFromHl7(locationA));
        msg.setVisitNumber(visitNumberA);

        String locationB = "T42E^T42E BY03^BY03-17";
        String visitNumberB = "0999999999";
        msg.setOtherVisitNumber(visitNumberB);
        msg.setOtherMrn(null);
        msg.setOtherNhsNumber("222222222");
        msg.setOtherFullLocationString(InterchangeValue.buildFromHl7(locationB));

        HospitalVisit visitA = hospitalVisitRepository.findByEncounter(visitNumberA).orElseThrow();
        HospitalVisit visitB = hospitalVisitRepository.findByEncounter(visitNumberB).orElseThrow();

        LocationVisit originalLocationVisitA = locationVisitRepository.findByHospitalVisitIdAndDischargeTimeIsNull(visitA).orElseThrow();
        LocationVisit originalLocationVisitB = locationVisitRepository.findByHospitalVisitIdAndDischargeTimeIsNull(visitB).orElseThrow();


        dbOps.processMessage(msg);

        LocationVisit swappedLocationVisitA = locationVisitRepository.findByHospitalVisitIdAndDischargeTimeIsNull(visitA).orElseThrow();
        LocationVisit swappedLocationVisitB = locationVisitRepository.findByHospitalVisitIdAndDischargeTimeIsNull(visitB).orElseThrow();

        assertEquals(originalLocationVisitB.getLocationId(), swappedLocationVisitA.getLocationId());
        assertEquals(originalLocationVisitA.getLocationId(), swappedLocationVisitB.getLocationId());

    }
}
