package uk.ac.ucl.rits.inform.datasinks.emapstar;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.MessageIgnoredException;
import uk.ac.ucl.rits.inform.informdb.Attribute;
import uk.ac.ucl.rits.inform.informdb.AttributeKeyMap;
import uk.ac.ucl.rits.inform.informdb.Encounter;
import uk.ac.ucl.rits.inform.informdb.Mrn;
import uk.ac.ucl.rits.inform.informdb.MrnEncounter;
import uk.ac.ucl.rits.inform.informdb.PatientFact;
import uk.ac.ucl.rits.inform.informdb.PatientProperty;
import uk.ac.ucl.rits.inform.informdb.Person;
import uk.ac.ucl.rits.inform.informdb.PersonMrn;
import uk.ac.ucl.rits.inform.interchange.AdtMessage;
import uk.ac.ucl.rits.inform.interchange.AdtOperationType;

/**
 * One instance of this class covers a single ADT operation.
 *
 * @author Jeremy Stein
 *
 */
public class AdtOperation {
    private static final Logger        logger = LoggerFactory.getLogger(InformDbOperations.class);

    private InformDbOperations dbOps;
    private AdtMessage adtMsg;
    private Encounter encounter;

    private Instant storedFrom;
    private Instant admissionDateTime;
    private Instant dischargeDateTime;
    private Instant recordedDateTime;
    private Instant transferOccurred;
    private Instant locationVisitStartTime;
    private Instant locationVisitValidFrom;

    private PatientFact onlyOpenBedVisit;

    /**
     * @return the encounter that is being manipulated
     */
    public Encounter getEncounter() {
        return encounter;
    }

    /**
     * @param dbOps      the dp ops service
     * @param adtMsg     the ADT Interchange message
     * @param storedFrom time to use for any new records that might be created
     * @throws MessageIgnoredException if message can be ignored
     */
    public AdtOperation(InformDbOperations dbOps, AdtMessage adtMsg, Instant storedFrom) throws MessageIgnoredException {
        this.adtMsg = adtMsg;
        this.dbOps = dbOps;
        this.storedFrom = storedFrom;
        determineTimestamps();
        if (adtMsg.getVisitNumber() != null) {
            encounter = AdtOperation.getCreateEncounter(adtMsg.getMrn(), adtMsg.getVisitNumber(), storedFrom, admissionDateTime, dbOps);
            onlyOpenBedVisit = InformDbOperations.getOnlyElement(InformDbOperations.getOpenValidLocationVisit(encounter));
        } else if (!adtMsg.getOperationType().equals(AdtOperationType.MERGE_BY_ID)) {
            // CSNs are not present in merge by ID messages, but in other messages this is an error
            throw new MessageIgnoredException(adtMsg, "CSN missing in a non-merge message: " + adtMsg.getOperationType());
        }
    }

