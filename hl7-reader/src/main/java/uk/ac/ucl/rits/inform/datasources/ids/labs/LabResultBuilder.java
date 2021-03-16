package uk.ac.ucl.rits.inform.datasources.ids.labs;

import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.Type;
import ca.uhn.hl7v2.model.Varies;
import ca.uhn.hl7v2.model.v26.datatype.CWE;
import ca.uhn.hl7v2.model.v26.datatype.FT;
import ca.uhn.hl7v2.model.v26.datatype.IS;
import ca.uhn.hl7v2.model.v26.datatype.NM;
import ca.uhn.hl7v2.model.v26.datatype.ST;
import ca.uhn.hl7v2.model.v26.datatype.TX;
import ca.uhn.hl7v2.model.v26.segment.NTE;
import ca.uhn.hl7v2.model.v26.segment.OBX;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import uk.ac.ucl.rits.inform.datasources.ids.exceptions.Hl7InconsistencyException;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.ValueType;
import uk.ac.ucl.rits.inform.interchange.lab.LabResultMsg;
import uk.ac.ucl.rits.inform.interchange.lab.LabResultStatus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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

    /**
     * @param obx        OBX segment
     * @param notes      Notes for OBX
     * @param normalFlag optional flag to ignore as normal
     */
    LabResultBuilder(OBX obx, List<NTE> notes, @Nullable String normalFlag) {
        this.obx = obx;
        this.notes = notes;
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
     * @throws DataTypeException         if the result time can't be parsed by HAPI
     * @throws Hl7InconsistencyException if custom value type is incompatible with parser
     */
    void constructMsg() throws DataTypeException, Hl7InconsistencyException {
        setTestIdentifiers();
        setValueAdjacentFields();
        setValue();
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
     */
    abstract void setCustomOverrides();


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
            logger.warn("Could not parse the PatientClass", e);
        }

        setReferenceRange(obx);
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
     * @throws Hl7InconsistencyException If custom data type is not compatible with parsing
     */
    protected void setValue() throws Hl7InconsistencyException {
        int repCount = obx.getObx5_ObservationValueReps();

        // The first rep is all that's needed for most data types
        Varies dataVaries = obx.getObx5_ObservationValue(0);
        Type data = dataVaries.getData();
        if (data instanceof ST
                || data instanceof FT
                || data instanceof TX
                || data instanceof NM) {
            setStringValueAndMimeType(obx);
            if (data instanceof NM) {
                if (repCount > 1) {
                    logger.warn("LabResult is Numerical (NM) result but repcount = {}", repCount);
                }
                try {
                    if (msg.getStringValue().isSave()) {
                        setNumericValueAndResultOperatorAndMimeType(msg.getStringValue().get());
                    }
                } catch (NumberFormatException e) {
                    logger.warn("LabResult numeric result couldn't be parsed. Will delete existing value: {}", msg.getStringValue());
                    msg.setNumericValue(InterchangeValue.delete());
                }
            }
        }
        setDataFromCustomValue(obx);
    }

    void setStringValueAndMimeType(OBX obx) {
        msg.setMimeType(ValueType.TEXT);
        // Store the string value for numeric types to allow for debugging in case new result operator needs to be added
        String stringValue = Arrays.stream(obx.getObx5_ObservationValue())
                .map(Varies::getData)
                .map(Type::toString)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("\n"));

        msg.setStringValue(InterchangeValue.buildFromHl7(stringValue));
    }

    /**
     * Set numeric value and result operator (if required).
     * @param inputValue string value
     */
    void setNumericValueAndResultOperatorAndMimeType(String inputValue) {
        msg.setMimeType(ValueType.NUMERIC);
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
     * Optionally set a value which is not a numeric or string type.
     * @param obx      obx segment
     * @throws Hl7InconsistencyException if custom data type is not compatible wth parsing
     */
    protected void setDataFromCustomValue(OBX obx) throws Hl7InconsistencyException {
        return;
    }

    /**
     * Gather all the NTE segments that relate to this OBX and save as concatenated value.
     * Ignores NTE-1 for now.
     */
    private void setComments() {
        Collection<String> allNotes = new ArrayList<>(notes.size());
        for (NTE nt : notes) {
            for (FT ft : nt.getNte3_Comment()) {
                allNotes.add(ft.getValueOrEmpty());
            }
        }
        msg.setNotes(InterchangeValue.buildFromHl7(String.join("\n", allNotes)));
    }
}
