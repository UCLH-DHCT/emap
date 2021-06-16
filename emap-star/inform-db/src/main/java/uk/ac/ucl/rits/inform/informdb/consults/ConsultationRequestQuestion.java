package uk.ac.ucl.rits.inform.informdb.consults;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.ac.ucl.rits.inform.informdb.Question;
import uk.ac.ucl.rits.inform.informdb.TemporalCore;
import uk.ac.ucl.rits.inform.informdb.annotation.AuditTable;
import uk.ac.ucl.rits.inform.informdb.consults.ConsultationRequest;
import uk.ac.ucl.rits.inform.informdb.consults.ConsultationRequestQuestionAudit;

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
 * Questions and answers for consultation requests.
 * @author Anika Cawthorn
 */
@SuppressWarnings("serial")
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@AuditTable
//@Table(indexes = {@Index(name = "lsq_lab_sample_id", columnList = "labSampleId"),
//        @Index(name = "lsq_question_id", columnList = "questionId")})
public class ConsultationRequestQuestion extends TemporalCore<uk.ac.ucl.rits.inform.informdb.consults.ConsultationRequestQuestion, ConsultationRequestQuestionAudit> {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long labSampleQuestionId;

    @ManyToOne
    @JoinColumn(name = "labSampleId", nullable = false)
    private ConsultationRequest consultationRequestId;

    @ManyToOne
    @JoinColumn(name = "questionId", nullable = false)
    private Question questionId;

    @Column(columnDefinition = "text")
    private String answer;

    public ConsultationRequestQuestion () {}

    public ConsultationRequestQuestion(ConsultationRequest consultationRequestId, Question questionId, Instant validFrom, Instant storedFrom) {
        this.consultationRequestId = consultationRequestId;
        this.questionId = questionId;
        setStoredFrom(storedFrom);
        setValidFrom(validFrom);
    }

    public ConsultationRequestQuestion(uk.ac.ucl.rits.inform.informdb.consults.ConsultationRequestQuestion other) {
        super(other);
        this.consultationRequestId = other.consultationRequestId;
        this.questionId = other.questionId;
        this.answer = other.answer;
    }

    @Override
    public uk.ac.ucl.rits.inform.informdb.consults.ConsultationRequestQuestion copy() {
        return new uk.ac.ucl.rits.inform.informdb.consults.ConsultationRequestQuestion(this);
    }

    @Override
    public ConsultationRequestQuestionAudit createAuditEntity(Instant validUntil, Instant storedUntil) {
        return new ConsultationRequestQuestionAudit(this, validUntil, storedUntil);
    }
}
