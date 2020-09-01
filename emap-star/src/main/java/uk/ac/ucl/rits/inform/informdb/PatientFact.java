package uk.ac.ucl.rits.inform.informdb;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * This represents a grouper for a fact about a patient.
 *
 * @author UCL RITS
 *
 */
@Entity
@JsonIgnoreProperties({"encounter", "valid", "childFacts", "childFactsAsMap", "parentFact"})
@Table(indexes = {
        @Index(name = "patient_fact_parent_fact_index", columnList = "parent_fact", unique = false),
        @Index(name = "patient_fact_encounter_index", columnList = "encounter", unique = false),
        @Index(columnList = "storedFrom", unique = false),
        @Index(columnList = "storedUntil", unique = false),
})
public class PatientFact extends Fact<PatientFact, PatientProperty> implements Serializable {
    /**
     */
    public PatientFact() {
    }

    /**
     * Copy constructor.
     * @param other object to copy from
     */
    public PatientFact(PatientFact other) {
        super(other);
        encounter = other.encounter;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PatientFact invalidateFact(Instant storedFromUntil, Instant invalidationDate) {
        PatientFact newFact = new PatientFact(this);
        this.setStoredUntil(storedFromUntil);
        newFact.setStoredFrom(storedFromUntil);
        newFact.setValidUntil(invalidationDate);
        this.getEncounter().addFact(newFact);
        return newFact;
    }

    private static final long serialVersionUID = -5867434510066589366L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long               patientFactId;

    @ManyToOne
    @JoinColumn(name = "encounter")
    private Encounter         encounter;

    @Override
    public Long getFactId() {
        return patientFactId;
    }

    @Override
    public void setFactId(Long patientFactId) {
        this.patientFactId = patientFactId;
    }

    /**
     * @return the encounter
     */
    public Encounter getEncounter() {
        return encounter;
    }

    /**
     * @param encounter the encounter to set
     */
    public void setEncounter(Encounter encounter) {
        this.encounter = encounter;
    }

    @Override
    public void addProperty(PatientProperty prop) {
        super.addProperty(prop, this);
    }

    @Override
    public void addChildFact(PatientFact fact) {
        childFacts.add(fact);
        fact.setParentFact(this);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(" factid = " + getFactId() + " ");
        sb.append(getValidUntil() == null ? " VALID " : " INVALID ");
        sb.append(getStoredUntil() == null ? " STORED " : " DELETED ");
        sb.append(this.getFactType().getShortName());
        sb.append("[id=" + getFactType().getAttributeId() + "]");
        sb.append(" --- ");
        PatientFact parentFact = getParentFact();
        String factId;
        if (parentFact == null) {
            factId = "[no parent]";
        } else if (parentFact.getFactId() == null) {
            factId = "null";
        } else {
            factId = parentFact.getFactId().toString();
        }
        sb.append(" parent factid = " + factId + " ");
        sb.append(" --- ");
        for (PatientProperty p : getProperties()) {
            sb.append("  " + p.toString() + ",  ");
        }
        return sb.toString();
    }

    /**
     * Smart equals method that is specific to a path test result. To be used for
     * determining if the existing value in the database should be overwritten with
     * the new value.
     *
     * @param other the other test result to compare
     * @return true iff they're equal
     * @throws IllegalArgumentException if either or both are not of type AttributeKeyMap.PATHOLOGY_TEST_RESULT
     */
    public boolean equalsPathologyResult(PatientFact other) {
        if (!this.isOfType(AttributeKeyMap.PATHOLOGY_TEST_RESULT)
                || !other.isOfType(AttributeKeyMap.PATHOLOGY_TEST_RESULT)) {
            throw new IllegalArgumentException(
                    String.format("Both facts must be of type PATHOLOGY_TEST_RESULT. Found: %s, %s", this.getFactType(),
                            other.getFactType()));
        }

        /**
         * The result time is updated with every new message even if nothing has
         * changed, so we must ignore it when testing for equality.
         */
        List<AttributeKeyMap> attributesToIgnore = new ArrayList<>();
        attributesToIgnore.add(AttributeKeyMap.PATHOLOGY_RESULT_TIME);
        return equalsIgnoringProperties(other, attributesToIgnore);
    }

}
