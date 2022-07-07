package uk.ac.ucl.rits.inform.datasources.ids;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.test.context.ActiveProfiles;

import uk.ac.ucl.rits.inform.interchange.AdvanceDecisionMessage;
import uk.ac.ucl.rits.inform.interchange.ConsultRequest;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessage;
import uk.ac.ucl.rits.inform.interchange.InterchangeMessageFactory;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.PatientInfection;
import uk.ac.ucl.rits.inform.interchange.adt.AdtMessage;
import uk.ac.ucl.rits.inform.interchange.adt.ImpliedAdtMessage;
import uk.ac.ucl.rits.inform.interchange.lab.LabOrderMsg;
import uk.ac.ucl.rits.inform.interchange.lab.LabResultMsg;
import uk.ac.ucl.rits.inform.interchange.visit_observations.Flowsheet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;




/**
 * Test that the HL7 output format matches that of the corresponding yaml files
 */
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Slf4j
public class TestHL7ParsingMatchesInterchangeFactoryOutput extends TestHl7MessageStream {
    InterchangeMessageFactory interchangeFactory = new InterchangeMessageFactory();

    private final HL7Utils.FileStoreWithMonitoredAccess messageFileStore;

    static private final String[] EXCLUDED_MESSAGE_FILES = new String[]{
            "caboodle_flowsheets_for_metadata_testing.yaml",
            "epic_flowsheets_for_metadata_testing.yaml",
            "dental_department_only.yaml",
            "medsurg_active_pool_bed.yaml",
            "acun_census_bed.yaml",
            "flowsheet_mpi_metadata.yaml",
            "flowsheet_metadata.yaml",
            "07_A06.yaml",
            "04_A02.yaml",
            "05_A02.yaml",
            "03_A02.yaml",
            "08_A03.yaml",
            "06_A02.yaml",
            "02_A01.yaml",
            "01_A01.yaml",
            "05_A02.yaml",
            "03_A02.yaml",
            "02_A02.yaml",
            "04_A12.yaml",
            "06_A03.yaml",
            "05_A03.yaml",
            "01_A01.yaml",
            "02_A02.yaml",
            "04_A13.yaml",
            "03_A03.yaml",
            "05_A03.yaml",
            "01_A01.yaml",
            "04_A02.yaml",
            "03_A01.yaml",
            "02_A11.yaml",
            "04_A03.yaml",
            "03_A02.yaml",
            "02_A01.yaml",
            "01_A04.yaml",
            "minimal.yaml",
            "closed_at_discharge.yaml",
            "cancelled.yaml",
            "con2.yaml",
            "con255.yaml",
            "updated_only.yaml",
            "02_orm_o01_sc_mg.yaml",
            "01_orm_o01_nw.yaml",
            "03_orm_o01_sn_telh.yaml",
            "04_orr_o02_telh.yaml",
            "05_orr_o02_na_fbcc.yaml",
            "04_orr_o02_cr_fbc.yaml",
            "01_orm_o01_nw_fbc_mg.yaml",
            "03_orm_o01_sn_fbcc.yaml",
            "02_orm_o01_ca_fbc.yaml",
            "01_orm_o01_nw.yaml",
            "04_orm_o01_sc.yaml",
            "02_orm_o01_ca.yaml",
            "03_orr_o02_cr.yaml",
            "03_orr_o02_na.yaml",
            "01_orm_o01_sn.yaml",
            "02_orm_o01_nw.yaml",
    };

    /**
     * Constructor for the test class. Populates all the message files
     * @throws IOException If a path cannot be accessed
     */
    TestHL7ParsingMatchesInterchangeFactoryOutput() throws IOException {

        this.messageFileStore = new HL7Utils().createMonitoredFileStore(
                new String[]{"src/test/resources/", "../Emap-Interchange/src/test/resources/"}
        );
    }

