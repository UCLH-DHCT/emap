package uk.ac.ucl.rits.inform.datasinks.emapstar;

import java.time.Instant;
import java.util.List;
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

    /**
     * @return the encounter that is being manipulated
     */
    public Encounter getEncounter() {
        return encounter;
    }

    private Instant storedFrom;

    private Instant admissionDateTime;

    private Instant recordedDateTime;

    private Instant transferOccurred;

    private Instant locationVisitStartTime;

    private Instant locationVisitValidFrom;


    public AdtOperation(InformDbOperations dbOps, AdtMessage adtMsg, Instant storedFrom) throws MessageIgnoredException {
        this.adtMsg = adtMsg;
        this.dbOps = dbOps;
        this.storedFrom = storedFrom;
        determineTimestamps();
        encounter = AdtOperation.getCreateEncounter(adtMsg.getMrn(), adtMsg.getVisitNumber(), storedFrom, admissionDateTime, dbOps);

    }

    /**
     * Use the AdtOperationType to determine which fields are correct ones to use.
     * Different messages types have different fields that are present/absent, and
     * they will sometimes have different meanings from message type to message type.
     */
    private void determineTimestamps() {
        admissionDateTime = adtMsg.getAdmissionDateTime();
        recordedDateTime = adtMsg.getRecordedDateTime();

        // Transfers can be inferred from non-transfer messages, but
        // different fields will indicate the transfer time.
        switch (adtMsg.getOperationType()) {
        case ADMIT_PATIENT:
            transferOccurred = adtMsg.getEventOccurredDateTime();
            break;
        case TRANSFER_PATIENT:
            transferOccurred = adtMsg.getEventOccurredDateTime();
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
     * @param personRep 
     * @param mrnRep 
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
     * @param encRepo 
     * @return the Encounter, existing or newly created
     * @throws MessageIgnoredException if message can't be processed
     */
    static Encounter getCreateEncounter(String mrnStr, String encounterStr, Instant storedFrom, Instant validFrom, InformDbOperations dbOps) throws MessageIgnoredException {
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


    public void performAdmit() throws MessageIgnoredException {
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
    }

    public void performUpdateInfo() throws MessageIgnoredException {
        String newLocation = adtMsg.getFullLocationString();
        InformDbOperations.addOrUpdateDemographics(encounter, adtMsg, storedFrom);

        /*
         * Detect when location has changed and perform a transfer. If there isn't an
         * open location then just do nothing. Used to throw an exception but this isn't
         * really an error and we still want the demographics to update above.
         */
        List<PatientFact> latestOpenLocationVisits = InformDbOperations.getOpenValidLocationVisit(encounter);
        PatientFact onlyOpenLocationVisit = InformDbOperations.getOnlyElement(latestOpenLocationVisits);
        if (onlyOpenLocationVisit != null) {
            PatientProperty knownlocation =
                    InformDbOperations.getOnlyElement(onlyOpenLocationVisit.getPropertyByAttribute(AttributeKeyMap.LOCATION, p -> p.isValid()));
            if (!newLocation.equals(knownlocation.getValueAsString())) {
                logger.warn(String.format("[mrn %s, visit num %s] IMPLICIT TRANSFER IN message of type (%s): |%s| -> |%s|", adtMsg.getMrn(),
                        adtMsg.getVisitNumber(), adtMsg.getOperationType(), knownlocation.getValueAsString(), newLocation));
                performTransfer();
            }
        }

    }

    public void performTransfer() throws MessageIgnoredException {
        List<PatientFact> latestOpenBedVisits = InformDbOperations.getOpenValidLocationVisit(encounter);
        if (latestOpenBedVisits.isEmpty()) {
            logger.warn("Received transfer for patient we didn't know was admitted - admitting them instead");
            performAdmit();
            return;
        }
        
        PatientFact latestOpenBedVisit = latestOpenBedVisits.get(0);
        String newTransferLocation = adtMsg.getFullLocationString();
        String currentKnownLocation =
                InformDbOperations.getOnlyElement(latestOpenBedVisit.getPropertyByAttribute(AttributeKeyMap.LOCATION, p -> p.isValid())).getValueAsString();
        if (newTransferLocation.equals(currentKnownLocation)) {
            // If we get an A02 with a new location that matches where we already thought
            // the patient was, don't perform an actual transfer.
            // In the test data, this sometimes happens following an A08 implied transfer.
            // Also, even if the location hasn't changed the patient class could have changed
            // (should be made explicit as an A06 or A07 but we don't distinguish A02/A06/A07 here).
            PatientProperty currentPatientClass = InformDbOperations.getOnlyElement(
                    latestOpenBedVisit.getPropertyByAttribute(AttributeKeyMap.PATIENT_CLASS, p -> p.isValid()));
            if (adtMsg.getPatientClass().equals(currentPatientClass.getValueAsString())) {
                String err = String.format("REDUNDANT transfer, location (%s) and patient class (%s) have not changed",
                        currentKnownLocation, currentPatientClass.getValueAsString());
                logger.warn(err);
                throw new MessageIgnoredException(adtMsg, err);
            } else {
                // Only patient class has changed, so update just that without creating a new location visit
                currentPatientClass.setValidUntil(transferOccurred);
                latestOpenBedVisit.addProperty(
                        InformDbOperations.buildPatientProperty(storedFrom, transferOccurred, AttributeKeyMap.PATIENT_CLASS, adtMsg.getPatientClass()));
            }
        } else {
            // locations have changed, do a "normal" transfer, patient class will get done as part of this
            addDischargeToVisit(latestOpenBedVisit, transferOccurred, storedFrom);
            String admitSource = adtMsg.getAdmitSource();
            logger.info(String.format(
                    "TRANSFERRING: MRN = %s, admitdatetime %s, admitsrc %s, eventOccurred %s, recorded %s",
                    adtMsg.getMrn(), admissionDateTime, admitSource, transferOccurred, recordedDateTime));
            // add a new visit to the current encounter
            Encounter encounterDoubleCheck = latestOpenBedVisit.getEncounter();
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
        performUpdateInfo();
    }
}
