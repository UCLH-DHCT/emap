package uk.ac.ucl.rits.inform.informdb.identity;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * This a single visit to the hospital. This is not necessarily an inpatient
 * visit, but includes outpatients, imaging, etc.
 * @author UCL RITS
 */
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Table(indexes = {@Index(name = "encounterIndex", columnList = "encounter", unique = false)})
public class HospitalVisit extends HospitalVisitParent {

    private static final long serialVersionUID = -6495238097074592105L;

    /**
     * A primary key for this row.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long hospitalVisitId;



    /**
     * Default constructor.
     */
    public HospitalVisit() {
    }

    /**
     * Build a hospital visit from an existing object.
     * @param other hospital visit
     */
    public HospitalVisit(HospitalVisit other) {
        super(other);
        hospitalVisitId = other.getHospitalVisitId();
        setEncounter(other.getEncounter());
    }

    /**
     * @return the hospitalVisitId
     */
    public Long getHospitalVisitId() {
        return hospitalVisitId;
    }

    /**
     * @param hospitalVisitId the hospitalVisitId to set
     */
    public void setHospitalVisitId(Long hospitalVisitId) {
        this.hospitalVisitId = hospitalVisitId;
    }

    @Override
    public HospitalVisit copy() {
        return new HospitalVisit(this);
    }
}