    private void testAdtMessage(String adtFileStem) throws Exception {
        log.info("Testing ADT message with stem '{}'", adtFileStem);
        System.out.println(adtFileStem);
        List<? extends EmapOperationMessage> messagesFromHl7Message = processSingleMessage(
                messageFileStore.get("Adt/" + adtFileStem + ".txt"));
        AdtMessage expectedAdtMessage = interchangeFactory.getAdtMessage(
                messageFileStore.get(adtFileStem + ".yaml"));
        Assertions.assertEquals(1, messagesFromHl7Message.size());
        Assertions.assertEquals(expectedAdtMessage, messagesFromHl7Message.get(0));
    }

    private void assertListOfMessagesEqual(List<? extends EmapOperationMessage> expectedMessages, List<? extends EmapOperationMessage> messagesFromHl7Message) {
        for (int i = 0; i < expectedMessages.size(); i++) {
            String failMessage = String.format("Failed on message %d", i);
            Assertions.assertEquals(expectedMessages.get(i), messagesFromHl7Message.get(i), failMessage);
        }
        Assertions.assertEquals(expectedMessages.size(), messagesFromHl7Message.size());
    }

    private void assertLabOrdersWithValueAsBytesEqual(List<LabOrderMsg> expectedMessages, List<? extends EmapOperationMessage> messagesFromHl7Message) {
        // first check values as byte and strip them out
        for (int orderIndex = 0; orderIndex < expectedMessages.size(); orderIndex++) {
            if (expectedMessages.get(orderIndex).getLabResultMsgs().isEmpty()) {
                continue;
            }
            LabOrderMsg expectedOrder = expectedMessages.get(orderIndex);
            LabOrderMsg hl7Order = (LabOrderMsg) messagesFromHl7Message.get(orderIndex);
            for (int resultIndex = 0; resultIndex < expectedOrder.getLabResultMsgs().size(); resultIndex++) {
                LabResultMsg expectedResult = expectedOrder.getLabResultMsgs().get(resultIndex);
                LabResultMsg hl7Result = hl7Order.getLabResultMsgs().get(resultIndex);
                if (expectedResult.getByteValue().isUnknown()) {
                    continue;
                }
                // check byte values
                byte[] expectedBytes = expectedResult.getByteValue().get();
                byte[] hl7Bytes = hl7Result.getByteValue().get();
                assertArrayEquals(expectedBytes, hl7Bytes);
                // remove byte values from rest of the check
                expectedResult.setByteValue(InterchangeValue.unknown());
                hl7Result.setByteValue(InterchangeValue.unknown());
            }
        }
        assertListOfMessagesEqual(expectedMessages, messagesFromHl7Message);
    }

    @Test
    public void testGenericAdtA01() throws Exception {
        testAdtMessage("generic/A01");
        testAdtMessage("generic/A01_b");

    }

    @Test
    public void testGenericAdtA02() throws Exception {
        testAdtMessage("generic/A02");
    }

    @Test
    public void testGenericAdtA03() throws Exception {
        testAdtMessage("generic/A03");
        testAdtMessage("generic/A03_death");
        testAdtMessage("generic/A03_death_2");
        testAdtMessage("generic/A03_death_3");

    }

    @Test
    public void testGenericAdtA04() throws Exception {
        testAdtMessage("generic/A04");
    }

    @Test
    public void testGenericAdtA06() throws Exception {
        testAdtMessage("generic/A06");
    }

    @Test
    public void testGenericAdtA08() throws Exception {
        testAdtMessage("generic/A08_v1");
        testAdtMessage("generic/A08_v2");
    }

    @Test
    public void testGenericAdtA11() throws Exception {
        testAdtMessage("generic/A11");
    }

    @Test
    public void testGenericAdtA12() throws Exception {
        testAdtMessage("generic/A12");
    }

    @Test
    public void testGenericAdtA13() throws Exception {
        testAdtMessage("generic/A13");
    }

    @Test
    void testPendingTransferAdtA15() throws Exception {
        testAdtMessage("pending/A15");
    }

    @Test
    public void testGenericAdtA17() throws Exception {
        testAdtMessage("generic/A17");
    }