    /**
     * Use the AdtOperationType to determine which fields are correct ones to use.
     * Different messages types have different fields that are present/absent, and
     * they will sometimes have different meanings from message type to message type.
     */
    private void determineTimestamps() {
        admissionDateTime = adtMsg.getAdmissionDateTime();
        recordedDateTime = adtMsg.getRecordedDateTime();

        dischargeDateTime = adtMsg.getDischargeDateTime();

        // Transfers can be inferred from non-transfer messages, but
        // different fields will indicate the transfer time.
        switch (adtMsg.getOperationType()) {
        case ADMIT_PATIENT:
            transferOccurred = adtMsg.getEventOccurredDateTime();
            break;
        case TRANSFER_PATIENT:
            transferOccurred = adtMsg.getEventOccurredDateTime();
            break;
        case DISCHARGE_PATIENT:
            if (admissionDateTime == null) {
                // This can happen occasionally, seems to be only/usually where EVN-4 = "ED_AFTER_DISMISS".
                // In this unusual case, use the discharge date instead. Note that this will only be used
                // if we have no prior record of the patient and are creating their admission record now.
                admissionDateTime = adtMsg.getDischargeDateTime();
            }
            break;
        case UPDATE_PATIENT_INFO:
            // A08 doesn't have an event time, so use the recorded time instead
            // Downside: recorded time is later than event time, so subsequent discharge
            // time
            // for this visit can be *earlier* than the arrival time if it's a very short
            // visit
            // or there was a big gap between A08 event + recorded time.
            transferOccurred = adtMsg.getRecordedDateTime();
            break;
        default:
            break;
        }

        // Location visit start time is normally the same as (hospital) admission time.
        // However, if this admission is being implied from a non-admit message,
        // then the location (bed) visit can start at a different time instead.

        // If we have to reconstruct the location (bed) visit from this message, which
        // would be the best fields to use?

        if (adtMsg.getOperationType().equals(AdtOperationType.TRANSFER_PATIENT)) {
            // Bed visit start time is the transfer time, thus leaving a gap where
            // we don't know where they were, which is the most
            // accurate representation of the data we have.
            locationVisitStartTime = adtMsg.getEventOccurredDateTime();
            locationVisitValidFrom = adtMsg.getEventOccurredDateTime();
        } else if (adtMsg.getOperationType().equals(AdtOperationType.CANCEL_ADMIT_PATIENT)) {
            locationVisitStartTime = admissionDateTime;
            locationVisitValidFrom = admissionDateTime;
        } else if (adtMsg.getOperationType().equals(AdtOperationType.CANCEL_TRANSFER_PATIENT)) {
            // we also don't know when the patient started being in their most recent location
            // (ie. the one before the one that got cancelled)
            locationVisitStartTime = null;
            locationVisitValidFrom = adtMsg.getEventOccurredDateTime();
        } else if (adtMsg.getOperationType().equals(AdtOperationType.CANCEL_DISCHARGE_PATIENT)) {
            // CANCEL_DISCHARGE_PATIENT messages do not carry the discharge time field :(
            locationVisitStartTime = null;
            locationVisitValidFrom = adtMsg.getEventOccurredDateTime();
        } else if (adtMsg.getOperationType().equals(AdtOperationType.DISCHARGE_PATIENT)) {
            locationVisitStartTime = null;
            locationVisitValidFrom = adtMsg.getDischargeDateTime();
        } else {
            locationVisitStartTime = admissionDateTime;
            locationVisitValidFrom = admissionDateTime;
        }
    }

    /**
     * Find an existing Mrn by its string representation, optionally creating it
     * first if it doesn't exist.
     *
     * @param mrnStr           The mrn
     * @param validFrom        If createIfNotExist, when did the Mrn first come into
     *                         existence (valid from). Ignored if !createIfNotExist
     * @param storedFrom       the storedFrom time to use if an object needs to be newly created
     * @param createIfNotExist whether to create if it doesn't exist
     * @param dbOps            db ops service
     * @return the Mrn, pre-existing or newly created, or null if it doesn't exist
     *         and !createIfNotExist
     */
    static Mrn getCreateMrn(String mrnStr, Instant validFrom, Instant storedFrom, boolean createIfNotExist, InformDbOperations dbOps) {
        if (createIfNotExist && (storedFrom == null || validFrom == null || mrnStr == null || mrnStr.isEmpty())) {
            throw new IllegalArgumentException(String.format(
                    "if createIfNotExist, storedFrom (%s) and validFrom (%s) and mrnStr (%s) must be non-null", storedFrom, validFrom, mrnStr));
        }
        Mrn mrn = dbOps.findByMrnString(mrnStr);
        if (mrn == null) {
            if (!createIfNotExist) {
                return null;
            }
            /*
             * If it's a new MRN then assume that it's also a new person (or at least we
             * don't know which person it is yet, and we'll have to wait for the merge
             * before we find out, so we'll have to create a new person for now)
             */
            logger.info("Creating a new MRN");
            mrn = new Mrn();
            mrn.setStoredFrom(storedFrom);
            mrn.setMrn(mrnStr);
            Person pers = new Person();
            pers.setCreateDatetime(storedFrom);
            pers.addMrn(mrn, validFrom, storedFrom);
            pers = dbOps.save(pers);
        } else {
            logger.info(String.format("Reusing an existing MRN %s with encounters: %s", mrn.getMrn(),
                    mrn.getEncounters().toString()));
        }
        return mrn;
    }

