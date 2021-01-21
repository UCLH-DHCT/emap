package uk.ac.ucl.rits.inform.datasinks.emapstar;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.HospitalVisitRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LabBatteryElementRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LabNumberRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LabOrderRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LabResultAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LabResultRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LabTestDefinitionRepository;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.informdb.identity.MrnToLive;
import uk.ac.ucl.rits.inform.informdb.labs.LabNumber;
import uk.ac.ucl.rits.inform.informdb.labs.LabResult;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.lab.LabOrderMsg;
import uk.ac.ucl.rits.inform.interchange.lab.LabResultMsg;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

class TestLabProcessing extends MessageProcessingBase {
    private List<LabOrderMsg> defaultORUR01s = messageFactory.getLabOrders("ORU_R01.yaml", "0000040");
    @Autowired
    private HospitalVisitRepository hospitalVisitRepository;
    @Autowired
    LabBatteryElementRepository labBatteryElementRepository;
    @Autowired
    LabNumberRepository labNumberRepository;
    @Autowired
    LabOrderRepository labOrderRepository;
    @Autowired
    LabResultRepository labResultRepository;
    @Autowired
    LabResultAuditRepository labResultAuditRepository;
    @Autowired
    LabTestDefinitionRepository labTestDefinitionRepository;
    private final Instant now = Instant.now();
    private final Instant past = Instant.parse("2001-01-01T00:00:00Z");

    private List<LabResult> getAllLabResults() {
        return StreamSupport.stream(labResultRepository.findAll().spliterator(), false).collect(Collectors.toList());
    }

    private void checkFirstMessageLabEntityCount() {
        Assertions.assertEquals(1, labNumberRepository.count(), "lab number should have been created");
        Assertions.assertEquals(4, labTestDefinitionRepository.count(), "labTestDefinitions should have been created");
        Assertions.assertEquals(4, labBatteryElementRepository.count(), "lab batteries type should have been created");
        Assertions.assertEquals(4, labOrderRepository.count(), "lab order should have been created");
        Assertions.assertEquals(4, labResultRepository.count(), "lab results should have been created");
        // will add lab collection when POCT is added
    }

    /**
     * no existing data. rows should be created for: mrns, so new mrn, mrn_to_live, core_demographics, hospital visit
     */
    @Test
    void testCreateNew() throws EmapOperationMessageProcessingException {
        processSingleMessage(defaultORUR01s.get(0));

        List<Mrn> mrns = getAllMrns();
        Assertions.assertEquals(1, mrns.size());
        Assertions.assertEquals("Corepoint", mrns.get(0).getSourceSystem());

        MrnToLive mrnToLive = mrnToLiveRepo.getByMrnIdEquals(mrns.get(0));
        Assertions.assertNotNull(mrnToLive);

        HospitalVisit visit = hospitalVisitRepository.findByEncounter(defaultEncounter).orElseThrow(NullPointerException::new);
        Assertions.assertEquals("Corepoint", visit.getSourceSystem());
        Assertions.assertNull(visit.getPatientClass());
        Assertions.assertNull(visit.getArrivalMethod());
        Assertions.assertNull(visit.getAdmissionTime());
        // then lab results:
        checkFirstMessageLabEntityCount();

        // -- for now skipping over, will also need to do the foreign keys for these when we get there
        // LabResultSensitivity
    }

    /**
     * Message sent twice, with later timestamp, shouldn't add in new entities.
     * Only results can be updated, so check that these haven't changed.
     */
    @Test
    void testDuplicateMessageWithSameData() throws EmapOperationMessageProcessingException {
        // process original message
        LabOrderMsg msg = defaultORUR01s.get(0);
        processSingleMessage(msg);
        List<LabResult> originalResults = getAllLabResults();
        // process duplicate message with updated times
        msg.setStatusChangeTime(now);
        for (LabResultMsg result : msg.getLabResultMsgs()) {
            result.setResultTime(now);
        }
        processSingleMessage(msg);

        List<LabResult> finalResults = getAllLabResults();
        checkFirstMessageLabEntityCount();
        for (int i = 0; i < finalResults.size(); i++) {
            LabResult originalLab = originalResults.get(i);
            LabResult finaLab = finalResults.get(i);
            Assertions.assertEquals(originalLab.getResultLastModifiedTime(), finaLab.getResultLastModifiedTime());
        }
    }

    /**
     * Message sent twice, with later timestamp, shouldn't add in new entities.
     * Only results can be updated, so check that these haven't changed.
     */
    @Test
    void testIncrementalLoad() throws EmapOperationMessageProcessingException {
        List<LabOrderMsg> messages = messageFactory.getLabOrders("incremental.yaml", "0000040");

        // process all other messages
        for (LabOrderMsg msg : messages) {
            processSingleMessage(msg);
        }

        LabResult updatedResult = labResultRepository.findByLabTestDefinitionIdTestLabCode("RDWU").orElseThrow();
        Assertions.assertNull(updatedResult.getAbnormalFlag());
        Assertions.assertEquals(12.7, updatedResult.getResultAsReal());
        // single result should have been changed, so one audit
        Assertions.assertEquals(1, labResultAuditRepository.count());

        // extra results should be added to results under the same EPIC lab number
        List<LabResult> epicResults = labResultRepository.findAllByLabNumberIdExternalLabNumber("94000002");
        Assertions.assertEquals(3, epicResults.size());
    }

    /**
     * First result is processed, then all results are set with earlier time.
     * First result message should not be updated by the subsequent results, but new results should be added
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    void testResultNotUpdatedIfEarlierThanDatabase() throws EmapOperationMessageProcessingException {

        List<LabOrderMsg> messages = messageFactory.getLabOrders("incremental.yaml", "0000040");

        // process first message which should not get updated by other incremental updated
        processSingleMessage(messages.get(0));

        // process all other messages, setting the time to be earlier than the first message time
        for (LabOrderMsg msg : messages) {
            for (LabResultMsg result : msg.getLabResultMsgs()) {
                result.setResultTime(past);
            }
            processSingleMessage(msg);
        }

        LabResult updatedResult = labResultRepository.findByLabTestDefinitionIdTestLabCode("RDWU").orElseThrow();
        Assertions.assertEquals("H", updatedResult.getAbnormalFlag());
        Assertions.assertEquals(15.7, updatedResult.getResultAsReal());
        // no results should have been changed
        Assertions.assertEquals(0, labResultAuditRepository.count());
        // extra results should be added to results under the same EPIC lab number
        List<LabResult> epicResults = labResultRepository.findAllByLabNumberIdExternalLabNumber("94000002");
        Assertions.assertEquals(3, epicResults.size());
    }

    /**
     * Processing a Labs result without an encounter should be processed to the end
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    void testEncounterNotRequired() throws EmapOperationMessageProcessingException {
        LabOrderMsg msg = defaultORUR01s.get(0);
        msg.setVisitNumber(null);
        processSingleMessage(msg);
        // all processed
        checkFirstMessageLabEntityCount();
        // lab number should not have a hospital visit
        labNumberRepository.findAll().forEach(ln -> Assertions.assertNull(ln.getHospitalVisitId()));
    }
}