    @Test
    void testCancelPendingTransferA26() throws Exception {
        testAdtMessage("pending/A26");
    }

    @Test
    public void testGenericAdtA29() throws Exception {
        testAdtMessage("generic/A29");
    }

    @Test
    public void testGenericAdtA40() throws Exception {
        testAdtMessage("generic/A40");
    }

    @Test
    public void testGenericAdtA45() throws Exception {
        testAdtMessage("generic/A45");
    }

    @Test
    public void testGenericAdtA47() throws Exception {
        testAdtMessage("generic/A47");
    }

    @Test
    public void testDoubleA01WithA13() throws Exception {
        testAdtMessage("DoubleA01WithA13/A03");
        testAdtMessage("DoubleA01WithA13/A03_2");
        testAdtMessage("DoubleA01WithA13/A08");
        testAdtMessage("DoubleA01WithA13/A13");
        testAdtMessage("DoubleA01WithA13/FirstA01");
        testAdtMessage("DoubleA01WithA13/SecondA01");
    }

    @Test
    public void testAdtPendingLocations() throws Exception {
        testAdtMessage("pending/A15");
        testAdtMessage("pending/A15_null_pending_location");
        testAdtMessage("pending/A26");
    }

    void checkConsultMatchesInterchange(String fileName) throws Exception {
        List<? extends EmapOperationMessage> messagesFromHl7Message = processSingleMessageAndRemoveAdt(
                messageFileStore.get("ConsultRequest/"+fileName+".txt"));
        ConsultRequest expected = interchangeFactory.getConsult(messageFileStore.get(String.format("%s.yaml", fileName)));
        assertEquals(1, messagesFromHl7Message.size());
        assertEquals(expected, messagesFromHl7Message.get(0));
    }

    @Test
    void testClosedAtDischarge() throws Exception {
        checkConsultMatchesInterchange("closed_at_discharge");
    }

    @Test
    void testCancelledConsult() throws Exception {
        checkConsultMatchesInterchange("cancelled");
    }

    @Test
    void testMinimalConsult() throws Exception {
        checkConsultMatchesInterchange("minimal");
    }

    @Test
    void testNotesConsult() throws Exception {
        checkConsultMatchesInterchange("notes");
    }

    void checkAdvanceDecisionMatchesInterchange(String fileName) throws Exception {
        checkAdvanceDecisionMatchesInterchange("AdvanceDecision/"+fileName+".txt",
                String.format("%s.yaml", fileName));
    }

    void checkAdvanceDecisionMatchesInterchange(String txtFileName, String yamlFileName) throws Exception{

         List<? extends EmapOperationMessage> messagesFromHl7Message = processSingleMessageAndRemoveAdt(
                messageFileStore.get(txtFileName));
        AdvanceDecisionMessage expected = interchangeFactory.getAdvanceDecision(messageFileStore.get(yamlFileName));
        assertEquals(1, messagesFromHl7Message.size());
        assertEquals(expected, messagesFromHl7Message.get(0));
    }

    @Test
    void testClosedAtDischargeAdvanceDecision() throws Exception {
        checkAdvanceDecisionMatchesInterchange("closed_at_discharge");
    }

    @Test
    void testCancelledAdvanceDecision() throws Exception {
        checkAdvanceDecisionMatchesInterchange("cancelled");
    }

    @Test
    void testMinimalAdvanceDecision() throws Exception {
        checkAdvanceDecisionMatchesInterchange("minimal");
    }

    @Test
    void testMinimalWithQuestionsAdvanceDecision() throws Exception {
        checkAdvanceDecisionMatchesInterchange("new_with_questions");
    }

    @Test
    void testMinimalWithMultipleQuestionsAdvanceDecision() throws Exception {
        checkAdvanceDecisionMatchesInterchange("minimal_w_questions");
    }

