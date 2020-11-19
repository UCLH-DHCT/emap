package uk.ac.ucl.rits.inform.informdb.visit_recordings;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.ac.ucl.rits.inform.informdb.TemporalCore;
import uk.ac.ucl.rits.inform.informdb.annotation.AuditTable;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.Instant;

/**
 * VisitObservations represent discrete nurse (or machine) recoded observations
 * about patients at specific time points.
 * @author Roma Klapaukh & Stef Piatek
 */
@SuppressWarnings("serial")
@Entity
@Table
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@AuditTable
public class VisitObservation extends TemporalCore<VisitObservation, VisitObservationAudit> {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long visitObservationId;

    @ManyToOne
    @JoinColumn(name = "visitObservationTypeId", nullable = false)
    private VisitObservationType visitObservationTypeId;

    @ManyToOne
    @JoinColumn(name = "hospitalVisitId", nullable = false)
    private HospitalVisit hospitalVisitId;

    /**
     * The time this individual observation was first made.
     */
    @Column(columnDefinition = "timestamp with time zone", nullable = false)
    private Instant recordingDatetime;

    private String valueAsText;
    private Double valueAsReal;
    private String unit;
    @Column(columnDefinition = "text")
    private String comment;

    /**
     * Default constructor.
     */
    public VisitObservation() {}

    /**
     * Build a new Visit observation from an existing one.
     * @param other existing visit observation
     */
    public VisitObservation(VisitObservation other) {
        super(other);
        this.visitObservationTypeId = other.visitObservationTypeId;
        this.hospitalVisitId = other.hospitalVisitId;
        this.valueAsText = other.valueAsText;
        this.valueAsReal = other.valueAsReal;
        this.unit = other.unit;
        this.comment = other.comment;
        this.recordingDatetime = other.recordingDatetime;
    }

    @Override
    public VisitObservation copy() {
        return new VisitObservation(this);
    }

    @Override
    public VisitObservationAudit createAuditEntity(Instant validUntil, Instant storedUntil) {
        throw new UnsupportedOperationException();
    }
}
