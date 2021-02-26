package uk.ac.ucl.rits.inform.datasinks.emapstar;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.IncompatibleDatabaseStateException;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.HospitalVisitRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabBatteryElementRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabBatteryRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabCollectionRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabIsolateRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabNumberRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabOrderRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabResultAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabResultRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabSensitivityRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabTestDefinitionRepository;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.informdb.identity.MrnToLive;
import uk.ac.ucl.rits.inform.informdb.labs.LabBatteryElement;
import uk.ac.ucl.rits.inform.informdb.labs.LabCollection;
import uk.ac.ucl.rits.inform.informdb.labs.LabIsolate;
import uk.ac.ucl.rits.inform.informdb.labs.LabNumber;
import uk.ac.ucl.rits.inform.informdb.labs.LabOrder;
import uk.ac.ucl.rits.inform.informdb.labs.LabResult;
import uk.ac.ucl.rits.inform.informdb.labs.LabSensitivity;
import uk.ac.ucl.rits.inform.informdb.labs.LabTestDefinition;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.OrderCodingSystem;
import uk.ac.ucl.rits.inform.interchange.lab.LabIsolateMsg;
import uk.ac.ucl.rits.inform.interchange.lab.LabOrderMsg;
import uk.ac.ucl.rits.inform.interchange.lab.LabResultMsg;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    LabBatteryRepository labBatteryRepository;
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
    LabIsolateRepository labIsolateRepository;
    @Autowired
    LabSensitivityRepository labSensitivityRepository;

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
        assertEquals(1, labBatteryRepository.count(), "lab battery should have been created");
        assertEquals(1, labOrderRepository.count(), "lab order should have been created");
        assertEquals(1, labCollectionRepository.count(), "lab collection should have been created");
        assertEquals(4, labTestDefinitionRepository.count(), "labTestDefinitions should have been created");
        assertEquals(4, labBatteryElementRepository.count(), "lab battery elements type should have been created");
        assertEquals(4, labResultRepository.count(), "lab results should have been created");
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
    void testChangeOfSourceSystemThrowsException() throws EmapOperationMessageProcessingException {
        // original message
        processSingleMessage(singleResult);
        // send again with another source
        singleResult.setSourceSystem("another source");

        assertThrows(IncompatibleDatabaseStateException.class, () -> processSingleMessage(singleResult));
    }

    @Test
    void testHappyPathLabTestDefinition() throws EmapOperationMessageProcessingException {
        processSingleMessage(singleResult);
        LabTestDefinition result = labTestDefinitionRepository.findByTestLabCode(singleResultTestCode).orElseThrow();
        assertEquals(OrderCodingSystem.WIN_PATH.name(), result.getLabProvider());
        assertEquals("CC", result.getLabDepartment());
    }

    @Test
    void testHappyPathLabBatteryElement() throws EmapOperationMessageProcessingException {
        processSingleMessage(singleResult);
        LabBatteryElement result = labBatteryElementRepository.findByLabTestDefinitionIdTestLabCode(singleResultTestCode).orElseThrow();
        assertEquals("IRON", result.getLabBatteryId().getBatteryCode());
    }

    @Test
    void testHappyPathLabOrder() throws EmapOperationMessageProcessingException {
        processSingleMessage(singleResult);
        LabOrder result = labOrderRepository.findByLabNumberIdInternalLabNumber(singleResultLabNumber).orElseThrow();
        assertEquals(statusChangeTime, result.getRequestDatetime());
        assertNull(result.getOrderDatetime());
        assertNull(result.getSampleDatetime());
    }

    @Test
    void testLabOrderClinicalInformation() throws EmapOperationMessageProcessingException {
        LabOrderMsg msg = singleResult;
        String clinicalInfo = "Pre-surgery bloods";
        msg.setClinicalInformation(InterchangeValue.buildFromHl7(clinicalInfo));
        processSingleMessage(msg);

        LabOrder result = labOrderRepository.findByLabNumberIdInternalLabNumber(singleResultLabNumber).orElseThrow();
        assertEquals(clinicalInfo, result.getClinicalInformation());
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
        String sampleType = "Tissue";
        String sampleSite = "Right Kidney";
        Instant collectTime = Instant.parse("2013-07-24T15:41:00Z");

        LabOrderMsg msg = singleResult;
        msg.setSpecimenType(InterchangeValue.buildFromHl7(sampleType));
        msg.setSampleSite(InterchangeValue.buildFromHl7(sampleSite));
        msg.setCollectionDateTime(collectTime);
        msg.setSampleReceivedTime(InterchangeValue.buildFromHl7(collectTime));
        //process message
        processSingleMessage(msg);
        // check results correct
        LabCollection collection = labCollectionRepository.findByLabNumberIdInternalLabNumber(singleResultLabNumber).orElseThrow();
        assertEquals(sampleType, collection.getSpecimenType());
        assertEquals(sampleSite, collection.getSampleSite());
        assertEquals(collectTime, collection.getSampleCollectionTime());
        assertEquals(collectTime, collection.getReceiptAtLab());
    }

    void processWithChangedSampleInformation(String initialValue, boolean changeSpecimenType) throws EmapOperationMessageProcessingException {
        LabOrderMsg msg = singleResult;
        msg.setSpecimenType(InterchangeValue.buildFromHl7(initialValue));
        msg.setSampleSite(InterchangeValue.buildFromHl7(initialValue));
        //process message
        processSingleMessage(msg);
        if (changeSpecimenType) {
            msg.setSpecimenType(InterchangeValue.buildFromHl7(String.format("%s2", initialValue)));
        } else {
            msg.setSampleSite(InterchangeValue.buildFromHl7(String.format("%s2", initialValue)));
        }
        processSingleMessage(msg);
    }

    /**
     * Ensure that unchangeable fields should throw exception if changed
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @TestFactory
    Iterable<DynamicTest> testLabCollectionThrowsExceptionWhenSampleInfoChanged() throws EmapOperationMessageProcessingException {
        return Arrays.asList(
                DynamicTest.dynamicTest(
                        "SpecimenType", () -> {
                            assertThrows(IncompatibleDatabaseStateException.class, () -> processWithChangedSampleInformation("initial", true));
                        }
                ),
                DynamicTest.dynamicTest(
                        "SampleSite", () -> {
                            assertThrows(IncompatibleDatabaseStateException.class, () -> processWithChangedSampleInformation("initial", false));
                        }
                )
        );
    }

    /**
     * Ensure that unknown unchangeable fields are updated
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @TestFactory
    Iterable<DynamicTest> testLabCollectionCanBeUpdatedIfPreviouslyUnknown() throws EmapOperationMessageProcessingException {
        return Arrays.asList(
                DynamicTest.dynamicTest(
                        "SpecimenType", () -> {
                            processWithChangedSampleInformation("", true);
                            LabCollection collection = labCollectionRepository
                                    .findByLabNumberIdInternalLabNumber(singleResultLabNumber).orElseThrow();
                            assertEquals("2", collection.getSpecimenType());
                        }
                ),
                DynamicTest.dynamicTest(
                        "SampleSite", () -> {
                            processWithChangedSampleInformation("", false);
                            LabCollection collection = labCollectionRepository
                                    .findByLabNumberIdInternalLabNumber(singleResultLabNumber).orElseThrow();
                            assertEquals("2", collection.getSampleSite());
                        }
                )
        );
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
        assertEquals(receivedTime, collection.getReceiptAtLab());
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
     * Processing of isolate table
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    void testLabIsolate() throws EmapOperationMessageProcessingException {
        String isolateCode = "CANALB";
        String isolateName = "Candida albicans";
        String cfu = "10,000 - 100,000 CFU/mL";
        String clinicalInformation = "Some clinical info";
        String cultureType = "Direct";
        LabIsolateMsg labIsolateMsg = new LabIsolateMsg();
        labIsolateMsg.setIsolateCode(isolateCode);
        labIsolateMsg.setIsolateName(isolateName);
        labIsolateMsg.setClinicalInformation(InterchangeValue.buildFromHl7(clinicalInformation));
        labIsolateMsg.setQuantity(InterchangeValue.buildFromHl7(cfu));
        labIsolateMsg.setCultureType(InterchangeValue.buildFromHl7(cultureType));

        LabOrderMsg msg = singleResult;
        LabResultMsg resultMsg = msg.getLabResultMsgs().get(0);
        resultMsg.setValueType("ST");
        resultMsg.setLabIsolate(labIsolateMsg);

        processSingleMessage(msg);

        LabIsolate isolate = labIsolateRepository.findByIsolateCode(isolateCode).orElseThrow();
        assertEquals(isolateName, isolate.getIsolateName());
        assertEquals(cfu, isolate.getQuantity());
        assertEquals(clinicalInformation, isolate.getClinicalInformation());
        assertEquals(cultureType, isolate.getCultureType());
    }

    /**
     * Two isolates should have been parsed./
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    void testMultipleIsolates() throws EmapOperationMessageProcessingException {
        LabOrderMsg msg = messageFactory.getLabOrders("winpath/sensitivity.yaml", "0000040").get(0);
        processSingleMessage(msg);

        List<LabIsolate> isolates = StreamSupport
                .stream(labIsolateRepository.findAll().spliterator(), false)
                .collect(Collectors.toList());
        assertEquals(2, isolates.size());
    }

    /**
     * An isolate has clinical information in it's lab result sensitivities, this should be the comment of the lab result for the isolate.
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    void testIsolateClinicalInformation() throws EmapOperationMessageProcessingException {
        LabOrderMsg msg = messageFactory.getLabOrders("winpath/sensitivity.yaml", "0000040").get(0);
        processSingleMessage(msg);
        LabIsolate isolate = labIsolateRepository.findByIsolateCode("KLEOXY").orElseThrow();
        assertEquals("Gentamicin resistant", isolate.getClinicalInformation());
    }

    /**
     * Two isolates with 5 sensitivities tested each, 10 sensitivities should be created.
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    void testLabSensitivitiesCreated() throws EmapOperationMessageProcessingException {
        LabOrderMsg msg = messageFactory.getLabOrders("winpath/sensitivity.yaml", "0000040").get(0);
        processSingleMessage(msg);

        List<LabSensitivity> sensitivities = StreamSupport
                .stream(labSensitivityRepository.findAll().spliterator(), false)
                .collect(Collectors.toList());
        assertEquals(10, sensitivities.size());
    }

    /**
     * Check the values for a result is as expected.
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    void testLabSensitivityValuesAdded() throws EmapOperationMessageProcessingException {
        LabOrderMsg msg = messageFactory.getLabOrders("winpath/sensitivity.yaml", "0000040").get(0);
        msg.setStatusChangeTime(statusChangeTime);
        processSingleMessage(msg);

        LabSensitivity sens = labSensitivityRepository
                .findByLabIsolateIdIsolateCodeAndAgent("KLEOXY", "VAK")
                .orElseThrow();

        assertEquals(statusChangeTime, sens.getReportingDatetime());
        assertEquals("S", sens.getSensitivity());
    }

    /**
     * Check the sensitivity changes when it is updated, and so does the reported time.
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    void testLabSensitivityChangedSensitivity() throws EmapOperationMessageProcessingException {
        LabOrderMsg msg = messageFactory.getLabOrders("winpath/sensitivity.yaml", "0000040").get(0);
        // original message
        msg.setStatusChangeTime(statusChangeTime);
        LabResultMsg result = msg.getLabResultMsgs().stream()
                .filter(r -> r.getLabIsolate() != null)
                .findFirst().orElseThrow();
        result.setAbnormalFlag(InterchangeValue.buildFromHl7("S"));
        msg.setLabResultMsgs(List.of(result));
        processSingleMessage(msg);

        // new message with later time and updated sensitivity
        Instant laterTime = statusChangeTime.plus(1, ChronoUnit.HOURS);
        msg.setStatusChangeTime(laterTime);
        String laterSensitivity = "R";
        LabIsolateMsg isolate = msg.getLabResultMsgs().get(0).getLabIsolate();
        for (LabResultMsg sensResult : isolate.getSensitivities()) {
            sensResult.setAbnormalFlag(InterchangeValue.buildFromHl7(laterSensitivity));
        }
        msg.setLabResultMsgs(List.of(result));
        processSingleMessage(msg);

        LabSensitivity sens = labSensitivityRepository
                .findByLabIsolateIdIsolateCodeAndAgent("KLEOXY", "VAK")
                .orElseThrow();

        assertEquals(laterTime, sens.getReportingDatetime());
        assertEquals(laterSensitivity, sens.getSensitivity());
    }

    /**
     * Check the sensitivity changes when it is updated, and so does the reported time.
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    void testLabSensitivityWithNoAgentIsSkipped() throws EmapOperationMessageProcessingException {
        LabOrderMsg msg = messageFactory.getLabOrders("winpath/sensitivity.yaml", "0000040").get(0);
        msg.setStatusChangeTime(statusChangeTime);
        LabResultMsg resultMsg = msg.getLabResultMsgs().stream()
                .filter(r -> r.getLabIsolate() != null)
                .findFirst().orElseThrow();
        LabResultMsg sensitivityResult = resultMsg.getLabIsolate()
                .getSensitivities().stream()
                .findFirst().orElseThrow();
        sensitivityResult.setStringValue(InterchangeValue.unknown());
        resultMsg.getLabIsolate().setSensitivities(List.of(sensitivityResult));
        msg.setLabResultMsgs(List.of(resultMsg));
        processSingleMessage(msg);

        assertEquals(0, labSensitivityRepository.count(), "No Lab sensitivity should be added");
    }


    /**
     * Only the status change time has changed on a sensitivity, should not update the reporting date time.
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    void testLabSensitivityWithOnlyLaterTime() throws EmapOperationMessageProcessingException {
        LabOrderMsg msg = messageFactory.getLabOrders("winpath/sensitivity.yaml", "0000040").get(0);
        // original message
        msg.setStatusChangeTime(statusChangeTime);
        LabResultMsg result = msg.getLabResultMsgs().stream()
                .filter(r -> r.getLabIsolate() != null)
                .findFirst().orElseThrow();
        msg.setLabResultMsgs(List.of(result));
        processSingleMessage(msg);

        // new message with only later time
        Instant laterTime = statusChangeTime.plus(1, ChronoUnit.HOURS);
        msg.setStatusChangeTime(laterTime);
        processSingleMessage(msg);

        LabSensitivity sens = labSensitivityRepository
                .findByLabIsolateIdIsolateCodeAndAgent("KLEOXY", "VAK")
                .orElseThrow();

        assertEquals(statusChangeTime, sens.getReportingDatetime());
    }
}
