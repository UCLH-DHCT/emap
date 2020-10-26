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
import uk.ac.ucl.rits.inform.interchange.adt.AdmitPatient;
import uk.ac.ucl.rits.inform.interchange.adt.DeletePersonInformation;
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
        msg.setMrn("60600000");
        msg.setNhsNumber("1111111111");
        msg.setVisitNumber("1234567890");
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
}
