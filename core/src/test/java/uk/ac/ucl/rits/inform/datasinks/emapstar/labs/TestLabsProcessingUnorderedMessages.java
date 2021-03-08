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
    Stream<DynamicTest> testIncrementalOrders() {
        String[] orderFiles = {"01_orm_o01_nw", "02_orm_o01_sc_mg", "03_orm_o01_sn_telh", "04_orr_o02_telh", "05_oru_r01"};
        labsPermutationTestProducer.setMessagePathAndORMDefaults("winpath/incremental_orders");
        labsPermutationTestProducer.setFinalStateChecker(this::checkIncrementalOrders);

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

    private void checkIncrementalOrders() {
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
    Stream<DynamicTest> testOrderCancellation() {
        String[] orderFiles = {
                "01_orm_o01_nw_fbc_mg", "02_orm_o01_ca_fbc", "03_orm_o01_sn_fbcc", "04_orr_o02_cr_fbc", "05_orr_o02_na_fbcc", "06_oru_r01_fbcc"
        };
        labsPermutationTestProducer.setMessagePathAndORMDefaults("winpath/cancel_orders");
        labsPermutationTestProducer.setFinalStateChecker(this::checkCancelOrders);

        List<Iterable<List<String>>> duplicatedNames = new ArrayList<>();
        for (int i = 0; i < orderFiles.length; i++) {
            List<String> filesWithOneDuplicate = duplicateAt(orderFiles, i);
            duplicatedNames.add(new ShuffleIterator<>(filesWithOneDuplicate));
        }

        return duplicatedNames.stream()
                .flatMap(pi -> StreamSupport.stream(pi.spliterator(), false))
                // can't recover if the order is after the cancellation so remove these
                .filter(files -> {
                    int lastOriginalOrder = files.lastIndexOf("01_orm_o01_nw_fbc_mg");
                    int lastORMCancel = files.lastIndexOf("02_orm_o01_ca_fbc");
                    int lastORRCancelConfirm = files.lastIndexOf("04_orr_o02_cr_fbc");
                    return lastOriginalOrder < Math.max(lastORMCancel, lastORRCancelConfirm);
                })
                .map(messageOrdering -> DynamicTest.dynamicTest(
                        String.format("Test %s", messageOrdering),
                        () -> labsPermutationTestProducer.buildTestFromPermutation(messageOrdering)));
    }

    private void checkCancelOrders() {
        LabSample labSample = labSampleRepository.findByExternalLabNumber("13U444444").orElseThrow();
        assertEquals("BLD", labSample.getSpecimenType()); // from 01 NW
        assertEquals(Instant.parse("2013-07-28T07:27:00Z"), labSample.getSampleCollectionTime()); // from 01 NW
        assertNull(labSample.getReceiptAtLab()); // not in messages

        Optional<LabOrder> cancelledOrder = labOrderRepository.findByLabBatteryIdBatteryCodeAndLabSampleId("FBC", labSample);
        assertTrue(cancelledOrder.isEmpty());


        LabOrder remainingOrder = labOrderRepository.findByLabBatteryIdBatteryCodeAndLabSampleId("FBCC", labSample).orElseThrow();
        assertEquals(Instant.parse("2013-07-28T08:41:00Z"), remainingOrder.getOrderDatetime()); // from 03 SN
        assertEquals("WinPath", remainingOrder.getSourceSystem()); // from 03 SN onwards
        assertEquals("12121212", remainingOrder.getInternalLabNumber()); // from 04 ORR o02
        assertNotNull(remainingOrder.getHospitalVisitId()); // from 03 SN
        assertEquals(Instant.parse("2013-07-29T03:24:00Z"), remainingOrder.getRequestDatetime()); // Updated by 06 ORU R01
    }


}
