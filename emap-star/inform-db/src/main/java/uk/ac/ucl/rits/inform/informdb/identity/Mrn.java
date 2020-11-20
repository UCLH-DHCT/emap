package uk.ac.ucl.rits.inform.informdb.identity;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.Check;

import lombok.Data;

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
@Data
@SuppressWarnings("serial")
@Entity
@Table(indexes = {@Index(name = "mrn_index", columnList = "mrn"),
        @Index(name = "nhs_number", columnList = "nhsNumber")})
@Check(constraints = "(mrn is not null) or (nhs_number is not null)")
public class Mrn implements Serializable {

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


    @Override
    public String toString() {
        return String.format("Mrn [mrnId=%d, mrn=%s, nhsNumber=%s, sourceSystem=%s]", mrnId, mrn, nhsNumber, sourceSystem);
    }

}
