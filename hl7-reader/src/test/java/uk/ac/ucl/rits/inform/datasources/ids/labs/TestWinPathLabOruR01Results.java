package uk.ac.ucl.rits.inform.datasources.ids.labs;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.ac.ucl.rits.inform.datasources.ids.exceptions.Hl7InconsistencyException;
import uk.ac.ucl.rits.inform.datasources.ids.exceptions.Hl7MessageIgnoredException;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessage;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.adt.ImpliedAdtMessage;
import uk.ac.ucl.rits.inform.interchange.lab.LabIsolateMsg;
import uk.ac.ucl.rits.inform.interchange.lab.LabOrderMsg;
import uk.ac.ucl.rits.inform.interchange.lab.LabResultMsg;
import uk.ac.ucl.rits.inform.interchange.lab.LabResultStatus;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test LabResults derived from ORU RO1 from Winpath
 * @author Stef Piatek
 */
@ActiveProfiles("test")
@SpringBootTest
class TestWinPathLabOruR01Results {
    @Autowired
    private LabReader labReader;
    private static final String FILE_TEMPLATE = "LabOrders/winpath/%s.txt";

    @Test
    void testSpecimenType() throws Exception {
        LabOrderMsg msg = labReader.process(FILE_TEMPLATE, "oru_ro1_text");
        assertEquals(InterchangeValue.buildFromHl7("CTNS"), msg.getSpecimenType());
    }

    /**
     * Test battery code and description are correctly parsed.
     */
    @Test
    void testTestCodes() throws Exception {
        LabOrderMsg msg = labReader.process(FILE_TEMPLATE, "oru_ro1_text");
        assertEquals("NCOV", msg.getTestBatteryLocalCode());
    }

    /**
     * Test LabResult result status is parsed correctly
     */
    @Test
    void testResultStatus() throws Exception {
        LabResultMsg result = labReader.getResult(FILE_TEMPLATE, "oru_ro1_text", "NCVS");
        assertEquals(LabResultStatus.FINAL, result.getResultStatus());
    }

    /**
     * OBX result status is unkonwn - should
     */
    @Test
    void testResultStatusUnkonwn() throws Exception {
        LabResultMsg result = labReader.getResult(FILE_TEMPLATE, "oru_ro1_text", "UNKNOWN_RS");
        assertEquals(LabResultStatus.UNKNOWN, result.getResultStatus());
    }

    /**
     * Test that string values and not numeric values are set for string values.
     */
    @Test
    void testStringResultOnlyParsed() throws Exception {
        LabOrderMsg msg = labReader.process(FILE_TEMPLATE, "oru_ro1_text");
        List<LabResultMsg> labResultMsgs = msg.getLabResultMsgs();
        Map<String, LabResultMsg> resultsByItemCode = labReader.getResultsByItemCode(labResultMsgs);
        LabResultMsg ncvs = resultsByItemCode.get("NCVS");
        LabResultMsg ncvp = resultsByItemCode.get("NCVP");
        LabResultMsg ncvl = resultsByItemCode.get("NCVL");

        assertTrue(ncvs.getNumericValue().isUnknown());
        assertEquals(InterchangeValue.buildFromHl7("CTNS"), ncvs.getStringValue());

        assertTrue(ncvp.getNumericValue().isUnknown());
        assertEquals(InterchangeValue.buildFromHl7("NOT detected"), ncvp.getStringValue());

        assertTrue(ncvl.getNumericValue().isUnknown());
        String ncvlResult = new StringBuilder("Please note that this test was performed using\n")
                .append("the Hologic Panther Fusion Assay.\n")
                .append("This new assay is currently not UKAS accredited,\n")
                .append("but is internally verified. UKAS extension\n")
                .append("to scope to include this has been submitted.").toString();
        assertEquals(InterchangeValue.buildFromHl7(ncvlResult), ncvl.getStringValue());
    }


    /**
     * Test that numeric value, and units are set
     */
    @Test
    void testNumericSimplePath() throws Exception {
        LabResultMsg result = labReader.getResult(FILE_TEMPLATE, "oru_ro1_numeric", "ALP");
        assertEquals(InterchangeValue.buildFromHl7(104.0), result.getNumericValue());
        assertEquals(InterchangeValue.buildFromHl7("IU/L"), result.getUnits());
        assertEquals("=", result.getResultOperator());
    }

    /**
     * Test that less than value sets the result operator and numeric value correctly
     */
    @Test
    void testResultOperatorLessThan() throws Exception {
        LabResultMsg result = labReader.getResult(FILE_TEMPLATE, "oru_ro1_numeric", "<VAL");
        assertEquals("<", result.getResultOperator());
        assertEquals(InterchangeValue.buildFromHl7(7.0), result.getNumericValue());
    }

    /**
     * Test that greater than value sets the result operator and numeric value correctly
     */
    @Test
    void testResultOperatorGreaterThan() throws Exception {
        LabResultMsg result = labReader.getResult(FILE_TEMPLATE, "oru_ro1_numeric", ">VAL");
        assertEquals(">", result.getResultOperator());
        assertEquals(InterchangeValue.buildFromHl7(7.0), result.getNumericValue());
    }

    /**
     * Test unknown result operator should still build, but with no known numeric result and the string value giving the original value
     */
    @Test
    void testUnknownResultOperator() throws Exception {
        LabResultMsg result = labReader.getResult(FILE_TEMPLATE, "oru_ro1_numeric", "UNKONWN_OPERATOR");
        assertTrue(result.getNumericValue().isDelete());
        assertEquals(InterchangeValue.buildFromHl7("?7"), result.getStringValue());
    }

