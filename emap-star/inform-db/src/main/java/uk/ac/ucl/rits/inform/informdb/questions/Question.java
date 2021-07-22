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
 * Questions that can be attached to several data types, such as lab samples or consultation requests. Independent of
 * which type of question, these are all held together in one table and reference back to the entity they relate too.
 *
 * @author Stef Piatek
 * @author Anika Cawthorn
 */
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@AuditTable
public class Question extends TemporalCore<Question, QuestionAudit> {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long questionId;

    @Column(columnDefinition = "text", nullable = false)
    private String question;

    public Question() {}

    /**
     * Minimal question constructor that requires that requires the question as such and the timestamps for when EMAP
     * started processing this data type and from which time point the question information is valid from.
     * @param question      The actual question string linked to a data type
     * @param validFrom     Timestamp from which information valid from
     * @param storedFrom    When EMAP started processing this data type
     */
    public Question(String question, Instant validFrom, Instant storedFrom) {
        this.question = question;
        setStoredFrom(storedFrom);
        setValidFrom(validFrom);
    }

    public Question(Question other) {
        super(other);
        this.questionId = other.questionId;
        this.question = other.question;
    }

    @Override
    public Question copy() {
        return new Question(this);
    }

    @Override
    public QuestionAudit createAuditEntity(Instant validUntil, Instant storedUntil) {
        return new QuestionAudit(this, validUntil, storedUntil);
    }
}
