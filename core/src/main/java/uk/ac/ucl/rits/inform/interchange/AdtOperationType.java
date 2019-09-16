package uk.ac.ucl.rits.inform.interchange;

/**
 * The ADT operations possible in our interchange format.
 * These do (at least currently) map quite closely to HL7 trigger events.
 *
 * @author Jeremy Stein
 */
public enum AdtOperationType {
    /**
     * Inpatient or emergency admission.
     */
    ADMIT_PATIENT,
    /**
     * Transfer a patient to a different location.
     */
    TRANSFER_PATIENT,
    /**
     * Discharge a patient.
     */
    DISCHARGE_PATIENT,
    /**
     * Change patient demographics/other info.
     */
    UPDATE_PATIENT_INFO,
    /**
     * Discharge decision was reversed or entered into the system in error.
     */
    CANCEL_DISCHARGE_PATIENT,
    /**
     * Merge the entire record of two patients.
     */
    MERGE_BY_ID,
}
