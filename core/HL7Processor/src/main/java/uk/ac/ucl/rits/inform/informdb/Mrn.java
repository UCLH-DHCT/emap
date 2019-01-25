package uk.ac.ucl.rits.inform.informdb;

import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

public class Mrn {

    @Id
    private int mrn_id; // the record ID
    
    private int person_id;
    private int mrn; // the actual MRN - doesn't this have to be a string to allow for various formats?
    private Timestamp store_datetime;
    private Timestamp end_datetime;
    private String source_system;
    private Timestamp event_time;
    public int getMrn_id() {
        return mrn_id;
    }
    public void setMrn_id(int mrn_id) {
        this.mrn_id = mrn_id;
    }
    public int getPerson_id() {
        return person_id;
    }
    public void setPerson_id(int person_id) {
        this.person_id = person_id;
    }
    public int getMrn() {
        return mrn;
    }
    public void setMrn(int mrn) {
        this.mrn = mrn;
    }
    public Timestamp getStore_datetime() {
        return store_datetime;
    }
    public void setStore_datetime(Timestamp store_datetime) {
        this.store_datetime = store_datetime;
    }
    public Timestamp getEnd_datetime() {
        return end_datetime;
    }
    public void setEnd_datetime(Timestamp end_datetime) {
        this.end_datetime = end_datetime;
    }
    public String getSource_system() {
        return source_system;
    }
    public void setSource_system(String source_system) {
        this.source_system = source_system;
    }
    public Timestamp getEvent_time() {
        return event_time;
    }
    public void setEvent_time(Timestamp event_time) {
        this.event_time = event_time;
    }
}
