package uk.ac.ucl.rits.inform.datasinks.emapstar;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.AuditLocationVisitRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.HospitalVisitRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LocationRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LocationVisitRepository;
import uk.ac.ucl.rits.inform.informdb.movement.AuditLocationVisit;
import uk.ac.ucl.rits.inform.informdb.movement.LocationVisit;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.Hl7Value;
import uk.ac.ucl.rits.inform.interchange.adt.AdmitPatient;
import uk.ac.ucl.rits.inform.interchange.adt.CancelAdmitPatient;
import uk.ac.ucl.rits.inform.interchange.adt.CancelDischargePatient;
import uk.ac.ucl.rits.inform.interchange.adt.CancelTransferPatient;
import uk.ac.ucl.rits.inform.interchange.adt.DeletePersonInformation;
import uk.ac.ucl.rits.inform.interchange.adt.DischargePatient;
import uk.ac.ucl.rits.inform.interchange.adt.TransferPatient;

import java.time.Instant;
import java.util.Optional;

class TestAdtProcessingLocation extends MessageProcessingBase {
    @Autowired
    private HospitalVisitRepository hospitalVisitRepository;
    @Autowired
    private LocationRepository locationRepository;
    @Autowired
    private LocationVisitRepository locationVisitRepository;
    @Autowired
    private AuditLocationVisitRepository auditLocationVisitRepository;

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

        Assertions.assertEquals(1L, getAllEntities(locationRepository).size());
        Assertions.assertEquals(1L, getAllEntities(locationVisitRepository).size());
        Assertions.assertEquals(0L, getAllEntities(auditLocationVisitRepository).size());
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
        Assertions.assertNotNull(dischargedVisit.getDischargeTime());

        // current location visit is different
        LocationVisit currentVisit = locationVisitRepository
                .findByDischargeTimeIsNullAndHospitalVisitIdHospitalVisitId(defaultHospitalVisitId)
                .orElseThrow(NullPointerException::new);
        Assertions.assertNotEquals(originalLocation, currentVisit.getLocation().getLocationString());

        // audit row for location when it had no discharge time
        AuditLocationVisit audit = auditLocationVisitRepository.findByLocationIdLocationString(originalLocation).orElseThrow(NullPointerException::new);
        Assertions.assertNull(audit.getDischargeTime());
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
        msg.setFullLocationString(Hl7Value.buildFromHl7(originalLocation));
        dbOps.processMessage(msg);

        // original location visit is discharged
        LocationVisit dischargedVisit = locationVisitRepository.findByLocationIdLocationString(originalLocation).orElseThrow(NullPointerException::new);
        Assertions.assertNotNull(dischargedVisit.getDischargeTime());

        // audit row for location when it had no discharge time
        AuditLocationVisit audit = auditLocationVisitRepository.findByLocationIdLocationString(originalLocation).orElseThrow(NullPointerException::new);
        Assertions.assertNull(audit.getDischargeTime());
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
        Assertions.assertNull(visit.getDischargeTime());

