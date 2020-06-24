package uk.ac.ucl.rits.inform.informdb.demographics;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import uk.ac.ucl.rits.inform.informdb.TemporalCore;

/**
 * A core demographic represents the main demographics stored around patients.
 * These are attached to an MRN and describe patient level, rather than visit
 * level, data.
 *
 * @author UCL RITS
 *
 */
@Entity
@Table(indexes = {})
public class CoreDemographic extends TemporalCore implements Serializable {

    private static final long serialVersionUID = -8269778602198494673L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long              core_demographic_id;
    @Column(nullable = false)
    private long              core_demographic_durable_id;

    @Column(nullable = false)
    private long              mrn_id;

    private String            firstname;
    private String            middlename;
    private String            lastname;

    private LocalDate         date_of_birth;
    private LocalDate         date_of_death;

    @Column(columnDefinition = "timestamp with time zone")
    private Instant           datetime_of_birth;
    @Column(columnDefinition = "timestamp with time zone")
    private Instant           datetime_of_death;

    private boolean           alive;
    private String            home_postcode;
    private String            sex;

    /**
     * @return the core_demographic_id
     */
    public long getCore_demographic_id() {
        return core_demographic_id;
    }

    /**
     * @param core_demographic_id the core_demographic_id to set
     */
    public void setCore_demographic_id(long core_demographic_id) {
        this.core_demographic_id = core_demographic_id;
    }

    /**
     * @return the core_demographic_durable_id
     */
    public long getCore_demographic_durable_id() {
        return core_demographic_durable_id;
    }

    /**
     * @param core_demographic_durable_id the core_demographic_durable_id to set
     */
    public void setCore_demographic_durable_id(long core_demographic_durable_id) {
        this.core_demographic_durable_id = core_demographic_durable_id;
    }

    /**
     * @return the mrn_id
     */
    public long getMrn_id() {
        return mrn_id;
    }

    /**
     * @param mrn_id the mrn_id to set
     */
    public void setMrn_id(long mrn_id) {
        this.mrn_id = mrn_id;
    }

    /**
     * @return the firstname
     */
    public String getFirstname() {
        return firstname;
    }

    /**
     * @param firstname the firstname to set
     */
    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    /**
     * @return the middlename
     */
    public String getMiddlename() {
        return middlename;
    }

    /**
     * @param middlename the middlename to set
     */
    public void setMiddlename(String middlename) {
        this.middlename = middlename;
    }

    /**
     * @return the lastname
     */
    public String getLastname() {
        return lastname;
    }

    /**
     * @param lastname the lastname to set
     */
    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    /**
     * @return the date_of_birth
     */
    public LocalDate getDate_of_birth() {
        return date_of_birth;
    }

    /**
     * @param date_of_birth the date_of_birth to set
     */
    public void setDate_of_birth(LocalDate date_of_birth) {
        this.date_of_birth = date_of_birth;
    }

    /**
     * @return the date_of_death
     */
    public LocalDate getDate_of_death() {
        return date_of_death;
    }

    /**
     * @param date_of_death the date_of_death to set
     */
    public void setDate_of_death(LocalDate date_of_death) {
        this.date_of_death = date_of_death;
    }

    /**
     * @return the datetime_of_birth
     */
    public Instant getDatetime_of_birth() {
        return datetime_of_birth;
    }

    /**
     * @param datetime_of_birth the datetime_of_birth to set
     */
    public void setDatetime_of_birth(Instant datetime_of_birth) {
        this.datetime_of_birth = datetime_of_birth;
    }

    /**
     * @return the datetime_of_death
     */
    public Instant getDatetime_of_death() {
        return datetime_of_death;
    }

    /**
     * @param datetime_of_death the datetime_of_death to set
     */
    public void setDatetime_of_death(Instant datetime_of_death) {
        this.datetime_of_death = datetime_of_death;
    }

    /**
     * @return the alive
     */
    public boolean isAlive() {
        return alive;
    }

    /**
     * @param alive the alive to set
     */
    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    /**
     * @return the home_postcode
     */
    public String getHome_postcode() {
        return home_postcode;
    }

    /**
     * @param home_postcode the home_postcode to set
     */
    public void setHome_postcode(String home_postcode) {
        this.home_postcode = home_postcode;
    }

    /**
     * @return the sex
     */
    public String getSex() {
        return sex;
    }

    /**
     * @param sex the sex to set
     */
    public void setSex(String sex) {
        this.sex = sex;
    }

}
