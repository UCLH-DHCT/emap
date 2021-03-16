package uk.ac.ucl.rits.inform.datasinks.emapstar.labs;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ucl.rits.inform.datasinks.emapstar.MessageProcessingBase;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.HospitalVisitRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabBatteryElementRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabBatteryRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabIsolateRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabOrderRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabResultAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabResultRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabSampleRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabSensitivityRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabTestDefinitionRepository;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.informdb.identity.MrnToLive;
import uk.ac.ucl.rits.inform.informdb.labs.LabBatteryElement;
import uk.ac.ucl.rits.inform.informdb.labs.LabIsolate;
import uk.ac.ucl.rits.inform.informdb.labs.LabResult;
import uk.ac.ucl.rits.inform.informdb.labs.LabSensitivity;
import uk.ac.ucl.rits.inform.informdb.labs.LabTestDefinition;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.OrderCodingSystem;
import uk.ac.ucl.rits.inform.interchange.ValueType;
import uk.ac.ucl.rits.inform.interchange.lab.LabIsolateMsg;
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

class TestLabResultProcessing extends MessageProcessingBase {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private LabOrderMsg fourResults;
    private LabOrderMsg singleResult;
    private List<LabOrderMsg> incremental;

    private final String singleResultTestCode = "FE";
    private final Instant statusChangeTime = Instant.parse("2013-07-24T16:46:00Z");

    @Autowired
    private HospitalVisitRepository hospitalVisitRepository;
    @Autowired
    LabBatteryRepository labBatteryRepository;
    @Autowired
    LabBatteryElementRepository labBatteryElementRepository;
    @Autowired
    LabOrderRepository labOrderRepository;
    @Autowired
    LabResultRepository labResultRepository;
    @Autowired
    LabResultAuditRepository labResultAuditRepository;
    @Autowired
    LabTestDefinitionRepository labTestDefinitionRepository;
    @Autowired
    LabSampleRepository labSampleRepository;
    @Autowired
    LabIsolateRepository labIsolateRepository;
    @Autowired
    LabSensitivityRepository labSensitivityRepository;

    private final Instant now = Instant.now();
    private final Instant past = Instant.parse("2001-01-01T00:00:00Z");

    public TestLabResultProcessing() {
        List<LabOrderMsg> messages = messageFactory.getLabOrders("winpath/ORU_R01.yaml", "0000040");
        fourResults = messages.get(0);
        singleResult = messages.get(1);
        incremental = messageFactory.getLabOrders("winpath/incremental.yaml", null);
    }

    private List<LabResult> getAllLabResults() {
        return StreamSupport.stream(labResultRepository.findAll().spliterator(), false).collect(Collectors.toList());
    }

    private void checkFirstMessageLabEntityCount() {
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
        // then lab results:
        checkFirstMessageLabEntityCount();
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
        List<LabResult> epicResults = labResultRepository.findAllByLabOrderIdInternalLabNumber("94000002");
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
        List<LabResult> epicResults = labResultRepository.findAllByLabOrderIdInternalLabNumber("94000002");
        assertEquals(3, epicResults.size());
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
        assertEquals(ValueType.NUMERIC.toString(), result.getMimeType());
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
        labResultMsg.setMimeType(ValueType.TEXT);
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
        assertEquals(ValueType.TEXT.toString(), result.getMimeType());
        assertNull(result.getValueAsReal());
        assertNull(result.getRangeLow());
        assertNull(result.getRangeHigh());
        assertNull(result.getResultOperator());
        assertNull(result.getUnits());
    }

