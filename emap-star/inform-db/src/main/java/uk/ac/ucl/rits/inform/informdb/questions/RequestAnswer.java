package uk.ac.ucl.rits.inform.informdb.questions;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.ac.ucl.rits.inform.informdb.TemporalCore;
import uk.ac.ucl.rits.inform.informdb.annotation.AuditTable;
import uk.ac.ucl.rits.inform.informdb.conditions.ConditionType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.time.Instant;

/**
 * Questions generated from specific data types (e.g. lab samples or consultation requests) trigger answers that or
 * held as RequestAnswers. RequestAnswers therefore not only hold the answer as such but also link the answer to the
 * question and the entity (i.e. data type) that triggered the question.
 * @author Anika Cawthorn
*/
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@AuditTable
public class RequestAnswer extends TemporalCore<RequestAnswer, RequestAnswerAudit> {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long answerId;

    @Column(columnDefinition = "text")
    private String answer;

    @ManyToOne
    @JoinColumn(name = "questionId", nullable = false)
    private Question questionId;

    @Column(nullable = false)
    private long parentId;

    public RequestAnswer() {}

    /**
     * Minimal request answer constructor that requires that the answer and the timestamps for when EMAP
     * started processing this data type and from which time point the question information is valid from.
     * @param questionId    Question this answer relates to
     * @param answer        The actual question string linked to a data type
     * @param parentId      Entity (e.g. lab sample or consultation request) that triggered the
     * @param validFrom     Timestamp from which information valid from
     * @param storedFrom    When EMAP started processing this data type
     */
    public RequestAnswer(Question questionId, String answer, long parentId, Instant validFrom, Instant storedFrom) {
        this.answer = answer;
        this.questionId = questionId;
        this.parentId = parentId;
        setStoredFrom(storedFrom);
        setValidFrom(validFrom);
    }

    public RequestAnswer(RequestAnswer other) {
        super(other);
        this.answerId = other.answerId;
        this.answer = other.answer;
        this.questionId = other.questionId;
        this.parentId = other.parentId;
    }

    @Override
    public RequestAnswer copy() {
        return new RequestAnswer(this);
    }

    @Override
    public RequestAnswerAudit createAuditEntity(Instant validUntil, Instant storedUntil) {
        return new RequestAnswerAudit(this, validUntil, storedUntil);
    }
}
