package uk.ac.ucl.rits.inform.informdb;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
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
public class Question  extends TemporalCore<Question, QuestionAudit> {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long questionId;

    @Column(nullable = false)
    private String parentTableType;

    @Column(nullable = false) // that probably wants to be a join column
    private long parentTableId;

    @Column(columnDefinition = "text", nullable = false)
    private String question;

    @Column(columnDefinition = "text")
    private String answer;

    public Question() {}

    /**
     * Minimal question constructor that requires all the mandatory fields for a question.
     * @param parentTableType   Data type this questions belongs to (e.g. lab sample or consultation request)
     * @param parentTableId     Identifier for table where this particular question is referenced
     * @param validFrom
     * @param storedFrom        When EMAP started processing this data type
     */
    public Question(String parentTableType, long parentTableId, Instant validFrom, Instant storedFrom) {
        this.parentTableType = parentTableType;
        this.parentTableId = parentTableId;
        setStoredFrom(storedFrom);
        setValidFrom(validFrom);
    }

    public Question(Question other) {
        super(other);
        this.questionId = other.questionId;
        this.parentTableType = other.getParentTableType();
        this.parentTableId = other.parentTableId;
        this.answer = other.answer;
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
