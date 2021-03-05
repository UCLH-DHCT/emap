package uk.ac.ucl.rits.inform.datasources.ids;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessage;
import uk.ac.ucl.rits.inform.interchange.Flowsheet;
import uk.ac.ucl.rits.inform.interchange.InterchangeMessageFactory;
import uk.ac.ucl.rits.inform.interchange.adt.AdtMessage;
import uk.ac.ucl.rits.inform.interchange.adt.ImpliedAdtMessage;
import uk.ac.ucl.rits.inform.interchange.lab.LabOrderMsg;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


/**
 *
 */
@ActiveProfiles("test")
@Slf4j
public class TestHL7ParsingMatchesInterchangeFactoryOutput extends TestHl7MessageStream {
    InterchangeMessageFactory interchangeFactory = new InterchangeMessageFactory();

    private void testAdtMessage(String adtFileStem) throws Exception {
        log.info("Testing ADT message with stem '{}'", adtFileStem);
        List<? extends EmapOperationMessage> messagesFromHl7Message = processSingleMessage("Adt/" + adtFileStem + ".txt");
        AdtMessage expectedAdtMessage = interchangeFactory.getAdtMessage(adtFileStem + ".yaml");
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
    public void testGenericAdtA17() throws Exception {
        testAdtMessage("generic/A17");
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
        testAdtMessage("DoubleA01WithA13/A13");
        testAdtMessage("DoubleA01WithA13/FirstA01");
        testAdtMessage("DoubleA01WithA13/SecondA01");
    }

    @Test
    public void testLabIncrementalLoad() throws Exception {
        List<? extends EmapOperationMessage> messagesFromHl7Message = processLabHl7AndFilterToLabOrderMsgs("LabOrders/winpath/Incremental.txt");
        List<LabOrderMsg> expectedOrders = interchangeFactory.getLabOrders("winpath/incremental.yaml", "0000000042");
        assertListOfMessagesEqual(expectedOrders, messagesFromHl7Message);
    }

    @Test
    public void testLabIncrementalDuplicateResultSegment() throws Exception {
        List<? extends EmapOperationMessage> messagesFromHl7Message = processLabHl7AndFilterToLabOrderMsgs("LabOrders/winpath/LabDuplicateResultSegment.txt");
        List<LabOrderMsg> expectedOrders = interchangeFactory.getLabOrders("winpath/incremental_duplicate_result_segment.yaml", "0000000042");
        assertListOfMessagesEqual(expectedOrders, messagesFromHl7Message);
    }

    @Test
    public void testLabOrderMsg() throws Exception {
        List<? extends EmapOperationMessage> messagesFromHl7Message = processSingleMessageAndRemoveAdt("LabOrders/winpath/ORU_R01.txt");
        List<LabOrderMsg> expectedOrders = interchangeFactory.getLabOrders("winpath/ORU_R01.yaml", "0000000042");
        assertListOfMessagesEqual(expectedOrders, messagesFromHl7Message);
    }

    @Test
    public void testLabOrderMsgProducesAdtFirst() throws Exception {
        EmapOperationMessage messageFromHl7 = processSingleMessage("LabOrders/winpath/ORU_R01.txt").get(0);
        AdtMessage expectedAdt = interchangeFactory.getAdtMessage("FromNonAdt/lab_oru_r01.yaml");
        Assertions.assertEquals(expectedAdt, messageFromHl7);
    }

    @Test
    public void testLabSensitivity() throws Exception {
        List<? extends EmapOperationMessage> messagesFromHl7Message = processSingleMessageAndRemoveAdt("LabOrders/winpath/Sensitivity.txt");
        List<LabOrderMsg> expectedOrders = interchangeFactory.getLabOrders("winpath/sensitivity.yaml", "0000000042");
        assertListOfMessagesEqual(expectedOrders, messagesFromHl7Message);
    }

    @Test
    public void testIncrementalIsolate1() throws Exception {
        List<? extends EmapOperationMessage> messagesFromHl7Message = processSingleMessageAndRemoveAdt("LabOrders/winpath/isolate_inc_1.txt");
        List<LabOrderMsg> expectedOrders = interchangeFactory.getLabOrders("winpath/isolate_inc_1.yaml", "0000000042");
        assertListOfMessagesEqual(expectedOrders, messagesFromHl7Message);
    }

    @Test
    public void testIncrementalIsolate2() throws Exception {
        List<? extends EmapOperationMessage> messagesFromHl7Message = processSingleMessageAndRemoveAdt("LabOrders/winpath/isolate_inc_2.txt");
        List<LabOrderMsg> expectedOrders = interchangeFactory.getLabOrders("winpath/isolate_inc_2.yaml", "0000000042");
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
            builtMessages.addAll(processSingleMessage(String.format(hl7PathTemplate, orderFile)));
            expectedOrders.add(interchangeFactory.buildLabOrderOverridingDefaults(
                    interchangeDefaults, String.format(interchangePathTemplate, orderFile)));
        }
        // add in final result
        builtMessages.addAll(processSingleMessage(String.format(hl7PathTemplate, "05_oru_r01")));
        expectedOrders.addAll(interchangeFactory.getLabOrders(String.format(interchangePathTemplate, "05_oru_r01"), "0000000042"));

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
        String[] orderFiles = {"01_orm_o01_nw_fbc_mg", "03_orm_o01_sn_fbcc", "05_orr_o02_na_fbcc",};
        for (String orderFile : orderFiles) {
            builtMessages.addAll(processSingleMessage(String.format(hl7PathTemplate, orderFile)));
            expectedOrders.add(interchangeFactory.buildLabOrderOverridingDefaults(
                    interchangeDefaults, String.format(interchangePathTemplate, orderFile)));
        }
        // add in final result
        builtMessages.addAll(processSingleMessage(String.format(hl7PathTemplate, "06_oru_ro1_fbcc")));
        expectedOrders.addAll(interchangeFactory.getLabOrders(String.format(interchangePathTemplate, "06_oru_ro1_fbcc"), "0000000042"));

        builtMessages = builtMessages.stream().filter(msg -> !(msg instanceof ImpliedAdtMessage)).collect(Collectors.toList());
        assertListOfMessagesEqual(expectedOrders, builtMessages);
    }

