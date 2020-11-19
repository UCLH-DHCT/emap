package uk.ac.ucl.rits.inform.datasinks.emapstar;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.HospitalVisitRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.VisitObservationAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.VisitObservationRepository;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.informdb.identity.MrnToLive;
import uk.ac.ucl.rits.inform.informdb.visit_recordings.VisitObservation;
import uk.ac.ucl.rits.inform.informdb.visit_recordings.VisitObservationAudit;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.Flowsheet;

import java.util.List;
import java.util.Optional;

class TestFlowsheetProcessing extends MessageProcessingBase {
    private List<Flowsheet> messages;
    @Autowired
    private HospitalVisitRepository hospitalVisitRepository;
    @Autowired
    private VisitObservationRepository visitObservationRepository;
    @Autowired
    private VisitObservationAuditRepository visitObservationAuditRepository;

    private String updateId = "8";
    private String deleteId = "28315";


    @BeforeEach
    void setup() {
        messages = messageFactory.getFlowsheets("hl7.yaml", "0000040");
    }

    /**
     * no existing mrns, so new mrn, mrn_to_live core_demographics, hospital visit and visit observation should be created.
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    void testCreateNewPatient() throws EmapOperationMessageProcessingException {
        for (Flowsheet msg : messages) {
            processSingleMessage(msg);
        }
        List<Mrn> mrns = getAllMrns();
        Assertions.assertEquals(1, mrns.size());
        Assertions.assertEquals("EPIC", mrns.get(0).getSourceSystem());

        MrnToLive mrnToLive = mrnToLiveRepo.getByMrnIdEquals(mrns.get(0));
        Assertions.assertNotNull(mrnToLive);

        HospitalVisit visit = hospitalVisitRepository.findByEncounter(defaultEncounter).orElseThrow(NullPointerException::new);

        // 7 flowsheets in input file, but one is a delete so only 6 should be created
        List<VisitObservation> observations = visitObservationRepository.findAllByHospitalVisitId(visit);
        Assertions.assertEquals(6, observations.size());
    }

    /**
     * Row already exists before message is encountered, numeric value is different in message, and message is updated more recently
     * Row should have updated value
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    @Sql("/populate_db.sql")
    void testRowUpdates() throws EmapOperationMessageProcessingException {
        HospitalVisit visit = hospitalVisitRepository.findByEncounter(defaultEncounter).orElseThrow(NullPointerException::new);
        VisitObservation preUpdateObservation = visitObservationRepository
                .findByHospitalVisitIdAndVisitObservationTypeIdIdInApplication(visit, updateId)
                .orElseThrow();

        for (Flowsheet msg : messages) {
            processSingleMessage(msg);
        }

        // value is updated
        VisitObservation updatedObservation = visitObservationRepository
                .findByHospitalVisitIdAndVisitObservationTypeIdIdInApplication(visit, updateId)
                .orElseThrow();
        Assertions.assertNotEquals(preUpdateObservation.getValueAsReal(), updatedObservation.getValueAsReal());

        // audit log for the old value
        VisitObservationAudit audit = visitObservationAuditRepository
                .findByHospitalVisitIdAndVisitObservationTypeIdIdInApplication(visit.getHospitalVisitId(), updateId)
                .orElseThrow();
        Assertions.assertEquals(preUpdateObservation.getValueAsReal(), audit.getValueAsReal());

    }

    /**
     * Message updated time is before the validFrom, so no update should happen
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    @Sql("/populate_db.sql")
    void testOldUpdateDoesNothing() throws EmapOperationMessageProcessingException {
        HospitalVisit visit = hospitalVisitRepository.findByEncounter(defaultEncounter).orElseThrow(NullPointerException::new);
        VisitObservation preUpdateObservation = visitObservationRepository
                .findByHospitalVisitIdAndVisitObservationTypeIdIdInApplication(visit, updateId)
                .orElseThrow();

        for (Flowsheet msg : messages) {
            msg.setUpdatedTime(past);
            processSingleMessage(msg);
        }

        VisitObservation updatedObservation = visitObservationRepository
                .findByHospitalVisitIdAndVisitObservationTypeIdIdInApplication(visit, updateId)
                .orElseThrow();

        Assertions.assertEquals(preUpdateObservation.getValueAsReal(), updatedObservation.getValueAsReal());
    }

    /**
     * Row already exists before message is encountered, numeric value is different in message, and message is updated more recently
     * Row should be deleted
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    @Sql("/populate_db.sql")
    void testRowDeletes() throws EmapOperationMessageProcessingException {
        HospitalVisit visit = hospitalVisitRepository.findByEncounter(defaultEncounter).orElseThrow(NullPointerException::new);
        VisitObservation preDeleteObservation = visitObservationRepository
                .findByHospitalVisitIdAndVisitObservationTypeIdIdInApplication(visit, deleteId)
                .orElseThrow();

        for (Flowsheet msg : messages) {
            processSingleMessage(msg);
        }

        // visit observation now does not exist
        Optional<VisitObservation> deletedObservation = visitObservationRepository
                .findByHospitalVisitIdAndVisitObservationTypeIdIdInApplication(visit, deleteId);
        Assertions.assertTrue(deletedObservation.isEmpty());

        // audit log for the old value
        VisitObservationAudit audit = visitObservationAuditRepository
                .findByHospitalVisitIdAndVisitObservationTypeIdIdInApplication(visit.getHospitalVisitId(), deleteId)
                .orElseThrow();
        Assertions.assertEquals(preDeleteObservation.getValueAsText(), audit.getValueAsText());
    }


    /**
     * Message updated time is before the validFrom, so no delete should happen
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    @Sql("/populate_db.sql")
    void testOldDeleteDoesNothing() throws EmapOperationMessageProcessingException {
        HospitalVisit visit = hospitalVisitRepository.findByEncounter(defaultEncounter).orElseThrow(NullPointerException::new);
        VisitObservation preDeleteObservation = visitObservationRepository
                .findByHospitalVisitIdAndVisitObservationTypeIdIdInApplication(visit, deleteId)
                .orElseThrow();

        for (Flowsheet msg : messages) {
            msg.setUpdatedTime(past);
            processSingleMessage(msg);
        }

        VisitObservation notDeletedObservation = visitObservationRepository
                .findByHospitalVisitIdAndVisitObservationTypeIdIdInApplication(visit, deleteId)
                .orElseThrow();

        Assertions.assertEquals(preDeleteObservation.getValueAsReal(), notDeletedObservation.getValueAsReal());
    }
}
