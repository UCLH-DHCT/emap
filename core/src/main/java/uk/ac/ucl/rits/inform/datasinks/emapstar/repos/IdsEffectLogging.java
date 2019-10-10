package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import java.time.Instant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/**
 * What effect did each IDS message have on Inform-db?
 *
 * Useful for debugging, seeing why a particular message didn't have the
 * intended effect, etc
 *
 * @author Jeremy Stein
 */
@Entity
public class IdsEffectLogging {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    private String sourceId;
    private Instant messageDatetime;
    private Instant processingStartTime;
    private Instant processingEndTime;
    private double queueReadDurationSeconds;
    private double processMessageDurationSeconds;
    private String returnStatus;
    private String mrn;
    private String messageType;
    @Column(columnDefinition = "text")
    private String message;

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
     * @param queueReadDurationSeconds how long it took to read from the queue
     */
    public void setQueueReadDuration(double queueReadDurationSeconds) {
        this.queueReadDurationSeconds = queueReadDurationSeconds;
    }

    /**
     * @param processMessageDurationSeconds how long it took to process the message
     */
    public void setProcessMessageDuration(double processMessageDurationSeconds) {
        this.processMessageDurationSeconds = processMessageDurationSeconds;
    }
}