    @Test
    public void testLabIncrementalLoad() throws Exception {
        List<? extends EmapOperationMessage> messagesFromHl7Message = processLabHl7AndFilterToLabOrderMsgs(
                messageFileStore.get("LabOrders/winpath/Incremental.txt"));
        List<LabOrderMsg> expectedOrders = interchangeFactory.getLabOrders(
                messageFileStore.get("winpath/incremental.yaml"), "0000000042");
        assertListOfMessagesEqual(expectedOrders, messagesFromHl7Message);
    }

    @Test
    public void testLabIncrementalDuplicateResultSegment() throws Exception {
        List<? extends EmapOperationMessage> messagesFromHl7Message = processLabHl7AndFilterToLabOrderMsgs(
                messageFileStore.get("LabOrders/winpath/LabDuplicateResultSegment.txt"));
        List<LabOrderMsg> expectedOrders = interchangeFactory.getLabOrders(
                messageFileStore.get("winpath/incremental_duplicate_result_segment.yaml"), "0000000042");
        assertListOfMessagesEqual(expectedOrders, messagesFromHl7Message);
    }

    @Test
    public void testLabOrderMsg() throws Exception {
        List<? extends EmapOperationMessage> messagesFromHl7Message = processSingleMessageAndRemoveAdt(
                messageFileStore.get("LabOrders/winpath/ORU_R01.txt"));
        List<LabOrderMsg> expectedOrders = interchangeFactory.getLabOrders(
                messageFileStore.get("winpath/ORU_R01.yaml"), "0000000042");
        assertListOfMessagesEqual(expectedOrders, messagesFromHl7Message);
    }

    @Test
    public void testLabOrderMsgProducesAdtFirst() throws Exception {
        EmapOperationMessage messageFromHl7 = processSingleMessage(
                messageFileStore.get("LabOrders/winpath/ORU_R01.txt")).get(0);
        AdtMessage expectedAdt = interchangeFactory.getAdtMessage(
                messageFileStore.get("FromNonAdt/lab_oru_r01.yaml"));
        Assertions.assertEquals(expectedAdt, messageFromHl7);
    }

    @Test
    public void testLabSensitivity() throws Exception {
        List<? extends EmapOperationMessage> messagesFromHl7Message = processSingleMessageAndRemoveAdt(
                messageFileStore.get("LabOrders/winpath/Sensitivity.txt"));
        List<LabOrderMsg> expectedOrders = interchangeFactory.getLabOrders(
                messageFileStore.get("winpath/sensitivity.yaml"), "0000000042");
        assertListOfMessagesEqual(expectedOrders, messagesFromHl7Message);
    }

    @Test
    public void testIncrementalIsolate1() throws Exception {
        List<? extends EmapOperationMessage> messagesFromHl7Message = processSingleMessageAndRemoveAdt(
                messageFileStore.get("LabOrders/winpath/isolate_inc_1.txt"));
        List<LabOrderMsg> expectedOrders = interchangeFactory.getLabOrders(
                messageFileStore.get("winpath/isolate_inc_1.yaml"), "0000000042");
        assertListOfMessagesEqual(expectedOrders, messagesFromHl7Message);
    }

    @Test
    public void testIncrementalIsolate2() throws Exception {
        List<? extends EmapOperationMessage> messagesFromHl7Message = processSingleMessageAndRemoveAdt(
                messageFileStore.get("LabOrders/winpath/isolate_inc_2.txt"));
        List<LabOrderMsg> expectedOrders = interchangeFactory.getLabOrders(
                messageFileStore.get("winpath/isolate_inc_2.yaml"), "0000000042");
        assertListOfMessagesEqual(expectedOrders, messagesFromHl7Message);
    }