        // audit row for location when it had no discharge time
        Optional<AuditLocationVisit> audit = auditLocationVisitRepository.findByLocationIdLocationString(originalLocation);
        Assertions.assertTrue(audit.isEmpty());
    }

    /**
     * Visit and location visit already exist in the database, old message given.
     * Should do nothing to location
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    @Sql("/populate_db.sql")
    void testTrustedMessageUpdatedUntrustedLocation() throws EmapOperationMessageProcessingException {
        TransferPatient msg = messageFactory.getAdtMessage("generic/A02.yaml");
        msg.setEventOccurredDateTime(past);
        msg.setMrn(null);
        msg.setNhsNumber("222222222");
        msg.setVisitNumber("0999999999");
        String untrustedLocation = "T11E^T11E BY02^BY02-17";

        dbOps.processMessage(msg);

        // original location visit is discharged
        LocationVisit dischargedVisit = locationVisitRepository.findByLocationIdLocationString(untrustedLocation).orElseThrow(NullPointerException::new);
        Assertions.assertNotNull(dischargedVisit.getDischargeTime());

        // current location visit is different
        LocationVisit currentVisit = locationVisitRepository
                .findByDischargeTimeIsNullAndHospitalVisitIdHospitalVisitId(defaultHospitalVisitId)
                .orElseThrow(NullPointerException::new);
        Assertions.assertNotEquals(untrustedLocation, currentVisit.getLocation().getLocationString());

        // audit row for location when it had no discharge time
        AuditLocationVisit audit = auditLocationVisitRepository.findByLocationIdLocationString(untrustedLocation).orElseThrow(NullPointerException::new);
        Assertions.assertNull(audit.getDischargeTime());
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
        Assertions.assertNull(locationVisit);

        // audit row for the existing location
        AuditLocationVisit audit = auditLocationVisitRepository.findByLocationIdLocationString(originalLocation).orElse(null);
        Assertions.assertNotNull(audit);
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
        Assertions.assertNotNull(locationVisit);

        // No audit rows
        Assertions.assertEquals(0L, getAllEntities(auditLocationVisitRepository).size());
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

        Assertions.assertEquals(0L, getAllEntities(locationVisitRepository).size());
        Assertions.assertEquals(0L, getAllEntities(auditLocationVisitRepository).size());
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
        Assertions.assertTrue(deletedVisit.isEmpty());
    }


    /**
     * No locations or location-visit in database.
     * Cancel transfer patient should create a new location visit.
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    void testCancelTransferDoesntExist() throws EmapOperationMessageProcessingException {
        CancelTransferPatient msg = messageFactory.getAdtMessage("generic/A12.yaml");
        dbOps.processMessage(msg);

        Assertions.assertEquals(1L, getAllEntities(locationVisitRepository).size());
        Assertions.assertEquals(0L, getAllEntities(auditLocationVisitRepository).size());
    }

    /**
     * location-visit exists in database.
     * Cancel transfer patient should delete the current open visit location and undischarge the previous location.
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    void testCancelTransfer() throws EmapOperationMessageProcessingException {
        CancelTransferPatient msg = messageFactory.getAdtMessage("generic/A12.yaml");
        // TODO: double check this is correct and rename the interchange field to be more clear
        msg.setPreviousLocation(originalLocation);
        String correctLocation = "T11E^T11E BY02^BY02-25";

        dbOps.processMessage(msg);

        // original location visit is deleted
        Optional<LocationVisit> deletedVisit = locationVisitRepository.findByLocationIdLocationString(originalLocation);
        Assertions.assertTrue(deletedVisit.isEmpty());

        // correct location is reopened
        LocationVisit reopenedVisit = locationVisitRepository.findByLocationIdLocationString(correctLocation).orElseThrow();
        Assertions.assertNull(reopenedVisit.getDischargeTime());
    }

    /**
     * No locations or location-visit in database.
     * Cancel transfer patient should create new location
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    void testCancelDischargeDoesntExist() throws EmapOperationMessageProcessingException {
        CancelDischargePatient msg = messageFactory.getAdtMessage("generic/A13.yaml");
        dbOps.processMessage(msg);

        Assertions.assertEquals(1L, getAllEntities(locationVisitRepository).size());
        Assertions.assertEquals(0L, getAllEntities(auditLocationVisitRepository).size());
    }

    /**
     * Encounter has discharged location visit
     * Cancel discharge message should remove the discharged date time from the original visit
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    void testCancelDischarge() throws EmapOperationMessageProcessingException {
        CancelDischargePatient msg = messageFactory.getAdtMessage("generic/A13.yaml");
        String correctLocation = "T06C^T06C SR41^SR41-41";
        msg.setFullLocationString(Hl7Value.buildFromHl7(correctLocation));
        msg.setVisitNumber("1234567890");
        msg.setMrn("60600000");
        msg.setNhsNumber("1111111111");
        dbOps.processMessage(msg);

        // correct location is reopened
        LocationVisit reopenedVisit = locationVisitRepository.findByLocationIdLocationString(correctLocation).orElseThrow();
        Assertions.assertNull(reopenedVisit.getDischargeTime());
    }
}
