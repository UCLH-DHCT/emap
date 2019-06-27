package uk.ac.ucl.rits.inform.pipeline.hl7;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.Type;
import ca.uhn.hl7v2.model.Varies;
import ca.uhn.hl7v2.model.v27.datatype.CWE;
import ca.uhn.hl7v2.model.v27.segment.OBR;
import ca.uhn.hl7v2.model.v27.segment.OBX;
import uk.ac.ucl.rits.inform.pipeline.exceptions.SkipPathologyResult;

/**
 * Turn part of an HL7 pathology result message into a (flatter) structure
 * more suited to our needs.
 *
 * @author Jeremy Stein
 */
public class PathologyResult {
    private static final Logger logger = LoggerFactory.getLogger(PathologyResult.class);

    private String valueType;

    private String testBatteryLocalCode;
    private String testBatteryLocalDescription;
    private String testBatteryCodingSystem;

    private String testItemLocalCode;
    private String testItemLocalDescription;
    private String testItemCodingSystem;

    private double numericValue;
    private String units;

    private Instant resultTime;

    /**
     * This class stores an individual result (ie. OBX segment)
     * because this maps 1:1 with a patient fact in Inform-db.
     * Although of course there is parent information in the HL7
     * message (ORC + OBR), this is flattened here.
     * @param obx the OBX segment for this result
     * @param obr the OBR segment for this result (will be the same segment shared with other OBXs)
     */

    public PathologyResult(OBX obx, OBR obr) {
        valueType = obx.getObx2_ValueType().getValueOrEmpty();
        if (!valueType.equals("NM")) {
            // ignore free text (FT), etc, for now
            throw new SkipPathologyResult("only handling numeric (NM), got " + valueType);
        }

        try {
            resultTime = HL7Utils.interpretLocalTime(obr.getObr7_ObservationDateTime());
        } catch (DataTypeException e) {
            resultTime = null;
            logger.error("resultTime parsing error", e);
        }
        // identifies the battery of tests that has been performed (eg. FBC)
        CWE obr4 = obr.getObr4_UniversalServiceIdentifier();
        testBatteryLocalCode = obr4.getCwe1_Identifier().getValueOrEmpty();
        testBatteryLocalDescription = obr4.getCwe2_Text().getValueOrEmpty();
        testBatteryCodingSystem = obr4.getCwe3_NameOfCodingSystem().getValueOrEmpty();

        if (!testBatteryLocalCode.equals("FBC") && !testBatteryLocalCode.equals("FBCE") && !testBatteryLocalCode.equals("FBCY")) {
            throw new SkipPathologyResult("ignoring all but FBCY, got " + valueType);
        }
        // identifies the particular test (eg. red cell count)
        CWE obx3 = obx.getObx3_ObservationIdentifier();
        testItemLocalCode = obx3.getCwe1_Identifier().getValueOrEmpty();
        testItemLocalDescription = obx3.getCwe2_Text().getValueOrEmpty();
        testItemCodingSystem = obx3.getCwe3_NameOfCodingSystem().getValueOrEmpty();

        Varies data = obx.getObx5_ObservationValue(0);
        Type data2 = data.getData();
        numericValue = Double.parseDouble(data2.toString());
        units = obx.getObx6_Units().getCwe1_Identifier().getValueOrEmpty();
    }

    @Override
    public String toString() {
        return "PathologyResult [testLocalCode=" + testBatteryLocalCode + ", testLocalDescription=" + testBatteryLocalDescription
                + ", testCodingSystem=" + testBatteryCodingSystem + ", valueType=" + valueType + ", numericValue="
                + numericValue + ", units=" + units + ", resultLocalCode=" + testItemLocalCode
                + ", resultLocalDescription=" + testItemLocalDescription + ", resultCodingSystem=" + testItemCodingSystem
                + "]";
    }

    /**
     * @return value type for the observation line (eg. NM)
     */
    public String getValueType() {
        return valueType;
    }

    /**
     * @return the local code for the suite of tests
     */
    public String getTestBatteryLocalCode() {
        return testBatteryLocalCode;
    }

    /**
     * @return the description for the suite of tests
     */
    public String getTestBatteryLocalDescription() {
        return testBatteryLocalDescription;
    }

    /**
     * @return the coding system (eg. WinPath)
     */
    public String getTestBatteryCodingSystem() {
        return testBatteryCodingSystem;
    }

    /**
     * @return the local code for the particular test
     */
    public String getTestItemLocalCode() {
        return testItemLocalCode;
    }

    /**
     * @return the description for the particular test
     */
    public String getTestItemLocalDescription() {
        return testItemLocalDescription;
    }

    /**
     * @return the coding system (eg. WinPath)
     */
    public String getTestItemCodingSystem() {
        return testItemCodingSystem;
    }

    /**
     * @return the numerical value of the test (if numerical)
     */
    public double getNumericValue() {
        return numericValue;
    }

    /**
     * @return the units for a numerical test
     */
    public String getUnits() {
        return units;
    }

    /**
     * @return the observation time for the test
     */
    public Instant getResultTime() {
        return resultTime;
    }
}
