package uk.ac.ucl.rits.inform.interchange.adt;

import uk.ac.ucl.rits.inform.interchange.InterchangeValue;

import java.time.Instant;

/**
 * Ensuring admission date time is used in a class.
 * <p>
 * Only used in specific circumstances because it can be used to mean either presentation time or admission time.
 * Doesn't seem to be used consistently by all message types so only use when you're sure that it's giving the correct value.
 * A01 and A03 (admit and discharge) seem to consistently use it for Admission time
 * A04 uses it for presentation time.
 */
public interface AdmissionDateTime {
    InterchangeValue<Instant> getAdmissionDateTime();

    void setAdmissionDateTime(InterchangeValue<Instant> admissionDateTime);
}
