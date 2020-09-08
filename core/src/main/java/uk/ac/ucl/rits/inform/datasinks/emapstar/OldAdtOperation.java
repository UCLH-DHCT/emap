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
import uk.ac.ucl.rits.inform.informdb.OldAttribute;
import uk.ac.ucl.rits.inform.informdb.OldAttributeKeyMap;
import uk.ac.ucl.rits.inform.informdb.Encounter;
import uk.ac.ucl.rits.inform.informdb.OldMrn;
import uk.ac.ucl.rits.inform.informdb.MrnEncounter;
import uk.ac.ucl.rits.inform.informdb.PatientFact;
import uk.ac.ucl.rits.inform.informdb.PatientProperty;
import uk.ac.ucl.rits.inform.informdb.Person;
import uk.ac.ucl.rits.inform.informdb.PersonMrn;
import uk.ac.ucl.rits.inform.interchange.OldAdtMessage;
import uk.ac.ucl.rits.inform.interchange.AdtOperationType;
import uk.ac.ucl.rits.inform.interchange.adt.DischargePatient;
import uk.ac.ucl.rits.inform.interchange.adt.MergeById;

/**
 * One instance of this class covers a single ADT operation.
 *
 * @author Jeremy Stein
 *
 */
public class OldAdtOperation implements AdtOperationInterface {
    private static final Logger        logger = LoggerFactory.getLogger(InformDbOperations.class);

    private InformDbOperations dbOps;
    private OldAdtMessage adtMsg;
    private Encounter encounter;

    private Instant storedFrom;
    private Instant admissionDateTime;
    private Instant dischargeDateTime;
    private Instant recordedDateTime;
    private Instant transferOccurred;
    private Instant locationVisitStartTime;
    private Instant locationVisitValidFrom;

    private PatientFact onlyOpenBedVisit;

    private String newTransferLocation;
    // Temporary fields to confirm that tests pass when using subclasses
    private DischargePatient dischargeMsg;
    private MergeById mergeMsg;

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
    public OldAdtOperation(InformDbOperations dbOps, OldAdtMessage adtMsg, Instant storedFrom) throws MessageIgnoredException {
        this.adtMsg = adtMsg;
        this.dbOps = dbOps;
        this.storedFrom = storedFrom;
        determineTimestamps();
        getCreateEncounterOrVisit(dbOps, adtMsg, storedFrom);
    }

    /**
     * Go ahead and process the ADT message.
     * @return a status string
     * @throws MessageIgnoredException if message is being ignored
     */
    @Override
    public String processMessage() throws MessageIgnoredException {
        String returnCode;
        returnCode = "OK";
        switch (adtMsg.getOperationType()) {
        case ADMIT_PATIENT:
            performAdmit();
            break;
        case TRANSFER_PATIENT:
            performTransfer();
            break;
        case DISCHARGE_PATIENT:
            this.performDischarge();
            break;
        case UPDATE_PATIENT_INFO:
            performAdmit();
            break;
        case CANCEL_ADMIT_PATIENT:
            this.performCancelAdmit();
            break;
        case CANCEL_TRANSFER_PATIENT:
            this.performCancelTransfer();
            break;
        case CANCEL_DISCHARGE_PATIENT:
            this.performCancelDischarge();
            break;
        case MERGE_BY_ID:
            this.performMergeById();
            break;
        default:
            returnCode = "Not implemented";
            logger.error(adtMsg.getOperationType() + " message type not implemented");
            break;
        }
        return returnCode;
    }

