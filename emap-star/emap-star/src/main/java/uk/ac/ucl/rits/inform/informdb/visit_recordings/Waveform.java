package uk.ac.ucl.rits.inform.informdb.visit_recordings;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.Type;
import uk.ac.ucl.rits.inform.informdb.TemporalCore;
import uk.ac.ucl.rits.inform.informdb.annotation.AuditTable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;
import java.time.Instant;

/**
 * \brief A waveform observation recorded about a patient.
 *
 * @author Jeremy Stein
 */
@Entity
@Table(indexes = {
        @Index(name = "waveform_datetime", columnList = "observationDatetime"),
        @Index(name = "waveform_location", columnList = "location"),
})
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@AuditTable
public class Waveform extends TemporalCore<Waveform, WaveformAudit> {

    /**
     * \brief Unique identifier in EMAP for this visitObservation record.
     *
     * This is the primary key for the visitObservation table.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "waveform_id_sequence")
    private long waveformId;

    /**
     * \brief Identifier for the VisitObservationType associated with this record.
     *
     * This is a foreign key that joins the visitObservation table to the VisitObservationType table.
     */
//    @ManyToOne
//    @JoinColumn(name = "visitObservationTypeId", nullable = false)
//    private VisitObservationType visitObservationTypeId;

    /**
     * \brief Identifier for the HospitalVisit associated with this record.
     *
     * This is a foreign key that joins the visitObservation table to the HospitalVisit table.
     */
//    @ManyToOne
//    @JoinColumn(name = "hospitalVisitId", nullable = false)
//    private HospitalVisit hospitalVisitId;

    /**
     * \brief Date and time at which this visitObservation was first made.
     * In the case of an array, this is the time of the *first* item in the array.
     *
     * The validFrom {@link TemporalCore#getValidFrom()} is the recording time, or last updated time.
     */
    @Column(columnDefinition = "timestamp with time zone", nullable = false)
    private Instant observationDatetime;

    private long samplingRate;

    private String location;

    /**
     * \brief Value as a floating point array.
     */
    @Type(type = "uk.ac.ucl.rits.inform.informdb.visit_recordings.WaveformArray")
    @Column(columnDefinition = "DOUBLE PRECISION ARRAY", nullable = false)
    private Double[] valuesArray;

    /* unit goes in visit observation type (or equivalent table...) */

    /**
     * Default constructor.
     */
    public Waveform() {}

    /**
     * Minimal information constructor.
     * @param observationDatetime    observation datetime
     * @param validFrom              Time of the message event
     * @param storedFrom             Time that emap-core encountered the message
     */
    public Waveform(
            Instant observationDatetime,
            Instant validFrom,
            Instant storedFrom) {
        this.observationDatetime = observationDatetime;
        setValidFrom(validFrom);
        setStoredFrom(storedFrom);
    }

    /**
     * Build a new Visit observation from an existing one.
     * @param other existing visit observation
     */
    public Waveform(Waveform other) {
        super(other);
        this.waveformId = other.waveformId;
        this.valuesArray = other.valuesArray;
        this.observationDatetime = other.observationDatetime;
        this.samplingRate = other.samplingRate;
        this.location = other.location;
    }

    @Override
    public Waveform copy() {
        return new Waveform(this);
    }

    @Override
    public WaveformAudit createAuditEntity(Instant validUntil, Instant storedUntil) {
        return new WaveformAudit(this, validUntil, storedUntil);
    }
}
