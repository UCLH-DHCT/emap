package uk.ac.ucl.rits.inform.datasources.ids;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.Type;
import ca.uhn.hl7v2.model.Varies;
import ca.uhn.hl7v2.model.v26.datatype.CE;
import ca.uhn.hl7v2.model.v26.datatype.CWE;
import ca.uhn.hl7v2.model.v26.datatype.FT;
import ca.uhn.hl7v2.model.v26.datatype.IS;
import ca.uhn.hl7v2.model.v26.datatype.NM;
import ca.uhn.hl7v2.model.v26.datatype.ST;
import ca.uhn.hl7v2.model.v26.datatype.TX;
import ca.uhn.hl7v2.model.v26.segment.NTE;
import ca.uhn.hl7v2.model.v26.segment.OBR;
import ca.uhn.hl7v2.model.v26.segment.OBX;
import uk.ac.ucl.rits.inform.interchange.PathologyResult;

/**
 * Turn part of an HL7 pathology result message into a (flatter) structure
 * more suited to our needs.
 *
 * @author Jeremy Stein
 */
public class PathologyResultBuilder {
    private static final Logger logger = LoggerFactory.getLogger(PathologyResultBuilder.class);

    private PathologyResult msg = new PathologyResult();

    /**
     * @return the underlying message we have now built
     */
    public PathologyResult getMessage() {
        return msg;
    }

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
    public PathologyResultBuilder(OBX obx, OBR obr, List<NTE> notes) throws DataTypeException {
        // see HL7 Table 0125 for value types
        // In addition to NM (Numeric), we get (descending popularity):
        //     ED (Encapsulated Data), ST (String), FT (Formatted text - display),
        //     TX (Text data - display), DT (Date), CE (deprecated and replaced by CNE or CWE, coded entry with or without exceptions)
        msg.setValueType(obx.getObx2_ValueType().getValueOrEmpty());
        // OBR segments for sensitivities don't have an OBR-22 status change time
        // so use the time from the parent?
        msg.setResultTime(HL7Utils.interpretLocalTime(obr.getObr22_ResultsRptStatusChngDateTime()));

        // each result needs to know this so sensitivities can be correctly assigned
        msg.setEpicCareOrderNumber(obr.getObr2_PlacerOrderNumber().getEi1_EntityIdentifier().getValueOrEmpty());
        // identifies the particular test (eg. red cell count)
        CWE obx3 = obx.getObx3_ObservationIdentifier();
        msg.setTestItemLocalCode(obx3.getCwe1_Identifier().getValueOrEmpty());
        msg.setTestItemLocalDescription(obx3.getCwe2_Text().getValueOrEmpty());
        msg.setTestItemCodingSystem(obx3.getCwe3_NameOfCodingSystem().getValueOrEmpty());
        msg.setResultStatus(obx.getObx11_ObservationResultStatus().getValueOrEmpty());
        msg.setObservationSubId(obx.getObx4_ObservationSubID().getValueOrEmpty());

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
        if (data instanceof ST
                || data instanceof FT
                || data instanceof TX
                || data instanceof NM) {
            // Store the string value for numerics too, as they can be
            // ranges or "less than" values
            msg.setStringValue(data.toString());
            // HAPI can return null from toString, fix this
            if (msg.getStringValue() == null) {
                msg.setStringValue("");
            }
            if (data instanceof NM) {
                try {
                    msg.setNumericValue(Double.parseDouble(msg.getStringValue()));
                } catch (NumberFormatException e) {
                    logger.debug(String.format("Non numeric result %s", msg.getStringValue()));
                }
            }
        } else if (data instanceof CE) {
            CE ceData = (CE) data;
            msg.setIsolateLocalCode(ceData.getCe1_Identifier().getValue());
            msg.setIsolateLocalDescription(ceData.getCe2_Text().getValue());
            msg.setIsolateCodingSystem(ceData.getCe3_NameOfCodingSystem().getValue());
            if (msg.getIsolateLocalCode() == null) {
                msg.setIsolateLocalCode("");
            }
            if (msg.getIsolateLocalDescription() == null) {
                msg.setIsolateLocalDescription("");
            }
            if (msg.getIsolateCodingSystem() == null) {
                msg.setIsolateCodingSystem("");
            }
        }
        // also need to handle case where (data instanceof ED)

        msg.setUnits(obx.getObx6_Units().getCwe1_Identifier().getValueOrEmpty());
        msg.setReferenceRange(obx.getObx7_ReferencesRange().getValueOrEmpty());
        String abnormalFlags = "";
        // will there ever be more than one abnormal flag in practice?
        for (IS flag : obx.getObx8_AbnormalFlags()) {
            abnormalFlags += flag.getValueOrEmpty();
        }
        msg.setAbnormalFlags(abnormalFlags);
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
        msg.setNotes(String.join(" ", allNotes));
    }

    /**
     * @return Does this observation contain only redundant information
     * that can be ignored? Eg. header and footer of a report intended
     * to be human-readable.
     */
    public boolean isIgnorable() {
        // this will need expanding as we discover new cases
        if (msg.getStringValue().equals("URINE CULTURE REPORT") || msg.getStringValue().equals("FLUID CULTURE REPORT")) {
            return true;
        }
        String pattern = "COMPLETE: \\d\\d/\\d\\d/\\d\\d";
        Pattern p = Pattern.compile(pattern);
        if (p.matcher(msg.getStringValue()).matches()) {
            return true;
        }
        return false;
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
        if (!pathologyResult.getIsolateLocalCode().isEmpty()) {
            msg.setIsolateLocalCode(pathologyResult.getIsolateLocalCode());
            msg.setIsolateLocalDescription(pathologyResult.getIsolateLocalDescription());
            msg.setIsolateCodingSystem(pathologyResult.getIsolateCodingSystem());
        }
    }
}
