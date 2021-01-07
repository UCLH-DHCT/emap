package uk.ac.ucl.rits.inform.informdb.labs;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.ac.ucl.rits.inform.informdb.AuditCore;
import uk.ac.ucl.rits.inform.informdb.TemporalCore;
import uk.ac.ucl.rits.inform.informdb.annotation.AuditTable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.time.Instant;

/**
 * This represents the definition of a single lab test by a single provider. An
 * individual test may feature more than once in this table, where there is more
 * than one lab provider that supplies it.
 * @author Roma Klapaukh
 */
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@AuditTable
public class LabTestDefinition extends TemporalCore<LabTestDefinition, AuditCore> {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long labTestDefinitionId;

    private long labTestDefinitionDurableId;

    /**
     * What system this code belongs to. Examples could be WinPath, or Epic.
     */
    @Column(nullable = false)
    private String labProvider;

    /**
     * The department within the lab responsible for the test.
     */
    private String labDepartment;

    /**
     * The code for this test as reported by the lab.
     */
    @Column(nullable = false)
    private String testLabCode;

    /**
     * The code for this test in a standardised vocabulary.
     */
    private String testStandardCode;

    /**
     * Where a standardised code is provided, this says which vocabulary was used.
     */
    private String standardisedVocabulary;

    public LabTestDefinition() {}

    public LabTestDefinition(LabTestDefinition other) {
        super(other);
        this.labTestDefinitionDurableId = other.labTestDefinitionDurableId;
        this.labProvider = other.labProvider;
        this.labDepartment = other.labDepartment;
        this.testLabCode = other.testLabCode;
        this.testStandardCode = other.testStandardCode;
        this.standardisedVocabulary = other.standardisedVocabulary;
    }

    @Override
    public LabTestDefinition copy() {
        return new LabTestDefinition(this);
    }

    @Override
    public AuditCore createAuditEntity(Instant validUntil, Instant storedUntil) {
        throw new UnsupportedOperationException();
    }
}
