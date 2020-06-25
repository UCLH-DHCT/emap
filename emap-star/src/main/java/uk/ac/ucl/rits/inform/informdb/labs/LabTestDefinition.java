package uk.ac.ucl.rits.inform.informdb.labs;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import uk.ac.ucl.rits.inform.informdb.TemporalCore;

/**
 * This represents the definition of a single lab test by a single provider. An
 * individual test may feature more than once in this table, where there is more
 * than one lab provider that supplies it.
 *
 * @author Roma Klapaukh
 *
 */
@Entity
public class LabTestDefinition extends TemporalCore<LabTestDefinition> {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long   labTestDefinitionId;

    private long   labTestDefinitionDurableId;

    /**
     * What system this code belongs to. Examples could be WinPath, or Epic.
     */
    @Column(nullable = false)
    private String labProvider;

    /**
     * The department within the lab responsible for the test.
     */
    private String labDepartment;

    /**
     * The code for this test as reported by the lab.
     */
    @Column(nullable = false)
    private String testLabCode;

    /**
     * The code for this test in a standardised vocabulary.
     */
    private String testStandardCode;

    /**
     * Where a standardised code is provided, this says which vocabulary was used.
     */
    private String standardisedVocabulary;

    public LabTestDefinition() {}

    public LabTestDefinition(LabTestDefinition other) {
        super(other);
        this.labTestDefinitionDurableId = other.labTestDefinitionDurableId;
        this.labProvider = other.labProvider;
        this.labDepartment = other.labDepartment;
        this.testLabCode = other.testLabCode;
        this.testStandardCode = other.testStandardCode;
        this.standardisedVocabulary = other.standardisedVocabulary;
    }

    /**
     * @return the labTestDefinitionId
     */
    public long getLabTestDefinitionId() {
        return labTestDefinitionId;
    }

    /**
     * @param labTestDefinitionId the labTestDefinitionId to set
     */
    public void setLabTestDefinitionId(long labTestDefinitionId) {
        this.labTestDefinitionId = labTestDefinitionId;
    }

    /**
     * @return the labTestDefinitionDurableId
     */
    public long getLabTestDefinitionDurableId() {
        return labTestDefinitionDurableId;
    }

    /**
     * @param labTestDefinitionDurableId the labTestDefinitionDurableId to set
     */
    public void setLabTestDefinitionDurableId(long labTestDefinitionDurableId) {
        this.labTestDefinitionDurableId = labTestDefinitionDurableId;
    }

    /**
     * @return the labProvider
     */
    public String getLabProvider() {
        return labProvider;
    }

    /**
     * @param labProvider the labProvider to set
     */
    public void setLabProvider(String labProvider) {
        this.labProvider = labProvider;
    }

    /**
     * @return the labDepartment
     */
    public String getLabDepartment() {
        return labDepartment;
    }

    /**
     * @param labDepartment the labDepartment to set
     */
    public void setLabDepartment(String labDepartment) {
        this.labDepartment = labDepartment;
    }

    /**
     * @return the testLabCode
     */
    public String getTestLabCode() {
        return testLabCode;
    }

    /**
     * @param testLabCode the testLabCode to set
     */
    public void setTestLabCode(String testLabCode) {
        this.testLabCode = testLabCode;
    }

    /**
     * @return the testStandardCode
     */
    public String getTestStandardCode() {
        return testStandardCode;
    }

    /**
     * @param testStandardCode the testStandardCode to set
     */
    public void setTestStandardCode(String testStandardCode) {
        this.testStandardCode = testStandardCode;
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

    @Override
    public LabTestDefinition copy() {
        return new LabTestDefinition(this);
    }

}
