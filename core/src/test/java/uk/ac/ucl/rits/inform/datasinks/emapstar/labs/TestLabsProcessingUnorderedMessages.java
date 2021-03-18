package uk.ac.ucl.rits.inform.datasinks.emapstar.labs;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ucl.rits.inform.datasinks.emapstar.MessageStreamBaseCase;
import uk.ac.ucl.rits.inform.datasinks.emapstar.concurrent.ShuffleIterator;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabBatteryElementRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabBatteryRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabIsolateRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabOrderAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabSampleQuestionRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabOrderRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabResultAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabResultRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabSampleRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabSensitivityRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabTestDefinitionRepository;
import uk.ac.ucl.rits.inform.informdb.labs.LabOrder;
import uk.ac.ucl.rits.inform.informdb.labs.LabSample;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestLabsProcessingUnorderedMessages extends MessageStreamBaseCase {
    @Autowired
    private LabsPermutationTestProducer labsPermutationTestProducer;

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
    @Autowired
    LabSampleQuestionRepository labSampleQuestionRepository;

    private List<String> duplicateAt(String[] strings, int duplicateIndex) {
        List<String> output = new ArrayList<>(List.of(strings));
        output.add(strings[duplicateIndex]);
        return output;
    }

    /**
     * Incremental order stream with a final result set.
     * @return A stream of all the possible valid orderings.
     */
    @TestFactory
    Stream<DynamicTest> testWinPathIncrementalOrders() {
        String[] orderFiles = {"01_orm_o01_nw", "02_orm_o01_sc_mg", "03_orm_o01_sn_telh", "04_orr_o02_telh", "05_oru_r01"};
        labsPermutationTestProducer.setMessagePathAndORMDefaults("winpath/incremental_orders");
        labsPermutationTestProducer.setFinalStateChecker(this::checkWinPathIncrementalOrders);

        List<Iterable<List<String>>> duplicatedNames = new ArrayList<>();
        for (int i = 0; i < orderFiles.length; i++) {
            List<String> filesWithOneDuplicate = duplicateAt(orderFiles, i);
            duplicatedNames.add(new ShuffleIterator<>(filesWithOneDuplicate));
        }

        return duplicatedNames.stream()
                .flatMap(pi -> StreamSupport.stream(pi.spliterator(), false))
                .map(messageOrdering -> DynamicTest.dynamicTest(
                        String.format("Test %s", messageOrdering),
                        () -> labsPermutationTestProducer.buildTestFromPermutation(messageOrdering)));
    }

    private void checkWinPathIncrementalOrders() {
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

    @TestFactory
    Stream<DynamicTest> testWinPathOrderCancellation() {
        String[] orderFiles = {
                "01_orm_o01_nw_fbc_mg", "02_orm_o01_ca_fbc", "03_orm_o01_sn_fbcc", "05_orr_o02_na_fbcc", "06_oru_r01_fbcc"
        };
        // "04_orr_o02_cr_fbc" is essentially a duplicate message so have removed it after initial testing (doesn't change anything)
        // but does reduce number of messages from 7 to 6 and time from 3 minutes for this test to 30 seconds
        labsPermutationTestProducer.setMessagePathAndORMDefaults("winpath/cancel_orders");
        labsPermutationTestProducer.setFinalStateChecker(this::checkWinPathCancelOrders);

        List<Iterable<List<String>>> duplicatedNames = new ArrayList<>();
        for (int i = 0; i < orderFiles.length; i++) {
            List<String> filesWithOneDuplicate = duplicateAt(orderFiles, i);
            duplicatedNames.add(new ShuffleIterator<>(filesWithOneDuplicate));
        }

        return duplicatedNames.stream()
                .flatMap(pi -> StreamSupport.stream(pi.spliterator(), false))
                .map(messageOrdering -> DynamicTest.dynamicTest(
                        String.format("Test %s", messageOrdering),
                        () -> labsPermutationTestProducer.buildTestFromPermutation(messageOrdering)));
    }

    private void checkWinPathCancelOrders() {
        LabSample labSample = labSampleRepository.findByExternalLabNumber("13U444444").orElseThrow();
        assertEquals("BLD", labSample.getSpecimenType()); // from 01 NW
        assertEquals(Instant.parse("2013-07-28T07:27:00Z"), labSample.getSampleCollectionTime()); // from 01 NW
        assertNull(labSample.getReceiptAtLab()); // not in messages

        Optional<LabOrder> cancelledOrder = labOrderRepository.findByLabBatteryIdBatteryCodeAndLabSampleId("FBC", labSample);
        assertTrue(cancelledOrder.isEmpty());


        LabOrder labOrder = labOrderRepository.findByLabBatteryIdBatteryCodeAndLabSampleId("FBCC", labSample).orElseThrow();
        assertEquals(Instant.parse("2013-07-28T08:41:00Z"), labOrder.getOrderDatetime()); // from 03 SN
        assertEquals("WinPath", labOrder.getSourceSystem()); // from 03 SN onwards
        assertEquals("12121212", labOrder.getInternalLabNumber()); // from 04 ORR o02
        assertNotNull(labOrder.getHospitalVisitId()); // from 03 SN
        assertEquals(Instant.parse("2013-07-29T03:24:00Z"), labOrder.getRequestDatetime()); // Updated by 06 ORU R01
    }


    /**
     * Incremental order stream with a final result set.
     * @return A stream of all the possible valid orderings.
     */
    @TestFactory
    Stream<DynamicTest> testCoPathIncrementalOrders() {
        String[] orderFiles = {"01_orm_o01_sn", "02_orm_o01_nw", "03_orr_o02_na", "04_oru_r01"};
        labsPermutationTestProducer.setMessagePathAndORMDefaults("co_path/incremental");
        labsPermutationTestProducer.setFinalStateChecker(this::checkCoPathIncrementalOrders);

        List<Iterable<List<String>>> duplicatedNames = new ArrayList<>();
        for (int i = 0; i < orderFiles.length; i++) {
            List<String> filesWithOneDuplicate = duplicateAt(orderFiles, i);
            duplicatedNames.add(new ShuffleIterator<>(filesWithOneDuplicate));
        }

        return duplicatedNames.stream()
                .flatMap(pi -> StreamSupport.stream(pi.spliterator(), false))
                .map(messageOrdering -> DynamicTest.dynamicTest(
                        String.format("Test %s", messageOrdering),
                        () -> labsPermutationTestProducer.buildTestFromPermutation(messageOrdering)));
    }

    private void checkCoPathIncrementalOrders() {
        LabSample labSample = labSampleRepository.findByExternalLabNumber("UR20-4444").orElseThrow();
        assertNull(labSample.getSpecimenType());
        assertEquals(Instant.parse("2020-05-22T11:07:00Z"), labSample.getSampleCollectionTime()); // from 01 SN
        assertNull(labSample.getReceiptAtLab());
        assertEquals("Stained slides x 6 Ref: 20/12322 from Barts Health", labSample.getCollectionMethod()); // from 01 SN

        LabOrder labOrder = labOrderRepository.findByLabBatteryIdBatteryCodeAndLabSampleId("CO_PATH", labSample).orElseThrow();
        assertEquals(Instant.parse("2020-05-22T11:10:00Z"), labOrder.getOrderDatetime()); // from 01 SN
        assertEquals("CoPath", labOrder.getSourceSystem()); // from 04 ORU
        assertEquals("12121212", labOrder.getInternalLabNumber()); // from 01 SN
        assertEquals("20S123234221", labOrder.getHospitalVisitId().getEncounter()); // from 01 SN
        assertEquals(Instant.parse("2020-05-22T11:10:00Z"), labOrder.getRequestDatetime()); // 03 NA
    }

    @TestFactory
    Stream<DynamicTest> testCoPathOrderCancellation() {
        // cancels order under one epic lab order number for a lab specimen, then creates a new order for the same specimen
        String[] orderFiles = {
                "01_orm_o01_nw", "02_orm_o01_ca", "03_orr_o02_cr", "04_orm_o01_sc", "05_oru_r01"
        };
        labsPermutationTestProducer.setMessagePathAndORMDefaults("co_path/cancel");
        labsPermutationTestProducer.setFinalStateChecker(this::checkCoPathCancelOrders);

        List<Iterable<List<String>>> duplicatedNames = new ArrayList<>();
        for (int i = 0; i < orderFiles.length; i++) {
            List<String> filesWithOneDuplicate = duplicateAt(orderFiles, i);
            duplicatedNames.add(new ShuffleIterator<>(List.of(orderFiles)));
        }

        return duplicatedNames.stream()
                .flatMap(pi -> StreamSupport.stream(pi.spliterator(), false))
                .map(messageOrdering -> DynamicTest.dynamicTest(
                        String.format("Test %s", messageOrdering),
                        () -> labsPermutationTestProducer.buildTestFromPermutation(messageOrdering)));
    }

    private void checkCoPathCancelOrders() {
        LabSample labSample = labSampleRepository.findByExternalLabNumber("UH20-4444").orElseThrow();
        assertEquals("BM", labSample.getSpecimenType()); // from 01 NW
        assertEquals(Instant.parse("2020-11-09T15:05:00Z"), labSample.getSampleCollectionTime()); // from 01 NW
        assertEquals(Instant.parse("2020-11-11T12:53:00Z"), labSample.getReceiptAtLab()); // 04 ORM SC

        LabOrder labOrder = labOrderRepository.findByLabBatteryIdBatteryCodeAndLabSampleId("CO_PATH", labSample).orElseThrow();
        assertNull(labOrder.getOrderDatetime()); // from 01 NW but cancelled, never added in this stream
        assertEquals("CoPath", labOrder.getSourceSystem()); // from 04 SC onwards
        assertEquals("12121212", labOrder.getInternalLabNumber()); // from 04 SC onwards
        assertEquals("123234221", labOrder.getHospitalVisitId().getEncounter()); // from 03 SN
        assertNull(labOrder.getRequestDatetime()); // from 01 NW but cancelled, never added in this stream
        assertEquals(3, labSampleQuestionRepository.count());
    }


}
