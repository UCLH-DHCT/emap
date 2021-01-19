package uk.ac.ucl.rits.inform.interchange.lab;

/**
 * Lab result status values.
 * @author Stef Piatek
 */
public enum LabResultStatus {
    FINAL("F"),
    CORRECTED("C"),
    PRELIMINARY("P"),
    INCOMPLETE("I"),
    RESULT_NOT_AVAILABLE("X"),
    DELETED("D"),
    INVALID_RESULT("W"),
    NONSTANDARD_VALUE("Z"),
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
