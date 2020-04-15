package uk.ac.ucl.rits.inform.datasinks.emapstar;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.AttributeError;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.DuplicateValueException;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.EmapStarIntegrityException;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.InvalidMrnException;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.MessageIgnoredException;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.AttributeRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.EncounterRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.MrnRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.PatientFactRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.PersonMrnRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.PersonRepository;
import uk.ac.ucl.rits.inform.informdb.Attribute;
import uk.ac.ucl.rits.inform.informdb.AttributeKeyMap;
import uk.ac.ucl.rits.inform.informdb.Encounter;
import uk.ac.ucl.rits.inform.informdb.Mrn;
import uk.ac.ucl.rits.inform.informdb.MrnEncounter;
import uk.ac.ucl.rits.inform.informdb.PatientFact;
import uk.ac.ucl.rits.inform.informdb.PatientProperty;
import uk.ac.ucl.rits.inform.informdb.Person;
import uk.ac.ucl.rits.inform.informdb.PersonMrn;
import uk.ac.ucl.rits.inform.informdb.ResultType;
import uk.ac.ucl.rits.inform.informdb.TemporalCore;
import uk.ac.ucl.rits.inform.interchange.AdtMessage;
import uk.ac.ucl.rits.inform.interchange.AdtOperationType;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessor;
import uk.ac.ucl.rits.inform.interchange.PathologyOrder;
import uk.ac.ucl.rits.inform.interchange.PathologyResult;
import uk.ac.ucl.rits.inform.interchange.VitalSigns;

/**
 * All the operations that can be performed on Inform-db.
 */
@Component
@EntityScan({ "uk.ac.ucl.rits.inform.datasinks.emapstar.repos", "uk.ac.ucl.rits.inform.informdb" })
public class InformDbOperations implements EmapOperationMessageProcessor {
    @Autowired
    private AttributeRepository        attributeRepo;
    @Autowired
    private PersonRepository           personRepo;
    @Autowired
    private MrnRepository              mrnRepo;
    @Autowired
    private EncounterRepository        encounterRepo;
    @Autowired
    private PatientFactRepository      patientFactRepository;
    @Autowired
    private PersonMrnRepository        personMrnRepo;

    private static final Logger        logger = LoggerFactory.getLogger(InformDbOperations.class);

    @Value("${:classpath:vocab.csv}")
    private Resource                   vocabFile;

    /**
     * Call when you are finished with this object.
     */
    public void close() {}

    /**
     * Load in attributes (vocab) from CSV file, if they don't already exist in DB.
     */
    @Transactional
    public void ensureVocabLoaded() {
        logger.info("Loading vocab from csv");
        try (Reader in = new InputStreamReader(this.vocabFile.getInputStream())) {
            Iterable<CSVRecord> records = CSVFormat.RFC4180.withFirstRecordAsHeader().parse(in);
            for (CSVRecord record : records) {
                long attributeId = Long.parseLong(record.get("attribute_id"));
                logger.trace("Testing " + attributeId);
                Attribute newAttr = new Attribute();
                newAttr.setAttributeId(attributeId);
                String shortname = record.get("short_name");
                newAttr.setShortName(shortname);
                String description = record.get("description");
                newAttr.setDescription(description);
                String resultType = record.get("result_type");
                newAttr.setResultType(ResultType.valueOf(resultType));
                String validFrom = record.get("valid_from");
                newAttr.setValidFrom(Instant.parse(validFrom));
                String validUntil = record.get("valid_until");
                if (!validUntil.isEmpty()) {
                    newAttr.setValidUntil(Instant.parse(validUntil));
                }
                Optional<Attribute> findExistingAttr = attributeRepo.findByAttributeId(attributeId);
                if (findExistingAttr.isPresent()) {
                    // If there is pre-existing data check everything matches
                    Attribute existingAttr = findExistingAttr.get();
                    if (!existingAttr.getShortName().equals(newAttr.getShortName())) {
                        throw new AttributeError(
                                String.format("Attribute id %d: Short name for attribute has changed from %s to %s",
                                        attributeId, existingAttr.getShortName(), newAttr.getShortName()));
                    }
                    if (!existingAttr.getResultType().equals(newAttr.getResultType())) {
                        throw new AttributeError(
                                String.format("Attribute id %d: Result type for attribute has changed from %s to %s",
                                        attributeId, existingAttr.getResultType(), newAttr.getResultType()));
                    }
                } else {
                    newAttr = attributeRepo.save(newAttr);
                }
            }
        } catch (IOException e) {
            logger.error(e.toString());
            throw new AttributeError("Failed to load vocab file: " + this.vocabFile.getFilename());
        }
        logger.info("Done loading vocab from csv");
    }

    /**
     * Process a pathology order message.
     * @param pathologyOrder the message
     * @return the return code
     * @throws EmapOperationMessageProcessingException if message could not be processed
     */
    @Override
    @Transactional
    public String processMessage(PathologyOrder pathologyOrder) throws EmapOperationMessageProcessingException {
        Instant storedFrom = Instant.now();
        addOrUpdatePathologyOrder(pathologyOrder, storedFrom);
        // be more specific about the type of OK in future
        return "OK";
    }

    /**
     * Process a patient movement (ADT) message.
     * @param adtMsg the message
     * @throws EmapOperationMessageProcessingException if message could not be processed
     */
    @Override
    @Transactional
    public String processMessage(AdtMessage adtMsg) throws EmapOperationMessageProcessingException {
        String returnCode;
        returnCode = "OK";
        Instant storedFrom = Instant.now();
        switch (adtMsg.getOperationType()) {
        case ADMIT_PATIENT:
            admitPatient(adtMsg, storedFrom);
            break;
        case TRANSFER_PATIENT:
            transferPatient(adtMsg, storedFrom);
            break;
        case DISCHARGE_PATIENT:
            dischargePatient(adtMsg, storedFrom);
            break;
        case UPDATE_PATIENT_INFO:
            updatePatientInfo(adtMsg, storedFrom);
            break;
        case CANCEL_ADMIT_PATIENT:
            cancelAdmitPatient(adtMsg, storedFrom);
            break;
        case CANCEL_DISCHARGE_PATIENT:
            cancelDischargePatient(adtMsg, storedFrom);
            break;
        case MERGE_BY_ID:
            mergeById(adtMsg, storedFrom);
            break;
        default:
            returnCode = "Not implemented";
            logger.error(returnCode);
            break;
        }
        return returnCode;
    }

    @Override
    @Transactional
    public String processMessage(VitalSigns msg) throws EmapOperationMessageProcessingException {
        String returnCode = "OK";
        String visitNumber = msg.getVisitNumber();
        String mrnStr = msg.getMrn();
        Instant storedFrom = Instant.now();
        Instant observationTime = msg.getObservationTimeTaken();
        Encounter enc = getCreateEncounter(mrnStr, visitNumber, storedFrom, observationTime);

        PatientFact vitalSign = new PatientFact();
        vitalSign.setFactType(getCreateAttribute(AttributeKeyMap.VITAL_SIGN));
        vitalSign.setValidFrom(observationTime);
        vitalSign.setStoredFrom(storedFrom);

        vitalSign.addProperty(buildPatientProperty(storedFrom, observationTime,
                AttributeKeyMap.VITAL_SIGNS_OBSERVATION_IDENTIFIER, msg.getVitalSignIdentifier()));
        vitalSign.addProperty(buildPatientProperty(storedFrom, observationTime,
                AttributeKeyMap.VITAL_SIGNS_UNIT, msg.getUnit()));
        if (msg.getStringValue() != null) {
            vitalSign.addProperty(buildPatientProperty(storedFrom, observationTime,
                    AttributeKeyMap.VITAL_SIGNS_STRING_VALUE, msg.getStringValue()));
        }
        if (msg.getNumericValue() != null) {
            vitalSign.addProperty(buildPatientProperty(storedFrom, observationTime,
                    AttributeKeyMap.VITAL_SIGNS_NUMERIC_VALUE, msg.getNumericValue()));
        }
        vitalSign.addProperty(buildPatientProperty(storedFrom, observationTime,
                AttributeKeyMap.VITAL_SIGNS_OBSERVATION_TIME, msg.getObservationTimeTaken()));

        enc.addFact(vitalSign);
        enc = encounterRepo.save(enc);
        return returnCode;
    }

