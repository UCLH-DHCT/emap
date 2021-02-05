package uk.ac.ucl.rits.inform.datasources.ids.labs;

import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.Type;
import ca.uhn.hl7v2.model.v26.datatype.CE;
import ca.uhn.hl7v2.model.v26.segment.NTE;
import ca.uhn.hl7v2.model.v26.segment.OBR;
import ca.uhn.hl7v2.model.v26.segment.OBX;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ucl.rits.inform.datasources.ids.HL7Utils;
import uk.ac.ucl.rits.inform.interchange.lab.LabResultMsg;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Builder for LabResults for WinPath.
 * @author Jeremy Stein
 * @author Stef Piatek
 */
public class WinPathResultBuilder extends LabResultBuilder {
    private static final Logger logger = LoggerFactory.getLogger(WinPathResultBuilder.class);
    private final Pattern completePattern = Pattern.compile("COMPLETE: \\d{1,2}/\\d{1,2}/\\d{2,4}");
    private final OBR obr;

    /**
     * @param obx   the OBX segment for this result
     * @param obr   the OBR segment for this result (will be the same segment shared with other OBXs)
     * @param notes Notes for the OBX segment
     */
    WinPathResultBuilder(OBX obx, OBR obr, List<NTE> notes) {
        super(obx, notes, null);
        this.obr = obr;
    }

    @Override
    void setResultTime() throws DataTypeException {
        // OBR segments for sensitivities don't have an OBR-22 status change time
        // so use the time from the parent?
        getMessage().setResultTime(HL7Utils.interpretLocalTime(obr.getObr22_ResultsRptStatusChngDateTime()));
    }

    @Override
    void setCustomOverrides() {
        // each result needs to know this so sensitivities can be correctly assigned
        getMessage().setEpicCareOrderNumber(obr.getObr2_PlacerOrderNumber().getEi1_EntityIdentifier().getValueOrEmpty());
    }

    /**
     * Set value of coded result (CE).
     * @param data     data item
     * @param repCount the number of parts of the data item
     */
    @Override
    protected void setCustomValue(Type data, int repCount) {
        if (data instanceof CE) {
            if (repCount > 1) {
                logger.warn("LabResult is coded (CE) result but repcount = {}", repCount);
            }
            // we are assuming that all coded data is an isolate, not a great assumption
            CE ceData = (CE) data;
            getMessage().setIsolateLocalCode(ceData.getCe1_Identifier().getValue());
            // Isolate coding system should default to empty string
            String isolateCodingSystem = ceData.getCe3_NameOfCodingSystem().getValue();
            getMessage().setIsolateCodingSystem(isolateCodingSystem == null ? "" : isolateCodingSystem);
        }
    }

    /**
     * @return Does this observation contain only redundant information
     * that can be ignored? Eg. header and footer of a report intended
     * to be human-readable.
     */
    boolean isIgnorable() {
        // this will need expanding as we discover new cases
        if (getMessage().getStringValue().isUnknown()) {
            return false;
        }
        if ("URINE CULTURE REPORT".equals(getMessage().getStringValue().get())
                || "FLUID CULTURE REPORT".equals(getMessage().getStringValue().get())) {
            return true;
        }
        return completePattern.matcher(getMessage().getStringValue().get()).matches();
    }

    /**
     * Merge another lab result into this one.
     * Eg. an adjacent OBX segment that is linked by a sub ID.
     * @param labResultMsg the other lab result to merge in
     */
    void mergeResult(LabResultMsg labResultMsg) {
        // Will need to identify HOW to merge results.
        // Eg. identify that LabResultMsg contains an isolate,
        // so only copy the isolate fields from it.
        if (!labResultMsg.getIsolateLocalCode().isEmpty()) {
            getMessage().setIsolateLocalCode(labResultMsg.getIsolateLocalCode());
            getMessage().setIsolateCodingSystem(labResultMsg.getIsolateCodingSystem());
        }
    }
}
