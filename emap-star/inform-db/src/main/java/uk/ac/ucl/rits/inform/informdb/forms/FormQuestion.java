package uk.ac.ucl.rits.inform.informdb.forms;

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
import java.time.Instant;

/**
 * Represents the Question in a form (basically the metadata for a form answer).
 * It may not literally be a question, eg. "Limb", you can think of that as a prompt.
 */
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@AuditTable
public class FormQuestion extends TemporalCore<FormQuestion, FormQuestionAudit> {
    /**
     * \brief Unique identifier in EMAP.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long formQuestionId;

    /**
     * \brief String identifier from the source system of the question.
     * .
     */
    @Column(nullable = false, unique = true)
    private String formQuestionSourceId;

    /**
     * \brief Name of the concept (ie. the "Question")
     * .
     */
    private String formQuestionConceptName;

    /**
     * \brief Abbreviated concept name (ie. the "Question")
     * .
     */
    private String formQuestionConceptAbbrevName;

    /**
     * \brief String description of a question, where available
     */
    private String formQuestionDescription;

    /**
     * \brief Type of answer expected (String, number, etc)
     * Do we store this same value (an instance of ZC_DATA_TYPE) anywhere else in Star?
     * HLX 60
     */
    private String formQuestionValueType;

    /**
     * \brief Categorical string value of the "type" of SDE this is.
     * Ie. is it related to an order, an encounter, a note, etc.
     * Warning: Unknown if this is the same for every instance of the same form.
     * If not, this field will have to be moved to FormAnswer.
     * (Convert to enum if we can be sure of possible values?)
     */
    private String smartDataElementContext;

    private FormQuestion(FormQuestion other) {
        super(other);
        this.formQuestionId = other.formQuestionId;
        this.formQuestionConceptName = other.formQuestionConceptName;
        this.formQuestionSourceId = other.formQuestionSourceId;
        this.formQuestionConceptAbbrevName = other.formQuestionConceptAbbrevName;
        this.formQuestionDescription = other.formQuestionDescription;
        this.formQuestionValueType = other.formQuestionValueType;
        this.smartDataElementContext = other.smartDataElementContext;
    }

    public FormQuestion() {
    }

    @Override
    public FormQuestion copy() {
        return new FormQuestion(this);
    }

    /**
     * @param validUntil  the event time that invalidated the current state
     * @param storedUntil the time that star started processing the message that invalidated the current state
     * @return A new audit entity with the current state of the object.
     */
    @Override
    public FormQuestionAudit createAuditEntity(Instant validUntil, Instant storedUntil) {
        return new FormQuestionAudit(this, validUntil, storedUntil);
    }
}
