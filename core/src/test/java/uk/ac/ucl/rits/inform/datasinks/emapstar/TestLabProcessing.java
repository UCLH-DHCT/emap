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
import uk.ac.ucl.rits.inform.informdb.labs.LabBatteryElement;
import uk.ac.ucl.rits.inform.informdb.labs.LabNumber;
import uk.ac.ucl.rits.inform.informdb.labs.LabOrder;
import uk.ac.ucl.rits.inform.informdb.labs.LabResult;
import uk.ac.ucl.rits.inform.informdb.labs.LabTestDefinition;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.lab.LabOrderMsg;
import uk.ac.ucl.rits.inform.interchange.lab.LabResultMsg;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

class TestLabProcessing extends MessageProcessingBase {
    private LabOrderMsg fourResults;
    private LabOrderMsg singleResult;
    private List<LabOrderMsg> incremental;

    private final String singleResultMrn = "40800000";
    private final String singleResultLabNumber = "13U444444";
    private final String singleResultTestCode = "FE";
    private final Instant singleResultRequestTime = Instant.parse("2013-07-24T16:46:00Z");

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

    public TestLabProcessing() {
        List<LabOrderMsg> messages = messageFactory.getLabOrders("ORU_R01.yaml", "0000040");
        fourResults = messages.get(0);
        singleResult = messages.get(1);
        incremental = messageFactory.getLabOrders("incremental.yaml", null);
    }

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
        processSingleMessage(fourResults);

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
        LabOrderMsg msg = fourResults;
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
     * Incremental load should change result from fist message
     */
    @Test
    void testIncrementalLoad() throws EmapOperationMessageProcessingException {
        // process all messages
        for (LabOrderMsg msg : incremental) {
            processSingleMessage(msg);
        }

        LabResult updatedResult = labResultRepository.findByLabTestDefinitionIdTestLabCode("RDWU").orElseThrow();
        Assertions.assertNull(updatedResult.getAbnormalFlag());
        Double resultToBeReplaced = incremental.get(0).getLabResultMsgs().get(0).getNumericValue().get();
        Assertions.assertNotEquals(resultToBeReplaced, updatedResult.getResultAsReal());
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

        List<LabOrderMsg> messages = incremental;

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
        LabOrderMsg msg = fourResults;
        msg.setVisitNumber(null);
        processSingleMessage(msg);
        // all processed
        checkFirstMessageLabEntityCount();
        // lab number should not have a hospital visit
        labNumberRepository.findAll().forEach(ln -> Assertions.assertNull(ln.getHospitalVisitId()));
    }

    @Test
    void testHappyPathLabNumber() throws EmapOperationMessageProcessingException {
        processSingleMessage(singleResult);
        LabNumber result = labNumberRepository.findByMrnIdMrn(singleResultMrn).orElseThrow();
        Assertions.assertEquals(singleResultLabNumber, result.getInternalLabNumber());
        Assertions.assertEquals("1", result.getSpecimenType());
        Assertions.assertEquals("12121213", result.getExternalLabNumber());
        Assertions.assertEquals("Corepoint", result.getSourceSystem());
    }

    @Test
    void testHappyPathLabTestDefinition() throws EmapOperationMessageProcessingException {
        processSingleMessage(singleResult);
        LabTestDefinition result = labTestDefinitionRepository.findByTestLabCode(singleResultTestCode).orElseThrow();
        Assertions.assertEquals("WinPath", result.getLabProvider());
        Assertions.assertEquals("CC", result.getLabDepartment());
    }

    @Test
    void testHappyPathLabBatteryElement() throws EmapOperationMessageProcessingException {
        processSingleMessage(singleResult);
        LabBatteryElement result = labBatteryElementRepository.findByLabTestDefinitionIdTestLabCode(singleResultTestCode).orElseThrow();
        Assertions.assertEquals("IRON", result.getBattery());
    }

    @Test
    void testHappyPathLabOrder() throws EmapOperationMessageProcessingException {
        processSingleMessage(singleResult);
        LabOrder result = labOrderRepository.findByLabNumberIdInternalLabNumber(singleResultLabNumber).orElseThrow();
        Assertions.assertEquals(singleResultRequestTime, result.getRequestDatetime());
        Assertions.assertNull(result.getOrderDatetime());
        Assertions.assertNull(result.getSampleDatetime());
    }