    /**
     * Search for encounters in the Mrn with a given encounter number.
     *
     * @param mrn       the Mrn to search
     * @param encounter the encounter ID to search for
     * @return all encounters that match
     */
    private List<Encounter> getEncounterWhere(Mrn mrn, String encounter) {
        List<MrnEncounter> existingMrnEncs = mrn.getEncounters();
        if (existingMrnEncs == null) {
            return null;
        }
        List<Encounter> matchingEncs = existingMrnEncs.stream().map(mrnE -> mrnE.getEncounter())
                .filter(e -> encounter.equals(e.getEncounter())).collect(Collectors.toList());
        return matchingEncs;
    }

    /**
     * All patient facts are in one table so some filtering is needed.
     *
     * @param pf the patient fact
     * @return whether it is a visit fact (ie. what used to be the VisitFact class)
     */
    private static boolean factIsVisitFact(PatientFact pf) {
        return pf.isOfType(AttributeKeyMap.HOSPITAL_VISIT)
                || AttributeKeyMap.isLocationVisitType(pf.getFactType());
    }

    /**
     * Filter for facts that are related to pathology.
     *
     * @param pf the patient fact
     * @return true if this is a pathology fact
     */
    private static boolean factIsPathFact(PatientFact pf) {
        String shortName = pf.getFactType().getShortName();
        // The only (current) use for this is to define demographic facts in terms of what they are not.
        // Identifying demographic facts positively would be a better approach.
        return shortName.startsWith("PATH_");
    }

    /**
     * @param encounter the Encounter to search in
     * @param pred      the predicate to check against each visit fact
     * @return all PatientFact objects in encounter which are visit facts AND match
     *         predicate pred
     */
    @Transactional
    private List<PatientFact> getVisitFactWhere(Encounter encounter, Predicate<? super PatientFact> pred) {
        return getFactWhere(encounter, f -> factIsVisitFact(f) && pred.test(f));
    }

    /**
     * @param encounter the Encounter to search in
     * @param pred      the predicate to check for each PatientFact
     * @return all PatientFact objects in encounter which match predicate pred
     */
    @Transactional
    private List<PatientFact> getFactWhere(Encounter encounter, Predicate<? super PatientFact> pred) {
        return getFactWhere(encounter.getFacts(), pred);
    }

    /**
     * @param encounter the Encounter to search in
     * @return all PatientFact objects in encounter which match predicate pred
     */
    @Transactional
    private List<PatientFact> getOpenValidLocationVisit(Encounter encounter) {
        return getFactWhere(encounter.getFacts(),
                f -> visitFactIsOpenAndValid(f) && AttributeKeyMap.isLocationVisitType(f.getFactType()));
    }

    /**
     * @param facts the list of facts to search in
     * @param pred      the predicate to check for each PatientFact
     * @return all PatientFact objects which match predicate pred
     */
    @Transactional
    private List<PatientFact> getFactWhere(List<PatientFact> facts, Predicate<? super PatientFact> pred) {
        if (facts == null) {
            return new ArrayList<PatientFact>();
        }
        List<PatientFact> matchingFacts = facts.stream().filter(pred).collect(Collectors.toList());
        return matchingFacts;
    }

    /**
     * @param encounter the Encounter to search in
     * @return all PatientFact objects in encounter which are NOT visit facts and
     *         are valid and stored as of the present moment
     */
    private List<PatientFact> getValidStoredDemographicFacts(Encounter encounter) {
        return getDemographicFactsWhere(encounter, f -> f.isValid() && factIsStored(f));
    }

    /**
     * @param encounter the Encounter to search in
     * @param pred      the predicate to check against each demographic fact
     * @return all PatientFact objects in encounter which are NOT visit facts and
     *         match pred
     */
    private List<PatientFact> getDemographicFactsWhere(Encounter encounter, Predicate<? super PatientFact> pred) {
        /*
         * Currently we assume that all non-visit facts are demographic facts, but we
         * are going to need some richer type information for Attributes to do this
         * properly.
         */
        return getFactWhere(encounter, f -> !factIsVisitFact(f) && !factIsPathFact(f) && pred.test(f));
    }

    /**
     * Check whether PatientFact has no discharge time property, indicating it's
     * still open.
     *
     * @param vf the visit fact to check
     * @return whether visit is still open (ie. not discharged)
     */
    private boolean visitFactIsOpen(PatientFact vf) {
        PatientProperty validDischargeTime = getOnlyElement(vf.getPropertyByAttribute(AttributeKeyMap.DISCHARGE_TIME).stream()
                .filter(p -> p.isValid()).collect(Collectors.toList()));
        return validDischargeTime == null;
    }

    /**
     * Check whether PatientFact is still open, and its valid until column is null, indicating that it has never
     * been invalidated.
     *
     * @param vf the visit fact to check
     * @return whether visit is still open and valid as of the present moment
     */
    private boolean visitFactIsOpenAndValid(PatientFact vf) {
        return visitFactIsOpen(vf) && vf.isValid();
    }

    /**
     * Check whether PatientFact's stored_until column is null, indicating that it
     * has not been unstored (deleted). Note: this does not perform time travel (ie.
     * check whether stored_until is null or in the future) Note: the storedness of the
     * underlying properties is not checked
     *
     * @param pf the patient fact to check
     * @return whether fact is stored as of the present moment
     */
    private boolean factIsStored(TemporalCore pf) {
        Instant storedUntil = pf.getStoredUntil();
        return storedUntil == null;
    }

    /**
     * Get all PatientFact objects on this encounter with the given visit type, or
     * null if none.
     *
     * @param encounter where to look for PatientFact objects
     * @param attr      the visit type (as an attribute)
     * @return all PatientFact objects of the specified type
     */
    private List<PatientFact> getVisitFactWhereVisitType(Encounter encounter, AttributeKeyMap attr) {
        return getVisitFactWhere(encounter, vf -> vf.isOfType(attr));
    }

    /**
     * @param encounter the Encounter to search in
     * @param attr      the type to match against
     * @return all open and valid Visit objects of the specified type for the
     *         Encounter
     */
    @Transactional
    private List<PatientFact> getOpenVisitFactWhereVisitType(Encounter encounter, AttributeKeyMap attr) {
        return getVisitFactWhere(encounter, vf -> vf.isOfType(attr) && visitFactIsOpenAndValid(vf));
    }

    /**
     * @param encounter the Encounter to search in
     * @param attr      the type to match against
     * @return all open and valid Visit objects of the specified type for the
     *         Encounter
     */
    private List<PatientFact> getClosedVisitFactWhereVisitType(Encounter encounter, AttributeKeyMap attr) {
        return getVisitFactWhere(encounter, vf -> vf.isOfType(attr) && !visitFactIsOpen(vf) && vf.isValid());
    }

