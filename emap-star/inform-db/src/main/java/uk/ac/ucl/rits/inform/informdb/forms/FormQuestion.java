package uk.ac.ucl.rits.inform.informdb.forms;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.ac.ucl.rits.inform.informdb.TemporalCore;
import uk.ac.ucl.rits.inform.informdb.TemporalFrom;
import uk.ac.ucl.rits.inform.informdb.annotation.AuditTable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.time.Instant;

/**
 * \brief Represents the Question in a form (basically the metadata for a form answer).
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
     * Eg. Epic it would be the Smart Data Element ID (HLV 40).
     */
    @Column(nullable = false, unique = true)
    private String internalId;

    /**
     * \brief Smart Data Element name (ie. the "Question")
     * HLX .2
     */
    private String conceptName;

    /**
     * \brief Smart Data Element abbreviated name (ie. the "Question")
     * HLX 50
     */
    private String conceptAbbrevName;

    /**
     * \brief String description of a question, where available
     */
    private String description;

    /**
     * \brief The data type of the answer, as described by the source system (String, number, categorical, etc)
     * For Epic, this is a value from ZC_DATA_TYPE (found in HLX 60).
     */
    private String internalValueType;

    private FormQuestion(FormQuestion other) {
        super(other);
        this.formQuestionId = other.formQuestionId;
        this.conceptName = other.conceptName;
        this.internalId = other.internalId;
        this.conceptAbbrevName = other.conceptAbbrevName;
        this.description = other.description;
        this.internalValueType = other.internalValueType;
    }

    public FormQuestion() {
    }

    public FormQuestion(TemporalFrom temporalFrom, String formQuestionId) {
        setTemporalFrom(temporalFrom);
        this.internalId = formQuestionId;
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
