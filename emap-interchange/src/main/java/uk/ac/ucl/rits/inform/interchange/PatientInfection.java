package uk.ac.ucl.rits.inform.interchange;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.springframework.lang.Nullable;

import java.io.Serializable;
import java.time.Instant;

/**
 * Interchange format of a PatientInterchange message.
 * @author Stef Piatek
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public class PatientInfection extends EmapOperationMessage implements Serializable {
    private static final long serialVersionUID = 9025316128466696423L;
    private String mrn;

    /**
     * Infection name.
     */
    private String infection;

    /**
     * Line number for infection.
     * Allows for multiple infections per patient to be tracked separately
     */
    private Long line;

    /**
     * Status of infection.
     */
    private String status;

    @Nullable
    private String comment;

    /**
     * Infection added at...
     */
    private Instant infectionAdded;

    /**
     * Infection resolved at...
     */
    @Nullable
    private Instant infectionResolved;

    /**
     * Onset of infection known at...
     */
    @Nullable
    private Instant infectionOnset;

    /**
     * @return mrn
     */
    public String getMrn() {
        return mrn;
    }

    /**
     * @param mrn mrn
     */
    public void setMrn(String mrn) {
        this.mrn = mrn;
    }

    /**
     * @return {@link PatientInfection#infection}
     */
    public String getInfection() {
        return infection;
    }

    /**
     * @param infection {@link PatientInfection#infection}
     */
    public void setInfection(String infection) {
        this.infection = infection;
    }

    /**
     * @return {@link PatientInfection#line}
     */
    public Long getLine() {
        return line;
    }

    /**
     * @param line {@link PatientInfection#line}
     */
    public void setLine(Long line) {
        this.line = line;
    }

    /**
     * @return {@link PatientInfection#status}
     */
    public String getStatus() {
        return status;
    }

    /**
     * @param status {@link PatientInfection#status}
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * @return comment
     */
    public String getComment() {
        return comment;
    }

    /**
     * @param comment comment
     */
    public void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * @return {@link PatientInfection#infectionAdded}
     */
    public Instant getInfectionAdded() {
        return infectionAdded;
    }

    /**
     * @param infectionAdded {@link PatientInfection#infectionAdded}
     */
    public void setInfectionAdded(Instant infectionAdded) {
        this.infectionAdded = infectionAdded;
    }

    /**
     * @return {@link PatientInfection#infectionResolved}
     */
    public Instant getInfectionResolved() {
        return infectionResolved;
    }

    /**
     * @param infectionResolved {@link PatientInfection#infectionResolved}
     */
    public void setInfectionResolved(Instant infectionResolved) {
        this.infectionResolved = infectionResolved;
    }

    /**
     * @return {@link PatientInfection#infectionOnset}
     */
    public Instant getInfectionOnset() {
        return infectionOnset;
    }

    /**
     * @param infectionOnset {@link PatientInfection#infectionOnset}
     */
    public void setInfectionOnset(Instant infectionOnset) {
        this.infectionOnset = infectionOnset;
    }

    /**
     * Call back to the processor so it knows what type this object is (ie. double dispatch).
     * @param processor the processor to call back to
     * @throws EmapOperationMessageProcessingException if message cannot be processed
     */
    @Override
    public void processMessage(EmapOperationMessageProcessor processor) throws EmapOperationMessageProcessingException {
        processor.processMessage(this);
    }

    @Override
    public String toString() {
        return new StringBuilder().append("PatientInfection(")
                .append("mrn='").append(mrn).append('\'')
                .append(", infection='").append(infection).append('\'')
                .append(", line=").append(line)
                .append(", status='").append(status).append('\'')
                .append(", comment='").append(comment).append('\'')
                .append(", infectionAdded=").append(infectionAdded)
                .append(", infectionResolved=").append(infectionResolved)
                .append(", infectionOnset=").append(infectionOnset)
                .append(", sourceSystem=").append(getSourceSystem())
                .append(')').toString();
    }
}
