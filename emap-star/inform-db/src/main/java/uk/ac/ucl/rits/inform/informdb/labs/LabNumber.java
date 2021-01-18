package uk.ac.ucl.rits.inform.informdb.labs;

import lombok.Data;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.io.Serializable;
import java.time.Instant;

/**
 * A lab number represents a reference number attached to a lab. As labs are
 * often done externally, there is separate fields to track the lab number from
 * the point of view of the EHR compared to the system processing the lab.
 * @author Roma Klapaukh
 * @author Stef Piatek
 */
@Entity
@Data
public class LabNumber implements Serializable {

    private static final long serialVersionUID = -5771782759320217911L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long labNumberId;

    @ManyToOne
    @JoinColumn(name = "mrnId", nullable = false)
    private Mrn mrnId;

    @ManyToOne
    @JoinColumn(name = "hospitalVisitId", nullable = false)
    private HospitalVisit hospitalVisitId;

    /**
     * Lab number in the EHR.
     */
    private String internalLabNumber;
    /**
     * Lab number for the system doing the lab test.
     */
    private String externalLabNumber;

    /**
     * Code for specimen type.
     */
    private String specimenType;
    private String sourceSystem;

    @Column(columnDefinition = "timestamp with time zone")
    private Instant storedFrom;

    public LabNumber() {
    }

    public LabNumber(
            Mrn mrnId, HospitalVisit hospitalVisitId, String internalLabNumber, String externalLabNumber, String specimenType,
            String sourceSystem, Instant storedFrom) {
        this.mrnId = mrnId;
        this.hospitalVisitId = hospitalVisitId;
        this.internalLabNumber = internalLabNumber;
        this.externalLabNumber = externalLabNumber;
        this.specimenType = specimenType;
        this.sourceSystem = sourceSystem;
        this.storedFrom = storedFrom;
    }
}
