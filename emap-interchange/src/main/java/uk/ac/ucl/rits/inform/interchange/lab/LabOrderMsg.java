package uk.ac.ucl.rits.inform.interchange.lab;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessage;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessor;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The top level of the lab tree, the order. Only the interchange format
 * is declared here, for serialisation purposes. Builder classes (eg. HL7
 * parser) construct this class.
 * @author Jeremy Stein
 * @author Stef Piatek
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

    /**
     * Best we can get to time the sample was collected (label printing time).
     * <p>
     * Constant throughout workflow.
     */
    private Instant collectionDateTime;

    /**
     * Last updated time for an order.
     */
    private Instant statusChangeTime;

    /**
     * Time the order was entered on the lab system.
     */
    private InterchangeValue<Instant> orderDateTime = InterchangeValue.unknown();

    /**
     * Time that the sample was received at the lab.
     */
    private InterchangeValue<Instant> sampleReceivedTime = InterchangeValue.unknown();

    /**
     * Time the order was requested.
     * (e.g. Time of the epic request time to WinPath)
     * Not set for POC testing.
     */
    private InterchangeValue<Instant> requestedDateTime = InterchangeValue.unknown();
    private String labDepartment = "";
    private String orderStatus;
    private String resultStatus;
    private String orderType;
    private String mrn;

    private String visitNumber;
    private String testBatteryLocalCode;
    private String testBatteryCodingSystem;

    private String parentObservationIdentifier = "";
    private String parentSubId = "";
    private InterchangeValue<String> clinicalInformation = InterchangeValue.unknown();

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
