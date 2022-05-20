package uk.ac.ucl.rits.inform.informdb.smart_data_elements;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.Check;
import uk.ac.ucl.rits.inform.informdb.TemporalCore;
import uk.ac.ucl.rits.inform.informdb.annotation.AuditTable;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.informdb.labs.LabOrder;

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
@Check(constraints =
        " hospital_visit_id is not null " +
                " OR lab_order_id is not null " +
                " OR mrn_id is not null ")
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

    /**
     * \brief The Mrn this form relates to, or null if it doesn't relate to one.
     */
    @ManyToOne
    @JoinColumn(name = "mrnId")
    private Mrn mrnId;

    /**
     * \brief The hospital visit this form relates to, or null if it doesn't relate to one.
     */
    @ManyToOne
    @JoinColumn(name = "hospitalVisitId")
    private HospitalVisit hospitalVisitId;

    /**
     * \brief The lab order this form relates to, or null if it doesn't relate to one.
     * I'm not currently sure if SmartForms can be attached to Lab Orders.
     */
    @ManyToOne
    @JoinColumn(name = "labOrderId")
    private LabOrder labOrderId;

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