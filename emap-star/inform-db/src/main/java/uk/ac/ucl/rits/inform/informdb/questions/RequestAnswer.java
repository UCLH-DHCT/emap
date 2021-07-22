package uk.ac.ucl.rits.inform.informdb.questions;

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
 * Questions generated from specific data types (e.g. lab samples or consultation requests) trigger answers that or
 * held as RequestAnswers. RequestAnswers therefore not only hold the answer as such but also link the answer to the
 * question and the entity (i.e. data type) that triggered the question.
 * @author Anika Cawthorn
*/
@SuppressWarnings("serial")
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

    public RequestAnswer() {}

    /**
     * Minimal request answer constructor that requires that the answer and the timestamps for when EMAP
     * started processing this data type and from which time point the question information is valid from.
     * @param answer        The actual question string linked to a data type
     * @param validFrom     Timestamp from which information valid from
     * @param storedFrom    When EMAP started processing this data type
     */
    public RequestAnswer(String answer, Instant validFrom, Instant storedFrom) {
        this.answer = answer;
        setStoredFrom(storedFrom);
        setValidFrom(validFrom);
    }

    public RequestAnswer(RequestAnswer other) {
        super(other);
        this.answerId = other.answerId;
        this.answer = other.answer;
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