    @Test
    public void testWinPathIncrementalOrders() throws Exception {
        String hl7PathTemplate = "LabOrders/winpath/incremental_orders/%s.txt";
        String interchangePathTemplate = "winpath/incremental_orders/%s.yaml";
        String interchangeDefaults = String.format(interchangePathTemplate, "orm_defaults");

        List<EmapOperationMessage> builtMessages = new ArrayList<>();
        List<LabOrderMsg> expectedOrders = new ArrayList<>();
        // build up order messages
        String[] orderFiles = {"01_orm_o01_nw", "02_orm_o01_sc_mg", "03_orm_o01_sn_telh", "04_orr_o02_telh"};
        for (String orderFile : orderFiles) {
            builtMessages.addAll(processSingleMessage(messageFileStore.get(String.format(hl7PathTemplate, orderFile))));
            expectedOrders.add(interchangeFactory.buildLabOrderOverridingDefaults(
                    interchangeDefaults, String.format(interchangePathTemplate, orderFile)));
        }
        // add in final result
        builtMessages.addAll(processSingleMessage(String.format(hl7PathTemplate, "05_oru_r01")));
        expectedOrders.addAll(interchangeFactory.getLabOrders(
                messageFileStore.get(String.format(interchangePathTemplate, "05_oru_r01")), "0000000042"));

        builtMessages = builtMessages.stream().filter(msg -> !(msg instanceof ImpliedAdtMessage)).collect(Collectors.toList());
        assertListOfMessagesEqual(expectedOrders, builtMessages);
    }

    @Test
    public void testWinPathCancelOrders() throws Exception {
        String hl7PathTemplate = "LabOrders/winpath/cancel_orders/%s.txt";
        String interchangePathTemplate = "winpath/cancel_orders/%s.yaml";
        String interchangeDefaults = String.format(interchangePathTemplate, "orm_defaults");

        List<EmapOperationMessage> builtMessages = new ArrayList<>();
        List<LabOrderMsg> expectedOrders = new ArrayList<>();
        // build up order messages
        String[] orderFiles = {"01_orm_o01_nw_fbc_mg", "02_orm_o01_ca_fbc", "03_orm_o01_sn_fbcc", "04_orr_o02_cr_fbc", "05_orr_o02_na_fbcc"};
        for (String orderFile : orderFiles) {
            builtMessages.addAll(processSingleMessage(messageFileStore.get(String.format(hl7PathTemplate, orderFile))));
            expectedOrders.add(interchangeFactory.buildLabOrderOverridingDefaults(
                    interchangeDefaults, String.format(interchangePathTemplate, orderFile)));
        }
        // add in final result
        builtMessages.addAll(processSingleMessage(String.format(hl7PathTemplate, "06_oru_r01_fbcc")));
        expectedOrders.addAll(interchangeFactory.getLabOrders(
                messageFileStore.get(String.format(interchangePathTemplate, "06_oru_r01_fbcc")), "0000000042"));

        builtMessages = builtMessages.stream().filter(msg -> !(msg instanceof ImpliedAdtMessage)).collect(Collectors.toList());
        assertListOfMessagesEqual(expectedOrders, builtMessages);
    }

    @Test
    public void testCoPathIncrementalOrder() throws Exception {
        String hl7PathTemplate = "LabOrders/co_path/incremental/%s.txt";
        String interchangePathTemplate = "co_path/incremental/%s.yaml";
        String interchangeDefaults = String.format(interchangePathTemplate, "orm_defaults");

        List<EmapOperationMessage> builtMessages = new ArrayList<>();
        List<LabOrderMsg> expectedOrders = new ArrayList<>();
        // build up order messages
        String[] orderFiles = {"01_orm_o01_sn", "02_orm_o01_nw", "03_orr_o02_na"};
        for (String orderFile : orderFiles) {
            builtMessages.addAll(processSingleMessage(messageFileStore.get(String.format(hl7PathTemplate, orderFile))));
            expectedOrders.add(interchangeFactory.buildLabOrderOverridingDefaults(
                    interchangeDefaults, String.format(interchangePathTemplate, orderFile)));
        }
        // add in final result
        builtMessages.addAll(processSingleMessage(String.format(hl7PathTemplate, "04_oru_r01")));
        expectedOrders.addAll(interchangeFactory.getLabOrders(
                messageFileStore.get(String.format(interchangePathTemplate, "04_oru_r01")), "0000000042"));

        builtMessages = builtMessages.stream().filter(msg -> !(msg instanceof ImpliedAdtMessage)).collect(Collectors.toList());
        assertLabOrdersWithValueAsBytesEqual(expectedOrders, builtMessages);
    }

