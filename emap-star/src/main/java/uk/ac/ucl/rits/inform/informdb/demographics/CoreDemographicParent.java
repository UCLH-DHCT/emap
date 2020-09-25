package uk.ac.ucl.rits.inform.informdb.demographics;

import uk.ac.ucl.rits.inform.informdb.TemporalCore;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Parent class that is not created as an entity to avoid polymorphic queries based on the original and audit table.
 * <p>
 * A core demographic represents the main demographics stored around patients.
 * These are attached to an MRN and describe patient level, rather than visit
 * level, data.
 * @author UCL RITS
 */
@MappedSuperclass
public class CoreDemographicParent extends TemporalCore<CoreDemographicParent> implements Serializable {

    private static final long serialVersionUID = -8269778602198494673L;


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

    public CoreDemographicParent() {}

    public CoreDemographicParent(CoreDemographicParent other) {
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
    public CoreDemographicParent copy() {
        return new CoreDemographicParent(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CoreDemographicParent that = (CoreDemographicParent) o;
        return mrnId == that.mrnId
                && alive == that.alive
                && Objects.equals(firstname, that.firstname)
                && Objects.equals(middlename, that.middlename)
                && Objects.equals(lastname, that.lastname)
                && Objects.equals(dateOfBirth, that.dateOfBirth)
                && Objects.equals(dateOfDeath, that.dateOfDeath)
                && Objects.equals(datetimeOfBirth, that.datetimeOfBirth)
                && Objects.equals(datetimeOfDeath, that.datetimeOfDeath)
                && Objects.equals(homePostcode, that.homePostcode)
                && Objects.equals(sex, that.sex);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mrnId, firstname, middlename, lastname, dateOfBirth, dateOfDeath,
                datetimeOfBirth, datetimeOfDeath, alive, homePostcode, sex);
    }
}
