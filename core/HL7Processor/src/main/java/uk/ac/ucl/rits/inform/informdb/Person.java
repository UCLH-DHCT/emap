package uk.ac.ucl.rits.inform.informdb;


import java.sql.Timestamp;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Person {
    
    public Person() {}

    public Person(int person_id, Timestamp create_datetime) {
        this.person_id = person_id;
        this.create_datetime = create_datetime;
    }

    @Id
    private int person_id;
    
    private Timestamp create_datetime;

    public int getPerson_id() {
        return person_id;
    }

    public void setPerson_id(int person_id) {
        this.person_id = person_id;
    }

    public Timestamp getCreate_datetime() {
        return create_datetime;
    }

    public void setCreate_datetime(Timestamp create_datetime) {
        this.create_datetime = create_datetime;
    }

    @Override
    public String toString() {
        return "Person [person_id=" + person_id + ", create_datetime=" + create_datetime + "]";
    }
}
