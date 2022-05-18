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
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.time.Instant;

/**
 * A filled out SmartForm. Basically a grouping of rows of SmartDataElement.
 */
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@AuditTable
public class SmartForm extends TemporalCore<SmartForm, SmartFormAudit> {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long smartFormId;

    /**
     * \brief The form definition of this form instance.
     */
    @ManyToOne
    @JoinColumn(name = "smartFormDefinitionId")
    private SmartFormDefinition smartFormDefinitionId;

    private Instant formFilingDateTime;

    private String formFilingUserId;

    @Override
    public SmartForm copy() {
        return null;
    }

    /**
     * @param validUntil  the event time that invalidated the current state
     * @param storedUntil the time that star started processing the message that invalidated the current state
     * @return A new audit entity with the current state of the object.
     */
    @Override
    public SmartFormAudit createAuditEntity(Instant validUntil, Instant storedUntil) {
        return null;
    }
}