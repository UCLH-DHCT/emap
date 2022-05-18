package uk.ac.ucl.rits.inform.informdb.smart_data_elements;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.ac.ucl.rits.inform.informdb.TemporalCore;
import uk.ac.ucl.rits.inform.informdb.annotation.AuditTable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.time.Instant;

/**
 * Stores the value assigned to a particular instance of a Smart Data Element (SDE).
 */
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@AuditTable
public class SmartDataElement extends TemporalCore<SmartDataElement, SmartDataElementAudit> {
    /**
     * \brief Unique identifier in EMAP for this instance of an SDE.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long smartDataElementId;

    @ManyToOne
    @JoinColumn(name = "smartDataElementDefinitionId")
    private SmartDataElementDefinition smartDataElementDefinitionId;

    @ManyToOne
    @JoinColumn(name = "smartFormId")
    private SmartForm smartFormId;

    /**
     * \brief Current value of the SDE - may be a multi-line string concatenated together.
     * HLV 50.
     */
    @Column(columnDefinition = "text")
    private String smartDataElementValue;

    // "context" field is currently in the metadata table, it may have to be moved here though.


    @Override
    public SmartDataElement copy() {
        return null;
    }

    /**
     * @param validUntil  the event time that invalidated the current state
     * @param storedUntil the time that star started processing the message that invalidated the current state
     * @return A new audit entity with the current state of the object.
     */
    @Override
    public SmartDataElementAudit createAuditEntity(Instant validUntil, Instant storedUntil) {
        return null;
    }
}