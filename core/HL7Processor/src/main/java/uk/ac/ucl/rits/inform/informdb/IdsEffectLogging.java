package uk.ac.ucl.rits.inform.informdb;

import java.time.Instant;

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
 * @author jeremystein
 */
@Entity
public class IdsEffectLogging {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    // the unique ID of the IDS message
    private int idsUnid;
    private Instant processingStartTime;
    private Instant processingEndTime;
    // the MRN as stated in the IDS
    private String mrn;
    // message type from IDS (aka trigger event)
    private String messageType;
    // text description of what action was taken based on this message
    private String message;

    public void setIdsUnid(int idsUnid) {
        this.idsUnid = idsUnid;
    }

    public void setProcessingStartTime(Instant processingStartTime) {
        this.processingStartTime = processingStartTime;
    }

    public void setProcessingEndTime(Instant processingEndTime) {
        this.processingEndTime = processingEndTime;
    }

    public void setMrn(String mrn) {
        this.mrn = mrn;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }
}
