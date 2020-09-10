package uk.ac.ucl.rits.inform.interchange.adt;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.io.Serializable;
import java.time.Instant;
import lombok.Data;
import lombok.EqualsAndHashCode;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessage;

/**
 * An interchange message describing patient movements or info. Closely corresponds
 * to the HL7 ADT message type.
 * @author Jeremy Stein
 */
@Data
@EqualsAndHashCode(callSuper = false)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public abstract class AdtMessage extends EmapOperationMessage implements Serializable, AdtMessageInterface {
    private static final long serialVersionUID = 804256024384466435L;
    private Instant recordedDateTime;
    private String eventReasonCode;
    private Instant eventOccurredDateTime;
    private String operatorId;
    private Instant admissionDateTime;
    private String admitSource;
    private String currentBed;
    private String currentRoomCode;
    private String currentWardCode;
    private String ethnicGroup;
    private String fullLocationString;
    private String hospitalService;
    private String mrn;
    private String nhsNumber;
    private Instant patientBirthDate;
    private String patientClass;
    private Instant patientDeathDateTime;
    private Boolean patientDeathIndicator;

    private String patientFamilyName;
    private String patientFullName;
    private String patientGivenName;
    private String patientMiddleName;
    private String patientReligion;
    private String patientSex;
    private String patientTitle;
    private String patientType;
    private String patientZipOrPostalCode;
    private String visitNumber;
}
