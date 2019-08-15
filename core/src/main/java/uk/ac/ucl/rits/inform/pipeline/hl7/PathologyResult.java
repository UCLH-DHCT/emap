package uk.ac.ucl.rits.inform.pipeline.hl7;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.Type;
import ca.uhn.hl7v2.model.Varies;
import ca.uhn.hl7v2.model.v26.datatype.CE;
import ca.uhn.hl7v2.model.v26.datatype.CWE;
import ca.uhn.hl7v2.model.v26.datatype.ED;
import ca.uhn.hl7v2.model.v26.datatype.FT;
import ca.uhn.hl7v2.model.v26.datatype.IS;
import ca.uhn.hl7v2.model.v26.datatype.NM;
import ca.uhn.hl7v2.model.v26.datatype.ST;
import ca.uhn.hl7v2.model.v26.datatype.TX;
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

    private String observationSubId;
    private Double numericValue;
    private String stringValue;

    // fields of the CE data type
    private String isolateLocalCode;
    private String isolateLocalDescription;
    private String isolateCodingSystem;

    private String units;
    private String referenceRange;
    private String resultStatus;

    private Instant resultTime;
    private String notes;

    /**
     * A sensitivity is just a nested pathology order with results.
     * HL7 has fields for working out parentage.
     * PathologySensitivity type can probably go away.
     */
    private List<PathologyOrder> pathologySensitivities = new ArrayList<>();

    private String epicCareOrderNumber;

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
        // OBR segments for sensitivities don't have an OBR-22 status change time
        // so use the time from the parent?
        resultTime = HL7Utils.interpretLocalTime(obr.getObr22_ResultsRptStatusChngDateTime());

        // each result needs to know this so sensitivities can be correctly assigned
        epicCareOrderNumber = obr.getObr2_PlacerOrderNumber().getEi1_EntityIdentifier().getValueOrEmpty();
        // identifies the particular test (eg. red cell count)
        CWE obx3 = obx.getObx3_ObservationIdentifier();
        testItemLocalCode = obx3.getCwe1_Identifier().getValueOrEmpty();
        testItemLocalDescription = obx3.getCwe2_Text().getValueOrEmpty();
        testItemCodingSystem = obx3.getCwe3_NameOfCodingSystem().getValueOrEmpty();
        resultStatus = obx.getObx11_ObservationResultStatus().getValueOrEmpty();
        observationSubId = obx.getObx4_ObservationSubID().getValueOrEmpty();

        populateObx(obx);
        populateNotes(notes);
    }

    /**
     * Populate OBX fields. Mainly tested where value type is NM - numeric.
     * @param obx the OBX segment
     */
    private void populateObx(OBX obx) {
        Varies dataVaries = obx.getObx5_ObservationValue(0);
        Type data = dataVaries.getData();
        this.stringValue = "";
        if (data instanceof ST
                || data instanceof FT
                || data instanceof TX
                || data instanceof NM) {
            // Store the string value for numerics too, as they can be
            // ranges or "less than" values
            this.stringValue = data.toString();
            // HAPI can return null from toString, fix this
            if (this.stringValue == null) {
                this.stringValue = "";
            }
            if (data instanceof NM) {
                try {
                    numericValue = Double.parseDouble(this.stringValue);
                } catch (NumberFormatException e) {
                    logger.debug(String.format("Non numeric result %s", this.stringValue));
                }
            }
        } else if (data instanceof CE) {
            CE ceData = (CE) data;
            isolateLocalCode = ceData.getCe1_Identifier().getValue();
            isolateLocalDescription = ceData.getCe2_Text().getValue();
            isolateCodingSystem = ceData.getCe3_NameOfCodingSystem().getValue();
            if (isolateLocalCode == null) {
                isolateLocalCode = "";
            }
            if (isolateLocalDescription == null) {
                isolateLocalDescription = "";
            }
            if (isolateCodingSystem == null) {
                isolateCodingSystem = "";
            }
        } else if (data instanceof ED) {
            logger.warn("ED not implemented yet");
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
    public Double getNumericValue() {
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
    public List<PathologyOrder> getPathologySensitivities() {
        return pathologySensitivities;
    }

    /**
     * @return the sub-ID that links observations together
     */
    public String getObservationSubId() {
        return observationSubId;
    }

    /**
     * @return Does this observation contain only redundant information
     * that can be ignored? Eg. header and footer of a report intended
     * to be human-readable.
     */
    public boolean isIgnorable() {
        return stringValue.equals("URINE CULTURE REPORT")
                // XXX: this needs some pattern matching and better rules
                || stringValue.equals("COMPLETE: 14/07/19");
    }

    /**
     * Merge another pathology result into this one.
     * Eg. an adjacent OBX segment that is linked by a sub ID.
     * @param pathologyResult the other pathology result to merge in
     */
    public void mergeResult(PathologyResult pathologyResult) {
        // Will need to identify HOW to merge results.
        // Eg. identify that pathologyResult contains an isolate,
        // so only copy the isolate fields from it.
        if (!pathologyResult.isolateLocalCode.isEmpty()) {
            this.isolateLocalCode = pathologyResult.isolateLocalCode;
            this.isolateLocalDescription = pathologyResult.isolateLocalDescription;
            this.isolateCodingSystem = pathologyResult.isolateCodingSystem;
        }
    }

    /**
     * @return the Epic order number that this result relates to
     */
    public String getEpicCareOrderNumber() {
        return epicCareOrderNumber;
    }

    /**
     * @return local code of the isolate
     */
    public String getIsolateLocalCode() {
        return isolateLocalCode;
    }

    /**
     * @return local description of the isolate
     */
    public String getIsolateLocalDescription() {
        return isolateLocalDescription;
    }

    /**
     * @return coding system of the isolate (eg. WinPath)
     */
    public String getIsolateCodingSystem() {
        return isolateCodingSystem;
    }
}