    private LabOrderMsg addLabIsolateAtResultTime(
            String isolateCode, String isolateName, String cfu, String clinicalInformation, String cultureType, Instant resultTime) {
        LabIsolateMsg labIsolateMsg = new LabIsolateMsg();
        labIsolateMsg.setIsolateId("1");
        labIsolateMsg.setIsolateCode(isolateCode);
        labIsolateMsg.setIsolateName(isolateName);
        labIsolateMsg.setClinicalInformation(InterchangeValue.buildFromHl7(clinicalInformation));
        labIsolateMsg.setQuantity(InterchangeValue.buildFromHl7(cfu));
        labIsolateMsg.setCultureType(InterchangeValue.buildFromHl7(cultureType));

        LabOrderMsg msg = singleResult;
        msg.setStatusChangeTime(resultTime);
        LabResultMsg resultMsg = msg.getLabResultMsgs().get(0);
        resultMsg.setMimeType(ValueType.LAB_ISOLATE);
        resultMsg.setLabIsolate(labIsolateMsg);
        resultMsg.setResultTime(resultTime);
        return msg;
    }

    /**
     * Lab result from an isolate should have a mime type of lab isolate and value set
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    void testLabResultFromIsolate() throws EmapOperationMessageProcessingException {
        LabOrderMsg msg = addLabIsolateAtResultTime("CANALB", "Candida albicans", "10,000 - 100,000 CFU/mL", null, null, statusChangeTime);

        processSingleMessage(msg);
        LabResult result = labResultRepository.findByLabTestDefinitionIdTestLabCode(singleResultTestCode).orElseThrow();
        assertEquals(ValueType.LAB_ISOLATE.toString(), result.getMimeType());
        assertNull(result.getValueAsText());
        assertNull(result.getValueAsBytes());
        assertNull(result.getValueAsReal());
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
        LabOrderMsg msg = addLabIsolateAtResultTime(isolateCode, isolateName, cfu, clinicalInformation, cultureType, statusChangeTime);


        processSingleMessage(msg);
        LabResult result = labResultRepository.findByLabTestDefinitionIdTestLabCode(singleResultTestCode).orElseThrow();
        assertEquals(ValueType.LAB_ISOLATE.toString(), result.getMimeType());

        LabIsolate isolate = labIsolateRepository.findByIsolateCode(isolateCode).orElseThrow();
        assertEquals(isolateName, isolate.getIsolateName());
        assertEquals(cfu, isolate.getQuantity());
        assertEquals(clinicalInformation, isolate.getClinicalInformation());
        assertEquals(cultureType, isolate.getCultureType());
    }

    @Test
    void testIsolateUpdatesCodeAndName() throws EmapOperationMessageProcessingException {
        String finalIsolateCode = "NEISU";
        String finalIsolateName = "Neisseria subflava";
        Instant laterTime = statusChangeTime.plus(1, ChronoUnit.HOURS);

        processSingleMessage(addLabIsolateAtResultTime("NEISSP", "Neisseria species", "", "", "", statusChangeTime));
        processSingleMessage(addLabIsolateAtResultTime(finalIsolateCode, finalIsolateName, "", "", "", laterTime));

        assertEquals(1, labIsolateRepository.count());
        LabIsolate isolate = labIsolateRepository.findByIsolateCode(finalIsolateCode).orElseThrow();
        assertEquals(finalIsolateName, isolate.getIsolateName());
        // should update the isolate time, but also the result's updated time
        assertEquals(laterTime, isolate.getValidFrom());
        assertEquals(laterTime, isolate.getLabResultId().getResultLastModifiedTime());
    }

    @Test
    void testEarlierIsolateMessageDoesntUpdate() throws EmapOperationMessageProcessingException {
        String finalIsolateCode = "code";
        String finalIsolateName = "name";

        processSingleMessage(addLabIsolateAtResultTime(finalIsolateCode, finalIsolateName, "", "", "", statusChangeTime));
        processSingleMessage(addLabIsolateAtResultTime("NEISSP", "Neisseria species", "", "", "", statusChangeTime.minus(1, ChronoUnit.HOURS)));

        assertEquals(1, labIsolateRepository.count());
        LabIsolate isolate = labIsolateRepository.findByIsolateCode(finalIsolateCode).orElseThrow();
        assertEquals(finalIsolateName, isolate.getIsolateName());
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
        msg.getLabResultMsgs().forEach(r -> r.setResultTime(statusChangeTime));
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
        msg.getLabResultMsgs().forEach(r -> r.setResultTime(statusChangeTime));
        LabResultMsg result = msg.getLabResultMsgs().stream()
                .filter(r -> r.getLabIsolate() != null)
                .findFirst().orElseThrow();
        result.setAbnormalFlag(InterchangeValue.buildFromHl7("S"));
        msg.setLabResultMsgs(List.of(result));
        processSingleMessage(msg);

        // new message with later time and updated sensitivity
        Instant laterTime = statusChangeTime.plus(1, ChronoUnit.HOURS);
        msg.setStatusChangeTime(laterTime);
        msg.getLabResultMsgs().forEach(r -> r.setResultTime(laterTime));
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
        msg.getLabResultMsgs().forEach(r -> r.setResultTime(statusChangeTime));
        LabResultMsg result = msg.getLabResultMsgs().stream()
                .filter(r -> r.getLabIsolate() != null)
                .findFirst().orElseThrow();
        msg.setLabResultMsgs(List.of(result));
        processSingleMessage(msg);

        // new message with only later time
        Instant laterTime = statusChangeTime.plus(1, ChronoUnit.HOURS);
        msg.setStatusChangeTime(laterTime);
        msg.getLabResultMsgs().forEach(r -> r.setResultTime(laterTime));
        processSingleMessage(msg);

        LabSensitivity sens = labSensitivityRepository
                .findByLabIsolateIdIsolateCodeAndAgent("KLEOXY", "VAK")
                .orElseThrow();

        assertEquals(statusChangeTime, sens.getReportingDatetime());
    }

    /**
     * Incremental update of isolate with sensitivities.
     * Only the new sensitivity should have the new result time.
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    void testIncrementalSensitivity() throws EmapOperationMessageProcessingException {
        LabOrderMsg inc1 = messageFactory.getLabOrders("winpath/isolate_inc_1.yaml", "0000040").get(0);
        LabOrderMsg inc2 = messageFactory.getLabOrders("winpath/isolate_inc_2.yaml", "0000040").get(0);

        Instant firstResultTime = Instant.parse("2020-09-12T12:13:00Z");
        Instant secondResultTime = Instant.parse("2020-09-23T11:58:00Z");


        processSingleMessage(inc1);
        processSingleMessage(inc2);

        LabResult result = labResultRepository.findByLabTestDefinitionIdTestLabCode("ISOLATE").orElseThrow();
        LabIsolate updatedIsolate = labIsolateRepository.findByIsolateCode("STAHAE").orElseThrow();
        LabIsolate notUpdatedIsolate = labIsolateRepository.findByIsolateCode("ENTFAM").orElseThrow();

        LabSensitivity notUpdatedSensitivityFromNotUpdatedIsolate = labSensitivityRepository
                .findByLabIsolateIdAndAgent(notUpdatedIsolate, "VM").orElseThrow();
        LabSensitivity notUpdatedSensitivityFromUpdatedIsolate = labSensitivityRepository
                .findByLabIsolateIdAndAgent(updatedIsolate, "VDP").orElseThrow();
        LabSensitivity newSensitivityFromUpdatedIsolate = labSensitivityRepository
                .findByLabIsolateIdAndAgent(updatedIsolate, "VCI").orElseThrow();

        // new sensitivity
        assertEquals(secondResultTime, newSensitivityFromUpdatedIsolate.getReportingDatetime());

        // non-updated entities
        assertEquals(firstResultTime, result.getResultLastModifiedTime());
        assertEquals(firstResultTime, updatedIsolate.getValidFrom());
        assertEquals(firstResultTime, notUpdatedIsolate.getValidFrom());
        assertEquals(firstResultTime, notUpdatedSensitivityFromNotUpdatedIsolate.getReportingDatetime());
        assertEquals(firstResultTime, notUpdatedSensitivityFromUpdatedIsolate.getReportingDatetime());
        // phew
    }
}
