package uk.ac.ucl.rits.inform.informdb.smart_data_elements;

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
 * SmartForm metadata.
 */
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@AuditTable
public class SmartFormDefinition extends TemporalCore<SmartFormDefinition, SmartFormDefinitionAudit> {
    /**
     * \brief Unique identifier in EMAP for this SmartForm description record.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long smartFormDefinitionId;

    /**
     * \brief The epic string ID of the SmartForm.
     * HLV ?? (Eg. "SmartForm1222")
     */
    private String epicSmartFormId;

    /**
     * \brief Text description of the SmartForm.
     */
    private String description;

    @Override
    public SmartFormDefinition copy() {
        return null;
    }

    /**
     * @param validUntil  the event time that invalidated the current state
     * @param storedUntil the time that star started processing the message that invalidated the current state
     * @return A new audit entity with the current state of the object.
     */
    @Override
    public SmartFormDefinitionAudit createAuditEntity(Instant validUntil, Instant storedUntil) {
        return null;
    }
}