    /**
     * Get existing encounter or create a new one if it doesn't exist.
     * Also create the MRN and/or Person if necessary.
     *
     * @param mrnStr            the MRN string to find/create
     * @param encounterStr      encounter ID (visit ID) to find/create
     * @param storedFrom        storedFrom time to use for newly created records - should be a time very close to the present
     * @param validFrom         validFrom times to use for newly created records - usually the admission time
     * @return the Encounter, existing or newly created
     * @throws MessageIgnoredException if message can't be processed
     */
    private Encounter getCreateEncounter(String mrnStr, String encounterStr, Instant storedFrom, Instant validFrom) throws MessageIgnoredException {
        logger.info(String.format("getCreateEncounter looking for existing encounter %s in MRN %s", encounterStr, mrnStr));
        // look for encounter by its encounter number only as this is sufficiently unique without also using the MRN
        Encounter existingEnc = encounterRepo.findEncounterByEncounter(encounterStr);
        if (existingEnc == null) {
            // The encounter didn't exist, so see if its MRN exists, creating if not.
            Mrn newOrExistingMrn = getCreateMrn(mrnStr, validFrom, storedFrom, true);
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
            MrnEncounter mrnMatching = getOnlyElement(existingEnc.getMrns().stream()
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
     *
     * @param adtMsg msg containing encounter details
     * @param storedFrom storedFrom time to use for new records
     * @return the created Encounter
     * @throws MessageIgnoredException if message can't be processed
     * @throws InvalidMrnException if Mrn field is empty
     * @throws EmapStarIntegrityException if there's a contradiction in the DB
     */
    @Transactional
    public Encounter admitPatient(AdtMessage adtMsg, Instant storedFrom)
            throws MessageIgnoredException, InvalidMrnException, EmapStarIntegrityException {
        String mrnStr = adtMsg.getMrn();
        Instant admissionTime = adtMsg.getAdmissionDateTime();
        // Location visit start time is normally the same as (hospital) admission time.
        // However, if this admission is being implied from a non-admit message,
        // then the location (bed) visit can start at a different time instead.
        Instant locationVisitStartTime;
        Instant locationVisitValidFrom;
        if (adtMsg.getOperationType().equals(AdtOperationType.TRANSFER_PATIENT)) {
            // Bed visit start time is the transfer time, thus leaving a gap where
            // we don't know where they were, which is the most
            // accurate representation of the data we have.
            locationVisitStartTime = adtMsg.getEventOccurredDateTime();
            locationVisitValidFrom = adtMsg.getEventOccurredDateTime();
        } else if (adtMsg.getOperationType().equals(AdtOperationType.DISCHARGE_PATIENT)) {
            locationVisitStartTime = null;
            locationVisitValidFrom = adtMsg.getDischargeDateTime();
        } else {
            locationVisitStartTime = admissionTime;
            locationVisitValidFrom = admissionTime;
        }
        if (mrnStr == null) {
            throw new InvalidMrnException(String.format("Missing mrn in message"));
        }
        // Encounter is usually a new one for an A01, but it is
        // possible to get a second A01 if the first admission gets deleted
        // and re-made. (User corrected an error in Epic we assume).
        // Therefore need to reuse the existing encounter and the open visit if it
        // exists.
        // (Better to move the hosp visit creation to the actual "new Encounter"?)
        Encounter enc = getCreateEncounter(mrnStr, adtMsg.getVisitNumber(), storedFrom, admissionTime);
        List<PatientFact> allHospitalVisits = getOpenVisitFactWhereVisitType(enc, AttributeKeyMap.HOSPITAL_VISIT);

        // This perhaps belongs in a getCreateHospitalVisit method, with an
        // InformDbDataIntegrity exception
        PatientFact hospitalVisit;
        switch (allHospitalVisits.size()) {
        case 0:
            hospitalVisit = addOpenHospitalVisit(enc, storedFrom, admissionTime, adtMsg.getPatientClass());
            addDemographicsToEncounter(enc, adtMsg, storedFrom);
            // create a new location visit with the new (or updated) location
            AttributeKeyMap visitType = visitTypeFromPatientClass(adtMsg.getPatientClass());
            addOpenLocationVisit(enc, visitType, storedFrom, locationVisitValidFrom, locationVisitStartTime, hospitalVisit,
                    adtMsg.getFullLocationString(), adtMsg.getPatientClass());
            break;
        case 1:
            hospitalVisit = allHospitalVisits.get(0);
            // We have received an admit message but there was already an
            // open hospital visit. Previously we would have invalidated the
            // existing bed visit and its properties and created a new one,
            // but now treat it as a transfer. You need an explicit cancel admit message
            // to get the old behaviour.
            transferPatient(adtMsg, storedFrom);
            break;
        default:
            throw new MessageIgnoredException(adtMsg, "More than 1 (count = " + allHospitalVisits.size()
                    + ") hospital visits in encounter " + adtMsg.getVisitNumber());
        }
        enc = encounterRepo.save(enc);
        logger.info(String.format("Encounter: %s", enc.toString()));
        return enc;
    }

    /**
     * Determine visit type from the patient class (which ultimately comes from HL7).
     * @param patientClass string from HL7
     * @return the fact type of the visit fact
     * @throws MessageIgnoredException if patient class is not recognised or
     *                                 shouldn't appear in a visit-generating
     *                                 message
     */
    private AttributeKeyMap visitTypeFromPatientClass(String patientClass) throws MessageIgnoredException {
        // For now everything's a bed visit, and we're not using AttributeKeyMap.OUTPATIENT_VISIT.
        // The patient class is also being separately recorded so this can be used if needed.
        return AttributeKeyMap.BED_VISIT;
    }

    /**
     * @param enc        the encounter to add to
     * @param adtMsg the message details to use
     * @param storedFrom storedFrom value to use for new records
     */
    private void addDemographicsToEncounter(Encounter enc, AdtMessage adtMsg, Instant storedFrom) {
        Map<String, PatientFact> demogs = buildPatientDemographics(adtMsg, storedFrom);
        demogs.forEach((k, v) -> enc.addFact(v));
    }

    /**
     * Build the demographics objects from a message but don't actually do anything
     * with them. Include visit related facts like patient class because these are
     * treated very similarly.
     *
     * @param adtMsg the msg to build demographics from
     * @param storedFrom storedFrom value to use for new records
     * @return Attribute->Fact key-value pairs
     */
    private Map<String, PatientFact> buildPatientDemographics(AdtMessage adtMsg, Instant storedFrom) {
        Map<String, PatientFact> demographics = new HashMap<>();
        Instant validFrom = adtMsg.getEventOccurredDateTime();
        if (validFrom == null) {
            // some messages (eg. A08) don't have an event occurred field
            validFrom = adtMsg.getRecordedDateTime();
        }
        PatientFact nameFact = new PatientFact();
        nameFact.setValidFrom(validFrom);
        nameFact.setStoredFrom(storedFrom);
        Attribute nameAttr = getCreateAttribute(AttributeKeyMap.NAME_FACT);
        nameFact.setFactType(nameAttr);
        addPropertyToFact(nameFact, storedFrom, AttributeKeyMap.FIRST_NAME, adtMsg.getPatientGivenName());
        addPropertyToFact(nameFact, storedFrom, AttributeKeyMap.MIDDLE_NAMES, adtMsg.getPatientMiddleName());
        addPropertyToFact(nameFact, storedFrom, AttributeKeyMap.FAMILY_NAME, adtMsg.getPatientFamilyName());
        demographics.put(AttributeKeyMap.NAME_FACT.getShortname(), nameFact);

        PatientFact generalDemoFact = new PatientFact();
        generalDemoFact.setValidFrom(validFrom);
        generalDemoFact.setStoredFrom(storedFrom);
        generalDemoFact.setFactType(getCreateAttribute(AttributeKeyMap.GENERAL_DEMOGRAPHIC));

        // will we have to worry about Instants and timezones shifting the date?
        addPropertyToFact(generalDemoFact, storedFrom, AttributeKeyMap.DOB, adtMsg.getPatientBirthDate());

        String hl7Sex = adtMsg.getPatientSex();
        Attribute sexAttrValue = getCreateAttribute(mapSex(hl7Sex));
        addPropertyToFact(generalDemoFact, storedFrom, AttributeKeyMap.SEX, sexAttrValue);

        addPropertyToFact(generalDemoFact, storedFrom, AttributeKeyMap.NHS_NUMBER, adtMsg.getNhsNumber());

        addPropertyToFact(generalDemoFact, storedFrom, AttributeKeyMap.POST_CODE, adtMsg.getPatientZipOrPostalCode());

        demographics.put(AttributeKeyMap.GENERAL_DEMOGRAPHIC.getShortname(), generalDemoFact);
        return demographics;
    }

    /**
     * A little mapping table to convert HL7 sex to Inform-db sex.
     *
     * @param hl7Sex hl7 sex
     * @return Inform-db sex
     */
    private AttributeKeyMap mapSex(String hl7Sex) {
        if (hl7Sex == null) {
            return AttributeKeyMap.UNKNOWN;
        }
        switch (hl7Sex) {
        case "M":
            return AttributeKeyMap.MALE;
        case "F":
            return AttributeKeyMap.FEMALE;
        case "A":
            return AttributeKeyMap.OTHER;
        case "O":
            return AttributeKeyMap.OTHER;
        case "U":
        default:
            return AttributeKeyMap.UNKNOWN;
        }
    }

    /**
     * Add a hospital visit fact to the specified Encounter. This visit is open, ie.
     * ongoing.
     *
     * @param enc            the Encounter to add to
     * @param storedFrom     storedFrom value to use for new records
     * @param visitBeginTime The start time of the visit
     * @param patientClass   the patient class
     * @return the hospital visit fact object
     */
    @Transactional
    private PatientFact addOpenHospitalVisit(Encounter enc, Instant storedFrom, Instant visitBeginTime, String patientClass) {
        PatientFact visitFact = new PatientFact();
        visitFact.setValidFrom(visitBeginTime);
        visitFact.setStoredFrom(storedFrom);
        Attribute hosp = getCreateAttribute(AttributeKeyMap.HOSPITAL_VISIT);
        visitFact.setFactType(hosp);
        visitFact.addProperty(
                buildPatientProperty(storedFrom, visitBeginTime, AttributeKeyMap.ARRIVAL_TIME, visitBeginTime));
        // Patient Class belongs in the hospital visit because it's then easier to query it if needed
        // instead of digging it out of bed visits.
        visitFact.addProperty(
                buildPatientProperty(storedFrom, visitBeginTime, AttributeKeyMap.PATIENT_CLASS, patientClass));
        enc.addFact(visitFact);
        return visitFact;
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
    private void addOpenLocationVisit(Encounter enc, AttributeKeyMap visitType, Instant storedFrom, Instant validFrom,
            Instant visitBeginTime, PatientFact hospitalVisit, String currentLocation, String patientClass) {
        PatientFact visitFact = new PatientFact();
        visitFact.setStoredFrom(storedFrom);
        visitFact.setValidFrom(validFrom);
        visitFact.setFactType(getCreateAttribute(visitType));

        // Sometimes it's impossible to determine when a bed visit started eg - the
        // first ADT message we receive for an encounter was a discharge message, so
        // visitBeginTime can be null
        visitFact.addProperty(
                buildPatientProperty(storedFrom, validFrom, AttributeKeyMap.ARRIVAL_TIME, visitBeginTime));
        visitFact.addProperty(
                buildPatientProperty(storedFrom, validFrom, AttributeKeyMap.LOCATION, currentLocation));
        visitFact.addProperty(
                buildPatientProperty(storedFrom, validFrom, AttributeKeyMap.PATIENT_CLASS, patientClass));

        hospitalVisit.addChildFact(visitFact);
        enc.addFact(visitFact);
    }

    /**
     * Close off the existing Visit and open a new one.
     *
     * @param adtMsg usually an A02 message but can be an A08
     * @param storedFrom storedFrom time to use for new records
     * @throws MessageIgnoredException if message can't be processed
     * @throws EmapStarIntegrityException if a contradiction between DB and the incoming message or itself
     * @throws InvalidMrnException mrn not specified
     */
    @Transactional
    public void transferPatient(AdtMessage adtMsg, Instant storedFrom)
            throws MessageIgnoredException, InvalidMrnException, EmapStarIntegrityException {
        // Docs: "The new patient location should appear in PV1-3 - Assigned Patient
        // Location while the old patient location should appear in PV1-6 - Prior
        // Patient Location."

        // Find the current PatientFact, close it off, and start a new one with its own
        // admit time + location.
        String mrnStr = adtMsg.getMrn();
        String visitNumber = adtMsg.getVisitNumber();

        Instant admissionDateTime = adtMsg.getAdmissionDateTime();
        Instant recordedDateTime = adtMsg.getRecordedDateTime();

        // The discharge datetime will be null, presumably because the patient hasn't
        // been discharged yet

        // Docs: "EVN-6 Event Occurred (DTM) 01278
        // Definition: This field contains the date/time that the event actually
        // occurred. For example, on a transfer (A02 transfer a patient), this field
        // would contain the date/time the patient was actually transferred."
        Instant eventOccurred = adtMsg.getEventOccurredDateTime();
        if (adtMsg.getOperationType().equals(AdtOperationType.UPDATE_PATIENT_INFO)) {
            // A08 doesn't have an event time, so use the recorded time instead
            // Downside: recorded time is later than event time, so subsequent discharge
            // time
            // for this visit can be *earlier* than the arrival time if it's a very short
            // visit
            // or there was a big gap between A08 event + recorded time.
            eventOccurred = adtMsg.getRecordedDateTime();
        }

        Encounter encounter = getCreateEncounter(mrnStr, visitNumber, storedFrom, admissionDateTime);

        List<PatientFact> latestOpenBedVisits = getOpenValidLocationVisit(encounter);
        if (latestOpenBedVisits.isEmpty()) {
            logger.warn("Received transfer for patient we didn't know was admitted - admitting them instead");
            admitPatient(adtMsg, storedFrom);
            return;
        }
        PatientFact latestOpenBedVisit = latestOpenBedVisits.get(0);
        String newTransferLocation = adtMsg.getFullLocationString();
        String currentKnownLocation =
                getOnlyElement(latestOpenBedVisit.getPropertyByAttribute(AttributeKeyMap.LOCATION, p -> p.isValid())).getValueAsString();
        if (newTransferLocation.equals(currentKnownLocation)) {
            // If we get an A02 with a new location that matches where we already thought
            // the patient was, don't perform an actual transfer.
            // In the test data, this sometimes happens following an A08 implied transfer.
            // Also, even if the location hasn't changed the patient class could have changed
            // (should be made explicit as an A06 or A07 but we don't distinguish A02/A06/A07 here).
            String err = "REDUNDANT transfer, location has not changed: " + currentKnownLocation;
            logger.warn(err);
            throw new MessageIgnoredException(adtMsg, err);
        }
        addDischargeToVisit(latestOpenBedVisit, eventOccurred, storedFrom);

        String admitSource = adtMsg.getAdmitSource();
        logger.info(String.format(
                "TRANSFERRING: MRN = %s, admitdatetime %s, admitsrc %s, eventOccurred %s, recorded %s",
                mrnStr, admissionDateTime, admitSource, eventOccurred, recordedDateTime));

        // add a new visit to the current encounter
        Encounter encounterDoubleCheck = latestOpenBedVisit.getEncounter();
        if (encounter != encounterDoubleCheck) {
            throw new MessageIgnoredException(adtMsg, "Different encounter: " + encounter + " | " + encounterDoubleCheck);
        }
        List<PatientFact> hospitalVisit = getOpenVisitFactWhereVisitType(encounter, AttributeKeyMap.HOSPITAL_VISIT);
        // link the bed visit to the parent (hospital) visit
        AttributeKeyMap visitType = visitTypeFromPatientClass(adtMsg.getPatientClass());
        addOpenLocationVisit(encounter, visitType, storedFrom, eventOccurred, eventOccurred, hospitalVisit.get(0),
                newTransferLocation, adtMsg.getPatientClass());
    }

    /**
     * Mark the specified visit as finished.
     *
     * @param adtMsg the A03 message detailing the discharge
     * @param storedFrom storedFrom time to use for new records
     * @throws MessageIgnoredException if message can't be processed
     * @throws EmapStarIntegrityException contradiction in the DB
     * @throws InvalidMrnException mrn not specified
     */
    @Transactional
    public void dischargePatient(AdtMessage adtMsg, Instant storedFrom)
            throws MessageIgnoredException, InvalidMrnException, EmapStarIntegrityException {
        String mrnStr = adtMsg.getMrn();
        String visitNumber = adtMsg.getVisitNumber();
        Instant admissionDateTime = adtMsg.getAdmissionDateTime();
        if (admissionDateTime == null) {
            // This can happen occasionally, seems to be only/usually where EVN-4 = "ED_AFTER_DISMISS".
            // In this unusual case, use the discharge date instead. Note that this will only be used
            // if we have no prior record of the patient and are creating their admission record now.
            admissionDateTime = adtMsg.getDischargeDateTime();
        }
        Encounter encounter = getCreateEncounter(mrnStr, visitNumber, storedFrom, admissionDateTime);
        PatientFact latestOpenBedVisit = getOnlyElement(getOpenValidLocationVisit(encounter));
        if (latestOpenBedVisit == null) {
            // If visit was not known about, admit the patient first before going on to discharge
            // It's not possible to tell when to start the bed visit from.
            encounter = admitPatient(adtMsg, storedFrom);
            latestOpenBedVisit = getOnlyElement(getOpenValidLocationVisit(encounter));
        }
        Instant eventOccurred = adtMsg.getEventOccurredDateTime();
        Instant dischargeDateTime = adtMsg.getDischargeDateTime();
        logger.info(String.format("DISCHARGE: MRN %s, visit %s, eventoccurred %s, dischargetime %s", mrnStr,
                visitNumber, eventOccurred, dischargeDateTime));
        if (dischargeDateTime == null) {
            throw new MessageIgnoredException(adtMsg, "Trying to discharge but the discharge date is null");
        } else {
            // Discharge from the bed visit and the hospital visit
            addDischargeToVisit(latestOpenBedVisit, dischargeDateTime, storedFrom);
            PatientFact hospVisit = latestOpenBedVisit.getParentFact();
            addDischargeToVisit(hospVisit, dischargeDateTime, storedFrom);

            String dischargeDisposition = adtMsg.getDischargeDisposition();
            // Add discharge disposition to hospital visit only, not bed.
            hospVisit.addProperty(buildPatientProperty(storedFrom, dischargeDateTime,
                    AttributeKeyMap.DISCHARGE_DISPOSITION, dischargeDisposition));
            String dischargeLocation = adtMsg.getDischargeLocation();
            hospVisit.addProperty(buildPatientProperty(storedFrom, dischargeDateTime,
                    AttributeKeyMap.DISCHARGE_LOCATION, dischargeLocation));
            Attribute deathIndicator = getBooleanAttribute(adtMsg.getPatientDeathIndicator());
            hospVisit.addProperty(buildPatientProperty(storedFrom, dischargeDateTime,
                    AttributeKeyMap.PATIENT_DEATH_INDICATOR, deathIndicator));
            // only set death time if patient is dead
            if (adtMsg.getPatientDeathIndicator()) {
                hospVisit.addProperty(buildPatientProperty(storedFrom, dischargeDateTime,
                        AttributeKeyMap.PATIENT_DEATH_TIME, adtMsg.getPatientDeathDateTime()));
            }
        }
    }

    /**
     * Turn a Boolean into an Emap-Star attribute.
     * @param booleanValue the normal Boolean
     * @return Emap-Star attributes BOOLEAN_TRUE and BOOLEAN_FALSE for true and false, or null for null
     */
    private Attribute getBooleanAttribute(Boolean booleanValue) {
        if (booleanValue == null) {
            return null;
        } else if (booleanValue.booleanValue()) {
            return getCreateAttribute(AttributeKeyMap.BOOLEAN_TRUE);
        } else {
            return getCreateAttribute(AttributeKeyMap.BOOLEAN_FALSE);
        }
    }

    /**
     * Compare function for visits by discharge time. Missing discharge time sorts as "high", ie. it is considered the most recent.
     * @param v1 visit to compare 1
     * @param v2 visit to compare 2
     * @return result of compareTo called on the discharge timestamps, ie. dischV1.compareTo(dischV2)
     */
    private int sortVisitByDischargeTime(PatientFact v1, PatientFact v2) {
        PatientProperty dischProp1 = getOnlyElement(v1.getPropertyByAttribute(AttributeKeyMap.DISCHARGE_TIME, p -> p.isValid()));
        Instant dischV1 = Instant.MAX;
        if (dischProp1 != null) {
            dischV1 = dischProp1.getValueAsDatetime();
        }
        PatientProperty dischProp2 = getOnlyElement(v2.getPropertyByAttribute(AttributeKeyMap.DISCHARGE_TIME, p -> p.isValid()));
        Instant dischV2 = Instant.MAX;
        if (dischProp2 != null) {
            dischV2 = dischProp2.getValueAsDatetime();
        }
        return dischV1.compareTo(dischV2);
    }

    /**
     * Cancel a pre-existing admission by invalidating the facts associated with it.
     * @param adtMsg the adt message
     * @param storedFrom storedFrom time to use for new records
     * @throws MessageIgnoredException if message can't be processed
     * @throws InvalidMrnException think this is useless
     * @throws EmapStarIntegrityException contradiction in the db
     */
    private void cancelAdmitPatient(AdtMessage adtMsg, Instant storedFrom)
            throws MessageIgnoredException, InvalidMrnException, EmapStarIntegrityException {
        String visitNumber = adtMsg.getVisitNumber();
        Encounter encounter = encounterRepo.findEncounterByEncounter(visitNumber);

        if (encounter == null) {
            logger.warn("Tried to cancel admit for patient we don't know about - admitting them");
            encounter = admitPatient(adtMsg, storedFrom);
        }

        List<PatientFact> latestOpenBedVisits = getOpenValidLocationVisit(encounter);
        if (latestOpenBedVisits.size() != 1) {
            throw new MessageIgnoredException(adtMsg, "No open location visit, cannot cancel admit" + visitNumber);
        }
        Instant cancellationTime = adtMsg.getEventOccurredDateTime();
        PatientFact onlyOpenLocationVisit = latestOpenBedVisits.get(0);
        PatientFact hospVisit = onlyOpenLocationVisit.getParentFact();
        // do the actual invalidations
        onlyOpenLocationVisit.invalidateAll(cancellationTime);
        hospVisit.invalidateAll(cancellationTime);
    }

    /**
     * Mark the visit specified by visit number as not discharged any more. Can either mean a discharge was
     * erroneously entered, or a decision to discharge was reversed.
     *
     * @param adtMsg the A13 message detailing the cancel discharge
     * @param storedFrom storedFrom time to use for new records
     * @throws MessageIgnoredException if message can't be processed
     */
    @Transactional
    private void cancelDischargePatient(AdtMessage adtMsg, Instant storedFrom) throws MessageIgnoredException {
        String visitNumber = adtMsg.getVisitNumber();
        // event occurred field seems to be populated despite the Epic example message showing it blank.
        Instant invalidationDate = adtMsg.getEventOccurredDateTime();
        // this must be non-null or the invalidation won't work
        if (invalidationDate == null) {
            throw new MessageIgnoredException(adtMsg, "Trying to cancel discharge but the event occurred date is null");
        }

        Encounter encounter = encounterRepo.findEncounterByEncounter(visitNumber);
        if (encounter == null) {
            throw new MessageIgnoredException(adtMsg, "Cannot cancel discharge for a visit that doesn't exist: " + visitNumber);
        }
        // Get the most recent bed visit.
        PatientFact mostRecentBedVisit = getVisitFactWhere(encounter,
                vf -> AttributeKeyMap.isLocationVisitType(vf.getFactType()) && vf.isValid()).stream()
                        .max((vf1, vf2) -> sortVisitByDischargeTime(vf1, vf2)).get();

        // Encounters should always have at least one visit.
        if (visitFactIsOpen(mostRecentBedVisit)) {
            // This is an error. The most recent bed visit is still open. Ie. the patient
            // has not been discharged, so we cannot cancel the discharge.
            // Possible cause is that we never received the A03.
            throw new MessageIgnoredException(adtMsg, visitNumber + " Cannot process A13 - most recent bed visit is still open");
        }
        PatientProperty bedDischargeTime = getOnlyElement(
                mostRecentBedVisit.getPropertyByAttribute(AttributeKeyMap.DISCHARGE_TIME, PatientProperty::isValid));
        // Do the actual cancel by invalidating the discharge time property on the
        // location visit, and multiple properties on the hospital visit
        bedDischargeTime.setValidUntil(invalidationDate);
        PatientFact hospitalVisit = mostRecentBedVisit.getParentFact();
        for (AttributeKeyMap a : Arrays.asList(AttributeKeyMap.DISCHARGE_TIME, AttributeKeyMap.DISCHARGE_DISPOSITION,
                AttributeKeyMap.DISCHARGE_LOCATION, AttributeKeyMap.PATIENT_DEATH_INDICATOR,
                AttributeKeyMap.PATIENT_DEATH_TIME)) {
            PatientProperty prop = getOnlyElement(hospitalVisit.getPropertyByAttribute(a, PatientProperty::isValid));
            if (prop != null) {
                prop.setValidUntil(invalidationDate);
            }
        }

        // The Epic spec for receiving an A13 says you can be put in a different place than the last one you were in,
        // ie. an implicit transfer. Does this ever happen for messages that Epic emits? Currently ignoring
        // the location field.

        mostRecentBedVisit = patientFactRepository.save(mostRecentBedVisit);
        hospitalVisit = patientFactRepository.save(hospitalVisit);
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
    private void addDischargeToVisit(PatientFact visit, Instant dischargeDateTime, Instant storedFrom) {
        Attribute dischargeTime = getCreateAttribute(AttributeKeyMap.DISCHARGE_TIME);
        PatientProperty visProp = new PatientProperty();
        visProp.setValidFrom(dischargeDateTime);
        visProp.setStoredFrom(storedFrom);
        visProp.setValueAsDatetime(dischargeDateTime);
        visProp.setPropertyType(dischargeTime);
        visit.addProperty(visProp);
    }

    private Map<String, Attribute> attributeCache = null;

    /**
     * Return a cached, persisted Attribute object with the given enum value.
     *
     * @param attrKM the enum value of the attribute
     * @return the Attribute object from the cache
     */
    @Transactional
    private Attribute getCreateAttribute(AttributeKeyMap attrKM) {
        if (attributeCache == null) {
            attributeCache = new HashMap<>();
            Set<Attribute> allAttrs = attributeRepo.findAll();
            for (Attribute a : allAttrs) {
                attributeCache.put(a.getShortName(), a);
            }
        }
        Attribute attribute = attributeCache.get(attrKM.getShortname());
        if (attribute != null) {
            return attribute;
        } else {
            throw new AttributeError("Tried to use attribute but wasn't found in db: " + attrKM.getShortname());
        }
    }

    /**
     * Add a property (key-value pair) to a pre-existing fact, only if its value is non-null.
     *
     * Mainly kept for backwards compatibility, consider using buildPatientProperty directly instead.
     *
     * @param fact      the fact to add to
     * @param storedFrom storedFrom time to use for new records
     * @param propertyType    the property key
     * @param factValue the property value
     */
    private void addPropertyToFact(PatientFact fact, Instant storedFrom, AttributeKeyMap propertyType, Object factValue) {
        if (factValue != null) {
            fact.addProperty(buildPatientProperty(storedFrom, fact.getValidFrom(), propertyType, factValue));
        }
    }

    /**
     * Filter on a predicate where at most one element should satisfy it.
     *
     * @param      <E> the type of the list elements
     * @param list the list to look in
     * @param pred the predicate to test with
     * @return the only element that satisfies it, or null if there are none that do
     * @throws DuplicateValueException if more than one element satisfies pred
     */
    private <E> E getOnlyElementWhere(List<E> list, Predicate<? super E> pred) {
        List<E> persons = list.stream().filter(pred).collect(Collectors.toList());
        return getOnlyElement(persons);
    }

    /**
     * @param      <E> the type of the list elements
     * @param list the given list
     * @return the only element in the given list, or null if empty or null, throws if >1
     * @throws DuplicateValueException if >1 element in list
     */
    private <E> E getOnlyElement(List<E> list) {
        if (list == null) {
            return null;
        }
        switch (list.size()) {
        case 0:
            return null;
        case 1:
            return list.get(0);
        default:
            throw new DuplicateValueException(String.format("List contained %d elements instead of 0-1", list.size()));
        }
    }

    /**
     * Handle an A08 message. This is supposed to be about patient info changes (ie.
     * demographics, but we also see changes to location (ie. transfers)
     * communicated only via an A08)
     *
     * @param adtMsg the message with the patient info
     * @param storedFrom storedFrom time to use for new records
     * @throws MessageIgnoredException if message can't be processed
     * @throws EmapStarIntegrityException if there's a contradiction in the DB
     * @throws InvalidMrnException mrn not specified
     */
    @Transactional
    private void updatePatientInfo(AdtMessage adtMsg, Instant storedFrom)
            throws MessageIgnoredException, InvalidMrnException, EmapStarIntegrityException {
        String visitNumber = adtMsg.getVisitNumber();
        String newLocation = adtMsg.getFullLocationString();

        Encounter encounter = encounterRepo.findEncounterByEncounter(visitNumber);
        if (encounter == null) {
            // Don't infer an admission with a patient update info message, because it seems
            // these sometimes occur independently of a patient being present in hospital.
            // They can have a null admission date, which causes a failed validFrom non-null constraint.
            // We may have to be more selective about this, maybe the EVN-4 can tell us
            // what the reason/circumstances were.
            throw new MessageIgnoredException(adtMsg, "Cannot find the visit " + visitNumber);
        }
        // Compare new demographics with old
        Map<String, PatientFact> newDemographics = buildPatientDemographics(adtMsg, storedFrom);
        Map<String, PatientFact> currentDemographics = getValidStoredDemographicFacts(encounter).stream()
                .collect(Collectors.toMap(f -> f.getFactType().getShortName(), f -> f));
        updateDemographics(encounter, currentDemographics, newDemographics);

        // detect when location has changed and perform a transfer
        List<PatientFact> latestOpenLocationVisits = getOpenValidLocationVisit(encounter);
        PatientFact onlyOpenLocationVisit = getOnlyElement(latestOpenLocationVisits);
        if (onlyOpenLocationVisit == null) {
            throw new MessageIgnoredException(adtMsg, "Got A08 but no open bed visit for visit " + visitNumber);
        }
        PatientProperty knownlocation =
                getOnlyElement(onlyOpenLocationVisit.getPropertyByAttribute(AttributeKeyMap.LOCATION, p -> p.isValid()));
        if (!newLocation.equals(knownlocation.getValueAsString())) {
            logger.warn(String.format("[mrn %s, visit num %s] IMPLICIT TRANSFER IN A08: |%s| -> |%s|", adtMsg.getMrn(),
                    visitNumber, knownlocation.getValueAsString(), newLocation));
            transferPatient(adtMsg, storedFrom);
        }
    }

    /**
     * If demographics have changed, update them and invalidate the old.
     *
     * @param encounter           the existing encounter that we may need to modify
     *                            demographics of
     * @param currentDemographics existing demographics (eg. from the db)
     * @param newDemographics     new demographics (eg. from the current message)
     */
    private void updateDemographics(Encounter encounter, Map<String, PatientFact> currentDemographics,
            Map<String, PatientFact> newDemographics) {
        logger.info(String.format("A08 comparing %d existing demographic facts to %s new facts",
                currentDemographics.size(), newDemographics.size()));
        for (String newKey : newDemographics.keySet()) {
            PatientFact newFact = newDemographics.get(newKey);
            PatientFact currentFact = currentDemographics.get(newKey);
            if (currentFact == null) {
                logger.info("fact does not exist, adding " + newFact.getFactType().getShortName());
                encounter.addFact(newFact);
            } else {
                if (newFact.equals(currentFact)) {
                    logger.info("fact exists and matches, no action: " + currentFact.getFactType().getShortName());
                } else {
                    // Just invalidate the entire fact and write in the new one.
                    // May try this on a per-property basis in future.
                    Instant invalidationDate = newFact.getValidFrom();
                    logger.info(
                            "fact exists but does not match, replacing: " + currentFact.getFactType().getShortName());
                    currentFact.invalidateAll(invalidationDate);
                    encounter.addFact(newFact);
                }
            }
        }
    }

    /**
     * Indicate in the DB that two MRNs now belong to the same person. One MRN is
     * designated the surviving MRN, although we can't really enforce this as we'll
     * continue to add further data to whichever MRN is specified in future, which
     * (if the source system is behaving) we'd hope would be the surviving MRN. The
     * best we could do is flag it as an error if new data is put against a
     * non-surviving MRN.
     *
     * @param adtMsg message containing merge info
     * @param storedFrom storedFrom time to use for new records
     * @throws MessageIgnoredException if merge time in message is blank or message can't be processed
     */
    @Transactional
    private void mergeById(AdtMessage adtMsg, Instant storedFrom) throws MessageIgnoredException {
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
        Mrn oldMrn = getCreateMrn(oldMrnStr, mergeTime, storedFrom, true);
        Mrn survivingMrn = getCreateMrn(survivingMrnStr, mergeTime, storedFrom, true);
        if (survivingMrn == null || oldMrn == null) {
            throw new MessageIgnoredException(adtMsg, String.format("MRNs %s or %s (%s or %s) are not previously known, do nothing",
                    oldMrnStr, survivingMrnStr, oldMrn, survivingMrn));
        }
        PersonMrn oldPersonMrn = getOnlyElementWhere(oldMrn.getPersons(), pm -> pm.isValid());
        PersonMrn survivingPersonMrn = getOnlyElementWhere(survivingMrn.getPersons(), pm -> pm.isValid());
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

        newOldPersonMrn = personMrnRepo.save(newOldPersonMrn);
        oldPersonMrn = personMrnRepo.save(oldPersonMrn);
    }

    /**
     * Convert the simplified data from the pathology message into Inform-db structures,
     * and merge with existing data depending on whether it's a new order or changes to an existing one.
     * @param pathologyOrder the pathology order details, may contain results
     * @param storedFrom storedFrom time to use for new records
     * @throws MessageIgnoredException if message can't be processed
     * @throws EmapStarIntegrityException contradiction in DB
     */
    @Transactional
    private void addOrUpdatePathologyOrder(PathologyOrder pathologyOrder, Instant storedFrom)
            throws MessageIgnoredException, EmapStarIntegrityException {
        String visitNumber = pathologyOrder.getVisitNumber();
        String epicCareOrderNumber = pathologyOrder.getEpicCareOrderNumber();
        String mrnStr = pathologyOrder.getMrn();
        PatientFact newPathologyOrder = buildPathologyOrderFact(pathologyOrder, storedFrom);
        Instant backupValidFrom = newPathologyOrder.getValidFrom();
        // build the order fact from the message data
        Pair<Encounter, PatientFact> encounterOrderPair = getEncounterForOrder(epicCareOrderNumber, visitNumber, mrnStr, storedFrom, backupValidFrom);
        Encounter encounter = encounterOrderPair.getLeft();
        PatientFact existingOrderRootFact = encounterOrderPair.getRight();

        logger.debug(String.format("new pathology order facts:\n%s", newPathologyOrder.toString()));

        // If we already know about the order, use the existing order from the DB as the parent,
        // otherwise use the newly created one.
        PatientFact parent;
        if (existingOrderRootFact == null) {
            // no existing, use new fact and add it to the encounter
            parent = newPathologyOrder;
            encounter.addFact(newPathologyOrder);
        } else {
            // use existing fact from DB (is already added to encounter)
            parent = existingOrderRootFact;
        }

        // Build the results fact(s) from the message data, if any.
        //
        // Add the results to the newly constructed order fact, although that fact may not
        // be used if there is an existing order.
        Map<String, PatientFact> newPathologyResults = buildPathologyResultsFacts(newPathologyOrder, storedFrom,
                pathologyOrder.getPathologyResults(), pathologyOrder.getTestBatteryLocalCode());

        if (existingOrderRootFact != null) {
            // (might also want to check if existingOrderRootFact itself has any changed properties)
            //
            // Which facts in resultFactsFromOrder are actually already present in existingOrderRootFact?
            List<PatientFact> existingFacts = getFactWhere(existingOrderRootFact.getChildFacts(),
                    f -> f.isValid()
                    && f.isOfType(AttributeKeyMap.PATHOLOGY_TEST_RESULT));
            Map<String, PatientFact> existingFactsAsMap = existingFacts.stream()
                    .collect(Collectors.toMap(ef -> uniqueKeyFromPathologyResultFact(ef), ef -> ef));
            Iterator<Entry<String, PatientFact>> newFacts = newPathologyResults.entrySet().iterator();
            int oldSize = newPathologyResults.size();
            while (newFacts.hasNext()) {
                Entry<String, PatientFact> newFactEntry = newFacts.next();
                PatientFact newFact = newFactEntry.getValue();
                String newFactKey = newFactEntry.getKey();
                PatientFact existingResult = existingFactsAsMap.get(newFactKey);
                if (existingResult != null) {
                    if (existingResult.equals(newFact)) {
                        logger.debug(
                                String.format("Ignoring fact, is equal to existing: %s", existingResult.toString()));
                    } else {
                        logger.debug(
                                String.format(
                                        "Ignoring fact, although needs updating.\n    Existing: %s\n    New: %s",
                                        existingResult.toString(), newFact.toString()));
                    }
                    newFacts.remove();
                } else {
                    logger.debug(String.format("Using new fact: %s", newFact.toString()));
                }
            }
            int newSize = newPathologyResults.size();
            logger.debug(String.format("From %d facts: %d added, %d ignored, ? updated", oldSize, newSize,
                    oldSize - newSize));
            for (PatientFact pathResult : newPathologyResults.values()) {
                existingOrderRootFact.addChildFact(pathResult);
            }
            logger.debug(String.format("Disconnecting temporary pathology order fact, old num children = %d",
                    newPathologyOrder.getChildFacts().size()));
            newPathologyOrder.getChildFacts().clear();
        }

        // Some child facts - eg. sensitivities are unable to work out their
        // valid from time because status change time is missing, fill
        // this in here.
        parent.cascadeValidFrom(null);


        // Add the child (and grandchild etc) facts directly to the encounter,
        // to allow them to be found by a single query knowing only the encounter.
        for (PatientFact child : newPathologyResults.values()) {
            encounter.addFact(child);
        }

        encounter = encounterRepo.save(encounter);
    }

    /**
     * A key for identifying pathology results so updates can be compared with existing ones.
     * @param pathologyResultFact the pathology result fact
     * @return a key that is unique within the order
     */
    private String uniqueKeyFromPathologyResultFact(PatientFact pathologyResultFact) {
        PatientFact orderFact = pathologyResultFact.getParentFact();
        PatientProperty testCode = getOnlyElement(pathologyResultFact.getPropertyByAttribute(AttributeKeyMap.PATHOLOGY_TEST_CODE));
        PatientProperty batteryCode = getOnlyElement(orderFact.getPropertyByAttribute(AttributeKeyMap.PATHOLOGY_TEST_BATTERY_CODE));
        return batteryCode.getValueAsString() + "_" + testCode.getValueAsString();
    }

    /**
     * Convert order details from an Emap-Interchange message to Emap-Star structures.
     * @param order the pathology order details
     * @param storedFrom storedFrom time to use for new records
     * @return a PatientFact object that represents the order
     */
    private PatientFact buildPathologyOrderFact(PathologyOrder order, Instant storedFrom) {
        // The valid from date should be the order time, when this fact became true.
        // However we are currently not getting this time, so try to find
        // another non-null time: the requested or collection/observation time
        Instant validFrom = order.getOrderDateTime();
        if (validFrom == null) {
            validFrom = order.getRequestedDateTime();
        }
        if (validFrom == null) {
            validFrom = order.getObservationDateTime();
        }

        PatientFact pathFact = new PatientFact();
        pathFact.setFactType(getCreateAttribute(AttributeKeyMap.PATHOLOGY_ORDER));
        pathFact.setValidFrom(validFrom);
        pathFact.setStoredFrom(storedFrom);
        pathFact.addProperty(buildPatientProperty(storedFrom, validFrom, AttributeKeyMap.PATHOLOGY_ORDER_CONTROL_ID,
                order.getOrderControlId()));
        pathFact.addProperty(buildPatientProperty(storedFrom, validFrom, AttributeKeyMap.PATHOLOGY_EPIC_ORDER_NUMBER,
                order.getEpicCareOrderNumber()));
        pathFact.addProperty(buildPatientProperty(storedFrom, validFrom, AttributeKeyMap.PATHOLOGY_TEST_BATTERY_CODE,
                order.getTestBatteryLocalCode()));
        pathFact.addProperty(buildPatientProperty(storedFrom, validFrom, AttributeKeyMap.PATHOLOGY_LAB_NUMBER,
                order.getLabSpecimenNumber()));
        pathFact.addProperty(buildPatientProperty(storedFrom, validFrom, AttributeKeyMap.PATHOLOGY_OCS_NUMBER,
                order.getLabSpecimenNumberOCS()));
        pathFact.addProperty(buildPatientProperty(storedFrom, validFrom, AttributeKeyMap.PATHOLOGY_COLLECTION_TIME,
                order.getObservationDateTime()));
        pathFact.addProperty(buildPatientProperty(storedFrom, validFrom, AttributeKeyMap.PATHOLOGY_ORDER_TIME,
                order.getOrderDateTime()));
        pathFact.addProperty(buildPatientProperty(storedFrom, validFrom, AttributeKeyMap.PATHOLOGY_ORDER_PATIENT_TYPE,
                order.getOrderType()));
        pathFact.addProperty(buildPatientProperty(storedFrom, validFrom, AttributeKeyMap.PATHOLOGY_ORDER_ORDER_STATUS,
                order.getOrderStatus()));
        pathFact.addProperty(buildPatientProperty(storedFrom, validFrom, AttributeKeyMap.PATHOLOGY_ORDER_RESULT_STATUS,
                order.getResultStatus()));
        pathFact.addProperty(buildPatientProperty(storedFrom, validFrom, AttributeKeyMap.PATHOLOGY_LAB_DEPARTMENT_CODE,
                order.getLabDepartment()));

        // Status change time is only given to us once per order/battery result, but we apply it
        // to each result within the order and call it the result time, because results can be returned bit by bit
        // so results within a battery may have different times.
        // Here, we also save it as the generic last status change time.
        pathFact.addProperty(buildPatientProperty(storedFrom, validFrom, AttributeKeyMap.PATHOLOGY_STATUS_CHANGE_TIME,
                order.getStatusChangeTime()));

        return pathFact;
    }

    /**
     * Build a patient property given the key/value pair.
     *
     * @param storedFrom the stored from temporal field
     * @param validFrom  the valid from temporal field
     * @param attrKM     the attribute key value
     * @param value      the actual value
     * @return the constructed PatientProperty
     */
    private PatientProperty buildPatientProperty(Instant storedFrom, Instant validFrom, AttributeKeyMap attrKM,
            Object value) {
        PatientProperty prop = new PatientProperty();
        prop.setValidFrom(validFrom);
        prop.setStoredFrom(storedFrom);
        prop.setPropertyType(getCreateAttribute(attrKM));
        if (value != null) {
            prop.setValue(value);
        }
        return prop;
    }

    /**
     * Make a PatientFact from each pathology result. Return in an indexed
     * collection for easy diffing (we expect to get partial results and final
     * results at different times and this might be useful).
     *
     * @param parent the parent PatientFact, either from the DB or newly constructed
     * @param storedFrom storedFrom time to use for new records
     * @param pathResults the pathology results
     * @param testBatteryLocalCode the battery local code for the order
     * @return all descendant PatientFact objects indexed by a unique identifier
     */
    private Map<String, PatientFact> buildPathologyResultsFacts(PatientFact parent, Instant storedFrom,
            List<? extends PathologyResult> pathResults, String testBatteryLocalCode) {
        Map<String, PatientFact> facts = new HashMap<>();
        for (PathologyResult pr : pathResults) {
            Instant resultTime = pr.getResultTime();
            PatientFact fact = new PatientFact();
            fact.setStoredFrom(storedFrom);
            fact.setValidFrom(resultTime);
            fact.setFactType(getCreateAttribute(AttributeKeyMap.PATHOLOGY_TEST_RESULT));

            String key = testBatteryLocalCode + "_" + pr.getTestItemLocalCode();

            fact.addProperty(buildPatientProperty(storedFrom, resultTime, AttributeKeyMap.PATHOLOGY_TEST_BATTERY_CODE,
                    testBatteryLocalCode));
            fact.addProperty(buildPatientProperty(storedFrom, resultTime, AttributeKeyMap.PATHOLOGY_TEST_CODE,
                    pr.getTestItemLocalCode()));
            fact.addProperty(buildPatientProperty(storedFrom, resultTime, AttributeKeyMap.PATHOLOGY_ISOLATE_CODE,
                    pr.getIsolateLocalCode()));
            PatientProperty result = buildPatientProperty(storedFrom, resultTime, AttributeKeyMap.PATHOLOGY_NUMERIC_VALUE,
                    pr.getNumericValue());
            result.setValueAsString(pr.getStringValue());
            fact.addProperty(result);
            fact.addProperty(
                    buildPatientProperty(storedFrom, resultTime, AttributeKeyMap.PATHOLOGY_UNITS, pr.getUnits()));
            fact.addProperty(
                    buildPatientProperty(storedFrom, resultTime, AttributeKeyMap.PATHOLOGY_REFERENCE_RANGE, pr.getReferenceRange()));
            fact.addProperty(
                    buildPatientProperty(storedFrom, resultTime, AttributeKeyMap.PATHOLOGY_RESULT_TIME, resultTime));
            fact.addProperty(
                    buildPatientProperty(storedFrom, resultTime, AttributeKeyMap.PATHOLOGY_RESULT_STATUS, pr.getResultStatus()));
            fact.addProperty(
                    buildPatientProperty(storedFrom, resultTime, AttributeKeyMap.RESULT_NOTES, pr.getNotes()));

            parent.addChildFact(fact);
            facts.put(key, fact);
            // each result can have zero or more sensitivities, which are actually just another type of order
            List<PathologyOrder> pathologySensitivities = pr.getPathologySensitivities();
            for (PathologyOrder sensOrder : pathologySensitivities) {
                // each sensitivity needs to be built as an order
                List<? extends PathologyResult> sensResults = sensOrder.getPathologyResults();
                Map<String, PatientFact> sensFacts = buildPathologyResultsFacts(fact, storedFrom, sensResults, sensOrder.getTestBatteryLocalCode());
                facts.putAll(sensFacts);
            }
        }
        return facts;
    }

    /**
     * Look up an encounter by an existing order number, or by the encounter number if
     * the order is previously unknown.
     * Move to repo?
     * @param epicCareOrderNumber the Epic order number to search by
     * @param visitNumber the encounter/visit number to search by
     * @param mrnStr MRN string to use if an MRN record needs creating
     * @param storedFrom storedFrom time to use for new records
     * @param backupValidFrom validFrom time to use if encounter/mrn/person needs creating
     * @return Pair containing the Encounter object that this order is attached to and the PatientFact object that is the root
     * object representing the order, if it exists (else null).
     * @throws MessageIgnoredException if the Encounter can't be found by any method
     * @throws EmapStarIntegrityException contradiction in DB
     */
    private Pair<Encounter, PatientFact> getEncounterForOrder(String epicCareOrderNumber, String visitNumber,
            String mrnStr, Instant storedFrom, Instant backupValidFrom)
            throws MessageIgnoredException, EmapStarIntegrityException {
        // We do get messages with blank Epic order numbers,
        // however searching on a blank order number will never do the right
        // thing, so in this case behave as if the order was not found.
        PatientFact existingPathologyOrder = null;
        if (!epicCareOrderNumber.isEmpty()) {
            existingPathologyOrder = getOnlyElement(
                    patientFactRepository.findAllPathologyOrdersByOrderNumber(epicCareOrderNumber));
        }
        // If this fails, try the lab number + order type because epic order num may be blank for lab initiated orders
        Encounter encounter;
        // order may or may not exist already
        if (existingPathologyOrder != null) {
            encounter = existingPathologyOrder.getEncounter();
            logger.info(String.format("Existing pathology order %s: \n%s", epicCareOrderNumber,
                    existingPathologyOrder.toString()));
        } else {
            // If seeing a result message for a previously unknown order, it should be allowed but logged as
            // a potential error, although when starting mid-HL7 stream there will always be
            // results for orders you haven't seen.
            // (also our test depends on this being allowed)
            logger.error(String.format("Couldn't find order with order number %s, searching by visit number instead", epicCareOrderNumber));
            if (!visitNumber.isEmpty()) {
                encounter = getCreateEncounter(mrnStr, visitNumber, storedFrom, backupValidFrom);
            } else {
                throw new MessageIgnoredException("Can't find encounter - can't search on empty visit number");
            }
        }
        if (!visitNumber.isEmpty() && !encounter.getEncounter().equals(visitNumber)) {
            // the visit number of the encounter for the existing order disagrees with the visit number
            // in the HL7 message.
            throw new EmapStarIntegrityException("parent encounter of existing order has encounter number "
                    + encounter.getEncounter() + ", expecting " + visitNumber);
        }
        return new ImmutablePair<>(encounter, existingPathologyOrder);
    }

    /**
     * @return how many encounters there are in total
     */
    public long countEncounters() {
        return encounterRepo.count();
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
     * @return the Mrn, pre-existing or newly created, or null if it doesn't exist
     *         and !createIfNotExist
     */
    private Mrn getCreateMrn(String mrnStr, Instant validFrom, Instant storedFrom, boolean createIfNotExist) {
        if (createIfNotExist && (storedFrom == null || validFrom == null || mrnStr == null || mrnStr.isEmpty())) {
            throw new IllegalArgumentException(String.format(
                    "if createIfNotExist, storedFrom (%s) and validFrom (%s) and mrnStr (%s) must be non-null", storedFrom, validFrom, mrnStr));
        }
        Mrn mrn = mrnRepo.findByMrnString(mrnStr);
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
            pers = personRepo.save(pers);
        } else {
            logger.info(String.format("Reusing an existing MRN %s with encounters: %s", mrn.getMrn(),
                    mrn.getEncounters().toString()));
        }
        return mrn;
    }


}
