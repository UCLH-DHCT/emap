package uk.ac.ucl.rits.inform.datasources.ids.labs;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.v26.segment.OBR;
import ca.uhn.hl7v2.model.v26.segment.OBX;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ucl.rits.inform.datasources.ids.HL7Utils;
import uk.ac.ucl.rits.inform.datasources.ids.exceptions.Hl7InconsistencyException;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.OrderCodingSystem;
import uk.ac.ucl.rits.inform.interchange.ValueType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

/**
 * Builder for LabResults for Imaging.
 * @author Stef Piatek
 */
public class ImageLabResultBuilder extends LabResultBuilder {
    private static final Logger logger = LoggerFactory.getLogger(ImageLabResultBuilder.class);
    private final OBR obr;
    private final List<OBX> obxSegments;
    private final String resultCode;
    private boolean ignored = false;
    private static final String CODING_SYSTEM = OrderCodingSystem.PACS.name();
    private static final Set<String> TEXT_TYPE = Set.of("TX", "ST");
    private static final String TEXT_RESULT_CODE = "GDT";
    private static final String ALLOWED_NON_TEXT_CODE = "INDICATIONS";

    /**
     * @param resultCode  code for the result type, if empty then this is the text report
     * @param obxSegments the OBX segments for this result type
     * @param obr         the OBR segment for this result (will be the same segment shared with other OBXs)
     */
    ImageLabResultBuilder(String resultCode, List<OBX> obxSegments, OBR obr) {
        super(obxSegments.get(0), new ArrayList<>(0), null);
        this.obxSegments = Collections.unmodifiableList(obxSegments);
        this.obr = obr;
        this.resultCode = resultCode;
    }

    /**
     * @return true if result should be ignored.
     */
    boolean isIgnored() {
        return ignored;
    }

    /**
     * Set the result time.
     * @throws DataTypeException if the result time can't be parsed by HAPI
     */
    @Override
    void setResultTime() throws DataTypeException {
        getMessage().setResultTime(HL7Utils.interpretLocalTime(obr.getObr22_ResultsRptStatusChngDateTime()));
    }

    /**
     * Any custom overriding methods to populate individual field data.
     */
    @Override
    void setCustomOverrides() {
        getMessage().setTestItemCodingSystem(CODING_SYSTEM);
    }

    /**
     * Set value as from a text report or pdf report.
     * @throws Hl7InconsistencyException if data type not TX or ED, multi-line values, subId changes, or pdf report not in base 64 US-ASCII encoding.
     * @throws HL7Exception              If value can't be encoded from hl7
     */
    @Override
    protected void setValueAndMimeType() throws Hl7InconsistencyException, HL7Exception {
        String dataType = getObx().getObx2_ValueType().getValueOrEmpty();
        String delimiter;
        ValueType valueType;
        if (isTextValue(dataType)) {
            delimiter = "\n";
            valueType = ValueType.TEXT;
        } else {
            throw new Hl7InconsistencyException(String.format("Imaging OBX type not recognised '%s'", dataType));
        }

        if (isAllowedTextResult()) {
            setMimeTypeAndTestCode(valueType);
            String observationIdAndSubId = getObservationIdAndSubId(getObx());
            String value = incrementallyBuildValue(observationIdAndSubId, delimiter);
            setValueOrIgnored(dataType, value);
        } else if (isNonTextReport()) {
            if (obxSegments.size() != 1) {
                throw new Hl7InconsistencyException(String.format("Imaging OBX should only have a single result, %d were found", obxSegments.size()));
            }
            setStringValueAndMimeType(obxSegments.get(0));
        } else {
            ignored = true;
        }
    }

    private boolean isAllowedTextResult() {
        return TEXT_RESULT_CODE.equals(resultCode);
    }

    /**
     * @return true if this is an allowed non-text result.
     */
    private boolean isNonTextReport() {
        return ALLOWED_NON_TEXT_CODE.equals(resultCode);
    }

    private void setMimeTypeAndTestCode(ValueType valueType) {
        getMessage().setMimeType(valueType);
        getMessage().setTestItemLocalCode(valueType.name());
    }

    private void setValueOrIgnored(String dataType, String value) {
        if (isTextValue(dataType)) {
            getMessage().setStringValue(InterchangeValue.buildFromHl7(value));
        } else {
            logger.debug("Ignoring data type {}", dataType);
            ignored = true;
        }
    }

    private boolean isTextValue(String dataType) {
        return TEXT_TYPE.contains(dataType);
    }

    /**
     * @param observationIdAndSubId observation and subId joined together
     * @param delimiter             delimiter between subId observations
     * @return StringJoiner of all observation lines
     * @throws Hl7InconsistencyException if  multi-line result
     * @throws HL7Exception              If value can't be encoded from hl7
     */
    private String incrementallyBuildValue(String observationIdAndSubId, String delimiter) throws Hl7InconsistencyException, HL7Exception {
        StringJoiner value = new StringJoiner(delimiter);
        for (OBX obx : obxSegments) {
            if (obx.getObx5_ObservationValueReps() > 1) {
                // If find this in real data, inspect and see if we need to join the observation values by "^"
                throw new Hl7InconsistencyException("OBX not expected to have multiple-line results");
            }
            value.add(obx.getObx5_ObservationValue(0).getData().encode());
        }
        return value.toString();
    }

    private String getObservationIdAndSubId(OBX obx) {
        return String.join("$",
                obx.getObx3_ObservationIdentifier().getCwe1_Identifier().getValueOrEmpty(), obx.getObx4_ObservationSubID().getValueOrEmpty());
    }

}
