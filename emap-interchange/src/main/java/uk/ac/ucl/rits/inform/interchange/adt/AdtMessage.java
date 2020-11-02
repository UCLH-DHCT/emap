package uk.ac.ucl.rits.inform.interchange.adt;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessage;
import uk.ac.ucl.rits.inform.interchange.Hl7Value;

import java.time.Instant;

/**
 * An interchange message describing patient movements or info. Closely corresponds
 * to the HL7 ADT message type.
 * @author Jeremy Stein
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public abstract class AdtMessage extends EmapOperationMessage {
    private static final long serialVersionUID = 804256024384466435L;
    private Instant recordedDateTime;
    private String eventReasonCode;
    private Instant eventOccurredDateTime;
    private String mrn;
    private String nhsNumber;
    private String visitNumber;
    private Hl7Value<String> modeOfArrival = Hl7Value.unknown();
    private Hl7Value<String> ethnicGroup = Hl7Value.unknown();
    private Hl7Value<String> fullLocationString = Hl7Value.unknown();
    private Hl7Value<Instant> patientBirthDate = Hl7Value.unknown();
    private Hl7Value<PatientClass> patientClass = Hl7Value.unknown();
    private Hl7Value<Instant> patientDeathDateTime = Hl7Value.unknown();
    private Hl7Value<Boolean> patientIsAlive = Hl7Value.unknown();

    private Hl7Value<String> patientFamilyName = Hl7Value.unknown();
    private Hl7Value<String> patientGivenName = Hl7Value.unknown();
    private Hl7Value<String> patientMiddleName = Hl7Value.unknown();
    private Hl7Value<String> patientReligion = Hl7Value.unknown();
    private Hl7Value<String> patientSex = Hl7Value.unknown();
    private Hl7Value<String> patientTitle = Hl7Value.unknown();
    private Hl7Value<String> patientZipOrPostalCode = Hl7Value.unknown();


    /**
     * Ideally the time the event occurred, but uses the message date time as a backup.
     * If no event occurred, the message date time can be days delayed, but this is the best we can do with limited information.
     * @return valid from instant.
     */
    public Instant bestGuessAtValidFrom() {
        return (eventOccurredDateTime == null) ? recordedDateTime : eventOccurredDateTime;
    }
}
