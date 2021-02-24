package uk.ac.ucl.rits.inform.informdb.labs;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.ac.ucl.rits.inform.informdb.TemporalCore;
import uk.ac.ucl.rits.inform.informdb.annotation.AuditTable;
import uk.ac.ucl.rits.inform.informdb.Question;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
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
@Table()
public class LabOrderQuestion extends TemporalCore<LabOrderQuestion, LabOrderQuestionAudit> {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long labOrderQuestionId;

    @ManyToOne
    @JoinColumn(name = "labOrderId", nullable = false)
    private LabOrder labOrderId;

    @ManyToOne
    @JoinColumn(name = "questionId", nullable = false)
    private Question questionId;

    @Column(columnDefinition = "text")
    private String answer;

    public LabOrderQuestion() {}

    public LabOrderQuestion(LabOrder labOrderId, Question questionId) {
        this.labOrderId = labOrderId;
        this.questionId = questionId;
    }

    public LabOrderQuestion(LabOrderQuestion other) {
        super(other);
        this.labOrderId = other.labOrderId;
        this.questionId = other.questionId;
        this.answer = other.answer;
    }

    @Override
    public LabOrderQuestion copy() {
        return new LabOrderQuestion(this);
    }

    @Override
    public LabOrderQuestionAudit createAuditEntity(Instant validUntil, Instant storedUntil) {
        return new LabOrderQuestionAudit(this, validUntil, storedUntil);
    }
}
