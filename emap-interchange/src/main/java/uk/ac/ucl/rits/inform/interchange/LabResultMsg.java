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
    private String valueType = "";

    private String testItemLocalCode = "";
    private String testItemLocalDescription = "";
    private String testItemCodingSystem = "";

    private String observationSubId = "";
    private Double numericValue;
    private String stringValue = "";

    private String isolateLocalCode = "";
    private String isolateLocalDescription = "";
    private String isolateCodingSystem = "";

    private String units = "";
    private String referenceRange = "";
    private String abnormalFlags = "";
    private String resultStatus = "";

    private Instant resultTime;
    private String notes = "";

    /**
     * A sensitivity is just a nested lab order with results.
     * HL7 has fields for working out parentage.
     */
    private List<LabOrderMsg> labSensitivities = new ArrayList<>();

    private String epicCareOrderNumber = "";

}
