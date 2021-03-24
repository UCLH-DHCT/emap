package uk.ac.ucl.rits.inform.datasources.ids.labs;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.Type;
import ca.uhn.hl7v2.model.v26.datatype.ED;
import ca.uhn.hl7v2.model.v26.datatype.ST;
import ca.uhn.hl7v2.model.v26.datatype.TX;
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
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builder for LabResults for CoPath.
 * @author Stef Piatek
 */
public class CoPathResultBuilder extends LabResultBuilder {
    private static final Logger logger = LoggerFactory.getLogger(CoPathResultBuilder.class);
    private final OBR obr;
    private final List<OBX> obxSegments;
    private static final Pattern REPORT_ENCODING = Pattern.compile("Content-Type: text/plain; charset=US-ASCII;.+Content-transfer-encoding: base64");
    private boolean ignored = false;
    private static final String CO_PATH = OrderCodingSystem.CO_PATH.name();
    private static final Set<String> TEXT_TYPE = Set.of("TX", "ST");

    /**
     * @param obxSegments the OBX segments for this result type
     * @param obr         the OBR segment for this result (will be the same segment shared with other OBXs)
     */
    CoPathResultBuilder(List<OBX> obxSegments, OBR obr) {
        super(obxSegments.get(0), new ArrayList<>(0), null);
        this.obxSegments = obxSegments;
        this.obr = obr;
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
    void setCustomOverrides() throws Hl7InconsistencyException {
        getMessage().setTestItemCodingSystem(CO_PATH);
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
        } else if ("ED".equals(dataType)) {
            delimiter = "";
            valueType = ValueType.PDF;
        } else {
            throw new Hl7InconsistencyException(String.format("CoPath OBX type not recognised '%s'", dataType));
        }

        setMimeTypeAndTestCode(valueType);
        String observationIdAndSubId = getObservationIdAndSubId(getObx());
        StringJoiner value = incrementallyBuildValue(observationIdAndSubId, delimiter);
        setValueOrIgnored(dataType, value);
    }

    private void setMimeTypeAndTestCode(ValueType valueType) {
        getMessage().setMimeType(valueType);
        getMessage().setTestItemLocalCode(valueType.name());
    }

    private void setValueOrIgnored(String dataType, StringJoiner value) throws Hl7InconsistencyException {
        if (isTextValue(dataType)) {
            getMessage().setStringValue(InterchangeValue.buildFromHl7(value.toString()));
        } else if (!"MIME".equals(value.toString())) {
            // already know that if it's not text, it's ED data type
            Matcher matcher = REPORT_ENCODING.matcher(value.toString());
            if (!matcher.find()) {
                throw new Hl7InconsistencyException("Encoding of report in unexpected format");
            }
            String dataValues = matcher.replaceFirst("");
            byte[] byteValues = Base64.getDecoder().decode(dataValues);
            getMessage().setByteValue(InterchangeValue.buildFromHl7(byteValues));
        } else {
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
     * @throws Hl7InconsistencyException if observationId and subId change, or multi-line result
     * @throws HL7Exception              If value can't be encoded from hl7
     */
    private StringJoiner incrementallyBuildValue(String observationIdAndSubId, String delimiter) throws Hl7InconsistencyException, HL7Exception {
        StringJoiner value = new StringJoiner(delimiter);
        for (OBX obx : obxSegments) {
            if (!observationIdAndSubId.equals(getObservationIdAndSubId(obx))) {
                throw new Hl7InconsistencyException("CoPath observationId and subId should not change within a type");
            }
            if (obx.getObx5_ObservationValueReps() > 1) {
                // If find this in real data, inspect and see if we need to join the observation values by "^"
                throw new Hl7InconsistencyException("CoPath OBX not expected to have multiple-line results");
            }
            value.add(obx.getObx5_ObservationValue(0).getData().encode());
        }
        return value;
    }

    private String getObservationIdAndSubId(OBX obx) {
        return String.join("$",
                obx.getObx3_ObservationIdentifier().getCwe1_Identifier().getValueOrEmpty(), obx.getObx4_ObservationSubID().getValueOrEmpty());
    }

}
