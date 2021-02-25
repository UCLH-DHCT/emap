package uk.ac.ucl.rits.inform.datasources.ids.labs;

import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.Type;
import ca.uhn.hl7v2.model.Varies;
import ca.uhn.hl7v2.model.v26.datatype.CE;
import ca.uhn.hl7v2.model.v26.datatype.ST;
import ca.uhn.hl7v2.model.v26.segment.NTE;
import ca.uhn.hl7v2.model.v26.segment.OBR;
import ca.uhn.hl7v2.model.v26.segment.OBX;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ucl.rits.inform.datasources.ids.HL7Utils;
import uk.ac.ucl.rits.inform.datasources.ids.exceptions.Hl7InconsistencyException;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.lab.LabIsolateMsg;
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
    private static final String CULTURE_TYPE_SUFFIX = " culture yields:";

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
     * @throws Hl7InconsistencyException if coded data that is not ISOLATE
     */
    @Override
    protected void setDataFromCustomValue(OBX obx) throws Hl7InconsistencyException {
        String testCode = obx.getObx3_ObservationIdentifier().getCwe1_Identifier().getValueOrEmpty();
        Varies dataVaries = obx.getObx5_ObservationValue(0);
        Type data = dataVaries.getData();
        if (!"ISOLATE".equals(testCode)) {
            if (data instanceof CE) {
                throw new Hl7InconsistencyException(String.format("Coded data which is not an ISOLATE test, instead is '%s'", testCode));
            }
            return;
        }
        int repCount = obx.getObx5_ObservationValueReps();
        if (repCount > 1) {
            logger.warn("ISOLATE lab result with repcount = {}", repCount);
        }
        LabIsolateMsg labIsolate = addLabIsolateWithEpicOrderIdAndSubId(obx);
        if (data instanceof ST) {
            addCultureOrQuantityAndUpdateMessageValue(labIsolate);
        } else if (data instanceof CE) {
            CE ceData = (CE) data;
            labIsolate.setIsolateCode(ceData.getCe1_Identifier().getValue().stripTrailing());
            labIsolate.setIsolateName(ceData.getCe2_Text().getValue());
        }
    }

    private LabIsolateMsg addLabIsolateWithEpicOrderIdAndSubId(OBX obx) {
        String epicOrderId = obr.getObr2_PlacerOrderNumber().getEi1_EntityIdentifier().getValueOrEmpty();
        String subId = obx.getObx4_ObservationSubID().getValueOrEmpty();
        LabIsolateMsg labIsolate = new LabIsolateMsg(epicOrderId, subId);
        getMessage().setLabIsolate(labIsolate);
        return labIsolate;
    }

    private void addCultureOrQuantityAndUpdateMessageValue(LabIsolateMsg labIsolate) {
        String cultureOrQuantity = getMessage().getStringValue().isSave() ? getMessage().getStringValue().get() : "";
        if (cultureOrQuantity.endsWith(CULTURE_TYPE_SUFFIX)) {
            labIsolate.setCultureType(InterchangeValue.buildFromHl7(cultureOrQuantity.replace(CULTURE_TYPE_SUFFIX, "")));
        } else {
            labIsolate.setQuantity(InterchangeValue.buildFromHl7(cultureOrQuantity));
        }
    }

    /**
     * Merge isolate information, updating current lab result with the other message values which have been set.
     * Eg. an adjacent OBX segment that is linked by a sub ID.
     * @param otherMsg the other lab result to merge in
     */
    void mergeIsolatesAndEnsureValue(LabResultMsg otherMsg) {
        LabIsolateMsg thisIsolate = getMessage().getLabIsolate();
        LabIsolateMsg otherIsolate = otherMsg.getLabIsolate();
        thisIsolate.mergeIsolateInfo(otherIsolate);

        // replace with type when interchange format merged in
        getMessage().setMimeType("link/lab_isolate");
        // remove previously set string value
        getMessage().setStringValue(InterchangeValue.unknown());
    }
}
