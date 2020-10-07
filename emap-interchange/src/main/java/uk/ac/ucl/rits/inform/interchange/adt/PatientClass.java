package uk.ac.ucl.rits.inform.interchange.adt;

/**
 * HL7 Patient class values, taken from HL7 Table 0004.
 */
public enum PatientClass {
    /**
     * Day case.
     */
    DAY_CASE("DAY CASE"),

    /**
     * Emergency.
     */
    EMERGENCY("E"),

    /**
     * Inpatient.
     */
    INPATIENT("I"),

    /**
     * Not applicable.
     */
    NOT_APPLICABLE("N"),

    /**
     * New Born in EPIC, obstetrics in HL7.
     */
    NEW_BORN("B"),

    /**
     * Surgery admit.
     */
    SURGICAL_ADMISSION("SURG ADMIT"),

    /**
     * Pre-admit.
     */
    PRE_ADMIT("P"),

    /**
     * Outpatient.
     */
    OUTPATIENT("O");

    final String hl7Code;

    PatientClass(String hl7Code) {
        this.hl7Code = hl7Code;
    }

    public static PatientClass findByHl7Code(String hl7Code) {
        for (PatientClass patientClass : values()) {
            if (patientClass.hl7Code.equals(hl7Code)) {
                return patientClass;
            }
        }
        throw new IllegalArgumentException(String.format("Patient Class %s is not a known type", hl7Code));
    }

}
