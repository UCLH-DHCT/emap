package uk.ac.ucl.rits.inform.informdb.forms;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.ac.ucl.rits.inform.informdb.TemporalCore;
import uk.ac.ucl.rits.inform.informdb.TemporalFrom;
import uk.ac.ucl.rits.inform.informdb.annotation.AuditTable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.time.Instant;

/**
 * \brief (Metadata) Junction table to represent the many-to-many relationship between form and questions.
 */
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@AuditTable
public class FormDefinitionFormQuestion extends TemporalCore<FormDefinitionFormQuestion, FormDefinitionFormQuestionAudit> {
    /**
     * \brief Unique identifier in EMAP for this record.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long formDefinitionFormQuestionId;

    @ManyToOne
    @JoinColumn
    private FormDefinition formDefinitionId;

    @ManyToOne
    @JoinColumn
    private FormQuestion formQuestionId;

    public FormDefinitionFormQuestion() {
    }

    public FormDefinitionFormQuestion(TemporalFrom temporalFrom, FormDefinition formDefinition, FormQuestion formQuestion) {
        setTemporalFrom(temporalFrom);
        this.formDefinitionId = formDefinition;
        formDefinition.getQuestions().add(this);
        this.formQuestionId = formQuestion;
    }

    private FormDefinitionFormQuestion(FormDefinitionFormQuestion other) {
        super(other);
        this.formDefinitionFormQuestionId = other.formDefinitionFormQuestionId;
        this.formDefinitionId = other.formDefinitionId;
        this.formQuestionId = other.formQuestionId;
    }

    @Override
    public FormDefinitionFormQuestion copy() {
        return new FormDefinitionFormQuestion(this);
    }

    /**
     * @param validUntil  the event time that invalidated the current state
     * @param storedUntil the time that star started processing the message that invalidated the current state
     * @return A new audit entity with the current state of the object.
     */
    @Override
    public FormDefinitionFormQuestionAudit createAuditEntity(Instant validUntil, Instant storedUntil) {
        return new FormDefinitionFormQuestionAudit(this, validUntil, storedUntil);
    }
}