    @Test
    public void testCoPathCancelOrders() throws Exception {
        String hl7PathTemplate = "LabOrders/co_path/cancel/%s.txt";
        String interchangePathTemplate = "co_path/cancel/%s.yaml";
        String interchangeDefaults = String.format(interchangePathTemplate, "orm_defaults");

        List<EmapOperationMessage> builtMessages = new ArrayList<>();
        List<LabOrderMsg> expectedOrders = new ArrayList<>();
        // build up order messages
        String[] orderFiles = {"01_orm_o01_nw", "02_orm_o01_ca", "03_orr_o02_cr", "04_orm_o01_sc"};
        for (String orderFile : orderFiles) {
            builtMessages.addAll(processSingleMessage(messageFileStore.get(String.format(hl7PathTemplate, orderFile))));
            expectedOrders.add(interchangeFactory.buildLabOrderOverridingDefaults(
                    interchangeDefaults, String.format(interchangePathTemplate, orderFile)));
        }
        // add in final result
        builtMessages.addAll(processSingleMessage(String.format(hl7PathTemplate, "05_oru_r01")));
        expectedOrders.addAll(interchangeFactory.getLabOrders(
                messageFileStore.get(String.format(interchangePathTemplate, "05_oru_r01")), "0000000042"));

        builtMessages = builtMessages.stream().filter(msg -> !(msg instanceof ImpliedAdtMessage)).collect(Collectors.toList());
        assertListOfMessagesEqual(expectedOrders, builtMessages);
    }

    @Test
    void testCoPathQuestions() throws Exception {
        String hl7PathTemplate = "LabOrders/co_path/%s.txt";
        String interchangePathTemplate = "co_path/%s.yaml";
        String orderFile = "orm_o01_questions";
        String interchangeDefaults = String.format(interchangePathTemplate, "orm_defaults");
        String interchangePath = String.format(interchangePathTemplate, orderFile);

        EmapOperationMessage builtMessage = processSingleMessage(messageFileStore.get(String.format(hl7PathTemplate, orderFile)))
                .stream()
                .filter(msg -> !(msg instanceof ImpliedAdtMessage))
                .findFirst().orElseThrow();
        LabOrderMsg expectedMessage = interchangeFactory.buildLabOrderOverridingDefaults(interchangeDefaults,
                messageFileStore.get(interchangePath));

        assertEquals(builtMessage, expectedMessage);
    }

    @Test
    void testCoPathByteValue() throws Exception {
        String hl7PathTemplate = "LabOrders/co_path/%s.txt";
        String interchangePathTemplate = "co_path/%s.yaml";
        String orderFile = "oru_r01_byte_value";

        LabOrderMsg builtMessage = (LabOrderMsg) processSingleMessage(messageFileStore.get(String.format(hl7PathTemplate, orderFile)))
                .stream()
                .filter(msg -> (msg instanceof LabOrderMsg))
                .findFirst().orElseThrow();
        LabOrderMsg expectedMessage = interchangeFactory.getLabOrder(
                messageFileStore.get(String.format(interchangePathTemplate, orderFile)));

        assertLabOrdersWithValueAsBytesEqual(List.of(builtMessage), List.of(expectedMessage));
    }

    @Test
    public void testPOCLabABL() throws Exception {
        List<? extends EmapOperationMessage> messagesFromHl7Message = processSingleMessageAndRemoveAdt(
                messageFileStore.get("LabOrders/abl90_flex/venous.txt"));
        List<LabOrderMsg> expectedOrders = interchangeFactory.getLabOrders(
                messageFileStore.get("abl90_flex/venous.yaml"), "0000000042");
        assertListOfMessagesEqual(expectedOrders, messagesFromHl7Message);
    }

