package uk.ac.ucl.rits.inform.interchange.lab;

/**
 * Lab result status values.
 * @author Stef Piatek
 */
public enum LabResultStatus {
    /**
     * final.
     */
    FINAL("F"),
    /**
     * corrected.
     */
    CORRECTED("C"),
    /**
     * preiminary.
     */
    PRELIMINARY("P"),
    /**
     * incomplete.
     */
    INCOMPLETE("I"),
    /**
     * not available.
     */
    RESULT_NOT_AVAILABLE("X"),
    /**
     * deleted.
     */
    DELETED("D"),
    /**
     * invalid.
     */
    INVALID_RESULT("W"),
    /**
     * non standard.
     */
    NONSTANDARD_VALUE("Z"),
    /**
     * unknown.
     */
    UNKNOWN("");

    final String labCode;

    LabResultStatus(String labCode) {
        this.labCode = labCode;
    }

    public static LabResultStatus findByHl7Code(String labCode) {
        for (LabResultStatus labResultStatus : values()) {
            if (labResultStatus.labCode.equals(labCode)) {
                return labResultStatus;
            }
        }
        throw new IllegalArgumentException(String.format("Patient Class '%s' is not a known type", labCode));
    }

}
