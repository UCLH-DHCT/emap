package uk.ac.ucl.rits.inform.informdb.visit_recordings;

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
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import java.time.Instant;

/**
 * VisitObservationType describes the meaning behind a specific observations. In
 * EHR systems these are often coded either with potentially ambiguous short
 * names, or sometimes just numbers. This table maps these system level terms
 * into standardised vocabularies to make their meanings clear.
 * @author Roma Klapaukh
 * @author Stef Piatek
 */
@Entity
@Data
@SuppressWarnings("serial")
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@NoArgsConstructor
@AuditTable
public class VisitObservationType extends TemporalCore<VisitObservationType, VisitObservationTypeAudit> {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long visitObservationTypeId;

    /**
     * The hospital system that emap received the data from (e.g. caboodle, clarity, HL7).
     */
    @Column(nullable = false)
    private String sourceSystem;

    /**
     * The data type in the source system.
     */
    @Column(nullable = false)
    private String sourceObservationType;

    /**
     * The code used by the hospital application to identify the observation type.
     */
    @Column(nullable = false)
    private String idInApplication;

    /**
     * Readable name for the hospital application observation type.
     */
    private String name;

    /**
     * Name displayed to users.
     */
    private String displayName;

    /**
     * Description of the data type.
     */
    private String description;

    /**
     * Mapping code for the observation from the standardised vocabulary system.
     */
    private String standardisedCode;

    /**
     * Nomenclature or classification system used.
     */
    private String standardisedVocabulary;

    /**
     * Data type expected to be returned.
     */
    private String primaryDataType;

    /**
     * Datetime that the data type was created in the source system.
     */
    private Instant creationTime;

    /**
     * Minimal information constructor.
     * @param idInApplication       Id from the application
     * @param sourceSystem          source system
     * @param sourceObservationType data type in the source system (e.g. flowsheet)
     */
    public VisitObservationType(String idInApplication, String sourceSystem, String sourceObservationType) {
        this.idInApplication = idInApplication;
        this.sourceSystem = sourceSystem;
        this.sourceObservationType = sourceObservationType;
    }

    /**
     * Build new entity from existing one.
     * @param other existing entity
     */
    public VisitObservationType(VisitObservationType other) {
        super(other);
        visitObservationTypeId = other.visitObservationTypeId;
        sourceSystem = other.sourceSystem;
        sourceObservationType = other.sourceObservationType;
        idInApplication = other.idInApplication;
        name = other.name;
        displayName = other.displayName;
        description = other.description;
        standardisedCode = other.standardisedCode;
        standardisedVocabulary = other.standardisedVocabulary;
        primaryDataType = other.primaryDataType;
        creationTime = other.creationTime;
    }

    @Override
    public VisitObservationType copy() {
        return new VisitObservationType(this);
    }

    /**
     * @param validUntil  the event time that invalidated the current state
     * @param storedUntil the time that star started processing the message that invalidated the current state
     * @return A new audit entity with the current state of the object.
     */
    @Override
    public VisitObservationTypeAudit createAuditEntity(Instant validUntil, Instant storedUntil) {
        return new VisitObservationTypeAudit(this, validUntil, storedUntil);
    }
}
