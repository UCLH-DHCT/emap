package uk.ac.ucl.rits.inform.interchange.lab;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import org.apache.commons.lang3.tuple.Pair;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Represent a lab result. Note that this doesn't implement
 * EmapOperationMessage because it's not a message type
 * by itself, it is owned by a message type (LabOrderMsg).
 * @author Jeremy Stein
 * @author Stef Piatek
 */
@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public class LabResultMsg implements Serializable {
    private static final long serialVersionUID = 140970942335476554L;
    private String valueType;

    private String testItemLocalCode = "";
    private String testItemCodingSystem = "";

    /**
     * Id to link an observation over multiple result fields which can then be combined.
     */
    private String observationSubId = "";
    private InterchangeValue<Double> numericValue = InterchangeValue.unknown();
    private InterchangeValue<String> stringValue = InterchangeValue.unknown();
    private InterchangeValue<String> units = InterchangeValue.unknown();
    private InterchangeValue<Double> referenceLow = InterchangeValue.unknown();
    private InterchangeValue<Double> referenceHigh = InterchangeValue.unknown();

    /**
     * Abnormal flags.
     * <p>
     * If this is empty, then it should be deleted in the database.
     */
    private InterchangeValue<String> abnormalFlag = InterchangeValue.delete();
    private String resultOperator;
    private LabResultStatus resultStatus = LabResultStatus.UNKNOWN;

    private Instant resultTime;
    private InterchangeValue<String> notes = InterchangeValue.unknown();

    /**
     * Lab Isolate is only ever contained within a result.
     * Map in the format {[epicOrderId, isolateId], labIsolateMessage}
     */
    private Map<Pair<String, String>, LabIsolateMsg> labIsolates = new HashMap<>();

    private String epicCareOrderNumber;

    /**
     * @return true if the type of result is numeric
     */
    @JsonIgnore
    public boolean isNumeric() {
        return "NM".equals(valueType) || "SN".equals(valueType);
    }

    /**
     * @return true if the type of result is text
     */
    @JsonIgnore
    public boolean isText() {
        return "ST".equals(valueType) || "TX".equals(valueType) || "FT".equals(valueType);
    }

}
