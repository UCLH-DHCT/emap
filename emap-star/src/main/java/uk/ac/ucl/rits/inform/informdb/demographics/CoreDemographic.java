package uk.ac.ucl.rits.inform.informdb.demographics;

import uk.ac.ucl.rits.inform.informdb.TemporalCore;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A core demographic represents the main demographics stored around patients.
 * These are attached to an MRN and describe patient level, rather than visit
 * level, data.
 * @author UCL RITS
 */
@Entity
@Table
public class CoreDemographic extends TemporalCore<CoreDemographic> implements Serializable {

    private static final long serialVersionUID = -8269778602198494673L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long coreDemographicId;

    @Column(nullable = false)
    private long mrnId;

    private String firstname;
    private String middlename;
    private String lastname;

    private LocalDate dateOfBirth;
    private LocalDate dateOfDeath;

    @Column(columnDefinition = "timestamp with time zone")
    private Instant datetimeOfBirth;
    @Column(columnDefinition = "timestamp with time zone")
    private Instant datetimeOfDeath;

    private boolean alive;
    private String homePostcode;
    private String sex;

    public CoreDemographic() {}

    public CoreDemographic(CoreDemographic other) {
        super(other);
        this.mrnId = other.mrnId;
        this.firstname = other.firstname;
        this.middlename = other.middlename;
        this.lastname = other.lastname;

        this.dateOfBirth = other.dateOfBirth;
        this.dateOfDeath = other.dateOfDeath;

        this.datetimeOfBirth = other.datetimeOfBirth;
        this.datetimeOfDeath = other.datetimeOfDeath;

        this.alive = other.alive;
        this.homePostcode = other.homePostcode;
        this.sex = other.sex;
    }

    /**
     * @return the coreDemographicId
     */
    public long getCoreDemographicId() {
        return coreDemographicId;
    }

    /**
     * @param coreDemographicId the coreDemographicId to set
     */
    public void setCoreDemographicId(long coreDemographicId) {
        this.coreDemographicId = coreDemographicId;
    }

    /**
     * @return the mrnId
     */
    public long getMrnId() {
        return mrnId;
    }

    /**
     * @param mrnId the mrnId to set
     */
    public void setMrnId(long mrnId) {
        this.mrnId = mrnId;
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
     * @return the dateOfBirth
     */
    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    /**
     * @param dateOfBirth the dateOfBirth to set
     */
    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    /**
     * @return the dateOfDeath
     */
    public LocalDate getDateOfDeath() {
        return dateOfDeath;
    }

    /**
     * @param dateOfDeath the dateOfDeath to set
     */
    public void setDateOfDeath(LocalDate dateOfDeath) {
        this.dateOfDeath = dateOfDeath;
    }

    /**
     * @return the datetimeOfBirth
     */
    public Instant getDatetimeOfBirth() {
        return datetimeOfBirth;
    }

    /**
     * @param datetimeOfBirth the datetimeOfBirth to set
     */
    public void setDatetimeOfBirth(Instant datetimeOfBirth) {
        this.datetimeOfBirth = datetimeOfBirth;
    }

    /**
     * @return the datetimeOfDeath
     */
    public Instant getDatetimeOfDeath() {
        return datetimeOfDeath;
    }

    /**
     * @param datetimeOfDeath the datetimeOfDeath to set
     */
    public void setDatetimeOfDeath(Instant datetimeOfDeath) {
        this.datetimeOfDeath = datetimeOfDeath;
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
     * @return the homePostcode
     */
    public String getHomePostcode() {
        return homePostcode;
    }

    /**
     * @param homePostcode the homePostcode to set
     */
    public void setHomePostcode(String homePostcode) {
        this.homePostcode = homePostcode;
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

    @Override
    public CoreDemographic copy() {
        return new CoreDemographic(this);
    }

}