    @Test
    public void testPOCLabABL() throws Exception {
        List<? extends EmapOperationMessage> messagesFromHl7Message = processSingleMessageAndRemoveAdt("LabOrders/abl90_flex/venous.txt");
        List<LabOrderMsg> expectedOrders = interchangeFactory.getLabOrders("abl90_flex/venous.yaml", "0000000042");
        assertListOfMessagesEqual(expectedOrders, messagesFromHl7Message);
    }

    @Test
    public void testPOCLabBioConnect() throws Exception {
        List<? extends EmapOperationMessage> messagesFromHl7Message = processSingleMessageAndRemoveAdt("LabOrders/bio_connect/glucose.txt");
        List<LabOrderMsg> expectedOrders = interchangeFactory.getLabOrders("bio_connect/glucose.yaml", "0000000042");
        assertListOfMessagesEqual(expectedOrders, messagesFromHl7Message);
    }

    @Test
    public void testVitalSigns() throws Exception {
        List<? extends EmapOperationMessage> messagesFromHl7Message = processSingleMessageAndRemoveAdt("VitalSigns/MixedHL7Message.txt");
        List<Flowsheet> expectedOrders = interchangeFactory.getFlowsheets("hl7.yaml", "0000000042");
        assertListOfMessagesEqual(expectedOrders, messagesFromHl7Message);
    }

    @Test
    public void testVitalSignsProducesAdtFirst() throws Exception {
        EmapOperationMessage messageFromHl7 = processSingleMessage("VitalSigns/MixedHL7Message.txt").get(0);
        AdtMessage expectedAdt = interchangeFactory.getAdtMessage("FromNonAdt/flowsheet_oru_r01.yaml");
        Assertions.assertEquals(expectedAdt, messageFromHl7);
    }
}
