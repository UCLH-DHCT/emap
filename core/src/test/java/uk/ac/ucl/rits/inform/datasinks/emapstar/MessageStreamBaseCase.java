package uk.ac.ucl.rits.inform.datasinks.emapstar;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import uk.ac.ucl.rits.inform.interchange.AdtMessage;
import uk.ac.ucl.rits.inform.interchange.AdtOperationType;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.VitalSigns;

/**
 * Create a stream of messages to simulate a chain of HL7 messages firing.
 *
 * @author Jeremy Stein & Roma Klapaukh
 */
public abstract class MessageStreamBaseCase extends MessageProcessingBaseCase {
    protected Instant        currentTime          = Instant.parse("2020-03-01T06:30:00.000Z");
    protected final String[] allLocations         = { "T42^BADGERS^WISCONSIN", "ED^BADGERS^HONEY", "ED^BADGERS^HOG",
            "ED^BADGERS^PALAWAN", "ED^BADGERS^JAPANESE", "ED^BADGERS^JAVAN", "ED^BADGERS^EURASIAN" };
    protected int            currentLocation      = 0;
    protected String         mrn                  = "1234ABCD";
    protected String         csn                  = "1234567890";
    protected String         patientClass         = "E";
    protected Instant        admissionTime        = null;
    protected Instant        dischargeTime        = null;
    protected String         nhsNumber            = "9999999999";
    protected String         name                 = "Fred Blogger";
    protected List<Instant>  transferTime     = new ArrayList<>();

    protected String         dischargeDisposition = "Peachy";
    protected String         dischargeLocation    = "Home";
    protected boolean        patientDied          = false;

    /**
     * Create a new MessageStreamBaseCase.
     */
    public MessageStreamBaseCase() {}

    /**
     * Step the clock forward one hour.
     */
    protected void stepClock() {
        this.currentTime = this.currentTime.plusSeconds(3600);
    }

    /**
     * Return a time and advance the clock.
     *
     * @return A time later than all the previous ones.
     */
    protected Instant nextTime() {
        Instant time = currentTime;
        this.stepClock();
        return time;
    }

    /**
     * Select the next location. When the end of the array is reached, loop around.
     */
    protected void stepLocation() {
        this.currentLocation = (this.currentLocation + 1) % this.allLocations.length;
    }

    /**
     * Select the previous location. When the start of the array is reached, loop
     * around.
     */
    protected void backLocation() {
        this.currentLocation = this.currentLocation - 1;
        if (this.currentLocation < 0) {
            this.currentLocation = this.allLocations.length + this.currentLocation;
        }
    }

    /**
     * Get the current location and advance to the next one.
     *
     * @return A location.
     */
    protected String nextLocation() {
        this.stepLocation();
        return allLocations[currentLocation];
    }

    /**
     * Return to the previous location and return it.
     *
     * @return A location.
     */
    protected String previousLocation() {
        this.backLocation();
        return allLocations[currentLocation];
    }

    /**
     * Make sure that the patient has an admission time.
     */
    protected void ensureAdmitted() {
        if (this.admissionTime == null) {
            this.admissionTime = this.nextTime();
        }
    }

    /**
     * Queue a vital signs message.
     */
    public void queueVitals() {
        VitalSigns vital = new VitalSigns();
        vital.setMrn(this.mrn);
        vital.setVisitNumber(this.csn);
        vital.setVitalSignIdentifier("HEART_RATE");
        vital.setNumericValue(92.);
        vital.setUnit("/min");
        vital.setObservationTimeTaken(this.nextTime());
        queueMessage(vital);
    }

    /**
     * Queue a patient update message.
     */
    public void queueUpdatePatientDetails() {
        this.ensureAdmitted();
        AdtMessage update = new AdtMessage();
        update.setOperationType(AdtOperationType.UPDATE_PATIENT_INFO);
        update.setAdmissionDateTime(this.admissionTime);
        update.setRecordedDateTime(this.admissionTime);
        update.setEventOccurredDateTime(this.nextTime());
        update.setMrn(this.mrn);
        update.setNhsNumber(this.nhsNumber);
        update.setVisitNumber(this.csn);
        update.setPatientFullName(this.name);
        update.setFullLocationString(allLocations[this.currentLocation]);
        update.setPatientClass(this.patientClass);
        queueMessage(update);
    }

