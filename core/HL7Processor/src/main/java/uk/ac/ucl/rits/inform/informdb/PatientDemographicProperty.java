package uk.ac.ucl.rits.inform.informdb;

import java.sql.Timestamp;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Entity
public class PatientDemographicProperty {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int property_id;

    @ManyToOne
    private PatientDemographicFact fact_id;
    
    @ManyToOne
    private Attribute attribute;
    private String value_as_string;
    private int value_as_integer;
    private boolean value_as_boolean;
    private float value_as_real;
    private Timestamp value_as_datetime;
    private int value_as_attribute;
    private int value_as_link;
    private Timestamp store_datetime;
    private Timestamp end_datetime;
    public int getProperty_id() {
        return property_id;
    }
    public void setProperty_id(int property_id) {
        this.property_id = property_id;
    }
    public PatientDemographicFact getFact_id() {
        return fact_id;
    }
    public void setFact_id(PatientDemographicFact fact_id) {
        this.fact_id = fact_id;
    }
    public Attribute getAttribute() {
        return attribute;
    }
    public void setAttribute(Attribute attribute) {
        this.attribute = attribute;
    }
    public String getValue_as_string() {
        return value_as_string;
    }
    public void setValue_as_string(String value_as_string) {
        this.value_as_string = value_as_string;
    }
    public int getValue_as_integer() {
        return value_as_integer;
    }
    public void setValue_as_integer(int value_as_integer) {
        this.value_as_integer = value_as_integer;
    }
    public boolean isValue_as_boolean() {
        return value_as_boolean;
    }
    public void setValue_as_boolean(boolean value_as_boolean) {
        this.value_as_boolean = value_as_boolean;
    }
    public float getValue_as_real() {
        return value_as_real;
    }
    public void setValue_as_real(float value_as_real) {
        this.value_as_real = value_as_real;
    }
    public Timestamp getValue_as_datetime() {
        return value_as_datetime;
    }
    public void setValue_as_datetime(Timestamp value_as_datetime) {
        this.value_as_datetime = value_as_datetime;
    }
    public int getValue_as_attribute() {
        return value_as_attribute;
    }
    public void setValue_as_attribute(int value_as_attribute) {
        this.value_as_attribute = value_as_attribute;
    }
    public int getValue_as_link() {
        return value_as_link;
    }
    public void setValue_as_link(int value_as_link) {
        this.value_as_link = value_as_link;
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
