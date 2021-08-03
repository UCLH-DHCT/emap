package uk.ac.ucl.rits.inform.informdb.labs;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.ac.ucl.rits.inform.informdb.Question;
import uk.ac.ucl.rits.inform.informdb.TemporalCore;
import uk.ac.ucl.rits.inform.informdb.annotation.AuditTable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.Instant;

/**
 * Questions and answers for a lab order.
 * @author Stef Piatek
 */
@SuppressWarnings("serial")
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@AuditTable
@Table(indexes = {@Index(name = "lsq_lab_sample_id", columnList = "labSampleId"),
        @Index(name = "lsq_question_id", columnList = "questionId")})
public class LabSampleQuestion extends TemporalCore<LabSampleQuestion, LabSampleQuestionAudit> {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long labSampleQuestionId;

    @ManyToOne
    @JoinColumn(name = "labSampleId", nullable = false)
    private LabSample labSampleId;

    @ManyToOne
    @JoinColumn(name = "questionId", nullable = false)
    private Question questionId;

    @Column(columnDefinition = "text")
    private String answer;

    public LabSampleQuestion() {}

    /**
     * Create valid LabSampleQuestion.
     * @param labSampleId parent LabSample
     * @param questionId  matching Question
     * @param validFrom   time that the message was valid from
     * @param storedFrom  time that emap core stared processing the message
     */
    public LabSampleQuestion(LabSample labSampleId, Question questionId, Instant validFrom, Instant storedFrom) {
        this.labSampleId = labSampleId;
        this.questionId = questionId;
        setStoredFrom(storedFrom);
        setValidFrom(validFrom);
    }

    private LabSampleQuestion(LabSampleQuestion other) {
        super(other);
        this.labSampleQuestionId = other.labSampleQuestionId;
        this.labSampleId = other.labSampleId;
        this.questionId = other.questionId;
        this.answer = other.answer;
    }


    @Override
    public LabSampleQuestion copy() {
        return new LabSampleQuestion(this);
    }

    @Override
    public LabSampleQuestionAudit createAuditEntity(Instant validUntil, Instant storedUntil) {
        return new LabSampleQuestionAudit(this, validUntil, storedUntil);
    }
}
