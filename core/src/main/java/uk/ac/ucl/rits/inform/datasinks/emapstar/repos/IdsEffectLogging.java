package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

/**
 * What effect did each IDS message have on Inform-db?
 *
 * Useful for debugging, seeing why a particular message didn't have the
 * intended effect, etc
 *
 * @author Jeremy Stein
 */
@Entity
@Table(name = "etl_per_message_logging",
       indexes = { @Index(columnList = "sourceId", unique = false) })
public class IdsEffectLogging {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    private String sourceId;
    private Instant messageDatetime;
    private Instant processingStartTime;
    private Instant processingEndTime;
    private double processMessageDurationSeconds;
    @Column(columnDefinition = "text")
    private String returnStatus;
    private String mrn;
    private String messageType;
    private String eventReasonCode;
    @Column(columnDefinition = "text")
    private String message;
    @Column(columnDefinition = "text")
    private String stackTrace;

    /**
     * @param sourceId the unique ID from the source system (eg. IDS unid)
     */
    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    /**
     * @param processingStartTime when did processing this message start
     */
    public void setProcessingStartTime(Instant processingStartTime) {
        this.processingStartTime = processingStartTime;
    }

    /**
     * @param processingEndTime when did processing this message finish
     */
    public void setProcessingEndTime(Instant processingEndTime) {
        this.processingEndTime = processingEndTime;
    }

    /**
     * @param mrn the MRN as stated in the IDS
     */
    public void setMrn(String mrn) {
        this.mrn = mrn;
    }

    /**
     * @param message text description of what action was taken based on this message
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * @param messageType message type from IDS (aka trigger event)
     */
    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    /**
     * @param messageDatetime the timestamp in the message itself
     */
    public void setMessageDatetime(Instant messageDatetime) {
        this.messageDatetime = messageDatetime;
    }

    /**
     * @param returnStatus the return code from the operation, which may indicate several types
     * of success or several types of failure.
     */
    public void setReturnStatus(String returnStatus) {
        this.returnStatus = returnStatus;
    }

    /**
     * @param processMessageDurationSeconds how long it took to process the message
     */
    public void setProcessMessageDuration(double processMessageDurationSeconds) {
        this.processMessageDurationSeconds = processMessageDurationSeconds;
    }

    /**
     * @param stackTrace stack trace text if you have one
     */
    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    /**
     * Convert stack trace from exception to text.
     *
     * @param th throwable containing a stack trace
     */
    public void setStackTrace(Throwable th) {
        StringWriter st = new StringWriter();
        th.printStackTrace(new PrintWriter(st));
        setStackTrace(st.toString());
    }

    /**
     * Unclear what we'll use this field for so log it somewhere convenient for now.
     *
     * @param eventReasonCode the hl7 event reason code
     */
    public void setEventReasonCode(String eventReasonCode) {
        this.eventReasonCode = eventReasonCode;
    }
}
