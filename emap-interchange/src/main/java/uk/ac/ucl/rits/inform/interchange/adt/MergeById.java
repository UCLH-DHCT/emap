package uk.ac.ucl.rits.inform.interchange.adt;

/**
 * Merge the entire record of two patients.
 * HL7 messages: A40
 */
public class MergeById extends AdtMessageBase {
    private static final long serialVersionUID = -2500473433999508161L;

    private String mergedPatientId;

    /**
     * @return the mergedPatientId
     */
    public String getMergedPatientId() {
        return mergedPatientId;
    }

    /**
     * @param mergedPatientId the mergedPatientId to set
     */
    public void setMergedPatientId(String mergedPatientId) {
        this.mergedPatientId = mergedPatientId;
    }


}
