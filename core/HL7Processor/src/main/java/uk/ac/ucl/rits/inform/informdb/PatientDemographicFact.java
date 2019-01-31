package uk.ac.ucl.rits.inform.informdb;

import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Entity
public class PatientDemographicFact {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int fact_id;

    // should this be CSN or something else?
    @ManyToOne
    private Encounter encounter;

    private int fact_type;
    private Timestamp store_datetime;
    private Timestamp end_datetime;

    public int getFact_id() {
        return fact_id;
    }

    public void setFact_id(int fact_id) {
        this.fact_id = fact_id;
    }

    public Encounter getEncounter() {
        return encounter;
    }

    public void setEncounter(Encounter encounter) {
        this.encounter = encounter;
    }

    public int getFact_type() {
        return fact_type;
    }

    public void setFact_type(int fact_type) {
        this.fact_type = fact_type;
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

}
