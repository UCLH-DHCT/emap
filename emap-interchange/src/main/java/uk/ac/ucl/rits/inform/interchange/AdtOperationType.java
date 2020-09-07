package uk.ac.ucl.rits.inform.interchange;

/**
 * The ADT operations possible in our interchange format.
 * These do (at least currently) map quite closely to HL7 trigger events.
 * TODO: Remove this once adt subclasses for each of these operations are created and implemented in emap core.
 * @author Jeremy Stein
 */
public enum AdtOperationType {
    /**
     * Inpatient, outpatient or emergency admission.
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
     * Decision to admit patient was reversed or was entered into the system in error.
     */
    CANCEL_ADMIT_PATIENT,
    /**
     * Decision to transfer patient was reversed or was entered into the system in error.
     */
    CANCEL_TRANSFER_PATIENT,
    /**
     * Decision to discharge patient was reversed or was entered into the system in error.
     */
    CANCEL_DISCHARGE_PATIENT,
    /**
     * Merge the entire record of two patients.
     */
    MERGE_BY_ID,
}
