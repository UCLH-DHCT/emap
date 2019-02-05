package uk.ac.ucl.rits.inform.informdb;

import java.sql.Timestamp;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Entity
public class Mrn {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private int mrn_id; // the ID for the record linking an MRN to a person

    @ManyToOne
    @JoinColumn(name = "person")
    private Person person;
    
    private String mrn; // the actual MRN
    private Timestamp store_datetime;
    private Timestamp end_datetime;
    private String source_system;
    private Timestamp event_time;

    public int getMrn_id() {
        return mrn_id;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public String getMrn() {
        return mrn;
    }

    public void setMrn(String mrn) {
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

    @Override
    public String toString() {
        return "Mrn [mrn_id=" + mrn_id + ", person=" + person + ", mrn=" + mrn + ", store_datetime=" + store_datetime
                + ", end_datetime=" + end_datetime + ", source_system=" + source_system + ", event_time=" + event_time
                + "]";
    }
}