    /**
     * Order with more recent status change time should update the temporal fields that are set (here request date time)
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    void testLabOrderUpdatesWithNewResults() throws EmapOperationMessageProcessingException {
        // first process original message
        processSingleMessage(singleResult);

        // set up message with later status change time and requested date
        LabOrderMsg msg = singleResult;
        Instant laterTime = singleResultRequestTime.plus(100, ChronoUnit.DAYS);
        msg.setStatusChangeTime(laterTime);
        msg.setRequestedDateTime(InterchangeValue.buildFromHl7(laterTime));
        // process new message
        processSingleMessage(msg);

        // check time has updated
        LabOrder result = labOrderRepository.findByLabNumberIdInternalLabNumber(singleResultLabNumber).orElseThrow();
        Assertions.assertEquals(laterTime, result.getRequestDatetime());
    }

    /**
     * Order with older status change time shouldn't update the temporal fields that are set (here request date time)
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    void testLabOrderFromThePastDoesntUpdateNonNullField() throws EmapOperationMessageProcessingException {
        // first process original message
        processSingleMessage(singleResult);

        // set up message with an earlier status change time and later requested date
        LabOrderMsg msg = singleResult;
        Instant laterTime = singleResultRequestTime.plus(100, ChronoUnit.DAYS);
        msg.setStatusChangeTime(singleResultRequestTime.minus(100, ChronoUnit.DAYS));
        msg.setRequestedDateTime(InterchangeValue.buildFromHl7(laterTime));
        // process new message
        processSingleMessage(msg);

        // check time has not updated
        LabOrder result = labOrderRepository.findByLabNumberIdInternalLabNumber(singleResultLabNumber).orElseThrow();
        Assertions.assertEquals(singleResultRequestTime, result.getRequestDatetime());
    }


    /**
     * Order with older recent status change time should update the temporal fields that are currently null
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    void testLabOrderFromThePastUpdatedNullTemporalData() throws EmapOperationMessageProcessingException {
        // first process original message
        processSingleMessage(singleResult);

        // set up message with later status change time and requested date
        LabOrderMsg msg = singleResult;
        Instant earlierTime = singleResultRequestTime.minus(100, ChronoUnit.DAYS);
        msg.setStatusChangeTime(earlierTime);
        msg.setRequestedDateTime(InterchangeValue.buildFromHl7(earlierTime));
        msg.setSampleEnteredTime(InterchangeValue.buildFromHl7(earlierTime));
        msg.setOrderDateTime(InterchangeValue.buildFromHl7(earlierTime));
        // process new message
        processSingleMessage(msg);

        LabOrder result = labOrderRepository.findByLabNumberIdInternalLabNumber(singleResultLabNumber).orElseThrow();
        // check that already set value hasn't updated
        Assertions.assertEquals(singleResultRequestTime, result.getRequestDatetime());
        // check time has updated for previously null fields
        Assertions.assertEquals(earlierTime, result.getOrderDatetime());
        Assertions.assertEquals(earlierTime, result.getSampleDatetime());
    }

    @Test
    void testHappyPathLabResultNumeric() throws EmapOperationMessageProcessingException {
        processSingleMessage(singleResult);
        LabResult result = labResultRepository.findByLabTestDefinitionIdTestLabCode(singleResultTestCode).orElseThrow();
        Assertions.assertNull(result.getResultAsText());
        Assertions.assertNull(result.getComment());
        Assertions.assertEquals(21.6, result.getResultAsReal());
        Assertions.assertEquals(6.6, result.getRangeLow());
        Assertions.assertEquals(26.0, result.getRangeHigh());
        Assertions.assertNull(result.getResultOperator());
        Assertions.assertEquals("umol/L", result.getUnits());
    }

    @Test
    void testHappyPathLabResultString() throws EmapOperationMessageProcessingException {
        // change result to message
        LabOrderMsg msg = singleResult;
        LabResultMsg labResultMsg = msg.getLabResultMsgs().get(0);
        labResultMsg.setNumericValue(InterchangeValue.unknown());
        labResultMsg.setReferenceLow(InterchangeValue.unknown());
        labResultMsg.setReferenceHigh(InterchangeValue.unknown());
        labResultMsg.setUnits(InterchangeValue.unknown());
        labResultMsg.setValueType("FT"); // string value
        String notes = "I am a note";
        String resultValue = "I am a result";
        labResultMsg.setNotes(InterchangeValue.buildFromHl7(notes));
        labResultMsg.setStringValue(InterchangeValue.buildFromHl7(resultValue));
        // process message
        processSingleMessage(msg);
        // test message
        LabResult result = labResultRepository.findByLabTestDefinitionIdTestLabCode(singleResultTestCode).orElseThrow();
        Assertions.assertEquals(resultValue, result.getResultAsText());
        Assertions.assertEquals(notes, result.getComment());
        Assertions.assertNull(result.getResultAsReal());
        Assertions.assertNull(result.getRangeLow());
        Assertions.assertNull(result.getRangeHigh());
        Assertions.assertNull(result.getResultOperator());
        Assertions.assertNull(result.getUnits());
    }

}
