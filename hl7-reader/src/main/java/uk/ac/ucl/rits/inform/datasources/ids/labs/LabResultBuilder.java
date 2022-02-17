package uk.ac.ucl.rits.inform.datasources.ids.labs;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.Varies;
import ca.uhn.hl7v2.model.v26.datatype.CWE;
import ca.uhn.hl7v2.model.v26.datatype.IS;
import ca.uhn.hl7v2.model.v26.segment.NTE;
import ca.uhn.hl7v2.model.v26.segment.OBX;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import uk.ac.ucl.rits.inform.datasources.ids.exceptions.Hl7InconsistencyException;
import uk.ac.ucl.rits.inform.datasources.ids.hl7.parser.NotesParser;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.ValueType;
import uk.ac.ucl.rits.inform.interchange.lab.LabResultMsg;
import uk.ac.ucl.rits.inform.interchange.lab.LabResultStatus;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

/**
 * Base builder class for Lab Results.
 * Each Lab result type should create it's own concrete subclass
 * @author Jeremy Stein
 * @author Stef Piatek
 */
public abstract class LabResultBuilder {
    private static final Logger logger = LoggerFactory.getLogger(LabResultBuilder.class);

    private final LabResultMsg msg = new LabResultMsg();
    private final OBX obx;
    private final List<NTE> notes;
    private final String normalFlag;
    private static final Set<String> DEFAULT_STRING_NUMERIC_TYPES = Set.of("ST", "FT", "TX", "NM");

    /**
     * @param obx        OBX segment
     * @param notes      Notes for OBX
     * @param normalFlag optional flag to ignore as normal
     */
    LabResultBuilder(OBX obx, Collection<NTE> notes, @Nullable String normalFlag) {
        this.obx = obx;
        this.notes = List.copyOf(notes);
        this.normalFlag = normalFlag;
    }

    /**
     * @return the OBX segment
     */
    OBX getObx() {
        return obx;
    }

    /**
     * @return the Lab Result Message.
     */
    LabResultMsg getMessage() {
        return msg;
    }

    /**
     * Construct Lab Result msg using set order of methods.
     * @throws HL7Exception              If hl7value can't be decoded
     * @throws Hl7InconsistencyException if custom value type is incompatible with parser
     */
    void constructMsg() throws HL7Exception, Hl7InconsistencyException {
        setTestIdentifiers();
        setValueAdjacentFields();
        setValueAndMimeType();
        setResultTime();
        setComments();
        setCustomOverrides();
    }

    /**
     * Set the result time.
     * @throws DataTypeException if the result time can't be parsed by HAPI
     */
    abstract void setResultTime() throws DataTypeException;

    /**
     * Any custom overriding methods to populate individual field data.
     * @throws Hl7InconsistencyException if hl7 message is malformed
     */
    abstract void setCustomOverrides() throws Hl7InconsistencyException;


    /**
     * Set test identifiers.
     */
    private void setTestIdentifiers() {
        CWE obx3 = obx.getObx3_ObservationIdentifier();
        msg.setTestItemLocalCode(obx3.getCwe1_Identifier().getValueOrEmpty());
        msg.setTestItemCodingSystem(obx3.getCwe3_NameOfCodingSystem().getValueOrEmpty());
    }

    /**
     * Set all fields that are related to the value, excluding the result time.
     */
    private void setValueAdjacentFields() {
        msg.setUnits(InterchangeValue.buildFromHl7(obx.getObx6_Units().getCwe1_Identifier().getValueOrEmpty()));
        msg.setObservationSubId(obx.getObx4_ObservationSubID().getValueOrEmpty());
        try {
            msg.setResultStatus(LabResultStatus.findByHl7Code(obx.getObx11_ObservationResultStatus().getValueOrEmpty()));
        } catch (IllegalArgumentException e) {
            logger.warn("Could not parse the ResultStatus", e);
        }
        try {
            setReferenceRange(obx);
        } catch (NumberFormatException e) {
            logger.error("Could not parse reference range", e);
        }
        setAbnormalFlag(obx);
    }

