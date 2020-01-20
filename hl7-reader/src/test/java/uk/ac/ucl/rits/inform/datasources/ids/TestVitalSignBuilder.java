package uk.ac.ucl.rits.inform.datasources.ids;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v26.message.ORU_R01;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ucl.rits.inform.interchange.ResultStatus;
import uk.ac.ucl.rits.inform.interchange.VitalSigns;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import static junit.framework.TestCase.assertEquals;

@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@SpringBootTest
public class TestVitalSignBuilder {
    private List<VitalSigns> vitalSigns;
    private VitalSigns firstVitalSign;

    @Before
    public void setUp() throws IOException, HL7Exception {
        String hl7 = HL7Utils.readHl7FromResource("VitalSignHL7Message.txt");
        Message hl7Msg = HL7Utils.parseHl7String(hl7);
        vitalSigns = new VitalSignBuilder("42", (ORU_R01) hl7Msg).getMessages();
        firstVitalSign = vitalSigns.get(0);
    }

    @Test
    public void testMRN() {
        String result = firstVitalSign.getMrn();
        assertEquals("6537077", result);
    }

    @Test
    public void testVisitNumber() {
        String result = firstVitalSign.getVisitNumber();
        assertEquals("1234TESTVISITNUM", result);
    }

    @Test
    public void testVitalSignIdentifier() {
        String result = firstVitalSign.getVitalSignIdentifier();
        assertEquals("EPIC$271649006", result);
    }

    @Test
    public void testNumericValue() {
        Double result = firstVitalSign.getNumericValue();
        assertEquals(132.0, result);
    }

    @Test
    public void testStringValue() {
        String result = vitalSigns.get(6).getStringValue();
        assertEquals("string value", result);
    }

    @Test
    public void testResultStatusFtoSave() {
        // result status is 'F' so should be converted to SAVE
        ResultStatus result = firstVitalSign.getResultStatus();
        assertEquals(ResultStatus.SAVE, result);
    }

    @Test
    public void testResultStatusCtoSave() {
        // result status is 'C' so should be converted to SAVE
        ResultStatus result = vitalSigns.get(1).getResultStatus();
        assertEquals(ResultStatus.SAVE, result);
    }

    @Test
    public void testResultStatusDtoDelete() {
        // result status is 'D' so should be converted to DELETE
        ResultStatus result = vitalSigns.get(2).getResultStatus();
        assertEquals(ResultStatus.DELETE, result);
    }

    @Test
    public void testResultStatusUnrecognisedSave() {
        // result status is 'NOT_EXPECTED' so should be converted to SAVE
        ResultStatus result = vitalSigns.get(3).getResultStatus();
        assertEquals(ResultStatus.SAVE, result);
    }

    @Test
    public void testUnit() {
        String result = firstVitalSign.getUnit();
        assertEquals("mm[Hg]", result);
    }

    @Test
    public void testObservationTimeTaken() {
        Instant result = firstVitalSign.getObservationTimeTaken();
        assertEquals(Instant.parse("2010-02-11T22:05:25.00Z"), result);
    }

    @Test
    public void testSourceMessageId() {
        String result = firstVitalSign.getSourceMessageId();
        assertEquals("42$01", result);
    }

}