    /**
     * Get existing encounter or create a new one if it doesn't exist.
     * Also create the MRN and/or Person if necessary.
     *
     * @param mrnStr            the MRN string to find/create
     * @param encounterStr      encounter ID (visit ID) to find/create
     * @param storedFrom        storedFrom time to use for newly created records - should be a time very close to the present
     * @param validFrom         validFrom times to use for newly created records - usually the admission time
     * @param dbOps             the db ops service
     * @return the Encounter, existing or newly created
     * @throws MessageIgnoredException if message can't be processed
     */
    public static Encounter getCreateEncounter(String mrnStr, String encounterStr, Instant storedFrom,
            Instant validFrom, InformDbOperations dbOps) throws MessageIgnoredException {
        logger.info(String.format("getCreateEncounter looking for existing encounter %s in MRN %s", encounterStr, mrnStr));
        // look for encounter by its encounter number only as this is sufficiently unique without also using the MRN
        Encounter existingEnc = dbOps.findEncounterByEncounter(encounterStr);
        if (existingEnc == null) {
            // The encounter didn't exist, so see if its MRN exists, creating if not.
            Mrn newOrExistingMrn = getCreateMrn(mrnStr, validFrom, storedFrom, true, dbOps);
            logger.info("getCreateEncounter CREATING NEW");
            Encounter enc = new Encounter();
            enc.setEncounter(encounterStr);
            newOrExistingMrn.addEncounter(enc, validFrom, storedFrom);
            return enc;
        } else {
            // Encounter did exist. See whether the given MRN matches the MRN we had on record for that encounter.
            // If they don't match just keep using the encounter, but this would suggest the source data has got
            // confused between the patient's current MRN and the MRN this encounter had when it was created.
            // This is a cause for concern if we need to create the encounter (other branch of this "if") because
            // we'll create it under the wrong MRN.
            String infoStr = "";
            MrnEncounter mrnMatching = InformDbOperations.getOnlyElement(existingEnc.getMrns().stream()
                    .filter(mrn -> mrn.getMrn().getMrn().equals(mrnStr)).collect(Collectors.toList()));
            if (mrnMatching == null) {
                infoStr = String.format("MRN %s was not associated with this encounter", mrnStr);
            } else {
                infoStr = String.format("MRN %s is associated with this encounter (valid = %s)",  mrnStr, mrnMatching.isValid());
            }
            logger.info(String.format("getCreateEncounter RETURNING EXISTING encounter %s (%s)", existingEnc.getEncounter(), infoStr));
            return existingEnc;
        }
    }

    /**
     * Add a new open bed/outpatient/ED ("location") visit to an existing
     * higher-level (hospital) visit.
     *
     * @param enc             the encounter to add the Visit to
     * @param visitType       the fact type of the location visit, should only be
     *                        BED_VISIT or OUTPATIENT_VISIT or similar
     * @param storedFrom      storedFrom time to use for new records
     * @param validFrom       the validFrom time to use for new records. Will
     *                        usually be the same as visitBeginTime, but it can't be
     *                        null
     * @param visitBeginTime  when the Visit (either location or hospital visit)
     *                        began. Will usually be the same as validFrom, but it
     *                        can be null if the start time can't be determined
     * @param hospitalVisit   the (hospital) visit to add the new location visit to
     * @param currentLocation location
     * @param patientClass    the patient class
     */
    @Transactional
    static
    void addOpenLocationVisit(Encounter enc, AttributeKeyMap visitType, Instant storedFrom, Instant validFrom,
            Instant visitBeginTime, PatientFact hospitalVisit, String currentLocation, String patientClass) {
        PatientFact visitFact = new PatientFact();
        visitFact.setStoredFrom(storedFrom);
        visitFact.setValidFrom(validFrom);
        visitFact.setFactType(InformDbOperations.getCreateAttribute(visitType));

        // Sometimes it's impossible to determine when a bed visit started eg - the
        // first ADT message we receive for an encounter was a discharge message, so
        // visitBeginTime can be null
        visitFact.addProperty(
                InformDbOperations.buildPatientProperty(storedFrom, validFrom, AttributeKeyMap.ARRIVAL_TIME, visitBeginTime));
        visitFact.addProperty(
                InformDbOperations.buildPatientProperty(storedFrom, validFrom, AttributeKeyMap.LOCATION, currentLocation));
        visitFact.addProperty(
                InformDbOperations.buildPatientProperty(storedFrom, validFrom, AttributeKeyMap.PATIENT_CLASS, patientClass));

        hospitalVisit.addChildFact(visitFact);
        enc.addFact(visitFact);
    }

