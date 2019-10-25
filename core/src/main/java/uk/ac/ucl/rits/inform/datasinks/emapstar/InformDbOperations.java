package uk.ac.ucl.rits.inform.datasinks.emapstar;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.cfg.NotYetImplementedException;
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
                String addedTime = record.get("added_time");
                newAttr.setAddedTime(Instant.parse(addedTime));
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
    }

    /**
     * Process a pathology order message.
     * @param pathologyOrder the message
     * @return the return code
     */
    @Override
    @Transactional
    public String processMessage(PathologyOrder pathologyOrder) {
        String returnCode;
        try {
            addOrUpdatePathologyOrder(pathologyOrder);
            // be more specific about the type of OK in future
            returnCode = "OK";
        } catch (MessageIgnoredException e) {
            logger.warn("Pathology order message ignored due to: " + e);
            returnCode = e.getClass().getSimpleName();
        } catch (EmapStarIntegrityException e) {
            logger.error(
                    "Message cannot be ignored but we have yet to implement the right error handling: " + e.toString());
            e.printStackTrace();
            returnCode = e.getClass().getSimpleName();
        }
        return returnCode;
    }

    /**
     * Process a patient movement (ADT) message.
     * @param adtMsg the message
     */
    @Override
    @Transactional
    public String processMessage(AdtMessage adtMsg) {
        String returnCode;
        try {
            returnCode = "OK";
            switch (adtMsg.getOperationType()) {
            case ADMIT_PATIENT:
                admitPatient(adtMsg);
                break;
            case TRANSFER_PATIENT:
                transferPatient(adtMsg);
                break;
            case DISCHARGE_PATIENT:
                dischargePatient(adtMsg);
                break;
            case UPDATE_PATIENT_INFO:
                updatePatientInfo(adtMsg);
                break;
            case CANCEL_DISCHARGE_PATIENT:
                cancelDischargePatient(adtMsg);
                break;
            case MERGE_BY_ID:
                mergeById(adtMsg);
                break;
            default:
                returnCode = "Not implemented";
                logger.error(returnCode);
                break;
            }
        } catch (MessageIgnoredException | InvalidMrnException e) {
            logger.error("Message ignored due to: " + e.toString());
            returnCode = e.getClass().getSimpleName();
        } catch (EmapStarIntegrityException e) {
            logger.error("Message cannot be ignored but we have yet to implement the right error handling: " + e.toString());
            e.printStackTrace();
            returnCode = e.getClass().getSimpleName();
        }
        return returnCode;
    }

    @Override
    @Transactional
    public String processMessage(VitalSigns msg) {
        String returnCode = "Not implemented";
        logger.error(returnCode);
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
        String shortName = pf.getFactType().getShortName();
        return shortName.equals(AttributeKeyMap.BED_VISIT.getShortname())
                || shortName.equals(AttributeKeyMap.HOSPITAL_VISIT.getShortname());
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
        List<PatientFact> facts = encounter.getFacts();
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
        return getDemographicFactsWhere(encounter, f -> factIsValid(f) && factIsStored(f));
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
     * Check whether PatientFact's valid_until column is null, indicating that it
     * has never been invalidated. Note: this does not perform time travel (ie.
     * check whether valid_until is null or in the future) Note: the validity of the
     * underlying properties is not checked
     *
     * @param pf the patient fact to check
     * @return whether fact is valid as of the present moment
     */
    private boolean factIsValid(TemporalCore pf) {
        Instant validUntil = pf.getValidUntil();
        return validUntil == null;
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
     * @param vf            the visit fact to check
     * @param visitTypeAttr the visit type to compare to
     * @return whether the visit fact vf is of the type visitTypeAttr
     */
    private boolean visitFactIsOfType(PatientFact vf, AttributeKeyMap visitTypeAttr) {
        return visitTypeAttr.getShortname().equals(vf.getFactType().getShortName());
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
        return getVisitFactWhere(encounter, vf -> visitFactIsOfType(vf, attr));
    }

    /**
     * @param encounter the Encounter to search in
     * @param attr      the type to match against
     * @return all open and valid Visit objects of the specified type for the
     *         Encounter
     */
    @Transactional
    private List<PatientFact> getOpenVisitFactWhereVisitType(Encounter encounter, AttributeKeyMap attr) {
        logger.info("getOpenVisitFactWhereVisitType: " + encounter + " " + attr);
        return getVisitFactWhere(encounter, vf -> visitFactIsOfType(vf, attr) && visitFactIsOpenAndValid(vf));
    }

    /**
     * @param encounter the Encounter to search in
     * @param attr      the type to match against
     * @return all open and valid Visit objects of the specified type for the
     *         Encounter
     */
    private List<PatientFact> getClosedVisitFactWhereVisitType(Encounter encounter, AttributeKeyMap attr) {
        logger.info("getClosedVisitFactWhereVisitType: " + encounter + " " + attr);
        return getVisitFactWhere(encounter, vf -> visitFactIsOfType(vf, attr) && !visitFactIsOpen(vf) && vf.isValid());
    }

    /**
     * Get existing encounter or create a new one if it doesn't exist.
     *
     * @param mrn              the MRN to search/create in
     * @param adtMsg contains encounter ID (visit ID) to search for
     * @return the Encounter, existing or newly created
     * @throws MessageIgnoredException if message can't be processed
     */
    private Encounter getCreateEncounter(Mrn mrn, AdtMessage adtMsg) throws MessageIgnoredException {
        logger.info("getCreateEncounter");
        String encounter = adtMsg.getVisitNumber();
        List<Encounter> existingEncs = getEncounterWhere(mrn, encounter);
        if (existingEncs == null || existingEncs.isEmpty()) {
            logger.info("getCreateEncounter CREATING NEW");
            Encounter enc = new Encounter();
            Instant storedFrom = Instant.now();
            enc.setEncounter(encounter);
            Instant validFrom = adtMsg.getAdmissionDateTime();
            mrn.addEncounter(enc, validFrom, storedFrom);
            return enc;
        } else if (existingEncs.size() > 1) {
            throw new MessageIgnoredException(
                    "More than one encounter with this ID, not sure how to handle this yet: " + encounter);
        } else {
            // return the only element
            logger.info("getCreateEncounter RETURNING EXISTING");
            return existingEncs.get(0);
        }
    }

    /**
     * Create a new encounter using the details given in the A01 message. This may
     * also entail creating a new Mrn and Person if these don't already exist.
     *
     * @param adtMsg msg containing encounter details
     * @return the created Encounter
     * @throws MessageIgnoredException if message can't be processed
     * @throws InvalidMrnException if Mrn field is empty
     * @throws EmapStarIntegrityException if there's a contradiction in the DB
     */
    @Transactional
    public Encounter admitPatient(AdtMessage adtMsg) throws MessageIgnoredException, InvalidMrnException, EmapStarIntegrityException {
        String mrnStr = adtMsg.getMrn();
        Instant admissionTime = adtMsg.getAdmissionDateTime();
        if (mrnStr == null) {
            throw new InvalidMrnException(String.format("Missing mrn in message"));
        }
        Mrn newOrExistingMrn = findOrAddMrn(mrnStr, admissionTime, true);
        // Encounter is usually a new one for an A01, but it is
        // possible to get a second A01 if the first admission gets deleted
        // and re-made. (User corrected an error in Epic we assume).
        // Therefore need to reuse the existing encounter and the open visit if it
        // exists.
        // (Better to move the hosp visit creation to the actual "new Encounter"?)
        Encounter enc = getCreateEncounter(newOrExistingMrn, adtMsg);
        List<PatientFact> allHospitalVisits = getOpenVisitFactWhereVisitType(enc, AttributeKeyMap.HOSPITAL_VISIT);

        // This perhaps belongs in a getCreateHospitalVisit method, with an
        // InformDbDataIntegrity exception
        PatientFact hospitalVisit;
        switch (allHospitalVisits.size()) {
        case 0:
            hospitalVisit = addOpenHospitalVisit(enc, admissionTime);
            addDemographicsToEncounter(enc, adtMsg);
            break;
        case 1:
            hospitalVisit = allHospitalVisits.get(0);
            // We have received an A01 but there was already an
            // open hospital visit, so invalidate the existing bed visit and its properties
            logger.info("Invalidating previous bed visit");
            List<PatientFact> allOpenBedVisits = getOpenVisitFactWhereVisitType(enc, AttributeKeyMap.BED_VISIT);
            if (allOpenBedVisits.size() != 1) {
                throw new EmapStarIntegrityException(
                        "Found an open hospital visit with open bed visit count != 1 - hosp visit = "
                                + hospitalVisit.getFactId());
            }
            // Need to check whether it's the bed visit that corresponds to the existing
            // hospital visit?
            PatientFact openBedVisit = allOpenBedVisits.get(0);
            Instant invalidTime = adtMsg.getEventOccurredDateTime();
            openBedVisit.invalidateAll(invalidTime);
            break;
        default:
            throw new MessageIgnoredException("More than 1 (count = " + allHospitalVisits.size()
                    + ") hospital visits in encounter " + adtMsg.getVisitNumber());
        }
        // create a new bed visit with the new (or updated) location
        addOpenBedVisit(enc, adtMsg.getAdmissionDateTime(), hospitalVisit,
                adtMsg.getFullLocationString());
        enc = encounterRepo.save(enc);
        logger.info("Encounter: " + enc.toString());
        return enc;
    }

    /**
     * @param enc        the encounter to add to
     * @param adtMsg the message details to use
     */
    private void addDemographicsToEncounter(Encounter enc, AdtMessage adtMsg) {
        Map<String, PatientFact> demogs = buildPatientDemographics(adtMsg);
        demogs.forEach((k, v) -> enc.addFact(v));
    }

    /**
     * Build the demographics objects from a message but don't actually do anything
     * with them.
     *
     * @param adtMsg the msg to build demographics from
     * @return Attribute->Fact key-value pairs
     */
    private Map<String, PatientFact> buildPatientDemographics(AdtMessage adtMsg) {
        Map<String, PatientFact> demographics = new HashMap<>();
        Instant validFrom = adtMsg.getEventOccurredDateTime();
        if (validFrom == null) {
            // some messages (eg. A08) don't have an event occurred field
            validFrom = adtMsg.getRecordedDateTime();
        }
        PatientFact nameFact = new PatientFact();
        nameFact.setValidFrom(validFrom);
        nameFact.setStoredFrom(Instant.now());
        Attribute nameAttr = getCreateAttribute(AttributeKeyMap.NAME_FACT);
        nameFact.setFactType(nameAttr);
        addPropertyToFact(nameFact, AttributeKeyMap.FIRST_NAME, adtMsg.getPatientGivenName());
        addPropertyToFact(nameFact, AttributeKeyMap.MIDDLE_NAMES, adtMsg.getPatientMiddleName());
        addPropertyToFact(nameFact, AttributeKeyMap.FAMILY_NAME, adtMsg.getPatientFamilyName());
        demographics.put(AttributeKeyMap.NAME_FACT.getShortname(), nameFact);

        PatientFact generalDemoFact = new PatientFact();
        generalDemoFact.setValidFrom(validFrom);
        generalDemoFact.setStoredFrom(Instant.now());
        generalDemoFact.setFactType(getCreateAttribute(AttributeKeyMap.GENERAL_DEMOGRAPHIC));

        // will we have to worry about Instants and timezones shifting the date?
        addPropertyToFact(generalDemoFact, AttributeKeyMap.DOB, adtMsg.getPatientBirthDate());

        String hl7Sex = adtMsg.getPatientSex();
        Attribute sexAttrValue = getCreateAttribute(mapSex(hl7Sex));
        addPropertyToFact(generalDemoFact, AttributeKeyMap.SEX, sexAttrValue);

        addPropertyToFact(generalDemoFact, AttributeKeyMap.NHS_NUMBER, adtMsg.getNhsNumber());

        addPropertyToFact(generalDemoFact, AttributeKeyMap.POST_CODE, adtMsg.getPatientZipOrPostalCode());

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
     * @param visitBeginTime The start time of the visit
     * @return the hospital visit fact object
     */
    @Transactional
    private PatientFact addOpenHospitalVisit(Encounter enc, Instant visitBeginTime) {
        PatientFact visitFact = new PatientFact();
        visitFact.setValidFrom(visitBeginTime);
        visitFact.setStoredFrom(Instant.now());
        Attribute hosp = getCreateAttribute(AttributeKeyMap.HOSPITAL_VISIT);
        visitFact.setFactType(hosp);
        addArrivalTimeToVisit(visitFact, visitBeginTime);
        enc.addFact(visitFact);
        return visitFact;
    }

    /**
     * Add a new open bed visit to an existing higher-level (hospital) visit.
     *
     * @param enc            the encounter to add the Visit to
     * @param visitBeginTime when the Visit began, which could be an admission
     * @param parentVisit    the (hospital?) visit that is a parent of the new bed
     *                       visit
     * @param currentBed     bed location
     */
    @Transactional
    private void addOpenBedVisit(Encounter enc, Instant visitBeginTime, PatientFact parentVisit, String currentBed) {
        PatientFact visitFact = new PatientFact();
        visitFact.setStoredFrom(Instant.now());
        visitFact.setValidFrom(visitBeginTime);
        Attribute hosp = getCreateAttribute(AttributeKeyMap.BED_VISIT);
        visitFact.setFactType(hosp);
        addArrivalTimeToVisit(visitFact, visitBeginTime);
        addLocationToVisit(visitFact, currentBed, visitBeginTime);
        visitFact.setParentFact(parentVisit);
        enc.addFact(visitFact);
    }

    /**
     * @param visitFact  the visit fact to add to
     * @param currentBed the current bed location
     * @param validFrom  the valid from timestamp to use
     */
    private void addLocationToVisit(PatientFact visitFact, String currentBed, Instant validFrom) {
        Attribute location = getCreateAttribute(AttributeKeyMap.LOCATION);
        PatientProperty locVisProp = new PatientProperty();
        locVisProp.setValidFrom(validFrom);
        locVisProp.setStoredFrom(Instant.now());
        locVisProp.setAttribute(location);
        locVisProp.setValueAsString(currentBed);
        visitFact.addProperty(locVisProp);
    }

    /**
     * @param visitFact        the visit fact to add to
     * @param visitArrivalTime the arrival time to add
     */
    private void addArrivalTimeToVisit(PatientFact visitFact, Instant visitArrivalTime) {
        Attribute arrivalTime = getCreateAttribute(AttributeKeyMap.ARRIVAL_TIME);
        PatientProperty arrVisProp = new PatientProperty();
        arrVisProp.setValidFrom(visitArrivalTime);
        arrVisProp.setStoredFrom(Instant.now());
        arrVisProp.setValueAsDatetime(visitArrivalTime);
        arrVisProp.setAttribute(arrivalTime);
        visitFact.addProperty(arrVisProp);
    }

    /**
     * Close off the existing Visit and open a new one.
     *
     * @param adtMsg usually an A02 message but can be an A08
     * @throws MessageIgnoredException if message can't be processed
     * @throws EmapStarIntegrityException if a contradiction between DB and the incoming message or itself
     * @throws InvalidMrnException mrn not specified
     */
    @Transactional
    public void transferPatient(AdtMessage adtMsg) throws MessageIgnoredException, InvalidMrnException, EmapStarIntegrityException {
        // Docs: "The new patient location should appear in PV1-3 - Assigned Patient
        // Location while the old patient location should appear in PV1-6 - Prior
        // Patient Location."

        // Find the current PatientFact, close it off, and start a new one with its own
        // admit time + location.
        String mrnStr = adtMsg.getMrn();
        String visitNumber = adtMsg.getVisitNumber();
        Encounter encounter = encounterRepo.findEncounterByEncounter(visitNumber);

        if (encounter == null) {
            logger.warn("Received transfer for patient we don't know about - admitting them");
            encounter = admitPatient(adtMsg);
            return;
        }

        List<PatientFact> latestOpenBedVisits = getOpenVisitFactWhereVisitType(encounter, AttributeKeyMap.BED_VISIT);
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
        if (latestOpenBedVisits.isEmpty()) {
            throw new MessageIgnoredException(
                    "No open bed visit, cannot transfer, did you miss an A13? visit " + visitNumber);
        }
        PatientFact latestOpenBedVisit = latestOpenBedVisits.get(0);
        String newTransferLocation = adtMsg.getFullLocationString();
        String currentKnownLocation =
                getOnlyElement(latestOpenBedVisit.getPropertyByAttribute(AttributeKeyMap.LOCATION, p -> p.isValid())).getValueAsString();
        if (newTransferLocation.equals(currentKnownLocation)) {
            // If we get an A02 with a new location that matches where we already thought
            // the patient was,
            // don't perform an actual transfer. In the test data, this happens in a
            // minority of cases
            // following an A08 implied transfer. Let's see what it does in the real data...
            String err = "[mrn " + mrnStr + "] REDUNDANT transfer, location has not changed: " + currentKnownLocation;
            logger.warn(err);
            throw new MessageIgnoredException(err);
        }
        addDischargeToVisit(latestOpenBedVisit, eventOccurred);

        Instant admissionDateTime = adtMsg.getAdmissionDateTime();
        Instant recordedDateTime = adtMsg.getRecordedDateTime();

        String admitSource = adtMsg.getAdmitSource();
        logger.info("TRANSFERRING: MRN = " + mrnStr);
        logger.info("    A02 details: adm " + admissionDateTime);
        logger.info("    A02 details: admitsrc/event/recorded " + admitSource + "/" + eventOccurred + "/"
                + recordedDateTime);

        // add a new visit to the current encounter
        Encounter encounterDoubleCheck = latestOpenBedVisit.getEncounter();
        if (encounter != encounterDoubleCheck) {
            throw new MessageIgnoredException("Different encounter: " + encounter + " | " + encounterDoubleCheck);
        }
        List<PatientFact> hospitalVisit = getOpenVisitFactWhereVisitType(encounter, AttributeKeyMap.HOSPITAL_VISIT);
        // link the bed visit to the parent (hospital) visit
        addOpenBedVisit(encounter, eventOccurred, hospitalVisit.get(0), newTransferLocation);
    }

    /**
     * Mark the specified visit as finished.
     *
     * @param adtWrap the A03 message detailing the discharge
     * @throws MessageIgnoredException if message can't be processed
     * @throws EmapStarIntegrityException contradiction in the DB
     * @throws InvalidMrnException mrn not specified
     */
    @Transactional
    public void dischargePatient(AdtMessage adtWrap) throws MessageIgnoredException, InvalidMrnException, EmapStarIntegrityException {
        String mrnStr = adtWrap.getMrn();
        String visitNumber = adtWrap.getVisitNumber();

        Encounter encounter = encounterRepo.findEncounterByEncounter(visitNumber);
        if (encounter == null) {
            // If encounter was not known about, create it before discharging it
            if (adtWrap.getAdmissionDateTime() == null) {
                throw new MessageIgnoredException(
                        "Cannot find the visit " + visitNumber + " and we don't know the admission date so can't create an admission");
            } else {
                encounter = admitPatient(adtWrap);
            }
        }
        PatientFact latestOpenBedVisit = getOnlyElement(getOpenVisitFactWhereVisitType(encounter, AttributeKeyMap.BED_VISIT));
        if (latestOpenBedVisit == null) {
            throw new MessageIgnoredException(
                    "No open bed visit, cannot discharge, did you miss an A13? visit " + visitNumber);
        }
        Instant eventOccurred = adtWrap.getEventOccurredDateTime();
        Instant dischargeDateTime = adtWrap.getDischargeDateTime();
        logger.info("DISCHARGE: MRN " + mrnStr);
        logger.info("A03: eventtime/dischargetime " + eventOccurred + "/" + dischargeDateTime);
        if (dischargeDateTime == null) {
            throw new MessageIgnoredException("Trying to discharge but the discharge date is null");
        } else {
            // Discharge from the bed visit and the hospital visit
            addDischargeToVisit(latestOpenBedVisit, dischargeDateTime);
            PatientFact hospVisit = latestOpenBedVisit.getParentFact();
            addDischargeToVisit(hospVisit, dischargeDateTime);
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
     * Mark the visit specified by visit number as not discharged any more. Can either mean a discharge was
     * erroneously entered, or a decision to discharge was reversed.
     *
     * @param adtMsg the A13 message detailing the cancel discharge
     * @throws MessageIgnoredException if message can't be processed
     */
    @Transactional
    private void cancelDischargePatient(AdtMessage adtMsg) throws MessageIgnoredException {
        String visitNumber = adtMsg.getVisitNumber();
        // event occurred field seems to be populated despite the Epic example message showing it blank.
        Instant invalidationDate = adtMsg.getEventOccurredDateTime();
        // this must be non-null or the invalidation won't work
        if (invalidationDate == null) {
            throw new MessageIgnoredException("Trying to cancel discharge but the event occurred date is null");
        }

        Encounter encounter = encounterRepo.findEncounterByEncounter(visitNumber);
        if (encounter == null) {
            throw new MessageIgnoredException("Cannot cancel discharge for a visit that doesn't exist: " + visitNumber);
        }
        // Get the most recent bed visit.
        PatientFact mostRecentBedVisit = getVisitFactWhere(encounter,
                vf -> visitFactIsOfType(vf, AttributeKeyMap.BED_VISIT) && vf.isValid()).stream()
                        .max((vf1, vf2) -> sortVisitByDischargeTime(vf1, vf2)).get();

        // Encounters should always have at least one visit.
        if (visitFactIsOpen(mostRecentBedVisit)) {
            // This is an error. The most recent bed visit is still open. Ie. the patient
            // has not been discharged, so we cannot cancel the discharge.
            // Possible cause is that we never received the A03.
            throw new MessageIgnoredException(visitNumber + " Cannot process A13 - most recent bed visit is still open");
        }
        PatientProperty bedDischargeTime = getOnlyElement(
                mostRecentBedVisit.getPropertyByAttribute(AttributeKeyMap.DISCHARGE_TIME, p -> p.isValid()));
        // Find the hospital visit corresponding to the bed visit
        PatientFact hospitalVisit = mostRecentBedVisit.getParentFact();
        PatientProperty hospDischargeTime = getOnlyElement(hospitalVisit.getPropertyByAttribute(AttributeKeyMap.DISCHARGE_TIME, p -> p.isValid()));
        // Do the actual cancel by invalidating the discharge time properties.
        bedDischargeTime.setValidUntil(invalidationDate);
        hospDischargeTime.setValidUntil(invalidationDate);

        // The Epic spec for receiving an A13 says you can be put in a different place than the last one you were in,
        // ie. an implicit transfer. Does this ever happen for messages that Epic emits? Currently ignoring
        // the location field.

        mostRecentBedVisit = patientFactRepository.save(mostRecentBedVisit);
        hospitalVisit = patientFactRepository.save(hospitalVisit);
    }

    /**
     * Mark a Visit as finished, which can happen either when transferring or
     * discharging a patient.
     *
     * @param visit             the visit to mark as finished
     * @param dischargeDateTime the discharge/transfer time
     */
    private void addDischargeToVisit(PatientFact visit, Instant dischargeDateTime) {
        Attribute dischargeTime = getCreateAttribute(AttributeKeyMap.DISCHARGE_TIME);
        PatientProperty visProp = new PatientProperty();
        visProp.setValidFrom(dischargeDateTime);
        visProp.setStoredFrom(Instant.now());
        visProp.setValueAsDatetime(dischargeDateTime);
        visProp.setAttribute(dischargeTime);
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
     * Add a property (key-value pair) to a pre-existing fact.
     *
     * @param fact      the fact to add to
     * @param attrKM    the property key
     * @param factValue the property value
     */
    private void addPropertyToFact(PatientFact fact, AttributeKeyMap attrKM, Object factValue) {
        if (factValue != null) {
            Attribute attr = getCreateAttribute(attrKM);
            PatientProperty prop = new PatientProperty();
            prop.setValidFrom(fact.getValidFrom());
            prop.setStoredFrom(Instant.now());
            prop.setAttribute(attr);
            prop.setValue(factValue);
            fact.addProperty(prop);
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
     * @return the only element in the given list, or null if empty, throws if >1
     * @throws DuplicateValueException if >1 element in list
     */
    private <E> E getOnlyElement(List<E> list) {
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
     * @throws MessageIgnoredException if message can't be processed
     * @throws EmapStarIntegrityException if there's a contradiction in the DB
     * @throws InvalidMrnException mrn not specified
     */
    @Transactional
    private void updatePatientInfo(AdtMessage adtMsg) throws MessageIgnoredException, InvalidMrnException, EmapStarIntegrityException {
        String visitNumber = adtMsg.getVisitNumber();
        String newLocation = adtMsg.getFullLocationString();

        Encounter encounter = encounterRepo.findEncounterByEncounter(visitNumber);
        if (encounter == null) {
            // Don't infer an admission with a patient update info message, because it seems
            // these sometimes occur independently of a patient being present in hospital.
            // They can have a null admission date, which causes a failed validFrom non-null constraint.
            // We may have to be more selective about this, maybe the EVN-4 can tell us
            // what the reason/circumstances were.
            throw new MessageIgnoredException("Cannot find the visit " + visitNumber);
        }
        // Compare new demographics with old
        Map<String, PatientFact> newDemographics = buildPatientDemographics(adtMsg);
        Map<String, PatientFact> currentDemographics = getValidStoredDemographicFacts(encounter).stream()
                .collect(Collectors.toMap(f -> f.getFactType().getShortName(), f -> f));
        updateDemographics(encounter, currentDemographics, newDemographics);

        // detect when location has changed and perform a transfer
        List<PatientFact> latestOpenBedVisits = getOpenVisitFactWhereVisitType(encounter, AttributeKeyMap.BED_VISIT);
        PatientFact onlyOpenBedVisit = getOnlyElement(latestOpenBedVisits);
        if (onlyOpenBedVisit == null) {
            throw new MessageIgnoredException("Got A08 but no open bed visit for visit " + visitNumber);
        }
        PatientProperty knownlocation =
                getOnlyElement(onlyOpenBedVisit.getPropertyByAttribute(AttributeKeyMap.LOCATION, p -> p.isValid()));
        if (!newLocation.equals(knownlocation.getValueAsString())) {
            logger.warn(String.format("[mrn %s, visit num %s] IMPLICIT TRANSFER IN A08: |%s| -> |%s|", adtMsg.getMrn(),
                    visitNumber, knownlocation.getValueAsString(), newLocation));
            transferPatient(adtMsg);
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
     * @throws MessageIgnoredException if merge time in message is blank or message can't be processed
     */
    @Transactional
    private void mergeById(AdtMessage adtMsg) throws MessageIgnoredException {
        String oldMrnStr = adtMsg.getMergedPatientId();
        String survivingMrnStr = adtMsg.getMrn();
        Instant mergeTime = adtMsg.getRecordedDateTime();
        logger.info(
                "MERGE: surviving mrn " + survivingMrnStr + ", oldMrn = " + oldMrnStr + ", merge time = " + mergeTime);
        if (mergeTime == null) {
            throw new MessageIgnoredException("event occurred null");
        }

        // The non-surviving Mrn is invalidated but still points to the old person
        // (we are recording the fact that between these dates, the hospital believed
        // that the mrn belonged to this person
        Mrn oldMrn = findOrAddMrn(oldMrnStr, null, false);
        Mrn survivingMrn = findOrAddMrn(survivingMrnStr, null, false);
        if (survivingMrn == null || oldMrn == null) {
            throw new MessageIgnoredException(String.format("MRNs %s or %s (%s or %s) are not previously known, do nothing",
                    oldMrnStr, survivingMrnStr, oldMrn, survivingMrn));
        }
        Instant now = Instant.now();

        PersonMrn oldPersonMrn = getOnlyElementWhere(oldMrn.getPersons(), pm -> pm.isValidAsOf(now));

        PersonMrn survivingPersonMrn = getOnlyElementWhere(survivingMrn.getPersons(), pm -> pm.isValidAsOf(now));

        if (survivingPersonMrn == null || oldPersonMrn == null) {
            throw new MessageIgnoredException(String.format(
                    "MRNs %s and %s exist but there was no currently valid person for one/both of them (%s and %s)",
                    oldMrnStr, survivingMrnStr, oldPersonMrn, survivingPersonMrn));
        }

        // Invalidate the old person<->mrn association
        oldPersonMrn.setValidUntil(mergeTime);

        // If we already thought they were the same person, do nothing further.
        // (Wait, I don't think this can happen, because they wouldn't both be valid)
        if (oldPersonMrn.getPerson().equals(survivingPersonMrn.getPerson())) {
            throw new MessageIgnoredException(
                    String.format("We already thought that MRNs %s and %s were the same person (%s)", oldMrnStr,
                            survivingMrnStr, oldPersonMrn.getPerson().getPersonId()));
        }

        // Create a new person<->mrn association that tells us that as of the merge time
        // the old MRN is believed to belong to the person associated with the surviving
        // Mrn
        PersonMrn newOldPersonMrn = new PersonMrn(survivingPersonMrn.getPerson(), oldMrn);
        newOldPersonMrn.setStoredFrom(Instant.now());
        newOldPersonMrn.setValidFrom(mergeTime);

        newOldPersonMrn = personMrnRepo.save(newOldPersonMrn);
        oldPersonMrn = personMrnRepo.save(oldPersonMrn);
    }

    /**
     * Convert the simplified data from the pathology message into Inform-db structures,
     * and merge with existing data depending on whether it's a new order or changes to an existing one.
     * @param pathologyOrder the pathology order details, may contain results
     * @throws MessageIgnoredException if message can't be processed
     * @throws EmapStarIntegrityException contradiction in DB
     */
    @Transactional
    private void addOrUpdatePathologyOrder(PathologyOrder pathologyOrder) throws MessageIgnoredException, EmapStarIntegrityException {
        String visitNumber = pathologyOrder.getVisitNumber();
        String epicCareOrderNumber = pathologyOrder.getEpicCareOrderNumber();

        Pair<Encounter, PatientFact> encounterOrderPair = getEncounterForOrder(epicCareOrderNumber, visitNumber);
        Encounter encounter = encounterOrderPair.getLeft();
        PatientFact existingOrderRootFact = encounterOrderPair.getRight();

        // build the order fact from the message data
        PatientFact pathologyOrderRootFact = buildPathologyOrderFacts(pathologyOrder);

        logger.info("new pathology order facts: ");
        logger.info(pathologyOrderRootFact.toString());

        // If we already know about the order, use the existing order from the DB as the parent,
        // otherwise use the newly created one.
        PatientFact parent;
        if (existingOrderRootFact == null) {
            // no existing, use new fact and add it to the encounter
            parent = pathologyOrderRootFact;
            encounter.addFact(pathologyOrderRootFact);
        } else {
            // use existing fact from DB (is already added to encounter)
            parent = existingOrderRootFact;
            // will need to see if anything has changed (do orders change much?)
            // updateFact(existingFact, newFact);
        }

        parent = patientFactRepository.save(parent);
        // Build the results fact(s) from the message data, if any.
        Map<String, PatientFact> resultFactsFromOrder = buildPathologyResultsFacts(parent,
                pathologyOrder.getPathologyResults(), encounter, pathologyOrder.getTestBatteryLocalCode());

        // Some child facts - eg. sensitivities are unable to work out their
        // valid from time because status change time is missing, fill
        // this in here.
        parent.cascadeValidFrom(null);

        // Add the child (and grandchild etc) facts directly to the encounter.
        for (PatientFact child : resultFactsFromOrder.values()) {
            encounter.addFact(child);
        }
        // We will need to do some more diffing here to check whether the results have changed.

        encounter = encounterRepo.save(encounter);
    }

    /**
     * Convert order details to Inform-db structures.
     * @param order the pathology order details
     * @return a PatientFact object that represents the order
     */
    private PatientFact buildPathologyOrderFacts(PathologyOrder order) {
        Instant storedFrom = Instant.now();
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
        prop.setAttribute(getCreateAttribute(attrKM));
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
     * @param pathResults the pathology results
     * @param encounter encounter to add each fact to
     * @param testBatteryLocalCode the battery local code for the order
     * @return all descendant PatientFact objects indexed by a unique identifier
     */
    private Map<String, PatientFact> buildPathologyResultsFacts(PatientFact parent, List<? extends PathologyResult> pathResults,
            Encounter encounter, String testBatteryLocalCode) {
        Map<String, PatientFact> facts = new HashMap<>();
        Instant storedFrom = Instant.now();
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

            parent.addChildFact(fact);
            facts.put(key, fact);
            // each result can have zero or more sensitivities, which are actually just another type of order
            List<PathologyOrder> pathologySensitivities = pr.getPathologySensitivities();
            for (PathologyOrder sensOrder : pathologySensitivities) {
                // each sensitivity needs to be built as an order
                List<? extends PathologyResult> sensResults = sensOrder.getPathologyResults();
                Map<String, PatientFact> sensFacts = buildPathologyResultsFacts(fact, sensResults, encounter, sensOrder.getTestBatteryLocalCode());
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
     * @return Pair containing the Encounter object that this order is attached to and the PatientFact object that is the root
     * object representing the order, if it exists (else null).
     * @throws MessageIgnoredException if the Encounter can't be found by any method
     * @throws EmapStarIntegrityException contradiction in DB
     */
    private Pair<Encounter, PatientFact> getEncounterForOrder(String epicCareOrderNumber, String visitNumber)
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
            logger.info("existing pathology order " + epicCareOrderNumber + ": ");
            logger.info(existingPathologyOrder.toString());
        } else {
            // If seeing a result message for a previously unknown order, it should be allowed but logged as
            // a potential error, although when starting mid-HL7 stream there will always be
            // results for orders you haven't seen.
            // (also our test depends on this being allowed)
            logger.error("Couldn't find order with order number " + epicCareOrderNumber + ", searching by visit number instead");
            if (!visitNumber.isEmpty()) {
                encounter = encounterRepo.findEncounterByEncounter(visitNumber);
                if (encounter == null) {
                    throw new MessageIgnoredException("Can't find encounter to attach results to: " + visitNumber);
                }
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
     * @param startTime        If createIfNotExist, when did the Mrn first come into
     *                         existence (valid from). Ignored if !createIfNotExist
     * @param createIfNotExist whether to create if it doesn't exist
     * @return the Mrn, pre-existing or newly created, or null if it doesn't exist
     *         and !createIfNotExist
     */
    // Mrn record if it doesn't exist.
    private Mrn findOrAddMrn(String mrnStr, Instant startTime, boolean createIfNotExist) {
        List<Mrn> allMrns = mrnRepo.findByMrnString(mrnStr);
        Mrn mrn;
        if (allMrns.isEmpty()) {
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
            Instant storedFrom = Instant.now();
            mrn.setCreateDatetime(storedFrom);
            mrn.setMrn(mrnStr);
            Person pers = new Person();
            pers.setCreateDatetime(storedFrom);
            pers.addMrn(mrn, startTime, storedFrom);
            pers = personRepo.save(pers);
        } else if (allMrns.size() > 1) {
            throw new NotYetImplementedException("Does this even make sense?");
        } else {
            logger.info("Reusing an existing MRN");
            mrn = allMrns.get(0);
        }
        return mrn;
    }


}
