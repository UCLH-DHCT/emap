package uk.ac.ucl.rits.inform.interchange.adt;

import uk.ac.ucl.rits.inform.interchange.Hl7Value;

import java.time.Instant;

/**
 * Ensuring admission date time is used in a class.
 */
public interface AdmissionDateTime {
    Hl7Value<Instant> getAdmissionDateTime();

    void setAdmissionDateTime(Hl7Value<Instant> admissionDateTime);
}