    /**
     * Mark a Visit as finished, which can happen either when transferring or
     * discharging a patient.
     * This could use buildPatientProperty rather than being deprecated?
     *
     * @param visit             the visit to mark as finished
     * @param dischargeDateTime the discharge/transfer time
     * @param storedFrom storedFrom value to use for new records
     */
    @Deprecated
    static
    void addDischargeToVisit(PatientFact visit, Instant dischargeDateTime, Instant storedFrom) {
        Attribute dischargeTime = InformDbOperations.getCreateAttribute(AttributeKeyMap.DISCHARGE_TIME);
        PatientProperty visProp = new PatientProperty();
        visProp.setValidFrom(dischargeDateTime);
        visProp.setStoredFrom(storedFrom);
        visProp.setValueAsDatetime(dischargeDateTime);
        visProp.setPropertyType(dischargeTime);
        visit.addProperty(visProp);
    }


    /**
     * Create a new encounter using the details given in the ADT message. This may
     * also entail creating a new Mrn and Person if these don't already exist.
     * This may occur as the result of not just an A01/A04 message, because A02 or
     * A03 messages can also trigger an "admit" if we didn't previously know about that patient.
     * For this reason, look more at the patient class than whether it's an A01 or A04
     * when determining whether to create a BED_VISIT instead of an OUTPATIENT_VISIT.
     *
     * ED flows tend to go A04+A08+A01 (all patient class = E). A04 and A01 are both considered
     * to be admits here, so treat this as a transfer from the A04 to the A01 location.
     * Note that this now breaks the previous workaround for the A01+(A11)+A01 sequence before the
     * A11 messages were added to the feed, which treated A01+A01 as the second A01 *correcting* the first's
     * patient location. Now we require an explicit A11 to count it as a correction,
     * which I think is OK as these got turned on in July 2019.
     * @return the open location visit just created by this method
     *
     * @throws MessageIgnoredException if message can't be processed
     */
    public PatientFact performAdmit() throws MessageIgnoredException {
        List<PatientFact> allHospitalVisits = InformDbOperations.getOpenVisitFactWhereVisitType(encounter, AttributeKeyMap.HOSPITAL_VISIT);

        // This perhaps belongs in a getCreateHospitalVisit method, with an
        // InformDbDataIntegrity exception
        PatientFact hospitalVisit;
        switch (allHospitalVisits.size()) {
        case 0:
            hospitalVisit = InformDbOperations.addOpenHospitalVisit(encounter, storedFrom, admissionDateTime, adtMsg.getPatientClass());
            InformDbOperations.addOrUpdateDemographics(encounter, adtMsg, storedFrom);
            // create a new location visit with the new (or updated) location
            AttributeKeyMap visitType = InformDbOperations.visitTypeFromPatientClass(adtMsg.getPatientClass());
            AdtOperation.addOpenLocationVisit(encounter, visitType, storedFrom, locationVisitValidFrom, locationVisitStartTime, hospitalVisit,
                    adtMsg.getFullLocationString(), adtMsg.getPatientClass());
            break;
        case 1:
            hospitalVisit = allHospitalVisits.get(0);
            // We have received an admit message but there was already an
            // open hospital visit. Previously we would have invalidated the
            // existing bed visit and its properties and created a new one,
            // but now treat it as a transfer. You need an explicit cancel admit message
            // to get the old behaviour.
            performTransfer();
            break;
        default:
            throw new MessageIgnoredException(adtMsg, "More than 1 (count = " + allHospitalVisits.size()
                    + ") hospital visits in encounter " + adtMsg.getVisitNumber());
        }
        encounter = dbOps.save(encounter);
        logger.info(String.format("Encounter: %s", encounter.toString()));
        return InformDbOperations.getOnlyElement(InformDbOperations.getOpenValidLocationVisit(encounter));
    }

