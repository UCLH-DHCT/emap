package uk.ac.ucl.rits.inform.interchange;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Represent a lab result. Note that this doesn't implement
 * EmapOperationMessage because it's not a message type
 * by itself, it is owned by a message type (LabOrderMsg).
 * @author Jeremy Stein
 */
@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public class LabResultMsg implements Serializable {
    private static final long serialVersionUID = 140970942335476554L;
    private String valueType;

    private String testItemLocalCode = "";
    private String testItemLocalDescription = "";
    private String testItemCodingSystem = "";

    private String observationSubId = "";
    private Hl7Value<Double> numericValue = Hl7Value.unknown();
    private Hl7Value<String> stringValue = Hl7Value.unknown();

    private String isolateLocalCode = "";
    private String isolateLocalDescription = "";
    private String isolateCodingSystem = "";

    private Hl7Value<String> units = Hl7Value.unknown();
    private Hl7Value<Double> referenceLow = Hl7Value.unknown();
    private Hl7Value<Double> referenceHigh = Hl7Value.unknown();
    private Hl7Value<String> abnormalFlags = Hl7Value.unknown();
    private String resultStatus;

    private Instant resultTime;
    private Hl7Value<String> notes = Hl7Value.unknown();

    /**
     * A sensitivity is just a nested lab order with results.
     * HL7 has fields for working out parentage.
     */
    private List<LabOrderMsg> labSensitivities = new ArrayList<>();

    private String epicCareOrderNumber;

    public boolean isAbnormal(){
        return abnormalFlags.isSave();
    }

}
