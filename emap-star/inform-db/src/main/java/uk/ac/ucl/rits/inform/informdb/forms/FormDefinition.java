package uk.ac.ucl.rits.inform.informdb.forms;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.ac.ucl.rits.inform.informdb.TemporalCore;
import uk.ac.ucl.rits.inform.informdb.annotation.AuditTable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.time.Instant;

/**
 * Form (Eg. SmartForm) metadata.
 */
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@AuditTable
public class FormDefinition extends TemporalCore<FormDefinition, FormDefinitionAudit> {
    /**
     * \brief Unique identifier in EMAP for this SmartForm description record.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long formDefinitionId;

    /**
     * \brief A string name for this form, as used by the source system.
     * HLV ?? (Eg. if a SmartForm, SmartForm ID = "SmartForm1222")
     */
    private String formName;

    /**
     * \brief Text description of the form.
     */
    private String description;

    @Override
    public FormDefinition copy() {
        return null;
    }

    /**
     * @param validUntil  the event time that invalidated the current state
     * @param storedUntil the time that star started processing the message that invalidated the current state
     * @return A new audit entity with the current state of the object.
     */
    @Override
    public FormDefinitionAudit createAuditEntity(Instant validUntil, Instant storedUntil) {
        return null;
    }
}