    /**
     * Set reference low and reference high fields from OBX7.
     * @param obx OBX segment
     */
    private void setReferenceRange(OBX obx) {
        String referenceRange = obx.getObx7_ReferencesRange().getValueOrEmpty();
        String[] rangeValues = referenceRange.split("-");
        if (rangeValues.length == 2) {
            Double lower = Double.parseDouble(rangeValues[0]);
            Double upper = Double.parseDouble(rangeValues[1]);
            msg.setReferenceLow(InterchangeValue.buildFromHl7(lower));
            msg.setReferenceHigh(InterchangeValue.buildFromHl7(upper));
        } else if (!referenceRange.isEmpty()) {
            if (referenceRange.charAt(0) == '<') {
                Double upper = Double.parseDouble(referenceRange.replace("<", ""));
                msg.setReferenceHigh(InterchangeValue.buildFromHl7(upper));
                msg.setReferenceLow(InterchangeValue.delete());
            } else if (referenceRange.charAt(0) == '>') {
                Double lower = Double.parseDouble(referenceRange.replace(">", ""));
                msg.setReferenceHigh(InterchangeValue.delete());
                msg.setReferenceLow(InterchangeValue.buildFromHl7(lower));
            } else {
                logger.warn("LabResult reference range not recognised: {}", referenceRange);
            }
        }
    }

    /**
     * Set abnormal flag, ignoring normal flag.
     * @param obx OBX segment
     */
    private void setAbnormalFlag(OBX obx) {
        StringBuilder abnormalFlags = new StringBuilder();
        // Can't find any example in database of multiple flags but keeping in iteration and warning in case this changes with new data types
        for (IS flag : obx.getObx8_AbnormalFlags()) {
            abnormalFlags.append(flag.getValueOrEmpty());
        }
        if (abnormalFlags.length() > 1) {
            logger.warn("LabResult had more than one abnormal flag: {}", abnormalFlags);
        }
        if (abnormalFlags.length() != 0 && !abnormalFlags.toString().equals(normalFlag)) {
            msg.setAbnormalFlag(InterchangeValue.buildFromHl7(abnormalFlags.toString()));
        }
    }

    /**
     * Populate results based on the observation type.
     * <p>
     * For numeric values, string values are also populated for debugging.
     * @throws HL7Exception If hl7value can't be decoded
     */
    protected void setSingleTextOrNumericValue() throws HL7Exception {
        int repCount = obx.getObx5_ObservationValueReps();

        String dataType = obx.getObx2_ValueType().getValueOrEmpty();
        if (DEFAULT_STRING_NUMERIC_TYPES.contains(dataType)) {
            setStringValueAndMimeType(obx);
            if ("NM".equals(dataType)) {
                if (repCount > 1) {
                    logger.warn("LabResult is Numerical (NM) result but repcount = {}", repCount);
                }
                msg.setMimeType(ValueType.NUMERIC);
                try {
                    if (msg.getStringValue().isSave()) {
                        setNumericValueAndResultOperator(msg.getStringValue().get());
                    }
                } catch (NumberFormatException e) {
                    logger.warn("LabResult numeric result couldn't be parsed. Will delete existing value: {}", msg.getStringValue());
                    msg.setNumericValue(InterchangeValue.delete());
                }
            }
        }
    }

    void setStringValueAndMimeType(OBX obx) throws HL7Exception {
        msg.setMimeType(ValueType.TEXT);
        // Store the string value for numeric types to allow for debugging in case new result operator needs to be added
        StringJoiner joiner = new StringJoiner("\n");
        for (Varies varies : obx.getObx5_ObservationValue()) {
            String value = varies.getData().encode();
            if (value != null) {
                joiner.add(value);
            }
        }
        String stringValue = joiner.toString();
        msg.setStringValue(InterchangeValue.buildFromHl7(stringValue));
    }

    /**
     * Set numeric value and result operator (if required).
     * @param inputValue string value
     */
    void setNumericValueAndResultOperator(String inputValue) {
        String value = inputValue;
        String resultOperator = "=";
        if (!value.isEmpty() && (value.charAt(0) == '>' || value.charAt(0) == '<')) {
            resultOperator = value.substring(0, 1);
            value = value.substring(1);
        }
        msg.setResultOperator(resultOperator);
        Double numericValue = Double.parseDouble(value);

        msg.setNumericValue(InterchangeValue.buildFromHl7(numericValue));
    }


    /**
     * Each parser should define how to parse their values.
     * Simplest case would just call {@link LabResultBuilder#setSingleTextOrNumericValue}
     * @throws Hl7InconsistencyException if data cannot be parsed.
     * @throws HL7Exception              If hl7value can't be decoded
     */
    abstract void setValueAndMimeType() throws Hl7InconsistencyException, HL7Exception;

    /**
     * Gather all the NTE segments that relate to this OBX and save as concatenated value.
     * Ignores NTE-1 for now.
     */
    private void setComments() {
        NotesParser parser = new NotesParser(notes);
        msg.setNotes(InterchangeValue.buildFromHl7(parser.getComments()));
    }
}
