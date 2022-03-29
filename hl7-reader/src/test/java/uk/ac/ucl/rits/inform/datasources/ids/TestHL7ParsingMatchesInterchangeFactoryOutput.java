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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * A file that contains a hl7 message
 */
class MessageFile{

    Integer accessCount;
    Path filePath;

    MessageFile(Path filePath){
        this.filePath = filePath;
        this.accessCount = 0;
    }

    public boolean hasBeenAccessed(){
        return this.accessCount > 0;
    }

    public void incrementAccessCount(){
        this.accessCount += 1;
    }

    public boolean filenameInPath(String filename){
        return this.filePath.endsWith(filename);
    }
}


/**
 * Test that the HL7 output format matches that of the corresponding yaml files
 */
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Slf4j
public class TestHL7ParsingMatchesInterchangeFactoryOutput extends TestHl7MessageStream {
    InterchangeMessageFactory interchangeFactory = new InterchangeMessageFactory();

    private final List<MessageFile> messageFiles;

    static private final String[] EXCLUDED_MESSAGE_FILES = new String[]{
        "Adt/TestForJunit.txt",
        "Adt/birth_datetime_gmt.txt",                   // TODO: No births?
        "Adt/birth_datetime_midnight_bst.txt",
        "Adt/birth_datetime_bst.txt",
        "Adt/birth_date_gmt.txt",
        "Adt/birth_date_bst.txt",
        "BloodProducts/bts_o31.txt",                    // TODO: No bloods?
        "BloodProducts/oru_r01.txt",
        "NotesParser/oru_r01_sub_comments.txt",         // TODO: No notes?
        "NotesParser/empty_question.txt",
        "NotesParser/oru_r01_multiline_comment.txt",
        "NotesParser/comment_and_questions.txt",
        "NotesParser/multiline_answer.txt",
        "NotesParser/empty_first_answer.txt",
        "NotesParser/repeat_question.txt",
        "NotesParser/oru_r01_comment.txt",
        //"AdvanceDecision/minimal_w_questions.txt",     // TODO: Probably new_with_questions.yaml, but doesn't match
        "AdvanceDecision/multiple_requests.txt",         // No yaml
        "ConsultRequest/multiple_requests.txt",          // No yaml
        "VitalSigns/MixedHL7Message.txt",                // TODO: No vital signs?
        "VitalSigns/MultiOBR.txt",
        "VitalSigns/datetime_parsing.txt",
        "PatientInfection/multiple_infections.txt",      // No yaml
        "PatientInfection/mumps_no_add_time.txt",        // No yaml
        "PatientInfection/a05.txt",                      // No yaml
        "PatientInfection/2019_06_infection.txt",        // No yaml
        "PatientInfection/2019_05_infection.txt",        // No yaml
        "PatientInfection/earlier_infection.txt",        // No yaml
        "PatientInfection/no_infections.txt",            // No yaml
        "PatientInfection/mumps_resolved.txt",           // No yaml
        "LabOrders/bio_connect/normal_flag.txt",         // No yaml
        "LabOrders/winpath/oru_ro1_numeric.txt",         // No yaml...
        "LabOrders/winpath/isolate_no_growth.txt",
        "LabOrders/winpath/not_allowed_order_control_id.txt",
        "LabOrders/winpath/incremental_orders/05_oru_r01.txt",   // TODO: Probably, incremental_order_defaults.yaml
        "LabOrders/winpath/isolate_quantity.txt",
        "LabOrders/winpath/mistmatch_epic_order_id.txt",
        "LabOrders/winpath/orm_o01_sc.txt",
        "LabOrders/winpath/patient_repeats.txt",
        "LabOrders/winpath/orm_o01_questions.txt",
        "LabOrders/winpath/orr_o01_na.txt",
        "LabOrders/winpath/isolate_sensitivity.txt",
        "LabOrders/winpath/isolate_multiple_orders.txt",
        "LabOrders/winpath/isolate_clinical_notes.txt",
        "LabOrders/winpath/orm_o01_sn.txt",
        "LabOrders/winpath/orr_o02_cr.txt",
        "LabOrders/winpath/non_isolate_ce.txt",
        "LabOrders/winpath/cancel_orders/06_oru_r01_fbcc.txt",
        "LabOrders/winpath/oru_ro1_text.txt",
        "LabOrders/winpath/orm_o01_nw.txt",
        "LabOrders/winpath/isolate_culture_type.txt",
        "LabOrders/winpath/isolate_child_no_epic_id.txt",
        "LabOrders/winpath/subid_no_isolate.txt",
        "LabOrders/winpath/orm_o01_oc.txt",
        "LabOrders/winpath/orm_o01_ca.txt",
        "LabOrders/co_path/histology_stream/01_orm_o01_sc.txt",
        "LabOrders/co_path/histology_stream/02_oru_r01_re.txt",
        "LabOrders/co_path/oru_r01_copath.txt",
        "LabOrders/co_path/cpeap/03_oru_r01_re.txt",
        "LabOrders/co_path/cpeap/02_orm_o01_sc.txt",
        "LabOrders/co_path/cpeap/01_orm_o01_nw.txt",
        "LabOrders/co_path/cancel/05_oru_r01.txt",
        "LabOrders/co_path/orm_o01_multi_collection.txt",
        "LabOrders/co_path/incremental/04_oru_r01.txt",
        "LabOrders/co_path/orm_o01_sc.txt",
        "LabOrders/co_path/oru_r01_corrupt_bytes.txt",
        "LabOrders/co_path/oru_r01_multiple_value_reps.txt",
        "LabOrders/co_path/oru_r01_empty_report.txt",
        "LabOrders/co_path/oru_r01_copathplus.txt",
        "LabOrders/co_path/oru_r01_id_change.txt",
        "LabOrders/co_path/orm_o01_sn.txt",
        "LabOrders/co_path/oru_r01_sub_id_change.txt",
        "LabOrders/co_path/oru_r01_unrecognised_data_type.txt",
        "LabOrders/co_path/orr_o02_cr.txt",
        "LabOrders/co_path/orr_o02_na.txt",
        "LabOrders/co_path/oru_r01_report_coding_unexpected.txt",
        "LabOrders/co_path/orm_o01_nw.txt",
        "LabOrders/co_path/orm_o01_oc.txt",
        "LabOrders/co_path/oru_r01_multi_obx_report.txt",
        "LabOrders/co_path/orm_o01_ca.txt",
        "LabOrders/abl90_flex/no_collection_time.txt",
        "LabOrders/abl90_flex/unit.txt",
        "LabOrders/abl90_flex/ignored.txt",               //  ..
        "LabOrders/bank_manager/oru_r01_bmcomment.txt",   // TODO: No bank manager?
        "LabOrders/bank_manager/oru_r01_result_comment.txt",
        "LabOrders/bank_manager/oru_r01_result.txt",
        "LabOrders/bank_manager/oru_r01_cancel.txt",
        "LabOrders/bank_manager/oru_r01_order.txt",
        "LabOrders/bank_manager/oru_r01_no_orc.txt"
    };