    @Test
    public void testPOCLabBioConnect() throws Exception {
        List<? extends EmapOperationMessage> messagesFromHl7Message = processSingleMessageAndRemoveAdt(
                messageFileStore.get("LabOrders/bio_connect/glucose.txt"));
        List<LabOrderMsg> expectedOrders = interchangeFactory.getLabOrders(
                messageFileStore.get("bio_connect/glucose.yaml"), "0000000042");
        assertListOfMessagesEqual(expectedOrders, messagesFromHl7Message);
    }

    @Test
    public void testVitalSigns() throws Exception {
        List<? extends EmapOperationMessage> messagesFromHl7Message = processSingleMessageAndRemoveAdt("VitalSigns/MixedHL7Message.txt");
        List<Flowsheet> expectedOrders = interchangeFactory.getFlowsheets(
                messageFileStore.get("hl7.yaml"), "0000000042");
        assertListOfMessagesEqual(expectedOrders, messagesFromHl7Message);
    }

    @Test
    public void testVitalSignsProducesAdtFirst() throws Exception {
        EmapOperationMessage messageFromHl7 = processSingleMessage(messageFileStore.get("VitalSigns/MixedHL7Message.txt")).get(0);
        AdtMessage expectedAdt = interchangeFactory.getAdtMessage(
                messageFileStore.get("FromNonAdt/flowsheet_oru_r01.yaml"));
        Assertions.assertEquals(expectedAdt, messageFromHl7);
    }

    @Test
    public void testPatientInfectionCreatesAdt() throws Exception {
        EmapOperationMessage messageFromHl7 = processSingleMessage(messageFileStore.get("PatientInfection/a05.txt")).get(0);
        AdtMessage expectedAdt = interchangeFactory.getAdtMessage(
                messageFileStore.get("FromNonAdt/patient_infection_a05.yaml"));
        Assertions.assertEquals(expectedAdt, messageFromHl7);
    }

    public void checkPatientInfectionMatchesInterchange(String txtFileName, String yamlFileName) throws Exception {
        List<EmapOperationMessage> messagesFromHl7 = processSingleMessage(messageFileStore.get(txtFileName))
                .stream()
                .filter(msg -> msg instanceof PatientInfection).collect(Collectors.toList());

        List<PatientInfection> expectedMessages = interchangeFactory.getPatientInfections(
                messageFileStore.get(yamlFileName));

        for (int i = 0; i < Math.max(messagesFromHl7.size(), expectedMessages.size()); i++){
            Assertions.assertEquals(expectedMessages.get(i), messagesFromHl7.get(i));
        }
    }

    @Test
    public void testMinimalPatientInfection() throws Exception {
        checkPatientInfectionMatchesInterchange("PatientInfection/a05.txt", "hl7/minimal_mumps.yaml");
    }

    @Test
    public void testResolvedPatientInfection() throws Exception {
        checkPatientInfectionMatchesInterchange("PatientInfection/mumps_resolved.txt", "mumps_resolved.yaml");
    }

    @Test
    public void testMultiplePatientInfection() throws Exception {
        checkPatientInfectionMatchesInterchange("PatientInfection/multiple_infections.txt",
                "multiple_infections.yaml");
    }

    /**
     * Ensure that all the non-excluded files have been accessed, thus the format of the hl7-processed message
     * checked against their yaml counterparts.
     * @throws Exception If not all the files have been accessed
     */
    @AfterAll
    void checkAllFilesHaveBeenAccessed() throws Exception{

        for (var f: messageFileStore){

            if (!f.fileNameEndsWith(".yaml")
                    || f.fileNameEndsWith("_defaults.yaml") // Implicitly considered as the non-prefixed version inherits
                    || Arrays.stream(EXCLUDED_MESSAGE_FILES).anyMatch(f::fileNameEndsWith)
                    || f.hasBeenAccessed()){
                continue;
            }

            throw new Exception("Not all the files have been accessed. Missed "+f.getFilePath());
        }
    }
}
