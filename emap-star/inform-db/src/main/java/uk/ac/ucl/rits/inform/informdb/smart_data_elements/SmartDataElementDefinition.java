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
 * Stores metadata for a smart data element (SDE).
 */
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@AuditTable
public class SmartDataElementDefinition extends TemporalCore<SmartDataElementDefinition, SmartDataElementDefinitionAudit> {
    /**
     * \brief Unique identifier in EMAP for this Smart Data Element description record.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long smartDataElementDefinitionId;

    /**
     * \brief String identifier from Epic for a Smart Data Element.
     * HLV 40.
     */
    private String smartDataElementIdString;

    /**
     * \brief String description of a Smart Data Element, where available
     */
    private String smartDataElementDescription;

    /**
     * \brief Categorical string value of the "type" of SDE this is. Ie. is it related to an order, an encounter, a note, etc.
     * Warning: Is this fixed for every instance of the same form? Or does it belong in the main SDE table?
     * (Convert to enum if we can be sure of possible values?)
     */
    private String smartDataElementContext;

    @Override
    public SmartDataElementDefinition copy() {
        return null;
    }

    /**
     * @param validUntil  the event time that invalidated the current state
     * @param storedUntil the time that star started processing the message that invalidated the current state
     * @return A new audit entity with the current state of the object.
     */
    @Override
    public SmartDataElementDefinitionAudit createAuditEntity(Instant validUntil, Instant storedUntil) {
        return null;
    }
}
