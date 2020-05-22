package uk.ac.ucl.rits.inform.datasinks.emapstar;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import uk.ac.ucl.rits.inform.interchange.AdtMessage;
import uk.ac.ucl.rits.inform.interchange.AdtOperationType;
import uk.ac.ucl.rits.inform.interchange.VitalSigns;

/**
 * Create a stream of messages to simulate a chain of HL7 messages firing.
 *
 * @author Jeremy Stein & Roma Klapaukh
 */
public abstract class MessageStreamBaseCase extends MessageProcessingBaseCase {
    protected Instant             currentTime          = Instant.parse("2020-03-01T06:30:00.000Z");
    protected final String[]      allLocations         = { "T42^BADGERS^WISCONSIN", "ED^BADGERS^HONEY",
            "ED^BADGERS^HOG", "ED^BADGERS^PALAWAN", "ED^BADGERS^JAPANESE", "ED^BADGERS^JAVAN", "ED^BADGERS^EURASIAN" };
    protected int                 currentLocation      = 0;
    protected String              mrn                  = "1234ABCD";
    protected String              csn                  = "1234567890";
    protected String              patientClass         = "E";
    protected Instant             admissionTime        = null;
    protected Instant             dischargeTime        = null;
    protected String              nhsNumber            = "9999999999";
    protected String              name                 = "Fred Blogger";
    protected final List<Instant> transferTime         = new ArrayList<>();

    protected String              dischargeDisposition = "Peachy";
    protected String              dischargeLocation    = "Home";
    protected boolean             patientDied          = false;
    protected Instant             deathTime            = null;

    protected double              vitalReading         = 92.;
    protected List<Instant>       vitalTime            = new ArrayList<>();

    /**
     * Create a new MessageStreamBaseCase.
     */
    public MessageStreamBaseCase() {}

    /**
     * Reset the state to allow for a new stream of stream of tests to be run with
     * the same instance.
     *
     * This does not reset the class to how it is instantiated though.
     */
    protected void reinitialise() {
        messageStream.clear();
        nextToProcess = 0;
        this.patientClass = "E";
        this.vitalTime.clear();
        this.transferTime.clear();
        this.admissionTime = null;
        this.dischargeTime = null;
        this.patientDied = false;
        this.deathTime = null;
        dischargeDisposition = "Peachy";
        dischargeLocation = "Home";
    }

    /**
     * Step the clock forward not quite exactly one hour. One hour looks too much
     * like a time zone bug when it causes an error.
     */
    protected void stepClock() {
        this.currentTime = this.currentTime.plusSeconds(3660);
    }

    /**
     * Return a time and advance the clock.
     *
     * @return A time later than all the previous ones.
     */
    protected Instant nextTime() {
        this.stepClock();
        return currentTime;
    }