    /**
     * Handle a change in patient info, which can occur with any message type.
     *
     * @throws MessageIgnoredException if message can't be processed
     */
    public void performUpdateInfo() throws MessageIgnoredException {
        String newLocation = adtMsg.getFullLocationString();
        InformDbOperations.addOrUpdateDemographics(encounter, adtMsg, storedFrom);

        /*
         * Detect when location has changed and perform a transfer. If there isn't an
         * open location then just do nothing. Used to throw an exception but this isn't
         * really an error and we still want the demographics to update above.
         */
        if (onlyOpenBedVisit != null) {
            PatientProperty knownlocation =
                    InformDbOperations.getOnlyElement(onlyOpenBedVisit.getPropertyByAttribute(AttributeKeyMap.LOCATION, p -> p.isValid()));
            if (!newLocation.equals(knownlocation.getValueAsString())) {
                logger.warn(
                        String.format("[mrn %s, visit num %s] IMPLICIT TRANSFER IN message of type (%s): |%s| -> |%s|",
                                adtMsg.getMrn(), adtMsg.getVisitNumber(), adtMsg.getOperationType(),
                                knownlocation.getValueAsString(), newLocation));
                performTransfer();
            }
        }

    }

    /**
     * Close off the existing Visit and open a new one.
     *
     * @throws MessageIgnoredException if message can't be processed
     */
    public void performTransfer() throws MessageIgnoredException {
        if (onlyOpenBedVisit == null) {
            logger.warn("Received transfer for patient we didn't know was admitted - admitting them instead");
            performAdmit();
            return;
        }

        String newTransferLocation = adtMsg.getFullLocationString();
        String currentKnownLocation = InformDbOperations
                .getOnlyElement(onlyOpenBedVisit.getPropertyByAttribute(AttributeKeyMap.LOCATION, p -> p.isValid()))
                .getValueAsString();
        if (newTransferLocation.equals(currentKnownLocation)) {
            // If we get an A02 with a new location that matches where we already thought
            // the patient was, don't perform an actual transfer.
            // In the test data, this sometimes happens following an A08 implied transfer.
            // Also, even if the location hasn't changed the patient class could have changed
            // (should be made explicit as an A06 or A07 but we don't distinguish A02/A06/A07 here).
            PatientProperty currentPatientClass = InformDbOperations.getOnlyElement(
                    onlyOpenBedVisit.getPropertyByAttribute(AttributeKeyMap.PATIENT_CLASS, p -> p.isValid()));
            if (adtMsg.getPatientClass().equals(currentPatientClass.getValueAsString())) {
                String err = String.format("REDUNDANT transfer, location (%s) and patient class (%s) have not changed",
                        currentKnownLocation, currentPatientClass.getValueAsString());
                logger.warn(err);
                throw new MessageIgnoredException(adtMsg, err);
            } else {
                // Only patient class has changed, so update just that without creating a new location visit
                currentPatientClass.setValidUntil(transferOccurred);
                onlyOpenBedVisit.addProperty(InformDbOperations.buildPatientProperty(storedFrom, transferOccurred,
                        AttributeKeyMap.PATIENT_CLASS, adtMsg.getPatientClass()));
            }
        } else {
            // locations have changed, do a "normal" transfer, patient class will get done as part of this
            addDischargeToVisit(onlyOpenBedVisit, transferOccurred, storedFrom);
            String admitSource = adtMsg.getAdmitSource();
            logger.info(String.format(
                    "TRANSFERRING: MRN = %s, admitdatetime %s, admitsrc %s, eventOccurred %s, recorded %s",
                    adtMsg.getMrn(), admissionDateTime, admitSource, transferOccurred, recordedDateTime));
            // add a new visit to the current encounter
            Encounter encounterDoubleCheck = onlyOpenBedVisit.getEncounter();
            if (encounter != encounterDoubleCheck) {
                throw new MessageIgnoredException(adtMsg, "Different encounter: " + encounter + " | " + encounterDoubleCheck);
            }
            List<PatientFact> hospitalVisit = InformDbOperations.getOpenVisitFactWhereVisitType(encounter, AttributeKeyMap.HOSPITAL_VISIT);
            // link the bed visit to the parent (hospital) visit
            AttributeKeyMap visitType = InformDbOperations.visitTypeFromPatientClass(adtMsg.getPatientClass());
            addOpenLocationVisit(encounter, visitType, storedFrom, transferOccurred, transferOccurred, hospitalVisit.get(0),
                    newTransferLocation, adtMsg.getPatientClass());
        }
        // demographics may have changed
        InformDbOperations.addOrUpdateDemographics(encounter, adtMsg, storedFrom);
    }


