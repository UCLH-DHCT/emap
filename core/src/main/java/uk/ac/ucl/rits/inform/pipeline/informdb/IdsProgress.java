package uk.ac.ucl.rits.inform.pipeline.informdb;

import java.time.Instant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * Keep track of what message number we have processed up to.
 */
@Entity
public class IdsProgress {
    @Id
    private int id;
    private int lastProcessedIdsUnid;
    @Column(columnDefinition = "timestamp with time zone")
    private Instant lastProcessedMessageDatetime;
    @Column(columnDefinition = "timestamp with time zone")
    private Instant lastProcessingDatetime;

    /**
     * Initialise. There is only ever one row, so it's always the same.
     */
    public IdsProgress() {
        id = 0;
        setLastProcessedIdsUnid(-1);
    }

    /**
     * @param lastProcessedIdsUnid the last processed message
     */
    public void setLastProcessedIdsUnid(int lastProcessedIdsUnid) {
        this.lastProcessedIdsUnid = lastProcessedIdsUnid;
    }

    /**
     * @return row id
     */
    public int getId() {
        return id;
    }
    /**
     * @return the last processed message
     */
    public int getLastProcessedIdsUnid() {
        return lastProcessedIdsUnid;
    }

    /**
     * @return the timestamp in the last processed message
     */
    public Instant getLastProcessedMessageDatetime() {
        return lastProcessedMessageDatetime;
    }

    /**
     * @param lastProcessedMessageDatetime the timestamp in the last processed message
     */
    public void setLastProcessedMessageDatetime(Instant lastProcessedMessageDatetime) {
        this.lastProcessedMessageDatetime = lastProcessedMessageDatetime;
    }

    /**
     * @return when the last processed message was processed
     */
    public Instant getLastProcessingDatetime() {
        return lastProcessingDatetime;
    }

    /**
     * @param lastProcessingDatetime when the last processed message was processed
     */
    public void setLastProcessingDatetime(Instant lastProcessingDatetime) {
        this.lastProcessingDatetime = lastProcessingDatetime;
    }
}
