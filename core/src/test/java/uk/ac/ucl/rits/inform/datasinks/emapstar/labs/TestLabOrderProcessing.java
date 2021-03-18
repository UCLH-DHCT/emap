package uk.ac.ucl.rits.inform.datasinks.emapstar.labs;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ucl.rits.inform.datasinks.emapstar.MessageProcessingBase;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.IncompatibleDatabaseStateException;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.RequiredDataMissingException;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.HospitalVisitRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabBatteryElementRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabBatteryRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabIsolateRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabOrderAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabOrderRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabResultAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabResultRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabSampleRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabSensitivityRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabTestDefinitionRepository;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.informdb.identity.MrnToLive;
import uk.ac.ucl.rits.inform.informdb.labs.LabBattery;
import uk.ac.ucl.rits.inform.informdb.labs.LabOrder;
import uk.ac.ucl.rits.inform.informdb.labs.LabSample;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.OrderCodingSystem;
import uk.ac.ucl.rits.inform.interchange.lab.LabOrderMsg;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestLabOrderProcessing extends MessageProcessingBase {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private LabOrderMsg fourResults;
    private LabOrderMsg singleResult;

    @Autowired
    private HospitalVisitRepository hospitalVisitRepository;
    @Autowired
    LabBatteryRepository labBatteryRepository;
    @Autowired
    LabBatteryElementRepository labBatteryElementRepository;
    @Autowired
    LabOrderRepository labOrderRepository;
    @Autowired
    LabOrderAuditRepository labOrderAuditRepository;
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

    private final String singleResultLabNumber = "13U444444";
    private final Instant statusChangeTime = Instant.parse("2013-07-24T16:46:00Z");
    private final Instant now = Instant.now();
    private final Instant past = Instant.parse("2001-01-01T00:00:00Z");

    public TestLabOrderProcessing() {
        List<LabOrderMsg> messages = messageFactory.getLabOrders("winpath/ORU_R01.yaml", "0000040");
        fourResults = messages.get(0);
        singleResult = messages.get(1);
    }

    private void checkFirstMessageLabEntityCount() {
        assertEquals(2, labBatteryRepository.count(), "lab battery should have been created");
        assertEquals(1, labOrderRepository.count(), "lab order should have been created");
        assertEquals(1, labSampleRepository.count(), "lab collection should have been created");
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
        // then lab order:
        checkFirstMessageLabEntityCount();
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
        labOrderRepository.findAll().forEach(lo -> assertNull(lo.getHospitalVisitId()));
    }

    @Test
    void testNoStatusChangeTimeThrows() {
        singleResult.setStatusChangeTime(null);
        assertThrows(RequiredDataMissingException.class, () -> processSingleMessage(singleResult));
    }

    @Test
    void testHappyPathLabOrder() throws EmapOperationMessageProcessingException {
        processSingleMessage(singleResult);
        LabOrder result = labOrderRepository.findByLabSampleIdExternalLabNumber(singleResultLabNumber).orElseThrow();
        assertEquals(statusChangeTime, result.getRequestDatetime());
        assertEquals("Corepoint", result.getSourceSystem());
        assertNull(result.getOrderDatetime());
    }

    @Test
    void testLabOrderClinicalInformation() throws EmapOperationMessageProcessingException {
        LabOrderMsg msg = singleResult;
        String clinicalInfo = "Pre-surgery bloods";
        msg.setClinicalInformation(InterchangeValue.buildFromHl7(clinicalInfo));
        processSingleMessage(msg);

        LabOrder result = labOrderRepository.findByLabSampleIdExternalLabNumber(singleResultLabNumber).orElseThrow();
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
        LabOrder result = labOrderRepository.findByLabSampleIdExternalLabNumber(singleResultLabNumber).orElseThrow();
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
        LabOrder result = labOrderRepository.findByLabSampleIdExternalLabNumber(singleResultLabNumber).orElseThrow();
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

        LabOrder result = labOrderRepository.findByLabSampleIdExternalLabNumber(singleResultLabNumber).orElseThrow();
        // check that already set value hasn't updated
        assertEquals(statusChangeTime, result.getRequestDatetime());
        // check time has updated for previously null fields
        assertEquals(earlierTime, result.getOrderDatetime());
    }

    /**
     * Creation of lab Sample row.
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    void testLabSampleDataCorrect() throws EmapOperationMessageProcessingException {
        // set relevant values
        String sampleType = "Tissue";
        String sampleSite = "Right Kidney";
        String collectionMethod = "Huge needle";
        Instant collectTime = Instant.parse("2013-07-24T15:41:00Z");

        LabOrderMsg msg = singleResult;
        msg.setSpecimenType(InterchangeValue.buildFromHl7(sampleType));
        msg.setSampleSite(InterchangeValue.buildFromHl7(sampleSite));
        msg.setCollectionMethod(InterchangeValue.buildFromHl7(collectionMethod));
        msg.setCollectionDateTime(collectTime);
        msg.setSampleReceivedTime(InterchangeValue.buildFromHl7(collectTime));
        //process message
        processSingleMessage(msg);
        // check results correct
        LabSample labSample = labSampleRepository.findByExternalLabNumber(singleResultLabNumber).orElseThrow();
        assertEquals(defaultMrn, labSample.getMrnId().getMrn());
        assertEquals(sampleType, labSample.getSpecimenType());
        assertEquals(sampleSite, labSample.getSampleSite());
        assertEquals(collectionMethod, labSample.getCollectionMethod());
        assertEquals(collectTime, labSample.getSampleCollectionTime());
        assertEquals(collectTime, labSample.getReceiptAtLab());
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
     * Ensure that unknown unchangeable fields are updated
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    void testLabSampleSiteCanBeUpdatedIfPreviouslyUnknown() throws EmapOperationMessageProcessingException {
        processWithChangedSampleInformation("", false);
        LabSample collection = labSampleRepository
                .findByExternalLabNumber(singleResultLabNumber).orElseThrow();
        assertEquals("2", collection.getSampleSite());
    }

    /**
     * First message has a collection time but no receipt, second message has the collection time and receipt time.
     * Entity should be updated to have the receipt time.
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    void testLabSampleUpdatesReceiptTime() throws EmapOperationMessageProcessingException {
        // process initial result
        processSingleMessage(singleResult);

        // add received time to be updated
        Instant receivedTime = statusChangeTime.plus(1, ChronoUnit.MINUTES);
        LabOrderMsg msg = singleResult;
        msg.setSampleReceivedTime(InterchangeValue.buildFromHl7(receivedTime));
        processSingleMessage(msg);

        LabSample labSample = labSampleRepository.findByExternalLabNumber(singleResultLabNumber).orElseThrow();
        assertEquals(receivedTime, labSample.getReceiptAtLab());
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

        LabSample labSample = labSampleRepository.findByExternalLabNumber(singleResultLabNumber).orElseThrow();
        assertEquals(laterTime, labSample.getSampleCollectionTime());
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

        LabSample labSample = labSampleRepository.findByExternalLabNumber(singleResultLabNumber).orElseThrow();
        assertNotEquals(laterTime, labSample.getSampleCollectionTime());
    }

    @Test
    void testIncrementalOrdersThenResult() throws EmapOperationMessageProcessingException {
        String interchangePathTemplate = "winpath/incremental_orders/%s.yaml";
        String interchangeDefaults = String.format(interchangePathTemplate, "orm_defaults");

        String[] orderFiles = {"01_orm_o01_nw", "02_orm_o01_sc_mg", "03_orm_o01_sn_telh", "04_orr_o02_telh"};
        for (String orderFile : orderFiles) {
            logger.info("Parsing order file: {}", orderFile);
            processSingleMessage(messageFactory.buildLabOrderOverridingDefaults(
                    interchangeDefaults, String.format(interchangePathTemplate, orderFile)));
        }
        for (LabOrderMsg resultMsg : messageFactory.getLabOrders(String.format(interchangePathTemplate, "05_oru_r01"), "0000000042")) {
            logger.info("Parsing ORU R01 for epic Id: {}", resultMsg.getEpicCareOrderNumber());
            processSingleMessage(resultMsg);
        }
        LabSample labSample = labSampleRepository.findByExternalLabNumber("13U444444").orElseThrow();
        assertEquals("BLD", labSample.getSpecimenType()); // from 01 NW
        assertEquals(Instant.parse("2013-07-28T07:27:00Z"), labSample.getSampleCollectionTime()); // from 01 NW
        assertEquals(Instant.parse("2013-07-28T08:45:00Z"), labSample.getReceiptAtLab()); // from 02 SC

        LabOrder originalOrder = labOrderRepository.findByLabBatteryIdBatteryCodeAndLabSampleId("MG", labSample).orElseThrow();
        assertEquals(Instant.parse("2013-07-28T07:08:06Z"), originalOrder.getOrderDatetime()); // from 01 NW
        assertEquals("unwell", originalOrder.getClinicalInformation()); // from 01 NW onwards
        assertEquals("91393667", originalOrder.getInternalLabNumber()); // from 01 NW onwards
        assertEquals("WinPath", originalOrder.getSourceSystem()); // from 02 SC onwards
        assertEquals(Instant.parse("2013-07-28T08:45:00Z"), originalOrder.getRequestDatetime()); // updated in 05 ORU R01

        LabOrder laterOrder = labOrderRepository.findByLabBatteryIdBatteryCodeAndLabSampleId("TELH", labSample).orElseThrow();
        assertEquals(Instant.parse("2013-07-28T08:55:00Z"), laterOrder.getOrderDatetime()); // from 03 SN
        assertEquals("WinPath", laterOrder.getSourceSystem()); // from 03 SN onwards
        assertEquals("12121212", laterOrder.getInternalLabNumber()); // from 04 ORR o02
        assertNotNull(laterOrder.getHospitalVisitId()); // from 04 ORR o02
        assertEquals(Instant.parse("2013-07-28T08:45:00Z"), laterOrder.getRequestDatetime()); // updated in 05 ORU R01
        assertEquals("unwell", laterOrder.getClinicalInformation()); // from 05 ORU R01
    }

    /**
     * Ensure that using original valid from when updating lab sample information.
     * Otherwise an update of a null/new field can cause the later information not to update.
     */
    @Test
    void testLabSampleValidFromWorkingCorrectly() throws EmapOperationMessageProcessingException {
        processSingleMessage(singleResult);
        Instant future = singleResult.getStatusChangeTime().plus(1, ChronoUnit.HOURS);

        singleResult.setCollectionDateTime(future);
        singleResult.setSampleReceivedTime(InterchangeValue.buildFromHl7(future));
        singleResult.setStatusChangeTime(future);
        processSingleMessage(singleResult);

        LabSample labSample = labSampleRepository.findByExternalLabNumber(singleResultLabNumber).orElseThrow();

        assertEquals(future, labSample.getSampleCollectionTime());
        assertEquals(future, labSample.getReceiptAtLab());
    }

    /**
     * Ensure that using original valid from when updating lab order information.
     * Otherwise an update of a null/new field can cause the later information not to update.
     */
    @Test
    void testLabOrderValidFromWorkingCorrectly() throws EmapOperationMessageProcessingException {
        processSingleMessage(singleResult);
        Instant future = singleResult.getStatusChangeTime().plus(1, ChronoUnit.HOURS);
        String newValue = "I'm new";
        singleResult.setStatusChangeTime(future);
        singleResult.setOrderDateTime(InterchangeValue.buildFromHl7(future));
        singleResult.setClinicalInformation(InterchangeValue.buildFromHl7(newValue));
        singleResult.setSourceSystem(newValue);
        processSingleMessage(singleResult);

        LabOrder labOrder = labOrderRepository.findByLabSampleIdExternalLabNumber(singleResultLabNumber).orElseThrow();

        assertEquals(future, labOrder.getOrderDatetime());
        assertEquals(newValue, labOrder.getClinicalInformation());
        assertEquals(newValue, labOrder.getSourceSystem());
    }

    /**
     * Cancelling an order that doesn't exist should create battery and sample but no order.
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    void testCancelOrderWithoutKnownOrder() throws EmapOperationMessageProcessingException {
        LabOrderMsg cancelMsg = singleResult;
        // no results and delete epic care order number
        cancelMsg.setLabResultMsgs(List.of());
        cancelMsg.setEpicCareOrderNumber(InterchangeValue.delete());

        processSingleMessage(cancelMsg);

        assertEquals(2, labBatteryRepository.count(), "lab battery should have been created");
        assertEquals(0, labOrderRepository.count(), "lab order should not have been created");
        assertEquals(1, labSampleRepository.count(), "lab collection should have been created");
    }

    /**
     * Process order message and then cancel it.
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    void testCreateOrderAndCancel() throws EmapOperationMessageProcessingException {
        LabOrderMsg originalOrder = singleResult;
        originalOrder.setLabResultMsgs(List.of());
        processSingleMessage(originalOrder);

        LabOrderMsg cancelMsg = singleResult;
        // delete epic care order number
        cancelMsg.setEpicCareOrderNumber(InterchangeValue.delete());
        cancelMsg.setStatusChangeTime(originalOrder.getStatusChangeTime().plusSeconds(1));

        processSingleMessage(cancelMsg);

        assertEquals(2, labBatteryRepository.count(), "lab battery should have been created");
        assertEquals(0, labOrderRepository.count(), "lab order should not have been created");
        assertEquals(1, labOrderAuditRepository.count(), "lab audit order should have been created");
        assertEquals(1, labSampleRepository.count(), "lab collection should have been created");
    }

    /**
     * If the current order time is after the cancel message's, it should not be deleted.
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    void testCancelTimeIsBeforeOrderTime() throws EmapOperationMessageProcessingException {
        LabOrderMsg originalOrder = singleResult;
        originalOrder.setLabResultMsgs(List.of());
        processSingleMessage(originalOrder);

        LabOrderMsg cancelMsg = singleResult;
        // delete epic care order number
        cancelMsg.setEpicCareOrderNumber(InterchangeValue.delete());
        cancelMsg.setStatusChangeTime(originalOrder.getStatusChangeTime().minusSeconds(1));

        processSingleMessage(cancelMsg);

        assertEquals(2, labBatteryRepository.count(), "lab battery should have been created");
        assertEquals(1, labOrderRepository.count(), "lab order should not have been created");
        assertEquals(1, labSampleRepository.count(), "lab collection should have been created");
    }

    /**
     * Don't expect that you can cancel an order which has results, so exception should be thrown to notify us about this happening.
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    void testOrderWithResultsCancelledThrows() throws EmapOperationMessageProcessingException {
        processSingleMessage(singleResult);

        LabOrderMsg cancelMsg = singleResult;
        // no results and delete epic care order number
        cancelMsg.setLabResultMsgs(List.of());
        cancelMsg.setStatusChangeTime(singleResult.getStatusChangeTime().plusSeconds(60));
        cancelMsg.setEpicCareOrderNumber(InterchangeValue.deleteFromValue(singleResult.getEpicCareOrderNumber().get()));

        assertThrows(IncompatibleDatabaseStateException.class, () -> processSingleMessage(cancelMsg));
    }

    @Test
    void testCoPathLabBatteryAddedAtStart() {
        String coPath = OrderCodingSystem.CO_PATH.name();
        Optional<LabBattery> coPathBattery = labBatteryRepository.findByBatteryCodeAndLabProvider(coPath, coPath);
        assertTrue(coPathBattery.isPresent());
        assertFalse(coPathBattery.get().getBatteryName().isEmpty());
    }

}
