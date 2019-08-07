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
import ca.uhn.hl7v2.model.v26.datatype.FT;
import ca.uhn.hl7v2.model.v26.datatype.IS;
import ca.uhn.hl7v2.model.v26.segment.NTE;
import ca.uhn.hl7v2.model.v26.segment.OBR;
import ca.uhn.hl7v2.model.v26.segment.OBX;

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
    private String referenceRange;
    private String resultStatus;

    private Instant resultTime;
    private String notes;

    private List<PathologySensitivity> pathologySensitivities = new ArrayList<>();

    /**
     * This class stores an individual result (ie. OBX segment)
     * because this maps 1:1 with a patient fact in Inform-db.
     * Although of course there is parent information in the HL7
     * message (ORC + OBR), this is flattened here.
     * @param obx the OBX segment for this result
     * @param obr the OBR segment for this result (will be the same segment shared with other OBXs)
     * @param notes list of NTE segments for this result
     * @throws DataTypeException if required datetime fields cannot be parsed
     */
    public PathologyResult(OBX obx, OBR obr, List<NTE> notes) throws DataTypeException {
        // see HL7 Table 0125 for value types
        // In addition to NM (Numeric), we get (descending popularity):
        //     ED (Encapsulated Data), ST (String), FT (Formatted text - display),
        //     TX (Text data - display), DT (Date), CE (deprecated and replaced by CNE or CWE, coded entry with or without exceptions)
        valueType = obx.getObx2_ValueType().getValueOrEmpty();
        resultTime = HL7Utils.interpretLocalTime(obr.getObr22_ResultsRptStatusChngDateTime());

        // identifies the particular test (eg. red cell count)
        CWE obx3 = obx.getObx3_ObservationIdentifier();
        testItemLocalCode = obx3.getCwe1_Identifier().getValueOrEmpty();
        testItemLocalDescription = obx3.getCwe2_Text().getValueOrEmpty();
        testItemCodingSystem = obx3.getCwe3_NameOfCodingSystem().getValueOrEmpty();
        resultStatus = obx.getObx11_ObservationResultStatus().getValueOrEmpty();

        populateNumeric(obx);
        populateNotes(notes);
    }

    /**
     * Populate OBX fields assuming the value type is NM - numeric.
     * @param obx the OBX segment
     */
    private void populateNumeric(OBX obx) {
        Varies data = obx.getObx5_ObservationValue(0);
        Type data2 = data.getData();
        this.stringValue = data2.toString();
        // HAPI can return null from toString, fix this
        if (this.stringValue == null) {
            this.stringValue = "";
        }
        try {
            numericValue = Double.parseDouble(this.stringValue);
        } catch (NumberFormatException e) {
            logger.debug(String.format("Non numeric result %s", this.stringValue));
        }
        units = obx.getObx6_Units().getCwe1_Identifier().getValueOrEmpty();
        referenceRange = obx.getObx7_ReferencesRange().getValueOrEmpty();
        // how many abnormal flags can we get in practice?
        IS[] abnormalFlags = obx.getObx8_AbnormalFlags();
    }

    /**
     * Gather all the NTE segments that relate to this OBX and save as concatenated value.
     * @param notes all NTE segments for the observation
     */
    private void populateNotes(List<NTE> notes) {
        List<String> allNotes = new ArrayList<>();
        for (NTE nt : notes) {
            FT[] fts = nt.getNte3_Comment();
            for (FT ft : fts) {
                allNotes.add(ft.getValueOrEmpty());
            }
        }
        this.notes = String.join(" ", allNotes);
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
     * @return the time the result was reported. Can differ within
     * a test battery if results are delivered bit by bit.
     */
    public Instant getResultTime() {
        return resultTime;
    }

    /**
     * @return the reference range for the numerical result
     */
    public String getReferenceRange() {
        return referenceRange;
    }

    /**
     * @return the result status. See HL7 Table 0085.
     */
    public String getResultStatus() {
        return resultStatus;
    }

    /**
     * @return the notes accompanying the result, if any
     */
    public String getNotes() {
        return notes;
    }

    /**
     * @return all sensitivities
     */
    public List<PathologySensitivity> getPathologySensitivities() {
        return pathologySensitivities;
    }
}
