package uk.ac.ucl.rits.inform.informdb.identity;

import org.hibernate.annotations.Check;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * This class represents the association of Medical Resource Number (MRN) to
 * an individual patient (a Person). This represents nothing more than a list of
 * all MRNs the system is aware of.
 * <p>
 * Over the course of its lifetime a single MRN may be associated with any
 * number of patients. However, it may only be associated with a single patient
 * at any given point in history.
 * @author UCL RITS
 */
@Entity
@Table(indexes = {@Index(name = "mrnIndex", columnList = "mrn", unique = true)})
@Check(constraints = "(mrn is not null) or (nhs_number is not null)")
public class Mrn implements Serializable {

    private static final long serialVersionUID = -4125275916062604528L;

    /**
     * The MrnId is the UID for the association of an MRN value to a Person.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long mrnId;

    @OneToMany(targetEntity = HospitalVisit.class, mappedBy = "mrnId", cascade = CascadeType.ALL)
    private List<HospitalVisit> hospitalVisits;

    /**
     * The value of the MRN identifier.
     */
    @Column(unique = true)
    private String mrn;

    /**
     * NHS number.
     */
    private String nhsNumber;

    /**
     * The system from which this MRN was initially discovered.
     */
    private String sourceSystem;

    /**
     * The datetime this row was written.
     */
    @Column(nullable = false, columnDefinition = "timestamp with time zone")
    private Instant storedFrom;

    /**
     * @return the mrnId
     */
    public Long getMrnId() {
        return mrnId;
    }

    /**
     * @param mrnId the mrnId to set
     */
    public void setMrnId(Long mrnId) {
        this.mrnId = mrnId;
    }

    /**
     * @return the mrn
     */
    public String getMrn() {
        return mrn;
    }

    /**
     * @param mrn the mrn to set
     */
    public void setMrn(String mrn) {
        this.mrn = mrn;
    }

    /**
     * @return the nhsNumber.
     */
    public String getNhsNumber() {
        return nhsNumber;
    }

    /**
     * @param nhsNumber the nhsNumber to set.
     */
    public void setNhsNumber(String nhsNumber) {
        this.nhsNumber = nhsNumber;
    }

    /**
     * @return the sourceSystem
     */
    public String getSourceSystem() {
        return sourceSystem;
    }

    /**
     * @param sourceSystem the sourceSystem to set
     */
    public void setSourceSystem(String sourceSystem) {
        this.sourceSystem = sourceSystem;
    }

    /**
     * Add an HospitalVisit to the relation list. Does not add the inverse
     * relationship.
     * @param hospitalVisit The HospitalVisit to add.
     */
    public void linkHospitalVisit(HospitalVisit hospitalVisit) {
        if (this.hospitalVisits == null) {
            this.hospitalVisits = new ArrayList<>();
        }
        this.hospitalVisits.add(hospitalVisit);
    }

    /**
     * Get the list of relationships where this Mrn is linked to a HospitalVisit.
     * @return the list of all Hospital Visit relationships
     */
    public List<HospitalVisit> getHospitalVisits() {
        return hospitalVisits;
    }

    /**
     * @param hospitalVisits the encounters to set
     */
    public void setHospitalVisits(List<HospitalVisit> hospitalVisits) {
        this.hospitalVisits = hospitalVisits;
    }

    @Override
    public String toString() {
        return String.format("Mrn [mrnId=%d, mrn=%s, nhsNumber=%s, sourceSystem=%s]", mrnId, mrn, nhsNumber, sourceSystem);
    }

    /**
     * @return the Instant this Mrn was first recorded in the database
     */
    public Instant getStoredFrom() {
        return storedFrom;
    }

    /**
     * @param storedFrom the Instant this Mrn was first recorded in the database
     */
    public void setStoredFrom(Instant storedFrom) {
        this.storedFrom = storedFrom;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Mrn mrn1 = (Mrn) o;
        return mrnId.equals(mrn1.mrnId)
                // not including hospital visits
                && Objects.equals(mrn, mrn1.mrn)
                && Objects.equals(nhsNumber, mrn1.nhsNumber)
                && Objects.equals(sourceSystem, mrn1.sourceSystem)
                && storedFrom.equals(mrn1.storedFrom);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mrnId, hospitalVisits, mrn, nhsNumber, sourceSystem, storedFrom);
    }
}
