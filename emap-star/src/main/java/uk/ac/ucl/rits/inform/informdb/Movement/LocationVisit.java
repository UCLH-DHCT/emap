package uk.ac.ucl.rits.inform.informdb.Movement;

import java.io.Serializable;
import java.time.Instant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import uk.ac.ucl.rits.inform.informdb.TemporalCore;

/**
 * This represents a patient being in a location for an amount of time. Every
 * location visit is part of a hospital visit, as you have to be in the hospital
 * before you can go to a specific location within it. Location visits can
 * optionally have a parent location visit. This happens when the patient is
 * still considered to be at the parent location (e.g. going down to an MRI
 * scanner from a ward bed doesn't vacate the ward bed).
 *
 * @author UCL RITS
 *
 */
@Entity
@JsonIgnoreProperties({})
@Table(indexes = {})
public class LocationVisit extends TemporalCore implements Serializable {

    private static final long serialVersionUID = -8228844390430073225L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long              bed_visit_id;
    @Column(nullable = false)
    private long              bed_visit_durable_id;

    @Column(nullable = false)
    private long              parent_hospital_visit_durable_id;
    private long              parent_bed_visit_durable_id;

    @Column(columnDefinition = "timestamp with time zone")
    private Instant           admission_time;
    @Column(columnDefinition = "timestamp with time zone")
    private Instant           discharge_time;
    private String            location;

    /**
     * @return the bed_visit_id
     */
    public long getBed_visit_id() {
        return bed_visit_id;
    }

    /**
     * @param bed_visit_id the bed_visit_id to set
     */
    public void setBed_visit_id(long bed_visit_id) {
        this.bed_visit_id = bed_visit_id;
    }

    /**
     * @return the bed_visit_durable_id
     */
    public long getBed_visit_durable_id() {
        return bed_visit_durable_id;
    }

    /**
     * @param bed_visit_durable_id the bed_visit_durable_id to set
     */
    public void setBed_visit_durable_id(long bed_visit_durable_id) {
        this.bed_visit_durable_id = bed_visit_durable_id;
    }

    /**
     * @return the parent_hospital_visit_durable_id
     */
    public long getParent_hospital_visit_durable_id() {
        return parent_hospital_visit_durable_id;
    }

    /**
     * @param parent_hospital_visit_durable_id the parent_hospital_visit_durable_id
     *                                         to set
     */
    public void setParent_hospital_visit_durable_id(long parent_hospital_visit_durable_id) {
        this.parent_hospital_visit_durable_id = parent_hospital_visit_durable_id;
    }

    /**
     * @return the parent_bed_visit_durable_id
     */
    public long getParent_bed_visit_durable_id() {
        return parent_bed_visit_durable_id;
    }

    /**
     * @param parent_bed_visit_durable_id the parent_bed_visit_durable_id to set
     */
    public void setParent_bed_visit_durable_id(long parent_bed_visit_durable_id) {
        this.parent_bed_visit_durable_id = parent_bed_visit_durable_id;
    }

    /**
     * @return the admission_time
     */
    public Instant getAdmission_time() {
        return admission_time;
    }

    /**
     * @param admission_time the admission_time to set
     */
    public void setAdmission_time(Instant admission_time) {
        this.admission_time = admission_time;
    }

    /**
     * @return the discharge_time
     */
    public Instant getDischarge_time() {
        return discharge_time;
    }

    /**
     * @param discharge_time the discharge_time to set
     */
    public void setDischarge_time(Instant discharge_time) {
        this.discharge_time = discharge_time;
    }

    /**
     * @return the location
     */
    public String getLocation() {
        return location;
    }

    /**
     * @param location the location to set
     */
    public void setLocation(String location) {
        this.location = location;
    }

}
