package uk.ac.ucl.rits.inform.informdb;

import java.sql.Timestamp;
import java.time.Instant;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class Person {

    public Person() {
        /*
         * Is there a circumstance where we'd want to allow the client to set the create
         * time?
         * Creating a person retrospectively? 
         */
        this.create_datetime = Timestamp.from(Instant.now());
    }

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private int person_id;

    private Timestamp create_datetime;

    public int getPerson_id() {
        return person_id;
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
