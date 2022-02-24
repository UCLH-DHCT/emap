package uk.ac.ucl.rits.inform.informdb.labs;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import uk.ac.ucl.rits.inform.informdb.TemporalCore;
import uk.ac.ucl.rits.inform.informdb.annotation.AuditTable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.Instant;

/**
 * A sequence from MRC-CLIMB.
 * <p>
 * Created for HOCI continuation project in order to store SARS-CoV2 sequences.
 * @author Stef Piatek
 */
@SuppressWarnings("serial")
@Entity
@Data
@Table(indexes = {@Index(name = "hs_lab_sample_id", columnList = "labSampleId")})
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
@AuditTable
public class ClimbSequence extends TemporalCore<ClimbSequence, ClimbSequenceAudit> {

    /**
     * \brief Unique identifier in EMAP for this record.
     * <p>
     * This is the primary key for the table.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long climbSequenceId;

    /**
     * \brief Identifier for the LabSample associated with this record.
     * <p>
     * Patient sequences will always have a labSampleId, and community samples will not have a labSampleId set.
     */
    @ManyToOne
    @JoinColumn(name = "labSampleId")
    private LabSample labSampleId;

    /**
     * \brief FASTA header.
     */
    private String fastaHeader;

    /**
     * \brief FASTA sequence.
     */
    @Column(columnDefinition = "text")
    private String sequence;

    /**
     * \brief COG-UK identifier for the sequence.
     */
    private String cogId;

    /**
     * \brief PHE identifier for the sequence.
     */
    @Column(nullable = false)
    private String pheId;

    /**
     * Create minimal ClimbSequence.
     * @param labSampleId parent LabSample
     */
    public ClimbSequence(LabSample labSampleId, String pheId) {
        this.labSampleId = labSampleId;
        this.pheId = pheId;
    }


    private ClimbSequence(ClimbSequence other) {
        super(other);
        climbSequenceId = other.climbSequenceId;
        fastaHeader = other.fastaHeader;
        sequence = other.sequence;
        labSampleId = other.labSampleId;
        cogId = other.cogId;
        pheId = other.pheId;
    }


    @Override
    public ClimbSequence copy() {
        return new ClimbSequence(this);
    }

    @Override
    public ClimbSequenceAudit createAuditEntity(Instant validUntil, Instant storedUntil) {
        return new ClimbSequenceAudit(this, validUntil, storedUntil);
    }
}
