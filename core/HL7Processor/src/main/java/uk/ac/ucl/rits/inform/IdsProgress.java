package uk.ac.ucl.rits.inform;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class IdsProgress {
    @Id
    private int id;
    private int lastProcessedIdsUnid;
    
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
}
