package uk.ac.ucl.rits.inform.interchange.adt;

/**
 * HL7 Patient class values, taken from HL7 Table 0004.
 */
public enum PatientClass {
    /**
     * Emergency.
     */
    E,

    /**
     * Inpatient.
     */
    I,

    /**
     * Outpatient.
     */
    O,

    /**
     * Pre-admit.
     */
    P,

    /**
     * Recurring Patient.
     */
    R,

    /**
     * Obstetrics.
     */
    B,

    /**
     * Day Hospital.
     */
    D,

    /**
     * Week Hospital.
     */
    W,

    /**
     * Psychiatric.
     */
    S,

    /**
     * Newborn.
     */
    K
}