    /**
     * Mark the specified visit as finished.
     *
     * @throws MessageIgnoredException if message can't be processed
     */
    public void performDischarge() throws MessageIgnoredException {
        if (onlyOpenBedVisit == null) {
            // If visit was not known about, admit the patient first before going on to discharge
            // It's not possible to tell when to start the bed visit from.
            onlyOpenBedVisit = performAdmit();
        }

        logger.info(String.format("DISCHARGE: MRN %s, visit %s, eventoccurred %s, dischargetime %s", adtMsg.getMrn(),
                adtMsg.getVisitNumber(), adtMsg.getEventOccurredDateTime(), dischargeDateTime));
        if (dischargeDateTime == null) {
            throw new MessageIgnoredException(adtMsg, "Trying to discharge but the discharge date is null");
        } else {
            // Discharge from the bed visit and the hospital visit
            AdtOperation.addDischargeToVisit(onlyOpenBedVisit, dischargeDateTime, storedFrom);
            PatientFact hospVisit = onlyOpenBedVisit.getParentFact();
            AdtOperation.addDischargeToVisit(hospVisit, dischargeDateTime, storedFrom);

            String dischargeDisposition = adtMsg.getDischargeDisposition();
            // Add discharge disposition to hospital visit only, not bed.
            hospVisit.addProperty(InformDbOperations.buildPatientProperty(storedFrom, dischargeDateTime,
                    AttributeKeyMap.DISCHARGE_DISPOSITION, dischargeDisposition));
            String dischargeLocation = adtMsg.getDischargeLocation();
            hospVisit.addProperty(InformDbOperations.buildPatientProperty(storedFrom, dischargeDateTime,
                    AttributeKeyMap.DISCHARGE_LOCATION, dischargeLocation));
            // demographics may have changed
            InformDbOperations.addOrUpdateDemographics(encounter, adtMsg, storedFrom);
        }
    }

    /**
     * Cancel a pre-existing admission by invalidating the facts associated with it.
     * @throws MessageIgnoredException if message can't be processed
     */
    public void performCancelAdmit() throws MessageIgnoredException {
        if (onlyOpenBedVisit == null) {
            onlyOpenBedVisit = performAdmit();
            if (onlyOpenBedVisit == null) {
                throw new MessageIgnoredException(adtMsg, "No open location visit, cannot cancel admit" + adtMsg.getVisitNumber());
            }
        }

        // It's usual for an HL7-originated ED admission that there will be two beds
        // visits at this point - one that resulted from the original A04 HL7 message
        // and one from the A01 that typically has a different location.
        // Make sure that all bed and hospital visits get invalidated in the case of a
        // cancel admit.
        // A side-effect of doing this is that the (corrected) admit message that follows
        // this cancel admit will open produce one bed visit in the new hospital visit.
        List<PatientFact> closedBedVisits = InformDbOperations.getClosedLocationVisitFact(encounter);

        Instant cancellationTime = adtMsg.getEventOccurredDateTime();
        PatientFact hospVisit = onlyOpenBedVisit.getParentFact();
        // do the actual invalidations
        onlyOpenBedVisit.invalidateAll(cancellationTime);
        hospVisit.invalidateAll(cancellationTime);
        for (PatientFact closedBedVisit : closedBedVisits) {
            closedBedVisit.invalidateAll(cancellationTime);
        }
    }