    /**
     * Queue a transfer message.
     */
    public void queueTransfer() {
        this.ensureAdmitted();
        AdtMessage transfer = new AdtMessage();
        Instant tTime = this.nextTime();
        this.transferTime.add(tTime);

        transfer.setOperationType(AdtOperationType.TRANSFER_PATIENT);
        transfer.setAdmissionDateTime(this.admissionTime);
        transfer.setEventOccurredDateTime(tTime);
        transfer.setMrn(this.mrn);
        transfer.setVisitNumber(this.csn);
        transfer.setPatientClass(this.patientClass);
        transfer.setPatientFullName(this.name);
        transfer.setFullLocationString(this.nextLocation());
        this.queueMessage(transfer);

    }

    /**
     * Queue a cancel admit message.
     */
    public void queueCancelAdmit() throws EmapOperationMessageProcessingException {
        this.ensureAdmitted();
        AdtMessage cancelAdmit = new AdtMessage();
        Instant expectedCancellationDateTime = this.nextTime();

        cancelAdmit.setOperationType(AdtOperationType.CANCEL_ADMIT_PATIENT);
        cancelAdmit.setAdmissionDateTime(this.admissionTime);
        cancelAdmit.setEventOccurredDateTime(expectedCancellationDateTime);
        cancelAdmit.setMrn(this.mrn);
        cancelAdmit.setVisitNumber(this.csn);
        cancelAdmit.setPatientClass(this.patientClass);
        cancelAdmit.setPatientFullName(this.name);
        cancelAdmit.setFullLocationString(this.previousLocation());
        this.queueMessage(cancelAdmit);

        this.admissionTime = null;
    }

    /**
     * Queue a cancel transfer message.
     */
    public void queueCancelTransfer() throws EmapOperationMessageProcessingException {
        this.ensureAdmitted();
        Instant erroneousTransferDateTime;
        if (this.transferTime.isEmpty()) {
            erroneousTransferDateTime = this.nextTime();
        } else {
            erroneousTransferDateTime = this.transferTime.remove(this.transferTime.size() - 1);
        }

        AdtMessage cancelTransfer = new AdtMessage();
        cancelTransfer.setOperationType(AdtOperationType.CANCEL_TRANSFER_PATIENT);
        cancelTransfer.setAdmissionDateTime(this.admissionTime);
        cancelTransfer.setEventOccurredDateTime(erroneousTransferDateTime);
        cancelTransfer.setRecordedDateTime(this.nextTime());
        cancelTransfer.setMrn(this.mrn);
        cancelTransfer.setVisitNumber(this.csn);
        cancelTransfer.setPatientClass(this.patientClass);
        cancelTransfer.setPatientFullName(this.name);
        cancelTransfer.setFullLocationString(this.previousLocation());

        this.queueMessage(cancelTransfer);
    }

    /**
     * Queue a discharge message.
     */
    public void queueDischarge() {
        this.ensureAdmitted();
        this.dischargeTime = this.nextTime();

        AdtMessage discharge = new AdtMessage();

        discharge.setOperationType(AdtOperationType.DISCHARGE_PATIENT);
        discharge.setAdmissionDateTime(this.admissionTime);
        discharge.setEventOccurredDateTime(this.dischargeTime);
        discharge.setMrn(this.mrn);
        discharge.setFullLocationString(this.allLocations[this.currentLocation]);
        discharge.setVisitNumber(this.csn);
        discharge.setPatientClass(this.patientClass);
        discharge.setPatientFullName(this.name);
        discharge.setDischargeDisposition(this.dischargeDisposition);
        discharge.setDischargeLocation(this.dischargeLocation);
        discharge.setDischargeDateTime(this.dischargeTime);
        discharge.setPatientDeathIndicator(this.patientDied);

        this.queueMessage(discharge);
    }

    /**
     * Queue a cancel discharge message.
     */
    public void queueCancelDischarge() {
        this.ensureAdmitted();
        AdtMessage cancelDischarge = new AdtMessage();
        cancelDischarge.setOperationType(AdtOperationType.CANCEL_DISCHARGE_PATIENT);
        cancelDischarge.setAdmissionDateTime(this.admissionTime);
        cancelDischarge.setEventOccurredDateTime(this.nextTime());
        cancelDischarge.setMrn(this.mrn);
        cancelDischarge.setVisitNumber(this.csn);
        cancelDischarge.setPatientClass(this.patientClass);
        cancelDischarge.setPatientFullName(this.name);
        cancelDischarge.setFullLocationString(this.allLocations[this.currentLocation]);
        // A13 messages do not carry the discharge time field

        this.queueMessage(cancelDischarge);
    }

}