    /**
     * Use the AdtOperationType to determine which fields are correct ones to use.
     * Different messages types have different fields that are present/absent, and
     * they will sometimes have different meanings from message type to message type.
     */
    private void determineTimestamps() {
        admissionDateTime = adtMsg.getAdmissionDateTime();
        recordedDateTime = adtMsg.getRecordedDateTime();

        if (dischargeMsg != null) {
            dischargeDateTime = dischargeMsg.getDischargeDateTime();
        }

        // true for all message types? Yes for admit, transfer and update_info...
        newTransferLocation = adtMsg.getFullLocationString();

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
                    admissionDateTime = dischargeMsg.getDischargeDateTime();
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
            // CANCEL_DISCHARGE_PATIENT messages do not carry the arrival time for the location
            locationVisitStartTime = null;
            locationVisitValidFrom = adtMsg.getRecordedDateTime();
        } else if (adtMsg.getOperationType().equals(AdtOperationType.DISCHARGE_PATIENT)) {
            locationVisitStartTime = null;
            locationVisitValidFrom = dischargeMsg.getDischargeDateTime();
        } else {
            locationVisitStartTime = admissionDateTime;
            locationVisitValidFrom = admissionDateTime;
        }
    }

    /* (non-Javadoc)
     * @see uk.ac.ucl.rits.inform.datasinks.emapstar.AdtOperationInterface#getCreateEncounterOrVisit(uk.ac.ucl.rits.inform.datasinks.emapstar.InformDbOperations, uk.ac.ucl.rits.inform.interchange.AdtMessage, java.time.Instant)
     */
    @Override
    public void getCreateEncounterOrVisit(InformDbOperations dbOps, OldAdtMessage adtMsg, Instant storedFrom)
            throws MessageIgnoredException {
        if (adtMsg.getVisitNumber() != null) {
            encounter = OldAdtOperation.getCreateEncounter(adtMsg.getMrn(), adtMsg.getVisitNumber(), storedFrom, admissionDateTime, dbOps);
            onlyOpenBedVisit = InformDbOperations.getOnlyElement(InformDbOperations.getOpenValidLocationVisit(encounter));
        } else if (!adtMsg.getOperationType().equals(AdtOperationType.MERGE_BY_ID)) {
            // CSNs are not present in merge by ID messages, but in other messages this is an error
            throw new MessageIgnoredException(adtMsg, "CSN missing in a non-merge message: " + adtMsg.getOperationType());
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
    static OldMrn getCreateMrn(String mrnStr, Instant validFrom, Instant storedFrom, boolean createIfNotExist, InformDbOperations dbOps) {
        if (createIfNotExist && (storedFrom == null || validFrom == null || mrnStr == null || mrnStr.isEmpty())) {
            throw new IllegalArgumentException(String.format(
                    "if createIfNotExist, storedFrom (%s) and validFrom (%s) and mrnStr (%s) must be non-null", storedFrom, validFrom, mrnStr));
        }
        OldMrn mrn = dbOps.findByMrnString(mrnStr);
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
            mrn = new OldMrn();
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
     * @param mrnStr       the MRN string to find/create
     * @param encounterStr encounter ID (visit ID) to find/create
     * @param storedFrom   storedFrom time to use for newly created records - should be a time very close to the present
     * @param validFrom    validFrom times to use for newly created records - usually the admission time
     * @param dbOps        the db ops service
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
            OldMrn newOrExistingMrn = getCreateMrn(mrnStr, validFrom, storedFrom, true, dbOps);
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
                    .filter(mrn -> mrn.getOldMrn().getMrn().equals(mrnStr)).collect(Collectors.toList()));
            if (mrnMatching == null) {
                infoStr = String.format("MRN %s was not associated with this encounter", mrnStr);
            } else {
                infoStr = String.format("MRN %s is associated with this encounter (valid = %s)", mrnStr, mrnMatching.isValid());
            }
            logger.info(String.format("getCreateEncounter RETURNING EXISTING encounter %s (%s)", existingEnc.getEncounter(), infoStr));
            return existingEnc;
        }
    }

    /**
     * Add a new open bed/outpatient/ED ("location") visit to an existing
     * higher-level (hospital) visit.
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
    static void addOpenLocationVisit(Encounter enc, OldAttributeKeyMap visitType, Instant storedFrom, Instant validFrom,
                                     Instant visitBeginTime, PatientFact hospitalVisit, String currentLocation, String patientClass) {
        PatientFact visitFact = new PatientFact();
        visitFact.setStoredFrom(storedFrom);
        visitFact.setValidFrom(validFrom);
        visitFact.setFactType(InformDbOperations.getCreateAttribute(visitType));

        // Sometimes it's impossible to determine when a bed visit started eg - the
        // first ADT message we receive for an encounter was a discharge message, so
        // visitBeginTime can be null
        visitFact.addProperty(
                InformDbOperations.buildPatientProperty(storedFrom, validFrom, OldAttributeKeyMap.ARRIVAL_TIME, visitBeginTime));
        visitFact.addProperty(
                InformDbOperations.buildPatientProperty(storedFrom, validFrom, OldAttributeKeyMap.LOCATION, currentLocation));

        hospitalVisit.addChildFact(visitFact);
        enc.addFact(visitFact);
    }

    /**
     * Mark a Visit as finished, which can happen either when transferring or
     * discharging a patient.
     * This could use buildPatientProperty rather than being deprecated?
     * @param visit             the visit to mark as finished
     * @param dischargeDateTime the discharge/transfer time
     * @param storedFrom        storedFrom value to use for new records
     */
    @Deprecated
    static void addDischargeToVisit(PatientFact visit, Instant dischargeDateTime, Instant storedFrom) {
        OldAttribute dischargeTime = InformDbOperations.getCreateAttribute(OldAttributeKeyMap.DISCHARGE_TIME);
        PatientProperty visProp = new PatientProperty();
        visProp.setValidFrom(dischargeDateTime);
        visProp.setStoredFrom(storedFrom);
        visProp.setValueAsDatetime(dischargeDateTime);
        visProp.setPropertyType(dischargeTime);
        visit.addProperty(visProp);
    }


    /* (non-Javadoc)
     * @see uk.ac.ucl.rits.inform.datasinks.emapstar.AdtOperationInterface#ensureAdmissionExists()
     */
    @Override
    public boolean ensureAdmissionExists() throws MessageIgnoredException {
        // This perhaps belongs in a getCreateHospitalVisit method, with an
        // InformDbDataIntegrity exception
        if (onlyOpenBedVisit == null) {
            PatientFact hospitalVisit = InformDbOperations.addOpenHospitalVisit(encounter, storedFrom, admissionDateTime, adtMsg.getPatientClass());
            // create a new location visit with the new (or updated) location
            OldAttributeKeyMap visitType = InformDbOperations.visitTypeFromPatientClass(adtMsg.getPatientClass());
            OldAdtOperation.addOpenLocationVisit(encounter, visitType, storedFrom, locationVisitValidFrom, locationVisitStartTime, hospitalVisit,
                    adtMsg.getFullLocationString(), adtMsg.getPatientClass());
            encounter = dbOps.save(encounter);
            logger.info(String.format("Encounter: %s", encounter.toString()));
            onlyOpenBedVisit = InformDbOperations.getOnlyElement(InformDbOperations.getOpenValidLocationVisit(encounter));
            return true;
        }
        return false;
    }

    /**
     * Ensure that the hospital visit's patient class is up to date.
     * @return true iff anything was actually updated
     * @throws MessageIgnoredException probably shouldn't do this...
     */
    private boolean ensurePatientClass() throws MessageIgnoredException {
        if (onlyOpenBedVisit == null) {
            throw new RuntimeException("ensureLocationAndClass: onlyOpenBedVisit == null");
        }
        /*
         * Detect when location has changed and perform a transfer. If there isn't an
         * open location then just do nothing. Used to throw an exception but this isn't
         * really an error and we still want the demographics to update above.
         */
        return dbOps.addOrUpdateProperty(onlyOpenBedVisit.getParentFact(),
                InformDbOperations.buildPatientProperty(storedFrom, transferOccurred, OldAttributeKeyMap.PATIENT_CLASS,
                        adtMsg.getPatientClass()));
    }

    /* (non-Javadoc)
     * @see uk.ac.ucl.rits.inform.datasinks.emapstar.AdtOperationInterface#ensureLocation()
     */
    @Override
    public boolean ensureLocation() throws MessageIgnoredException {
        if (onlyOpenBedVisit == null) {
            throw new RuntimeException("ensureLocation: onlyOpenBedVisit == null");
        }
        String currentKnownLocation = InformDbOperations
                .getOnlyElement(onlyOpenBedVisit.getPropertyByAttribute(OldAttributeKeyMap.LOCATION, PatientProperty::isValid))
                .getValueAsString();
        if (!newTransferLocation.equals(currentKnownLocation)) {
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
            PatientFact hospVisit = onlyOpenBedVisit.getParentFact();
            // link the bed visit to the parent (hospital) visit
            OldAttributeKeyMap visitType = InformDbOperations.visitTypeFromPatientClass(adtMsg.getPatientClass());
            addOpenLocationVisit(encounter, visitType, storedFrom, transferOccurred, transferOccurred, hospVisit,
                    newTransferLocation, adtMsg.getPatientClass());
            return true;
        }
        return false;
    }

    /* (non-Javadoc)
     * @see uk.ac.ucl.rits.inform.datasinks.emapstar.AdtOperationInterface#performAdmit()
     */
    @Override
    public void performAdmit() throws MessageIgnoredException {
        ensureAdmissionExists();
        InformDbOperations.addOrUpdateDemographics(encounter, adtMsg, storedFrom);
        ensurePatientClass();
        ensureLocation();
    }

    /* (non-Javadoc)
     * @see uk.ac.ucl.rits.inform.datasinks.emapstar.AdtOperationInterface#performTransfer()
     */
    @Override
    public void performTransfer() throws MessageIgnoredException {
        boolean anyChanges = false;
        anyChanges |= ensureAdmissionExists();
        anyChanges |= InformDbOperations.addOrUpdateDemographics(encounter, adtMsg, storedFrom);
        anyChanges |= ensurePatientClass();
        anyChanges |= ensureLocation();
        if (!anyChanges) {
            String err = String.format(
                    "REDUNDANT transfer: location (%s), patient class (%s), demographics and death status have not changed",
                    newTransferLocation, adtMsg.getPatientClass());
            logger.warn(err);
            throw new MessageIgnoredException(adtMsg, err);
        }
    }

    /* (non-Javadoc)
     * @see uk.ac.ucl.rits.inform.datasinks.emapstar.AdtOperationInterface#performDischarge()
     */
    @Override
    public void performDischarge() throws MessageIgnoredException {
        // If visit was not known about, admit the patient first before going on to discharge
        // It's not possible to tell when to start the bed visit from.
        ensureAdmissionExists();

        logger.info(String.format("DISCHARGE: MRN %s, visit %s, eventoccurred %s, dischargetime %s", dischargeMsg.getMrn(),
                dischargeMsg.getVisitNumber(), dischargeMsg.getEventOccurredDateTime(), dischargeDateTime));
        if (dischargeDateTime == null) {
            throw new MessageIgnoredException(dischargeMsg, "Trying to discharge but the discharge date is null");
        } else {
            // Discharge from the bed visit and the hospital visit
            OldAdtOperation.addDischargeToVisit(onlyOpenBedVisit, dischargeDateTime, storedFrom);
            PatientFact hospVisit = onlyOpenBedVisit.getParentFact();
            OldAdtOperation.addDischargeToVisit(hospVisit, dischargeDateTime, storedFrom);

            String dischargeDisposition = dischargeMsg.getDischargeDisposition();
            // Add discharge disposition to hospital visit only, not bed.
            hospVisit.addProperty(InformDbOperations.buildPatientProperty(storedFrom, dischargeDateTime,
                    OldAttributeKeyMap.DISCHARGE_DISPOSITION, dischargeDisposition));
            String dischargeLocation = dischargeMsg.getDischargeLocation();
            hospVisit.addProperty(InformDbOperations.buildPatientProperty(storedFrom, dischargeDateTime,
                    OldAttributeKeyMap.DISCHARGE_LOCATION, dischargeLocation));
            // demographics may have changed
            InformDbOperations.addOrUpdateDemographics(encounter, dischargeMsg, storedFrom);
        }
    }

    /* (non-Javadoc)
     * @see uk.ac.ucl.rits.inform.datasinks.emapstar.AdtOperationInterface#performCancelAdmit()
     */
    @Override
    public void performCancelAdmit() throws MessageIgnoredException {
        InformDbOperations.addOrUpdateDemographics(encounter, adtMsg, storedFrom);

        ensureAdmissionExists();

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
        onlyOpenBedVisit.invalidateAll(storedFrom, cancellationTime);
        hospVisit.invalidateAll(storedFrom, cancellationTime);
        for (PatientFact closedBedVisit : closedBedVisits) {
            closedBedVisit.invalidateAll(storedFrom, cancellationTime);
        }
    }

    /* (non-Javadoc)
     * @see uk.ac.ucl.rits.inform.datasinks.emapstar.AdtOperationInterface#performCancelTransfer()
     */
    @Override
    public void performCancelTransfer() throws MessageIgnoredException {
        Instant cancellationDateTime = adtMsg.getRecordedDateTime();
        // the new location, which is the location before the erroneous transfer was made
        String newCorrectLocation = adtMsg.getFullLocationString();
        // the transfer time of the transfer being cancelled, NOT the cancellation time
        Instant originalTransferDateTime = adtMsg.getEventOccurredDateTime();

        InformDbOperations.addOrUpdateDemographics(encounter, adtMsg, storedFrom);

        // If visit was not known about, admit the patient first.
        // We now have their current location and can stop.
        // (Don't go so far as to create their cancelled bed visit and
        // then invalidate it).
        if (ensureAdmissionExists()) {
            return;
        }

        PatientFact hospVisit = onlyOpenBedVisit.getParentFact();
        // invalidate the erroneous transfer
        onlyOpenBedVisit.invalidateAll(storedFrom, cancellationDateTime);

        // reopen the previous bed visit by invalidating its discharge time property
        Optional<PatientFact> mostRecentBedVisitOptional = InformDbOperations.getVisitFactWhere(encounter,
                vf -> OldAttributeKeyMap.isLocationVisitType(vf.getFactType()) && vf.isValid()).stream()
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
                    mostRecentBedVisit.getPropertyByAttribute(OldAttributeKeyMap.DISCHARGE_TIME, PatientProperty::isValid));
            bedDischargeTime.invalidateProperty(storedFrom, cancellationDateTime, null);
        } else {
            /*
             * If there is no previous location (this situation should only happen if we've
             * come in mid-stream), then re-create the previous location visit as best we
             * can: We can't know when it started, so set arrival time as null and use the
             * point where it ended as the valid_from.
             */
            OldAttributeKeyMap visitType = InformDbOperations.visitTypeFromPatientClass(adtMsg.getPatientClass());
            OldAdtOperation.addOpenLocationVisit(encounter, visitType, storedFrom, originalTransferDateTime, null, hospVisit,
                    newCorrectLocation, adtMsg.getPatientClass());
        }
    }

    /* (non-Javadoc)
     * @see uk.ac.ucl.rits.inform.datasinks.emapstar.AdtOperationInterface#performCancelDischarge()
     */
    @Override
    public void performCancelDischarge() throws MessageIgnoredException {
        // Event occurred field contains the original time of the discharge that is being cancelled,
        // so use the event recorded date for when the cancellation happened. (Zero length
        // time intervals technically mean the row was never valid).
        Instant invalidationDate = adtMsg.getRecordedDateTime();
        // this must be non-null for the invalidation - all A13 messages seen so far do have this field
        if (invalidationDate == null) {
            throw new MessageIgnoredException(adtMsg, "Trying to cancel discharge but the event recorded date is null");
        }
        InformDbOperations.addOrUpdateDemographics(encounter, adtMsg, storedFrom);

        // Get the most recent bed visit, open or closed. It will hopefully be closed.
        Optional<PatientFact> mostRecentBedVisitOptional = InformDbOperations.getVisitFactWhere(encounter,
                vf -> OldAttributeKeyMap.isLocationVisitType(vf.getFactType()) && vf.isValid()).stream()
                .max((vf1, vf2) -> InformDbOperations.sortVisitByDischargeTime(vf1, vf2));

        if (!mostRecentBedVisitOptional.isPresent()) {
            // If we have no existing visit, admit the patient.
            // We now have their current location and can stop.
            // (Don't go so far as to create the discharge and
            // then invalidate it).
            ensureAdmissionExists();
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
                mostRecentBedVisit.getPropertyByAttribute(OldAttributeKeyMap.DISCHARGE_TIME, PatientProperty::isValid));
        bedDischargeTime.invalidateProperty(storedFrom, invalidationDate, null);
        PatientFact hospitalVisit = mostRecentBedVisit.getParentFact();
        for (OldAttributeKeyMap a : Arrays.asList(OldAttributeKeyMap.DISCHARGE_TIME, OldAttributeKeyMap.DISCHARGE_DISPOSITION,
                OldAttributeKeyMap.DISCHARGE_LOCATION)) {
            PatientProperty prop = InformDbOperations.getOnlyElement(hospitalVisit.getPropertyByAttribute(a, PatientProperty::isValid));
            if (prop != null) {
                prop.invalidateProperty(storedFrom, invalidationDate, null);
            }
        }

        // The Epic spec for receiving an A13 says you can be put in a different place than the last one you were in,
        // ie. an implicit transfer. Does this ever happen for messages that Epic emits? Currently ignoring
        // the location field.

        mostRecentBedVisit = dbOps.save(mostRecentBedVisit);
        hospitalVisit = dbOps.save(hospitalVisit);
    }

    /* (non-Javadoc)
     * @see uk.ac.ucl.rits.inform.datasinks.emapstar.AdtOperationInterface#performMergeById()
     */
    @Override
    public void performMergeById() throws MessageIgnoredException {
        String retiredMrnStr = mergeMsg.getMergedPatientId();
        String survivingMrnStr = mergeMsg.getMrn();
        Instant mergeTime = mergeMsg.getRecordedDateTime();
        logger.info(String.format("MERGE: surviving mrn %s, retiredMrn = %s, merge time = %s", survivingMrnStr, retiredMrnStr,
                mergeTime));
        if (mergeTime == null) {
            throw new MessageIgnoredException(mergeMsg, "event occurred null");
        }

        // The non-surviving Mrn is invalidated but still points to the old person
        // (we are recording the fact that between these dates, the hospital believed
        // that the mrn belonged to this person)
        OldMrn retiredMrn = getCreateMrn(retiredMrnStr, mergeTime, storedFrom, true, dbOps);
        OldMrn survivingMrn = getCreateMrn(survivingMrnStr, mergeTime, storedFrom, true, dbOps);
        if (survivingMrn == null || retiredMrn == null) {
            throw new MessageIgnoredException(mergeMsg, String.format("MRNs %s or %s (%s or %s) are not previously known, do nothing",
                    retiredMrnStr, survivingMrnStr, retiredMrn, survivingMrn));
        }
        PersonMrn retiredPersonMrn = InformDbOperations.getOnlyElementWhere(retiredMrn.getPersons(), pm -> pm.isValid());
        PersonMrn survivingPersonMrn = InformDbOperations.getOnlyElementWhere(survivingMrn.getPersons(), pm -> pm.isValid());
        if (survivingPersonMrn == null || retiredPersonMrn == null) {
            throw new MessageIgnoredException(mergeMsg, String.format(
                    "MRNs %s and %s exist but there was no currently valid person for one/both of them (%s and %s)",
                    retiredMrnStr, survivingMrnStr, retiredPersonMrn, survivingPersonMrn));
        }

        // If we already thought they were the same person, do nothing further.
        if (retiredPersonMrn.getPerson().equals(survivingPersonMrn.getPerson())) {
            throw new MessageIgnoredException(mergeMsg,
                    String.format("We already thought that MRNs %s and %s were the same person (%s)", retiredMrnStr,
                            survivingMrnStr, retiredPersonMrn.getPerson().getPersonId()));
        }

        survivingPersonMrn.setLive(true);

        // Invalidate the old person<->mrn association
        PersonMrn retiredInvalidPersonMrn = retiredPersonMrn.invalidate(storedFrom, mergeTime);
        retiredInvalidPersonMrn = dbOps.save(retiredInvalidPersonMrn);

        // Create a new person<->mrn association that tells us that as of the merge time
        // the old MRN is believed to belong to the person associated with the surviving MRN
        Person survivingPerson = survivingPersonMrn.getPerson();
        PersonMrn survivingPersonRetiredMrn = new PersonMrn(survivingPerson, retiredMrn);
        survivingPersonRetiredMrn.setStoredFrom(storedFrom);
        survivingPersonRetiredMrn.setValidFrom(mergeTime);
        survivingPersonRetiredMrn.setLive(false);
        survivingPerson.linkMrn(survivingPersonRetiredMrn);
        retiredMrn.linkPerson(survivingPersonRetiredMrn);

        survivingPersonRetiredMrn = dbOps.save(survivingPersonRetiredMrn);
        retiredPersonMrn = dbOps.save(retiredPersonMrn);
    }
}