    /**
     * Cancel the most recent bed visit by invalidating it.
     *
     * @throws MessageIgnoredException    if message can't be processed
     */
    public void performCancelTransfer() throws MessageIgnoredException {
        Instant cancellationDateTime = adtMsg.getRecordedDateTime();
        // the new location, which is the location before the erroneous transfer was made
        String newCorrectLocation = adtMsg.getFullLocationString();
        // the transfer time of the transfer being cancelled, NOT the cancellation time
        Instant originalTransferDateTime = adtMsg.getEventOccurredDateTime();

        if (onlyOpenBedVisit == null) {
            // If visit was not known about, admit the patient first.
            // We now have their current location and can stop.
            // (Don't go so far as to create their cancelled bed visit and
            // then invalidate it).
            performAdmit();
            return;
        }

        PatientFact hospVisit = onlyOpenBedVisit.getParentFact();
        // invalidate the erroneous transfer
        onlyOpenBedVisit.invalidateAll(cancellationDateTime);

        // reopen the previous bed visit by invalidating its discharge time property
        Optional<PatientFact> mostRecentBedVisitOptional = InformDbOperations.getVisitFactWhere(encounter,
                vf -> AttributeKeyMap.isLocationVisitType(vf.getFactType()) && vf.isValid()).stream()
                        .max((vf1, vf2) -> InformDbOperations.sortVisitByDischargeTime(vf1, vf2));

        if (mostRecentBedVisitOptional.isPresent()) {
            /**
             * Previous visit exists, reopen it
             */
            PatientFact mostRecentBedVisit = mostRecentBedVisitOptional.get();

            if (InformDbOperations.visitFactIsOpen(mostRecentBedVisit)) {
                // it was already open, that's too weird
                throw new MessageIgnoredException(adtMsg,
                        String.format("Can't cancel transfer for CSN %s, prior bed visit was already open", adtMsg.getVisitNumber()));
            }

            // reopen it by invalidating its discharge time
            PatientProperty bedDischargeTime = InformDbOperations.getOnlyElement(
                    mostRecentBedVisit.getPropertyByAttribute(AttributeKeyMap.DISCHARGE_TIME, PatientProperty::isValid));
            bedDischargeTime.setValidUntil(cancellationDateTime);
        } else {
            /*
             * If there is no previous location (this situation should only happen if we've
             * come in mid-stream), then re-create the previous location visit as best we
             * can: We can't know when it started, so set arrival time as null and use the
             * point where it ended as the valid_from.
             */
            AttributeKeyMap visitType = InformDbOperations.visitTypeFromPatientClass(adtMsg.getPatientClass());
            AdtOperation.addOpenLocationVisit(encounter, visitType, storedFrom, originalTransferDateTime, null, hospVisit,
                    newCorrectLocation, adtMsg.getPatientClass());
        }
    }

    /**
     * Mark the visit specified by visit number as not discharged any more. Can either mean a discharge was
     * erroneously entered, or a decision to discharge was reversed.
     *
     * @throws MessageIgnoredException if message can't be processed
     */
    public void performCancelDischarge() throws MessageIgnoredException {
        // event occurred field seems to be populated despite the Epic example message showing it blank.
        Instant invalidationDate = adtMsg.getEventOccurredDateTime();
        // this must be non-null or the invalidation won't work
        if (invalidationDate == null) {
            throw new MessageIgnoredException(adtMsg, "Trying to cancel discharge but the event occurred date is null");
        }
        // Get the most recent bed visit.
        Optional<PatientFact> mostRecentBedVisitOptional = InformDbOperations.getVisitFactWhere(encounter,
                vf -> AttributeKeyMap.isLocationVisitType(vf.getFactType()) && vf.isValid()).stream()
                        .max((vf1, vf2) -> InformDbOperations.sortVisitByDischargeTime(vf1, vf2));

        if (!mostRecentBedVisitOptional.isPresent()) {
            // If we have no existing visit, admit the patient.
            // We now have their current location and can stop.
            // (Don't go so far as to create the discharge and
            // then invalidate it).
            performAdmit();
            return;
        }
        PatientFact mostRecentBedVisit = mostRecentBedVisitOptional.get();

        // Encounters should always have at least one visit.
        if (InformDbOperations.visitFactIsOpen(mostRecentBedVisit)) {
            // This is an error. The most recent bed visit is still open. Ie. the patient
            // has not been discharged, so we cannot cancel the discharge.
            // Possible cause is that we never received the A03.
            throw new MessageIgnoredException(adtMsg, adtMsg.getVisitNumber() + " Cannot process A13 - most recent bed visit is still open");
        }
        PatientProperty bedDischargeTime = InformDbOperations.getOnlyElement(
                mostRecentBedVisit.getPropertyByAttribute(AttributeKeyMap.DISCHARGE_TIME, PatientProperty::isValid));
        // Do the actual cancel by invalidating the discharge time property on the
        // location visit, and multiple properties on the hospital visit
        bedDischargeTime.setValidUntil(invalidationDate);
        PatientFact hospitalVisit = mostRecentBedVisit.getParentFact();
        for (AttributeKeyMap a : Arrays.asList(AttributeKeyMap.DISCHARGE_TIME, AttributeKeyMap.DISCHARGE_DISPOSITION,
                AttributeKeyMap.DISCHARGE_LOCATION)) {
            PatientProperty prop = InformDbOperations.getOnlyElement(hospitalVisit.getPropertyByAttribute(a, PatientProperty::isValid));
            if (prop != null) {
                prop.setValidUntil(invalidationDate);
            }
        }

        // The Epic spec for receiving an A13 says you can be put in a different place than the last one you were in,
        // ie. an implicit transfer. Does this ever happen for messages that Epic emits? Currently ignoring
        // the location field.

        mostRecentBedVisit = dbOps.save(mostRecentBedVisit);
        hospitalVisit = dbOps.save(hospitalVisit);
    }

