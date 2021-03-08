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
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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

    /**
     * @return A stream of all the possible valid orderings.
     */
    @TestFactory
    Stream<DynamicTest> testUnorderedMoves() {
        String[] orderFiles = {"01_orm_o01_nw", "02_orm_o01_sc_mg", "03_orm_o01_sn_telh", "04_orr_o02_telh", "05_oru_r01"};
        labsPermutationTestProducer.setMessagePathAndORMDefaults("winpath/incremental_orders");
        labsPermutationTestProducer.setFinalStateChecker(this::checkIncrementalOrders);
        Iterable<List<String>> permutationIterator = new ShuffleIterator<>(List.of(orderFiles));

        return StreamSupport.stream(permutationIterator.spliterator(), false)
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

}