    /**
     * Range is 35-104 -> should be parsed correctly
     */
    @Test
    void testSimpleRange() throws Exception {
        LabResultMsg result = labReader.getResult(FILE_TEMPLATE, "oru_ro1_numeric", "ALP");
        assertEquals(InterchangeValue.buildFromHl7(104.0), result.getReferenceHigh());
        assertEquals(InterchangeValue.buildFromHl7(35.0), result.getReferenceLow());
    }

    /**
     * Range is <7.2. Upper limit should be 7.2 - lower should delete if exists
     */
    @Test
    void testLessThanRange() throws Exception {
        LabResultMsg result = labReader.getResult(FILE_TEMPLATE, "oru_ro1_numeric", "LESS_RANGE");
        assertEquals(InterchangeValue.buildFromHl7(7.2), result.getReferenceHigh());
        assertTrue(result.getReferenceLow().isDelete());
    }

    /**
     * Range is >7.2. Lower limit should be 7.2, upper should delete if exists
     */
    @Test
    void testGreaterThanRange() throws Exception {
        LabResultMsg result = labReader.getResult(FILE_TEMPLATE, "oru_ro1_numeric", "GREATER_RANGE");
        assertTrue(result.getReferenceHigh().isDelete());
        assertEquals(InterchangeValue.buildFromHl7(7.2), result.getReferenceLow());
    }

    /**
     * Range is 0-2-7.2. Unparsable so should not set a range
     */
    @Test
    void testRangeUnparsable() throws Exception {
        LabResultMsg result = labReader.getResult(FILE_TEMPLATE, "oru_ro1_numeric", "UNPARSABLE_RANGE");
        assertTrue(result.getReferenceHigh().isUnknown());
        assertTrue(result.getReferenceLow().isUnknown());
    }

    @Test
    void testNotes() throws Exception {
        LabResultMsg result = labReader.getResult(FILE_TEMPLATE, "oru_ro1_numeric", "SFOL");
        InterchangeValue<String> expected = InterchangeValue.buildFromHl7("Folate result assumes no folic acid supplement\non day of sampling");
        assertEquals(expected, result.getNotes());
    }

    @Test
    void testAbnormalFlagPresent() throws Exception {
        LabResultMsg result = labReader.getResult(FILE_TEMPLATE, "oru_ro1_numeric", "VB12");
        assertEquals(InterchangeValue.buildFromHl7("H"), result.getAbnormalFlag());
    }

    @Test
    void testAbnormalFlagAbsent() throws Exception {
        LabResultMsg result = labReader.getResult(FILE_TEMPLATE, "oru_ro1_numeric", "ALP");
        assertTrue(result.getAbnormalFlag().isDelete());
    }

    @Test
    void testClinicalInformation() throws Exception {
        LabOrderMsg orderMsg = labReader.process(FILE_TEMPLATE, "oru_ro1_text");
        String expected = "Can you confirm this order has been discussed with and approved by a Virologist-";
        assertEquals(InterchangeValue.buildFromHl7(expected), orderMsg.getClinicalInformation());
    }

    /**
     * OBR and OBC epic Id don't agree, should throw.
     */
    @Test
    void testMismatchEpicId() {
        assertThrows(Hl7InconsistencyException.class, () -> labReader.process(FILE_TEMPLATE, "mistmatch_epic_order_id"));
    }

    /**
     * Not expecting WinPath to have multiple patient results in a single message.
     */
    @Test
    void testPatientRepeatsThrows() {
        assertThrows(Hl7MessageIgnoredException.class, () -> labReader.process(FILE_TEMPLATE, "patient_repeats"));
    }

    /**
     * Order control Id not allowed, the order should not be outputted.
     */
    @Test
    void testNotAllowedOrderControlId() throws Exception {
        List<? extends EmapOperationMessage> msgs = labReader
                .processSingleMessage(String.format(FILE_TEMPLATE, "not_allowed_order_control_id"))
                .stream().filter(msg -> !(msg instanceof ImpliedAdtMessage)).collect(Collectors.toList());
        assertTrue(msgs.isEmpty());
    }

    /**
     * Non-ISOLATE coded data should throw an exception
     */
    @Test
    void testNonIsolateCoded() {
        assertThrows(Hl7InconsistencyException.class, () -> labReader.process(FILE_TEMPLATE, "non_isolate_ce"));
    }

    /**
     * No growth has trailing spaces in the code, these should be removed
     * "NG5   ^No growth after 5 days incubation" -> "NG5"
     * @throws Exception shouldn't happen
     */
    @Test
    void testNoGrowthCodeIsStripped() throws Exception {
        LabOrderMsg orderMsg = labReader.process(FILE_TEMPLATE, "culture_no_growth");
        List<LabResultMsg> result = orderMsg.getLabResultMsgs()
                .stream()
                .filter(rs -> "1".equals(rs.getObservationSubId()))
                .collect(Collectors.toList());
        assertEquals(1, result.size());
        String ng5 = result.get(0).getLabIsolate().getIsolateCode();
        assertEquals("NG5", ng5);
    }

    @Test
    void testClinicalInformationIsAddedToSensitivity() throws Exception {
        LabOrderMsg orderMsg = labReader.process(FILE_TEMPLATE, "isolate_clinical_notes");
        List<LabResultMsg> result = orderMsg.getLabResultMsgs()
                .stream()
                .filter(rs -> "1".equals(rs.getObservationSubId()))
                .collect(Collectors.toList());
        assertEquals(1, result.size());
        InterchangeValue<String> clinical = result.get(0).getLabIsolate().getClinicalInformation();

        assertEquals(InterchangeValue.buildFromHl7("Gentamicin resistant"), clinical);
    }


}
