package uk.ac.ucl.rits.inform.datasinks.emapstar;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.HospitalVisitRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LabBatteryElementRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LabCollectionRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LabNumberRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LabOrderRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LabResultAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LabResultRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LabResultSensitivityRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LabTestDefinitionRepository;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.informdb.identity.MrnToLive;
import uk.ac.ucl.rits.inform.informdb.labs.LabBatteryElement;
import uk.ac.ucl.rits.inform.informdb.labs.LabCollection;
import uk.ac.ucl.rits.inform.informdb.labs.LabNumber;
import uk.ac.ucl.rits.inform.informdb.labs.LabOrder;
import uk.ac.ucl.rits.inform.informdb.labs.LabResult;
import uk.ac.ucl.rits.inform.informdb.labs.LabResultSensitivity;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class TestLabProcessing extends MessageProcessingBase {
    private LabOrderMsg fourResults;
    private LabOrderMsg singleResult;
    private List<LabOrderMsg> incremental;

    private final String singleResultMrn = "40800000";
    private final String singleResultLabNumber = "13U444444";
    private final String singleResultTestCode = "FE";
    private final Instant statusChangeTime = Instant.parse("2013-07-24T16:46:00Z");

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
    @Autowired
    LabCollectionRepository labCollectionRepository;
    @Autowired
    LabResultSensitivityRepository labResultSensitivityRepository;

    private final Instant now = Instant.now();
    private final Instant past = Instant.parse("2001-01-01T00:00:00Z");

    public TestLabProcessing() {
        List<LabOrderMsg> messages = messageFactory.getLabOrders("winpath/ORU_R01.yaml", "0000040");
        fourResults = messages.get(0);
        singleResult = messages.get(1);
        incremental = messageFactory.getLabOrders("winpath/incremental.yaml", null);
    }

    private List<LabResult> getAllLabResults() {
        return StreamSupport.stream(labResultRepository.findAll().spliterator(), false).collect(Collectors.toList());
    }

    private void checkFirstMessageLabEntityCount() {
        assertEquals(1, labNumberRepository.count(), "lab number should have been created");
        assertEquals(4, labTestDefinitionRepository.count(), "labTestDefinitions should have been created");
        assertEquals(4, labBatteryElementRepository.count(), "lab batteries type should have been created");
        assertEquals(4, labOrderRepository.count(), "lab order should have been created");
        assertEquals(4, labResultRepository.count(), "lab results should have been created");
        assertEquals(1, labCollectionRepository.count(), "lab collection should have been created");
    }

    /**
     * no existing data. rows should be created for: mrns, so new mrn, mrn_to_live, core_demographics, hospital visit
     */
    @Test
    void testCreateNew() throws EmapOperationMessageProcessingException {
        processSingleMessage(fourResults);

        List<Mrn> mrns = getAllMrns();
        assertEquals(1, mrns.size());
        assertEquals("Corepoint", mrns.get(0).getSourceSystem());

        MrnToLive mrnToLive = mrnToLiveRepo.getByMrnIdEquals(mrns.get(0));
        assertNotNull(mrnToLive);

        HospitalVisit visit = hospitalVisitRepository.findByEncounter(defaultEncounter).orElseThrow(NullPointerException::new);
        assertEquals("Corepoint", visit.getSourceSystem());
        assertNull(visit.getPatientClass());
        assertNull(visit.getArrivalMethod());
        assertNull(visit.getAdmissionTime());
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
            assertEquals(originalLab.getResultLastModifiedTime(), finaLab.getResultLastModifiedTime());
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
        assertNull(updatedResult.getAbnormalFlag());
        Double resultToBeReplaced = incremental.get(0).getLabResultMsgs().get(0).getNumericValue().get();
        assertNotEquals(resultToBeReplaced, updatedResult.getValueAsReal());
        assertEquals(12.7, updatedResult.getValueAsReal());
        // single result should have been changed, so one audit
        assertEquals(1, labResultAuditRepository.count());

        // extra results should be added to results under the same EPIC lab number
        List<LabResult> epicResults = labResultRepository.findAllByLabNumberIdExternalLabNumber("94000002");
        assertEquals(3, epicResults.size());
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
        assertEquals("H", updatedResult.getAbnormalFlag());
        assertEquals(15.7, updatedResult.getValueAsReal());
        // no results should have been changed
        assertEquals(0, labResultAuditRepository.count());
        // extra results should be added to results under the same EPIC lab number
        List<LabResult> epicResults = labResultRepository.findAllByLabNumberIdExternalLabNumber("94000002");
        assertEquals(3, epicResults.size());
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
        labNumberRepository.findAll().forEach(ln -> assertNull(ln.getHospitalVisitId()));
    }

    @Test
    void testHappyPathLabNumber() throws EmapOperationMessageProcessingException {
        processSingleMessage(singleResult);
        LabNumber result = labNumberRepository.findByMrnIdMrn(singleResultMrn).orElseThrow();
        assertEquals(singleResultLabNumber, result.getInternalLabNumber());
        assertEquals("12121213", result.getExternalLabNumber());
        assertEquals("Corepoint", result.getSourceSystem());
    }

    @Test
    void testHappyPathLabTestDefinition() throws EmapOperationMessageProcessingException {
        processSingleMessage(singleResult);
        LabTestDefinition result = labTestDefinitionRepository.findByTestLabCode(singleResultTestCode).orElseThrow();
        assertEquals("WinPath", result.getLabProvider());
        assertEquals("CC", result.getLabDepartment());
    }

    @Test
    void testHappyPathLabBatteryElement() throws EmapOperationMessageProcessingException {
        processSingleMessage(singleResult);
        LabBatteryElement result = labBatteryElementRepository.findByLabTestDefinitionIdTestLabCode(singleResultTestCode).orElseThrow();
        assertEquals("IRON", result.getBattery());
    }

    @Test
    void testHappyPathLabOrder() throws EmapOperationMessageProcessingException {
        processSingleMessage(singleResult);
        LabOrder result = labOrderRepository.findByLabNumberIdInternalLabNumber(singleResultLabNumber).orElseThrow();
        assertEquals(statusChangeTime, result.getRequestDatetime());
        assertNull(result.getOrderDatetime());
        assertNull(result.getSampleDatetime());
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
        Instant laterTime = statusChangeTime.plus(100, ChronoUnit.DAYS);
        msg.setStatusChangeTime(laterTime);
        msg.setRequestedDateTime(InterchangeValue.buildFromHl7(laterTime));
        // process new message
        processSingleMessage(msg);

        // check time has updated
        LabOrder result = labOrderRepository.findByLabNumberIdInternalLabNumber(singleResultLabNumber).orElseThrow();
        assertEquals(laterTime, result.getRequestDatetime());
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
        Instant laterTime = statusChangeTime.plus(100, ChronoUnit.DAYS);
        msg.setStatusChangeTime(statusChangeTime.minus(100, ChronoUnit.DAYS));
        msg.setRequestedDateTime(InterchangeValue.buildFromHl7(laterTime));
        // process new message
        processSingleMessage(msg);

        // check time has not updated
        LabOrder result = labOrderRepository.findByLabNumberIdInternalLabNumber(singleResultLabNumber).orElseThrow();
        assertEquals(statusChangeTime, result.getRequestDatetime());
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
        Instant earlierTime = statusChangeTime.minus(100, ChronoUnit.DAYS);
        msg.setStatusChangeTime(earlierTime);
        msg.setRequestedDateTime(InterchangeValue.buildFromHl7(earlierTime));
        msg.setSampleReceivedTime(InterchangeValue.buildFromHl7(earlierTime));
        msg.setOrderDateTime(InterchangeValue.buildFromHl7(earlierTime));
        // process new message
        processSingleMessage(msg);

        LabOrder result = labOrderRepository.findByLabNumberIdInternalLabNumber(singleResultLabNumber).orElseThrow();
        // check that already set value hasn't updated
        assertEquals(statusChangeTime, result.getRequestDatetime());
        // check time has updated for previously null fields
        assertEquals(earlierTime, result.getOrderDatetime());
        assertEquals(earlierTime, result.getSampleDatetime());
    }

    @Test
    void testHappyPathLabResultNumeric() throws EmapOperationMessageProcessingException {
        processSingleMessage(singleResult);
        LabResult result = labResultRepository.findByLabTestDefinitionIdTestLabCode(singleResultTestCode).orElseThrow();
        assertNull(result.getValueAsText());
        assertNull(result.getComment());
        assertEquals(21.6, result.getValueAsReal());
        assertEquals(6.6, result.getRangeLow());
        assertEquals(26.0, result.getRangeHigh());
        assertEquals("=", result.getResultOperator());
        assertEquals("umol/L", result.getUnits());
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
        assertEquals(resultValue, result.getValueAsText());
        assertEquals(notes, result.getComment());
        assertNull(result.getValueAsReal());
        assertNull(result.getRangeLow());
        assertNull(result.getRangeHigh());
        assertNull(result.getResultOperator());
        assertNull(result.getUnits());
    }

    /**
     * Creation of lab collection row.
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    void testLabCollectionDataCorrect() throws EmapOperationMessageProcessingException {
        // set relevant values
        String sampleType = "BLD";
        Instant collectTime = Instant.parse("2013-07-24T15:41:00Z");

        LabOrderMsg msg = singleResult;
        msg.setSpecimenType(sampleType);
        msg.setCollectionDateTime(collectTime);
        msg.setSampleReceivedTime(InterchangeValue.buildFromHl7(collectTime));
        //process message
        processSingleMessage(msg);
        // check results correct
        LabCollection collection = labCollectionRepository.findByLabNumberIdInternalLabNumber(singleResultLabNumber).orElseThrow();
        assertEquals(sampleType, collection.getSampleType());
        assertEquals(collectTime, collection.getSampleCollectionTime());
        assertEquals(collectTime, collection.getSampleReceiptTime());
    }

    /**
     * First message has a collection time but no receipt, second message has the collection time and receipt time.
     * Entity should be updated to have the receipt time.
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    void testLabCollectionUpdatesReceiptTime() throws EmapOperationMessageProcessingException {
        // process initial result
        processSingleMessage(singleResult);

        // add received time to be updated
        Instant receivedTime = statusChangeTime.plus(1, ChronoUnit.MINUTES);
        LabOrderMsg msg = singleResult;
        msg.setSampleReceivedTime(InterchangeValue.buildFromHl7(receivedTime));
        processSingleMessage(msg);

        LabCollection collection = labCollectionRepository.findByLabNumberIdInternalLabNumber(singleResultLabNumber).orElseThrow();
        assertEquals(receivedTime, collection.getSampleReceiptTime());
    }

    /**
     * Message with a later status change time can update the collection date time if it's different
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    void testNewSampleReceiptTimeUpdates() throws EmapOperationMessageProcessingException {
        // process initial result
        processSingleMessage(singleResult);

        // later status change and collection time
        Instant laterTime = statusChangeTime.plus(1, ChronoUnit.DAYS);
        LabOrderMsg msg = singleResult;
        msg.setStatusChangeTime(laterTime);
        msg.setCollectionDateTime(laterTime);
        processSingleMessage(msg);

        LabCollection collection = labCollectionRepository.findByLabNumberIdInternalLabNumber(singleResultLabNumber).orElseThrow();
        assertEquals(laterTime, collection.getSampleCollectionTime());
    }


    /**
     * Message with a earlier status change time won't updated the collection date time, even if that is later than the current time.
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    void testOldSampleReceiptTimeDoesntUpdate() throws EmapOperationMessageProcessingException {
        // process initial result
        processSingleMessage(singleResult);

        // later status change and collection time
        Instant earlierTime = statusChangeTime.minus(1, ChronoUnit.DAYS);
        Instant laterTime = statusChangeTime.plus(1, ChronoUnit.DAYS);
        LabOrderMsg msg = singleResult;
        msg.setStatusChangeTime(earlierTime);
        msg.setCollectionDateTime(laterTime);
        processSingleMessage(msg);

        LabCollection collection = labCollectionRepository.findByLabNumberIdInternalLabNumber(singleResultLabNumber).orElseThrow();
        assertNotEquals(laterTime, collection.getSampleCollectionTime());
    }

    /**
     * Processing of isolate should have the isolate type as the result, the cfu (string value in msg) as the unit
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    void testLabIsolateProcessingResults() throws EmapOperationMessageProcessingException {
        String isolate = "CANALB^Candida albicans";
        String cfu = "10,000 - 100,000 CFU/mL";
        LabOrderMsg msg = singleResult;
        LabResultMsg resultMsg = msg.getLabResultMsgs().get(0);
        resultMsg.setValueType("ST");
        resultMsg.setIsolateCodeAndText(isolate);
        resultMsg.setStringValue(InterchangeValue.buildFromHl7(cfu));

        processSingleMessage(msg);

        LabResult result = labResultRepository.findByLabTestDefinitionIdTestLabCode(singleResultTestCode).orElseThrow();
        assertEquals(isolate, result.getValueAsText());
        assertEquals(cfu, result.getUnits());
    }

    /**
     * Unlike other labs, a message for a single order can have multiple isolates.
     * 2 different microbes cultured, so should have two results which have a test definition of ISOLATE
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    void testMultipleIsolates() throws EmapOperationMessageProcessingException {
        LabOrderMsg msg = messageFactory.getLabOrders("winpath/sensitivity.yaml", "0000040").get(0);
        processSingleMessage(msg);

        List<LabResult> isolates = StreamSupport
                .stream(labResultRepository.findAll().spliterator(), false)
                .filter(lr -> "ISOLATE".equals(lr.getLabTestDefinitionId().getTestLabCode()))
                .collect(Collectors.toList());
        assertEquals(2, isolates.size());
    }

    /**
     * Two isolates with 5 sensitivities tested each, 10 sensitivities should be created.
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    void testLabSensitivitiesCreated() throws EmapOperationMessageProcessingException {
        LabOrderMsg msg = messageFactory.getLabOrders("winpath/sensitivity.yaml", "0000040").get(0);
        processSingleMessage(msg);

        List<LabResultSensitivity> sensitivities = StreamSupport
                .stream(labResultSensitivityRepository.findAll().spliterator(), false)
                .collect(Collectors.toList());
        assertEquals(10, sensitivities.size());

    }
}
