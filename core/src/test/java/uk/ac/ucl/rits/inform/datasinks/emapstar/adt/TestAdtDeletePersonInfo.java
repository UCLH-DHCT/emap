package uk.ac.ucl.rits.inform.datasinks.emapstar.adt;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import uk.ac.ucl.rits.inform.datasinks.emapstar.MessageProcessingBase;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.FormAnswerAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.FormAnswerRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.FormAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.FormRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LocationVisitAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LocationVisitRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.PlannedMovementAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.PlannedMovementRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabOrderAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabOrderRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabResultAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabResultRepository;
import uk.ac.ucl.rits.inform.informdb.labs.LabResultAudit;
import uk.ac.ucl.rits.inform.informdb.movement.LocationVisit;
import uk.ac.ucl.rits.inform.informdb.movement.LocationVisitAudit;
import uk.ac.ucl.rits.inform.interchange.ConsultRequest;
import uk.ac.ucl.rits.inform.interchange.adt.DeletePersonInformation;
import uk.ac.ucl.rits.inform.interchange.adt.PendingTransfer;
import uk.ac.ucl.rits.inform.interchange.lab.LabOrderMsg;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Separate test class for ensuring that deletion of patient information ADT messages are handled correctly.
 * @author Stef Piatek
 */
class TestAdtDeletePersonInfo extends MessageProcessingBase {

    @Autowired
    private LocationVisitRepository locationVisitRepository;
    @Autowired
    private LocationVisitAuditRepository locationVisitAuditRepository;

    @Autowired
    private LabOrderRepository labOrderRepository;
    @Autowired
    private LabOrderAuditRepository labOrderAuditRepository;
    @Autowired
    private LabResultRepository labResultRepo;
    @Autowired
    private LabResultAuditRepository labResultAuditRepo;
    @Autowired
    private FormRepository formRepository;
    @Autowired
    private FormAnswerRepository formAnswerRepository;
    @Autowired
    private FormAuditRepository formAuditRepository;
    @Autowired
    private FormAnswerAuditRepository formAnswerAuditRepository;
    @Autowired
    private PlannedMovementRepository plannedMovementRepo;
    @Autowired
    private PlannedMovementAuditRepository plannedMovementAuditRepo;


    private static final String ORIGINAL_LOCATION = "T42E^T42E BY03^BY03-17";
    private static final long DEFAULT_HOSPITAL_VISIT_ID = 4001;

    /**
     * Given multiple dependent rows on encounter `123412341234`
     * When a "delete patient information" message is received for the patient in the encounter
     * Then dependent rows should be audited and deleted
     * @throws Exception shouldn't happen
     */
    @Test
    @Sql("/populate_db.sql")
    void testDeletePersonInformationWithCascade() throws Exception {
        // -- Arrange
        List<LabOrderMsg> labOrderMsgs = messageFactory.getLabOrders("winpath/ORU_R01.yaml", "0000040");
        ConsultRequest consultMsg = messageFactory.getConsult("minimal.yaml");
        PendingTransfer pendingAdtMsg = messageFactory.getAdtMessage("pending/A15.yaml");
        for (var loMsg : labOrderMsgs) {
            dbOps.processMessage(loMsg);
        }
        dbOps.processMessage(consultMsg);
        dbOps.processMessage(pendingAdtMsg);

        // -- Act
        DeletePersonInformation msg = messageFactory.getAdtMessage("generic/A29.yaml");
        msg.setEventOccurredDateTime(Instant.now());
        dbOps.processMessage(msg);

        // -- Assert

        // original location does not exist
        LocationVisit locationVisit = locationVisitRepository.findByLocationIdLocationString(ORIGINAL_LOCATION).orElse(null);
        assertNull(locationVisit);

        // audit row for the existing location
        assertTrue(locationVisitAuditRepository.findByLocationIdLocationString(ORIGINAL_LOCATION).isPresent());

        // PlannedMovement should be deleted and audited
        var movements = plannedMovementRepo.findAllByHospitalVisitIdEncounter(defaultEncounter);
        assertTrue(movements.isEmpty());
        var movementAudits = plannedMovementAuditRepo.findAllByHospitalVisitId(DEFAULT_HOSPITAL_VISIT_ID);
        assertFalse(movementAudits.isEmpty());

        // Forms have been deleted and moved to audit table
        assertEquals(0, formAnswerRepository.count());
        assertEquals(0, formRepository.count());

        assertEquals(67, formAnswerAuditRepository.count());
        assertTrue(formAnswerAuditRepository.existsByFormAnswerId(230001L));
        assertEquals(11, formAuditRepository.count());
        assertTrue(formAuditRepository.existsByFormId(210001L));

        // Ensure live lab rows are missing, and audit rows do exist
        var labOrderAudits = labOrderAuditRepository.findAllByHospitalVisitIdIn(List.of(DEFAULT_HOSPITAL_VISIT_ID));
        assertEquals(2, labOrderAudits.size());
        for (var loa : labOrderAudits) {
            // shouldn't exist in live table
            assertEquals(Optional.empty(), labOrderRepository.findById(loa.getLabOrderId()));

            List<LabResultAudit> labResultAudits = labResultAuditRepo.findAllByLabOrderIdIn(List.of(loa.getLabOrderId()));
            assertFalse(labResultAudits.isEmpty());
            for (var lra : labResultAudits) {
                assertEquals(Optional.empty(), labResultRepo.findById(lra.getLabResultId()));
            }
        }
    }

    /**
     * @throws Exception shouldn't happen
     */
    @Test
    @Sql("/populate_db.sql")
    void testDeletePersonInformation() throws Exception {
        DeletePersonInformation msg = messageFactory.getAdtMessage("generic/A29.yaml");
        // process message
        dbOps.processMessage(msg);

        // original location does not exist
        LocationVisit locationVisit = locationVisitRepository.findByLocationIdLocationString(ORIGINAL_LOCATION).orElse(null);
        assertNull(locationVisit);

        // audit row for the existing location
        LocationVisitAudit audit = locationVisitAuditRepository.findByLocationIdLocationString(ORIGINAL_LOCATION).orElse(null);
        assertNotNull(audit);
    }

    /**
     * Message is older than database, so no deletes should take place.
     * @throws Exception shouldn't happen
     */
    @Test
    @Sql("/populate_db.sql")
    void testOldDeleteMessageHasNoEffect() throws Exception {
        DeletePersonInformation msg = messageFactory.getAdtMessage("generic/A29.yaml");
        msg.setEventOccurredDateTime(Instant.parse("2000-01-01T00:00:00Z"));
        // process message
        dbOps.processMessage(msg);

        // original location does still exist
        LocationVisit locationVisit = locationVisitRepository.findByLocationIdLocationString(ORIGINAL_LOCATION).orElse(null);
        assertNotNull(locationVisit);

        // No audit rows
        assertEquals(0L, getAllEntities(locationVisitAuditRepository).size());
    }

}
