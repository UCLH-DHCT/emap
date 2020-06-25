package uk.ac.ucl.rits.inform.informdb.visit_recordings;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/**
 * VisitObservationType describes the meaning behind a specific observations. In
 * EHR systems these are often coded either with potentially ambiguous short
 * names, or sometimes just numbers. This table maps these system level terms
 * into standardised vocabularies to make their meanings clear.
 *
 * @author Roma Klapaukh
 *
 */
@Entity
public class VisitObservationType {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long   visitObservationType;

    @Column(nullable = false)
    private String sourceSystem;

    @Column(nullable = false)
    private String sourceSystemCode;
    private String sourceSystemName;
    private String standardisedCode;
    private String standardisedVocabulary;
    private String primaryDataType;

    public VisitObservationType() {}

    /**
     * @return the visitObservationType
     */
    public long getVisitObservationType() {
        return visitObservationType;
    }

    /**
     * @param visitObservationType the visitObservationType to set
     */
    public void setVisitObservationType(long visitObservationType) {
        this.visitObservationType = visitObservationType;
    }

    /**
     * @return the sourceSystem
     */
    public String getSourceSystem() {
        return sourceSystem;
    }

    /**
     * @param sourceSystem the sourceSystem to set
     */
    public void setSourceSystem(String sourceSystem) {
        this.sourceSystem = sourceSystem;
    }

    /**
     * @return the sourceSystemCode
     */
    public String getSourceSystemCode() {
        return sourceSystemCode;
    }

    /**
     * @param sourceSystemCode the sourceSystemCode to set
     */
    public void setSourceSystemCode(String sourceSystemCode) {
        this.sourceSystemCode = sourceSystemCode;
    }

    /**
     * @return the standardisedCode
     */
    public String getStandardisedCode() {
        return standardisedCode;
    }

    /**
     * @param standardisedCode the standardisedCode to set
     */
    public void setStandardisedCode(String standardisedCode) {
        this.standardisedCode = standardisedCode;
    }

    /**
     * @return the standardisedVocabulary
     */
    public String getStandardisedVocabulary() {
        return standardisedVocabulary;
    }

    /**
     * @param standardisedVocabulary the standardisedVocabulary to set
     */
    public void setStandardisedVocabulary(String standardisedVocabulary) {
        this.standardisedVocabulary = standardisedVocabulary;
    }

    /**
     * @return the sourceSystemName
     */
    public String getSourceSystemName() {
        return sourceSystemName;
    }

    /**
     * @param sourceSystemName the sourceSystemName to set
     */
    public void setSourceSystemName(String sourceSystemName) {
        this.sourceSystemName = sourceSystemName;
    }

    /**
     * @return the primaryDataType
     */
    public String getPrimaryDataType() {
        return primaryDataType;
    }

    /**
     * @param primaryDataType the primaryDataType to set
     */
    public void setPrimaryDataType(String primaryDataType) {
        this.primaryDataType = primaryDataType;
    }

}
