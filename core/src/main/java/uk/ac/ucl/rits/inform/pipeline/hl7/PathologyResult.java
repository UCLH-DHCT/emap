package uk.ac.ucl.rits.inform.pipeline.hl7;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.Type;
import ca.uhn.hl7v2.model.Varies;
import ca.uhn.hl7v2.model.v26.datatype.CWE;
import ca.uhn.hl7v2.model.v26.segment.OBR;
import ca.uhn.hl7v2.model.v26.segment.OBX;
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

    private String testItemLocalCode;
    private String testItemLocalDescription;
    private String testItemCodingSystem;

    private double numericValue;
    private String stringValue;
    private String units;

    private Instant resultTime;

    private List<PathologySensitivity> pathologySensitivities = new ArrayList<>();

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

        // identifies the particular test (eg. red cell count)
        CWE obx3 = obx.getObx3_ObservationIdentifier();
        testItemLocalCode = obx3.getCwe1_Identifier().getValueOrEmpty();
        testItemLocalDescription = obx3.getCwe2_Text().getValueOrEmpty();
        testItemCodingSystem = obx3.getCwe3_NameOfCodingSystem().getValueOrEmpty();

        Varies data = obx.getObx5_ObservationValue(0);
        Type data2 = data.getData();
        this.stringValue = data2.toString();
        try {
            numericValue = Double.parseDouble(this.stringValue);
        } catch (NumberFormatException e) {
            logger.debug(String.format("Non numeric result %s", this.stringValue));
        }
        units = obx.getObx6_Units().getCwe1_Identifier().getValueOrEmpty();
    }

    /**
     * @return value type for the observation line (eg. NM)
     */
    public String getValueType() {
        return valueType;
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
     * Get the String representation of the result.
     *
     * @return the String representation of the results.
     */
    public String getStringValue() {
        return this.stringValue;
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
