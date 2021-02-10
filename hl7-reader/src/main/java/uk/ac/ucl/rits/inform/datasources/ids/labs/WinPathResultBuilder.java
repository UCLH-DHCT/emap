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
import uk.ac.ucl.rits.inform.datasources.ids.exceptions.Hl7InconsistencyException;
import uk.ac.ucl.rits.inform.interchange.lab.LabResultMsg;

import java.util.List;

/**
 * Builder for LabResults for WinPath.
 * @author Jeremy Stein
 * @author Stef Piatek
 */
public class WinPathResultBuilder extends LabResultBuilder {
    private static final Logger logger = LoggerFactory.getLogger(WinPathResultBuilder.class);
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
     * @param testCode expected test code
     * @param repCount the number of parts of the data item
     * @throws Hl7InconsistencyException if coded data that is not ISOLATE
     */
    @Override
    protected void setCustomValue(Type data, String testCode, int repCount) throws Hl7InconsistencyException {
        if (!(data instanceof CE)) {
            return;
        }
        if (!"ISOLATE".equals(testCode)) {
            throw new Hl7InconsistencyException(String.format("Coded data which is not an ISOLATE test, instead is '%s'", testCode));
        }

        if (repCount > 1) {
            logger.warn("LabResult is coded (CE) result but repcount = {}", repCount);
        }
        // we are assuming that all coded data is an isolate, not a great assumption
        CE ceData = (CE) data;
        String isolateCode = ceData.getCe1_Identifier().getValue().stripTrailing();
        String isolateText = ceData.getCe2_Text().getValue();
        getMessage().setIsolateCodeAndText(String.format("%s^%s", isolateCode, isolateText));
    }

    /**
     * Merge another lab result into this one.
     * Eg. an adjacent OBX segment that is linked by a sub ID.
     * @param labResultMsg the other lab result to merge in
     */
    void mergeResult(LabResultMsg labResultMsg) {
        if (!labResultMsg.getIsolateCodeAndText().isEmpty()) {
            getMessage().setIsolateCodeAndText(labResultMsg.getIsolateCodeAndText());
        }
    }
}