    /**
     * Constructor for the test class. Populates all the message files
     * @throws IOException If a path cannot be accessed
     */
    TestHL7ParsingMatchesInterchangeFactoryOutput() throws IOException {

        this.messageFiles = new ArrayList<>();

        for (Path path: listFiles(Paths.get("src/test/resources/"), "txt")){

            if (Arrays.stream(EXCLUDED_MESSAGE_FILES).anyMatch(path::endsWith)){
                continue;
            }

            this.messageFiles.add(new MessageFile(path));
        }
    }

    /**
     * List all the files in the current and child directories
     * @param path  Directory to search from
     * @return List of paths
     * @throws IOException If the walk fails
     */
    private static List<Path> listFiles(Path path, String ext) throws IOException {

        List<Path> result;
        try (Stream<Path> walk = Files.walk(path)) {
            result = walk
                    .filter(Files::isRegularFile)
                    .filter(p -> (p.toString().endsWith(ext)))
                    .collect(Collectors.toList());
        }
        return result;
    }

    /**
     * Process a filename ensuring that it exists in the resource directory and so in the messageFiles, while
     * incrementing the access count
     * @param fileName Name of the file
     * @return The filename back
     */
    private String filename(String fileName) throws IOException{

        for (MessageFile file : messageFiles){
            if (file.filenameInPath(fileName)){
                file.incrementAccessCount();
                return fileName;
            }
        }

        throw new IOException("Failed to find "+fileName+" in the list of message files");
    }

    private void testAdtMessage(String adtFileStem) throws Exception {
        log.info("Testing ADT message with stem '{}'", adtFileStem);
        List<? extends EmapOperationMessage> messagesFromHl7Message = processSingleMessage(filename("Adt/" + adtFileStem + ".txt"));
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
        testAdtMessage("DoubleA01WithA13/A08");
        testAdtMessage("DoubleA01WithA13/A13");
        testAdtMessage("DoubleA01WithA13/FirstA01");
        testAdtMessage("DoubleA01WithA13/SecondA01");
    }

