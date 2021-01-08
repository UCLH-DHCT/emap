package uk.ac.ucl.rits.inform.interchange;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * The top level of the lab tree, the order. Only the interchange format
 * is declared here, for serialisation purposes. Builder classes (eg. HL7
 * parser) construct this class.
 * @author Jeremy Stein
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public class LabOrderMsg extends EmapOperationMessage implements Serializable {
    private static final long serialVersionUID = -8476559759815762054L;

    private List<LabResultMsg> labResultMsgs = new ArrayList<>();
    private String orderControlId;
    private String epicCareOrderNumber;
    private String labSpecimenNumber;
    private String specimenType;
    private Instant orderDateTime;
    private Instant sampleEnteredTime;
    private String labDepartment;
    private String orderStatus;
    private String resultStatus;
    private String orderType;
    private String mrn;

    private String visitNumber;
    private Instant requestedDateTime;
    private Instant observationDateTime;
    private String testBatteryLocalCode;
    private String testBatteryLocalDescription;
    private String testBatteryCodingSystem;
    private Instant statusChangeTime;

    private String parentObservationIdentifier;
    private String parentSubId;

    /**
     * @return int number of lab results in list
     */
    @JsonIgnore
    public int getNumLabResults() {
        return labResultMsgs.size();
    }

    /**
     * Add a LabResult to list.
     * @param result LabResult to add
     */
    public void addLabResult(LabResultMsg result) {
        labResultMsgs.add(result);
    }

    /**
     * @param labResultMsgs the labResults to set
     */
    public void setLabResultMsgs(List<LabResultMsg> labResultMsgs) {
        this.labResultMsgs = labResultMsgs;
    }

    /**
     * Call back to the processor so it knows what type this object is (ie. double
     * dispatch).
     * @param processor the processor to call back to
     * @throws EmapOperationMessageProcessingException if message cannot be
     *                                                 processed
     */
    @Override
    public void processMessage(EmapOperationMessageProcessor processor)
            throws EmapOperationMessageProcessingException {
        processor.processMessage(this);
    }
}
