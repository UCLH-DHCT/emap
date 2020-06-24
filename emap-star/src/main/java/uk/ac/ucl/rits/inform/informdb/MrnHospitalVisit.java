package uk.ac.ucl.rits.inform.informdb;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * An MRN has multiple encounters, and an hospitalVisit may (over time) be
 * associated with several MRNs. Each MrnHospitalVisit represents a single temporary
 * instance of such a relationship.
 *
 * @author Jeremy Stein
 *
 */
@Entity
@Table(name = "mrn_encounter", indexes = {
        @Index(columnList = "mrn", unique = false),
        @Index(columnList = "hospitalVisit", unique = false),
        @Index(name = "mrn_encounter_stored_from_index", columnList = "storedFrom", unique = false),
        @Index(name = "mrn_encounter_stored_until_index", columnList = "storedUntil", unique = false),
        @Index(name = "mrn_encounter_valid_from_index", columnList = "validFrom", unique = false),
        @Index(name = "mrn_encounter_valid_until_index", columnList = "validUntil", unique = false),
})
@JsonIgnoreProperties({"mrn", "valid"})
public class MrnHospitalVisit extends TemporalCore implements Serializable {

    private static final long serialVersionUID = 4153619042373632717L;

    /**
     * A unique row identifier.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long   mrnHospitalVisitId;

    /**
     * The MRN this hospital visit happened under
     */
    @ManyToOne
    @JoinColumn(name = "mrnId", nullable = false)
    private Mrn       mrnId;

    /**
     * The durable key of the hospitalVisit that happened under this MRN.
     */
    @Column(nullable = false)
    private Long hospitalVisitDurableId;

    /**
     * Create a new Mrn/HospitalVisit association.
     */
    public MrnHospitalVisit() {}

    /**
     * Create a new MRN/HospitalVisit association.
     *
     * @param mrn the MRN
     * @param enc the HospitalVisit
     */
    public MrnHospitalVisit(Mrn mrn, long hospitalVisitDurableId) {
        this.mrnId = mrn;
        this.hospitalVisitDurableId = hospitalVisitDurableId;
    }

    /**
     * @return the MRN in the association
     */
    public Mrn getMrn() {
        return mrnId;
    }

    /**
     * @return the HospitalVisit in the association
     */
    public long getLospitalVisitDurableId() {
        return hospitalVisitDurableId;
    }

    /**
     * @return the mrnHospitalVisitId
     */
    public Long getMrnEncounterId() {
        return mrnHospitalVisitId;
    }

    /**
     * @param mrnHospitalVisitId the mrnHospitalVisitId to set
     */
    public void setMrnEncounterId(Long mrnEncounterId) {
        this.mrnHospitalVisitId = mrnEncounterId;
    }

    /**
     * @param mrn the mrn to set
     */
    public void setMrn(Mrn mrn) {
        this.mrnId = mrn;
    }

    /**
     * @param hospitalVisit the hospitalVisit to set
     */
    public void setHospitalVisitDurableId(long hospitalVisitDurableId) {
        this.hospitalVisitDurableId = hospitalVisitDurableId;
    }

    @Override
    public String toString() {
        return String.format("MrnHospitalVisit [mrnHospitalVisitId=%d, mrnId=%d, hospitalVisitDurableKey=%d]", mrnHospitalVisitId, mrnId.getMrnId(),
                this.hospitalVisitDurableId);
    }
}