    /**
     * Indicate in the DB that two MRNs now belong to the same person. One MRN is
     * designated the surviving MRN, although we can't prevent data being added to
     * whichever MRN/CSN is specified in future messages, which (if the source
     * system is behaving) we'd hope would be the surviving MRN. The best we could
     * do is flag it as an error if new data is put against a non-surviving MRN.
     *
     * @throws MessageIgnoredException if merge time in message is blank or message
     *                                 can't be processed
     */
    public void performMergeById() throws MessageIgnoredException {
        String oldMrnStr = adtMsg.getMergedPatientId();
        String survivingMrnStr = adtMsg.getMrn();
        Instant mergeTime = adtMsg.getRecordedDateTime();
        logger.info(String.format("MERGE: surviving mrn %s, oldMrn = %s, merge time = %s", survivingMrnStr, oldMrnStr,
                mergeTime));
        if (mergeTime == null) {
            throw new MessageIgnoredException(adtMsg, "event occurred null");
        }

        // The non-surviving Mrn is invalidated but still points to the old person
        // (we are recording the fact that between these dates, the hospital believed
        // that the mrn belonged to this person)
        Mrn oldMrn = getCreateMrn(oldMrnStr, mergeTime, storedFrom, true, dbOps);
        Mrn survivingMrn = getCreateMrn(survivingMrnStr, mergeTime, storedFrom, true, dbOps);
        if (survivingMrn == null || oldMrn == null) {
            throw new MessageIgnoredException(adtMsg, String.format("MRNs %s or %s (%s or %s) are not previously known, do nothing",
                    oldMrnStr, survivingMrnStr, oldMrn, survivingMrn));
        }
        PersonMrn oldPersonMrn = InformDbOperations.getOnlyElementWhere(oldMrn.getPersons(), pm -> pm.isValid());
        PersonMrn survivingPersonMrn = InformDbOperations.getOnlyElementWhere(survivingMrn.getPersons(), pm -> pm.isValid());
        if (survivingPersonMrn == null || oldPersonMrn == null) {
            throw new MessageIgnoredException(adtMsg, String.format(
                    "MRNs %s and %s exist but there was no currently valid person for one/both of them (%s and %s)",
                    oldMrnStr, survivingMrnStr, oldPersonMrn, survivingPersonMrn));
        }

        // If we already thought they were the same person, do nothing further.
        if (oldPersonMrn.getPerson().equals(survivingPersonMrn.getPerson())) {
            throw new MessageIgnoredException(adtMsg,
                    String.format("We already thought that MRNs %s and %s were the same person (%s)", oldMrnStr,
                            survivingMrnStr, oldPersonMrn.getPerson().getPersonId()));
        }

        survivingPersonMrn.setLive(true);

        // Invalidate the old person<->mrn association
        oldPersonMrn.setValidUntil(mergeTime);

        // Create a new person<->mrn association that tells us that as of the merge time
        // the old MRN is believed to belong to the person associated with the surviving MRN
        Person survivingPerson = survivingPersonMrn.getPerson();
        PersonMrn newOldPersonMrn = new PersonMrn(survivingPerson, oldMrn);
        newOldPersonMrn.setStoredFrom(storedFrom);
        newOldPersonMrn.setValidFrom(mergeTime);
        newOldPersonMrn.setLive(false);
        survivingPerson.linkMrn(newOldPersonMrn);
        oldMrn.linkPerson(newOldPersonMrn);

        newOldPersonMrn = dbOps.save(newOldPersonMrn);
        oldPersonMrn = dbOps.save(oldPersonMrn);
    }
}