    /**
     * Get the current location of the patient.
     *
     * @return Current location.
     */
    protected String currentLocation() {
        return this.allLocations[this.currentLocation];
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
     * See the next location without changing state. When the end of the array is reached, loop around.
     */
    protected String peekNextLocation() {
        int loc = (this.currentLocation + 1) % this.allLocations.length;
        return allLocations[loc];
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
     * Get the time of the last transfer
     *
     * @return The time of the last transfer
     */
    protected Instant lastTransferTime() {
        if (this.transferTime.isEmpty()) {
            return null;
        }
        return this.transferTime.get(this.transferTime.size() - 1);
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
    public void queueVital() {
        Instant vitalTime = this.nextTime();
        this.vitalTime.add(vitalTime);

        VitalSigns vital = new VitalSigns();
        vital.setMrn(this.mrn);
        vital.setVisitNumber(this.csn);
        vital.setVitalSignIdentifier("HEART_RATE");
        vital.setNumericValue(vitalReading);
        vital.setUnit("/min");
        vital.setObservationTimeTaken(vitalTime);
        queueMessage(vital);
    }

    /**
     * Queue a patient update message.
     */
    public void queueUpdatePatientDetails() {
        boolean impliedTransfer = this.admissionTime == null;

        this.ensureAdmitted();

        this.stepClock();

        if (impliedTransfer) {
            this.transferTime.add(this.currentTime);
        }

        AdtMessage update = new AdtMessage();
        update.setOperationType(AdtOperationType.UPDATE_PATIENT_INFO);
        update.setAdmissionDateTime(this.admissionTime);
        update.setRecordedDateTime(this.admissionTime);
        update.setEventOccurredDateTime(this.currentTime);
        update.setMrn(this.mrn);
        update.setNhsNumber(this.nhsNumber);
        update.setVisitNumber(this.csn);
        update.setPatientFullName(this.name);
        update.setFullLocationString(allLocations[this.currentLocation]);
        update.setPatientClass(this.patientClass);
        update.setPatientDeathIndicator(this.patientDied);
        update.setPatientDeathDateTime(this.deathTime);
        queueMessage(update);
    }

    /**
     * Queue an admit message that doesn't perform a transfer. If you want to
     * simulate an A06 change do <code>patientClass="I"</code> before calling this
     * method.
     */
    public void queueAdmit() {
        this.queueAdmit(false);
    }

    /**
     * Queue an admit message that may or may not perform a transfer.
     *
     * @param transfer If true will also advance the patient to the next location.
     */
    public void queueAdmit(boolean transfer) {
        Instant eventTime = this.nextTime();

        if (this.admissionTime == null || transfer) {
            this.transferTime.add(eventTime);
        }
        if (this.admissionTime == null) {
            this.admissionTime = eventTime;
        }
        if (transfer) {
            this.stepLocation();
        }

        AdtMessage admit = new AdtMessage();
        admit.setOperationType(AdtOperationType.ADMIT_PATIENT);
        admit.setAdmissionDateTime(this.admissionTime);
        admit.setEventOccurredDateTime(eventTime);
        admit.setMrn(this.mrn);
        admit.setVisitNumber(this.csn);
        admit.setPatientClass(this.patientClass);
        admit.setPatientFullName(this.name);
        admit.setFullLocationString(this.currentLocation());
        admit.setPatientDeathIndicator(this.patientDied);
        admit.setPatientDeathDateTime(this.deathTime);
        this.queueMessage(admit);
    }

    /**
     * Queue a moving transfer message.
     */
    public void queueTransfer() {
        this.queueTransfer(true);
    }

    /**
     * Queue a transfer message.
     *
     * @param updateLocation if false the patient location will not be advanced.
     */
    public void queueTransfer(boolean updateLocation) {
        this.ensureAdmitted();

        // Handle non-moving update
        String location;

        Instant tTime = this.nextTime();

        if (updateLocation) {
            location = nextLocation();
            this.transferTime.add(tTime);
        } else {
            location = currentLocation();
        }

        AdtMessage transfer = new AdtMessage();
        transfer.setOperationType(AdtOperationType.TRANSFER_PATIENT);
        transfer.setAdmissionDateTime(this.admissionTime);
        transfer.setEventOccurredDateTime(tTime);
        transfer.setMrn(this.mrn);
        transfer.setVisitNumber(this.csn);
        transfer.setPatientClass(this.patientClass);
        transfer.setPatientFullName(this.name);
        transfer.setFullLocationString(location);
        transfer.setPatientDeathIndicator(this.patientDied);
        transfer.setPatientDeathDateTime(deathTime);
        this.queueMessage(transfer);

    }

    /**
     * Queue a cancel admit message.
     */
    public void queueCancelAdmit() {
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
        cancelAdmit.setPatientDeathIndicator(this.patientDied);
        cancelAdmit.setPatientDeathDateTime(deathTime);

        this.queueMessage(cancelAdmit);

        this.admissionTime = null;
    }

    /**
     * Queue a cancel transfer message.
     */
    public void queueCancelTransfer() {
        this.ensureAdmitted();
        Instant erroneousTransferDateTime;
        if (this.transferTime.isEmpty()) {
            erroneousTransferDateTime = this.nextTime();
        } else {
            erroneousTransferDateTime = this.transferTime.remove(this.transferTime.size() - 1);
        }

        if(this.transferTime.isEmpty()) {
            // This is really acting as a place holder for NULL
            this.transferTime.add(Instant.MIN);
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
        cancelTransfer.setPatientDeathIndicator(this.patientDied);
        cancelTransfer.setPatientDeathDateTime(deathTime);

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
        discharge.setPatientDeathDateTime(deathTime);

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

    /**
     * Queue a merge MRN message
     *
     * @param mergedMrn    The mrn that will stop being used
     * @param survivingMrn The mrn that will be used going forwards
     */
    public void queueMerge(String mergedMrn, String survivingMrn) {
        AdtMessage merge = new AdtMessage();

        merge.setOperationType(AdtOperationType.MERGE_BY_ID);
        merge.setRecordedDateTime(this.nextTime());
        merge.setMrn(mergedMrn);
        merge.setMergedPatientId(survivingMrn);

        this.queueMessage(merge);
    }
}
