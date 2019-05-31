package uk.ac.ucl.rits.inform.informdb;

import java.time.Instant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class IdsProgress {
    @Id
    private int id;
    private int lastProcessedIdsUnid;
    @Column(columnDefinition = "timestamp with time zone")
    private Instant lastProcessedMessageDatetime;
    @Column(columnDefinition = "timestamp with time zone")
    private Instant lastProcessingDatetime;
    
    public IdsProgress() {
        // there is only one row
        id = 0;
        setLastProcessedIdsUnid(-1); 
    }
    public void setLastProcessedIdsUnid(int lastProcessedIdsUnid) {
        this.lastProcessedIdsUnid = lastProcessedIdsUnid;
    }
    public int getId() {
        return id;
    }
    public int getLastProcessedIdsUnid() {
        return lastProcessedIdsUnid;
    }
    public Instant getLastProcessedMessageDatetime() {
        return lastProcessedMessageDatetime;
    }
    public void setLastProcessedMessageDatetime(Instant lastProcessedMessageDatetime) {
        this.lastProcessedMessageDatetime = lastProcessedMessageDatetime;
    }
    public Instant getLastProcessingDatetime() {
        return lastProcessingDatetime;
    }
    public void setLastProcessingDatetime(Instant lastProcessingDatetime) {
        this.lastProcessingDatetime = lastProcessingDatetime;
    }
}
