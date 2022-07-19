package uk.ac.ucl.rits.inform.informdb.forms;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.ac.ucl.rits.inform.informdb.TemporalCore;
import uk.ac.ucl.rits.inform.informdb.annotation.AuditTable;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Form metadata.
 */
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@AuditTable
public class FormDefinition extends TemporalCore<FormDefinition, FormDefinitionAudit> {
    /**
     * \brief Unique identifier in EMAP for this Form description record.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long formDefinitionId;

    /**
     * \brief The unique string ID that the source system (Epic) uses for this form.
     * .
     * .
     * Eg. "2056"
     */
    @Column(nullable = false, unique = true)
    private String formSourceId;

    /**
     *
     * \brief A string name for this form, as used by the source system.
     *
     * .
     * .
     * Eg. "UCLH TEP ADVANCED"
     */
    private String formName;

    /**
     * \brief Patient friendly name of the form.
     * .
     * .
     * Only about 10% of forms specify this field.
     */
    @Column(columnDefinition = "text")
    private String patientFriendlyName;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "formDefinitionId")
    private List<FormDefinitionFormQuestion> questions = new ArrayList<>();

    public FormDefinition() {
    }

    private FormDefinition(FormDefinition other) {
        super(other);
        this.formDefinitionId = other.formDefinitionId;
        this.formSourceId = other.formSourceId;
        this.formName = other.formName;
        this.patientFriendlyName = other.patientFriendlyName;
    }

    @Override
    public FormDefinition copy() {
        return new FormDefinition(this);
    }

    /**
     * @param validUntil  the event time that invalidated the current state
     * @param storedUntil the time that star started processing the message that invalidated the current state
     * @return A new audit entity with the current state of the object.
     */
    @Override
    public FormDefinitionAudit createAuditEntity(Instant validUntil, Instant storedUntil) {
        return new FormDefinitionAudit(this, validUntil, storedUntil);
    }
}
