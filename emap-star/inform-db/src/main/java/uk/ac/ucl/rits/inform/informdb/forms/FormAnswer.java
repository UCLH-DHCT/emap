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
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.time.Instant;

/**
 * \brief Stores the value assigned to a particular instance of an answered form question.
 * Eg. the value of a Smart Data Element (SDE).
 */
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@AuditTable
public class FormAnswer extends TemporalCore<FormAnswer, FormAnswerAudit> {
    /**
     * \brief Unique identifier in EMAP for this instance of an SDE.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long formAnswerId;

    /**
     * \brief Metadata for this answer - ie. what was the question?
     */
    @ManyToOne
    @JoinColumn(name = "formQuestionId", nullable = false)
    private FormQuestion formQuestionId;

    /**
     * \brief The instance of a filled-in form that this answer belongs to.
     */
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "formId", nullable = false)
    private Form formId;

    /**
     * \brief A unique ID for this form answer that can be used to track back to the source system.
     * For an Epic SDE this would be the HLV ID (HLV .1)
     */
    private String internalId;

    /**
     * \brief Categorical string value of the "context" of an SDE.
     * Eg. is it related to an order, an encounter, a note, etc.
     * This value is not the same for every instance of the same SDE, hence why
     * it goes in FormAnswer and not FormQuestion.
     */
    private String context;

    /**
     * \brief Current value of the SDE - may be a multi-line string concatenated together.
     * HLV 50.
     */
    @Column(columnDefinition = "text")
    private String valueAsString;

    /**
     * \brief Current value of the SDE if it's numerical, else null.
     * HLV 50, influenced by HLV 60.
     */
    private Double valueAsNumber;

    /**
     * \brief Current value of the SDE if it's of type boolean, else null.
     * HLV 50, influenced by HLV 60.
     */
    private Boolean valueAsBoolean;

    /**
     * \brief Current value of the SDE if it's a UTC instant, else null.
     * HLV 50, influenced by HLV 60.
     */
    @Column(columnDefinition = "timestamp with time zone")
    private Instant valueAsUtcDatetime;

    public FormAnswer() {
    }

    private FormAnswer(FormAnswer other) {
        super(other);
        this.formAnswerId = other.formAnswerId;
        this.formQuestionId = other.formQuestionId;
        this.formId = other.formId;
        this.internalId = other.internalId;
        this.context = other.context;
        this.valueAsString = other.valueAsString;
        this.valueAsNumber = other.valueAsNumber;
        this.valueAsBoolean = other.valueAsBoolean;
        this.valueAsUtcDatetime = other.valueAsUtcDatetime;
    }

    @Override
    public FormAnswer copy() {
        return new FormAnswer(this);
    }

    /**
     * @param validUntil  the event time that invalidated the current state
     * @param storedUntil the time that star started processing the message that invalidated the current state
     * @return A new audit entity with the current state of the object.
     */
    @Override
    public FormAnswerAudit createAuditEntity(Instant validUntil, Instant storedUntil) {
        return new FormAnswerAudit(this, validUntil, storedUntil);
    }
}