    void checkConsultMatchesInterchange(String fileName) throws Exception {
        List<? extends EmapOperationMessage> messagesFromHl7Message = processSingleMessageAndRemoveAdt(
                filename("ConsultRequest/"+fileName+".txt"));
        ConsultRequest expected = interchangeFactory.getConsult(String.format("%s.yaml", fileName));
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
                filename(txtFileName));
        AdvanceDecisionMessage expected = interchangeFactory.getAdvanceDecision(yamlFileName);
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
        checkAdvanceDecisionMatchesInterchange("AdvanceDecision/minimal_w_questions.txt", "new_with_questions.yaml");
    }

    @Test
    public void testLabIncrementalLoad() throws Exception {
        List<? extends EmapOperationMessage> messagesFromHl7Message = processLabHl7AndFilterToLabOrderMsgs(
                filename("LabOrders/winpath/Incremental.txt"));
        List<LabOrderMsg> expectedOrders = interchangeFactory.getLabOrders("winpath/incremental.yaml", "0000000042");
        assertListOfMessagesEqual(expectedOrders, messagesFromHl7Message);
    }

    @Test
    public void testLabIncrementalDuplicateResultSegment() throws Exception {
        List<? extends EmapOperationMessage> messagesFromHl7Message = processLabHl7AndFilterToLabOrderMsgs(
                filename("LabOrders/winpath/LabDuplicateResultSegment.txt"));
        List<LabOrderMsg> expectedOrders = interchangeFactory.getLabOrders("winpath/incremental_duplicate_result_segment.yaml", "0000000042");
        assertListOfMessagesEqual(expectedOrders, messagesFromHl7Message);
    }

    @Test
    public void testLabOrderMsg() throws Exception {
        List<? extends EmapOperationMessage> messagesFromHl7Message = processSingleMessageAndRemoveAdt(
                filename("LabOrders/winpath/ORU_R01.txt"));
        List<LabOrderMsg> expectedOrders = interchangeFactory.getLabOrders("winpath/ORU_R01.yaml", "0000000042");
        assertListOfMessagesEqual(expectedOrders, messagesFromHl7Message);
    }

    @Test
    public void testLabOrderMsgProducesAdtFirst() throws Exception {
        EmapOperationMessage messageFromHl7 = processSingleMessage(
                filename("LabOrders/winpath/ORU_R01.txt")).get(0);
        AdtMessage expectedAdt = interchangeFactory.getAdtMessage("FromNonAdt/lab_oru_r01.yaml");
        Assertions.assertEquals(expectedAdt, messageFromHl7);
    }

    @Test
    public void testLabSensitivity() throws Exception {
        List<? extends EmapOperationMessage> messagesFromHl7Message = processSingleMessageAndRemoveAdt(
                filename("LabOrders/winpath/Sensitivity.txt"));
        List<LabOrderMsg> expectedOrders = interchangeFactory.getLabOrders("winpath/sensitivity.yaml", "0000000042");
        assertListOfMessagesEqual(expectedOrders, messagesFromHl7Message);
    }

    @Test
    public void testIncrementalIsolate1() throws Exception {
        List<? extends EmapOperationMessage> messagesFromHl7Message = processSingleMessageAndRemoveAdt(
                filename("LabOrders/winpath/isolate_inc_1.txt"));
        List<LabOrderMsg> expectedOrders = interchangeFactory.getLabOrders("winpath/isolate_inc_1.yaml", "0000000042");
        assertListOfMessagesEqual(expectedOrders, messagesFromHl7Message);
    }

    @Test
    public void testIncrementalIsolate2() throws Exception {
        List<? extends EmapOperationMessage> messagesFromHl7Message = processSingleMessageAndRemoveAdt(
                filename("LabOrders/winpath/isolate_inc_2.txt"));
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
            builtMessages.addAll(processSingleMessage(filename(String.format(hl7PathTemplate, orderFile))));
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
        String[] orderFiles = {"01_orm_o01_nw_fbc_mg", "02_orm_o01_ca_fbc", "03_orm_o01_sn_fbcc", "04_orr_o02_cr_fbc", "05_orr_o02_na_fbcc"};
        for (String orderFile : orderFiles) {
            builtMessages.addAll(processSingleMessage(filename(String.format(hl7PathTemplate, orderFile))));
            expectedOrders.add(interchangeFactory.buildLabOrderOverridingDefaults(
                    interchangeDefaults, String.format(interchangePathTemplate, orderFile)));
        }
        // add in final result
        builtMessages.addAll(processSingleMessage(String.format(hl7PathTemplate, "06_oru_r01_fbcc")));
        expectedOrders.addAll(interchangeFactory.getLabOrders(String.format(interchangePathTemplate, "06_oru_r01_fbcc"), "0000000042"));

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
            builtMessages.addAll(processSingleMessage(filename(String.format(hl7PathTemplate, orderFile))));
            expectedOrders.add(interchangeFactory.buildLabOrderOverridingDefaults(
                    interchangeDefaults, String.format(interchangePathTemplate, orderFile)));
        }
        // add in final result
        builtMessages.addAll(processSingleMessage(String.format(hl7PathTemplate, "04_oru_r01")));
        expectedOrders.addAll(interchangeFactory.getLabOrders(String.format(interchangePathTemplate, "04_oru_r01"), "0000000042"));

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
            builtMessages.addAll(processSingleMessage(filename(String.format(hl7PathTemplate, orderFile))));
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
    void testCoPathQuestions() throws Exception {
        String hl7PathTemplate = "LabOrders/co_path/%s.txt";
        String interchangePathTemplate = "co_path/%s.yaml";
        String orderFile = "orm_o01_questions";
        String interchangeDefaults = String.format(interchangePathTemplate, "orm_defaults");
        String interchangePath = String.format(interchangePathTemplate, orderFile);

        EmapOperationMessage builtMessage = processSingleMessage(filename(String.format(hl7PathTemplate, orderFile)))
                .stream()
                .filter(msg -> !(msg instanceof ImpliedAdtMessage))
                .findFirst().orElseThrow();
        LabOrderMsg expectedMessage = interchangeFactory.buildLabOrderOverridingDefaults(interchangeDefaults, interchangePath);

        assertEquals(builtMessage, expectedMessage);
    }

    @Test
    void testCoPathByteValue() throws Exception {
        String hl7PathTemplate = "LabOrders/co_path/%s.txt";
        String interchangePathTemplate = "co_path/%s.yaml";
        String orderFile = "oru_r01_byte_value";

        LabOrderMsg builtMessage = (LabOrderMsg) processSingleMessage(filename(String.format(hl7PathTemplate, orderFile)))
                .stream()
                .filter(msg -> (msg instanceof LabOrderMsg))
                .findFirst().orElseThrow();
        LabOrderMsg expectedMessage = interchangeFactory.getLabOrder(String.format(interchangePathTemplate, orderFile));

        assertLabOrdersWithValueAsBytesEqual(List.of(builtMessage), List.of(expectedMessage));
    }

    @Test
    public void testPOCLabABL() throws Exception {
        List<? extends EmapOperationMessage> messagesFromHl7Message = processSingleMessageAndRemoveAdt(
                filename("LabOrders/abl90_flex/venous.txt"));
        List<LabOrderMsg> expectedOrders = interchangeFactory.getLabOrders("abl90_flex/venous.yaml", "0000000042");
        assertListOfMessagesEqual(expectedOrders, messagesFromHl7Message);
    }

    @Test
    public void testPOCLabBioConnect() throws Exception {
        List<? extends EmapOperationMessage> messagesFromHl7Message = processSingleMessageAndRemoveAdt(
                filename("LabOrders/bio_connect/glucose.txt"));
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

    @Test
    public void testPatientInfectionCreatesAdt() throws Exception {
        EmapOperationMessage messageFromHl7 = processSingleMessage("PatientInfection/a05.txt").get(0);
        AdtMessage expectedAdt = interchangeFactory.getAdtMessage("FromNonAdt/patient_infection_a05.yaml");
        Assertions.assertEquals(expectedAdt, messageFromHl7);
    }

    @Test
    public void testPatientInfection() throws Exception {
        EmapOperationMessage messageFromHl7 = processSingleMessage("PatientInfection/a05.txt")
                .stream()
                .filter(msg -> msg instanceof PatientInfection)
                .findFirst().orElseThrow();
        PatientInfection expected = interchangeFactory.getPatientInfections("hl7/minimal_mumps.yaml").get(0);
        Assertions.assertEquals(expected, messageFromHl7);
    }

    @AfterAll
    void checkAllFilesHaveBeenAccessed() throws Exception{

        if (messageFiles.stream().allMatch(MessageFile::hasBeenAccessed)) {
            return;
        }

        messageFiles.stream()
                .filter(m -> (!m.hasBeenAccessed()))
                .forEach(m -> System.out.println(m.filePath));
        throw new Exception("Not all the files have been accessed");
    }

}
