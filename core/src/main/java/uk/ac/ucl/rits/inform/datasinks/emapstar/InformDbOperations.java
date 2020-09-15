package uk.ac.ucl.rits.inform.datasinks.emapstar;

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
import uk.ac.ucl.rits.inform.datasinks.emapstar.dataprocessors.AdtOperation;
import uk.ac.ucl.rits.inform.datasinks.emapstar.dataprocessors.FlowsheetProcessor;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.AttributeError;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.DuplicateValueException;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.EmapStarIntegrityException;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.MessageIgnoredException;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.AttributeRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.EncounterRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.HospitalVisitRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.OldMrnRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.OldPersonRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.PatientFactRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.PersonMrnRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.PersonRepository;
import uk.ac.ucl.rits.inform.informdb.Encounter;
import uk.ac.ucl.rits.inform.informdb.MrnEncounter;
import uk.ac.ucl.rits.inform.informdb.OldAttribute;
import uk.ac.ucl.rits.inform.informdb.OldAttributeKeyMap;
import uk.ac.ucl.rits.inform.informdb.OldMrn;
import uk.ac.ucl.rits.inform.informdb.OldResultType;
import uk.ac.ucl.rits.inform.informdb.OldTemporalCore;
import uk.ac.ucl.rits.inform.informdb.PatientFact;
import uk.ac.ucl.rits.inform.informdb.PatientProperty;
import uk.ac.ucl.rits.inform.informdb.Person;
import uk.ac.ucl.rits.inform.informdb.PersonMrn;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessor;
import uk.ac.ucl.rits.inform.interchange.OldAdtMessage;
import uk.ac.ucl.rits.inform.interchange.PathologyOrder;
import uk.ac.ucl.rits.inform.interchange.PathologyResult;
import uk.ac.ucl.rits.inform.interchange.PatientInfection;
import uk.ac.ucl.rits.inform.interchange.VitalSigns;
import uk.ac.ucl.rits.inform.interchange.adt.AdtMessage;
import uk.ac.ucl.rits.inform.interchange.adt.DischargePatient;
import uk.ac.ucl.rits.inform.interchange.adt.MergeById;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * All the operations that can be performed on Inform-db.
 */
@Component
@EntityScan({"uk.ac.ucl.rits.inform.datasinks.emapstar.repos", "uk.ac.ucl.rits.inform.informdb"})
public class InformDbOperations implements EmapOperationMessageProcessor {
    @Autowired
    private AttributeRepository attributeRepo;
    @Autowired
    private OldPersonRepository oldPersonRepository;
    @Autowired
    private OldMrnRepository mrnRepo;
    @Autowired
    private EncounterRepository encounterRepo;
    @Autowired
    private PatientFactRepository patientFactRepo;
    @Autowired
    private PersonMrnRepository personMrnRepo;

    // V2
    @Autowired
    private PersonRepository personRepo;
    @Autowired
    private HospitalVisitRepository hospitalVisitRepo;
    @Autowired
    private AdtOperation adtOperation;
    @Autowired
    private FlowsheetProcessor flowsheetProcessor;

    private static final Logger logger = LoggerFactory.getLogger(InformDbOperations.class);

    @Value("${:classpath:vocab.csv}")
    private Resource vocabFile;

    /**
     * Call when you are finished with this object.
     */
    public void close() {}

    /**
     * @param encounter encounter to save
     * @return the new saved encounter
     */
    public Encounter save(Encounter encounter) {
        return encounterRepo.save(encounter);
    }


    public HospitalVisit findHospitalVisitByEncounter(String encounter) {
        return hospitalVisitRepo.findByEncounter(encounter);
    }

    /**
     * @param encounterStr encounter string to search by
     * @return the encounter found, or null if not found
     */
    public Encounter findEncounterByEncounter(String encounterStr) {
        return encounterRepo.findEncounterByEncounter(encounterStr);
    }

    /**
     * @param mrnStr mrn string to search by
     * @return mrn found, or null if not found
     */
    public OldMrn findByMrnString(String mrnStr) {
        return mrnRepo.findByMrnString(mrnStr);
    }

    /**
     * @param person person to save
     * @return new saved person
     */
    public Person save(Person person) {
        return oldPersonRepository.save(person);
    }

    /**
     * @param pf patient fact to save
     * @return newly saved patient fact
     */
    public PatientFact save(PatientFact pf) {
        return patientFactRepo.save(pf);
    }

    /**
     * @param personMrn person mrn association to save
     * @return newly saved personmrn
     */
    public PersonMrn save(PersonMrn personMrn) {
        return personMrnRepo.save(personMrn);
    }

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
                OldAttribute newAttr = new OldAttribute();
                newAttr.setAttributeId(attributeId);
                String shortname = record.get("short_name");
                newAttr.setShortName(shortname);
                String description = record.get("description");
                newAttr.setDescription(description);
                String resultType = record.get("result_type");
                newAttr.setResultType(OldResultType.valueOf(resultType));
                String validFrom = record.get("valid_from");
                newAttr.setValidFrom(Instant.parse(validFrom));
                String validUntil = record.get("valid_until");
                if (!validUntil.isEmpty()) {
                    newAttr.setValidUntil(Instant.parse(validUntil));
                }
                Optional<OldAttribute> findExistingAttr = attributeRepo.findByAttributeId(attributeId);
                if (findExistingAttr.isPresent()) {
                    // If there is pre-existing data check everything matches
                    OldAttribute existingAttr = findExistingAttr.get();
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
     * Load the vocab from the CSV file into the DB, and populate the in-memory attribute cache.
     */
    @PostConstruct
    private synchronized void populateAttributeCache() {
        ensureVocabLoaded();
        logger.info("populating attribute cache");
        if (attributeCache == null) {
            attributeCache = new HashMap<>();
            Set<OldAttribute> allAttrs = attributeRepo.findAll();
            for (OldAttribute a : allAttrs) {
                logger.info("adding to attribute cache attribute " + a.getShortName());
                attributeCache.put(a.getShortName(), a);
            }
        }
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
    public String processMessage(OldAdtMessage adtMsg) throws EmapOperationMessageProcessingException {
        Instant storedFrom = Instant.now();
        OldAdtOperation adtOperation = oldAdtOperationFactory(adtMsg, storedFrom);
        return adtOperation.processMessage();
    }

    /**
     * @param msg the ADT message to process
     * @return return code
     * @throws EmapOperationMessageProcessingException if message cannot be processed
     */
    @Override
    public String processMessage(AdtMessage msg) throws EmapOperationMessageProcessingException {
        Instant storedFrom = Instant.now();
        return adtOperation.processMessage(msg, storedFrom);
    }

    /**
     * @param msg the MergeById message to process
     * @return return code
     * @throws EmapOperationMessageProcessingException if message cannot be processed
     */
    @Override
    public String processMessage(MergeById msg) throws EmapOperationMessageProcessingException {
        Instant storedFrom = Instant.now();
        return adtOperation.processMessage(msg, storedFrom);
    }

    /**
     * @param msg the DischargePatient message to process
     * @return return code
     * @throws EmapOperationMessageProcessingException if message cannot be processed
     */
    @Override
    public String processMessage(DischargePatient msg) throws EmapOperationMessageProcessingException {
        throw new MessageIgnoredException("Not implemented yet");
    }

    @Override
    @Transactional
    public String processMessage(VitalSigns msg) throws EmapOperationMessageProcessingException {
        String returnCode = "OK";
        Instant storedFrom = Instant.now();
        flowsheetProcessor.processMessage(msg, storedFrom);

        // v1
        String visitNumber = msg.getVisitNumber();
        String mrnStr = msg.getMrn();
        Instant observationTime = msg.getObservationTimeTaken();
        Encounter enc = OldAdtOperation.getCreateEncounter(mrnStr, visitNumber, storedFrom, observationTime, this);

        PatientFact vitalSign = new PatientFact();
        vitalSign.setFactType(getCreateAttribute(OldAttributeKeyMap.VITAL_SIGN));
        vitalSign.setValidFrom(observationTime);
        vitalSign.setStoredFrom(storedFrom);

        vitalSign.addProperty(buildPatientProperty(storedFrom, observationTime,
                OldAttributeKeyMap.VITAL_SIGNS_OBSERVATION_IDENTIFIER, msg.getVitalSignIdentifier()));
        vitalSign.addProperty(buildPatientProperty(storedFrom, observationTime,
                OldAttributeKeyMap.VITAL_SIGNS_UNIT, msg.getUnit()));
        if (msg.getStringValue() != null) {
            vitalSign.addProperty(buildPatientProperty(storedFrom, observationTime,
                    OldAttributeKeyMap.VITAL_SIGNS_STRING_VALUE, msg.getStringValue()));
        }
        if (msg.getNumericValue() != null) {
            vitalSign.addProperty(buildPatientProperty(storedFrom, observationTime,
                    OldAttributeKeyMap.VITAL_SIGNS_NUMERIC_VALUE, msg.getNumericValue()));
        }
        vitalSign.addProperty(buildPatientProperty(storedFrom, observationTime,
                OldAttributeKeyMap.VITAL_SIGNS_OBSERVATION_TIME, msg.getObservationTimeTaken()));

        enc.addFact(vitalSign);
        enc = encounterRepo.save(enc);
        return returnCode;
    }

    /**
     * @param msg the PatientInfection message to process
     * @return return code
     * @throws EmapOperationMessageProcessingException if message cannot be processed
     */
    @Override
    public String processMessage(PatientInfection msg) {
        return null;
    }

    /**
     * Search for encounters in the Mrn with a given encounter number.
     * @param mrn       the Mrn to search
     * @param encounter the encounter ID to search for
     * @return all encounters that match
     */
    private List<Encounter> getEncounterWhere(OldMrn mrn, String encounter) {
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
     * @param pf the patient fact
     * @return whether it is a visit fact (ie. what used to be the VisitFact class)
     */
    private static boolean factIsVisitFact(PatientFact pf) {
        return pf.isOfType(OldAttributeKeyMap.HOSPITAL_VISIT)
                || OldAttributeKeyMap.isLocationVisitType(pf.getFactType());
    }

    /**
     * Filter for facts that are related to vital signs.
     * Better attribute metadata would be a better solution than this.
     * @param pf the patient fact
     * @return true if this is a vital signs fact
     */
    private static boolean factIsVitalSignFact(PatientFact pf) {
        String shortName = pf.getFactType().getShortName();
        // The only (current) use for this is to define demographic facts in terms of what they are not.
        // Identifying demographic facts positively would be a better approach.
        return shortName.startsWith("VIT_");
    }

    /**
     * Filter for facts that are related to pathology.
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
     * predicate pred
     */
    @Transactional
    public static List<PatientFact> getVisitFactWhere(Encounter encounter, Predicate<? super PatientFact> pred) {
        return getFactWhere(encounter, f -> factIsVisitFact(f) && pred.test(f));
    }

    /**
     * @param encounter the Encounter to search in
     * @param pred      the predicate to check for each PatientFact
     * @return all PatientFact objects in encounter which match predicate pred
     */
    @Transactional
    public static List<PatientFact> getFactWhere(Encounter encounter, Predicate<? super PatientFact> pred) {
        return getFactWhere(encounter.getFacts(), pred);
    }

    /**
     * @param encounter the Encounter to search in
     * @return all PatientFact objects in encounter which match predicate pred
     */
    @Transactional
    public static List<PatientFact> getOpenValidLocationVisit(Encounter encounter) {
        return getFactWhere(encounter.getFacts(),
                f -> visitFactIsOpenAndValid(f) && OldAttributeKeyMap.isLocationVisitType(f.getFactType()));
    }

    /**
     * @param facts the list of facts to search in
     * @param pred  the predicate to check for each PatientFact
     * @return all PatientFact objects which match predicate pred
     */
    @Transactional
    public static List<PatientFact> getFactWhere(List<PatientFact> facts, Predicate<? super PatientFact> pred) {
        if (facts == null) {
            return new ArrayList<PatientFact>();
        }
        List<PatientFact> matchingFacts = facts.stream().filter(pred).collect(Collectors.toList());
        return matchingFacts;
    }

    /**
     * @param encounter the Encounter to search in
     * @return all PatientFact objects in encounter which are NOT visit facts and
     * are valid and stored as of the present moment
     */
    static List<PatientFact> getValidStoredDemographicFacts(Encounter encounter) {
        return getDemographicFactsWhere(encounter, f -> f.isValid() && factIsStored(f));
    }

    /**
     * @param encounter the Encounter to search in
     * @param pred      the predicate to check against each demographic fact
     * @return all PatientFact objects in encounter which are NOT visit facts and
     * match pred
     */
    private static List<PatientFact> getDemographicFactsWhere(Encounter encounter, Predicate<? super PatientFact> pred) {
        /*
         * Currently we assume that all non-visit facts are demographic facts, but we
         * are going to need some richer type information for Attributes to do this
         * properly.
         */
        return getFactWhere(encounter,
                f -> !factIsVisitFact(f) && !factIsPathFact(f) && !factIsVitalSignFact(f) && pred.test(f));
    }

    /**
     * Check whether PatientFact has no discharge time property, indicating it's
     * still open.
     * @param vf the visit fact to check
     * @return whether visit is still open (ie. not discharged)
     */
    public static boolean visitFactIsOpen(PatientFact vf) {
        PatientProperty validDischargeTime = getOnlyElement(vf.getPropertyByAttribute(OldAttributeKeyMap.DISCHARGE_TIME).stream()
                .filter(p -> p.isValid()).collect(Collectors.toList()));
        return validDischargeTime == null;
    }

    /**
     * Check whether PatientFact is still open, and its valid until column is null, indicating that it has never
     * been invalidated.
     * @param vf the visit fact to check
     * @return whether visit is still open and valid as of the present moment
     */
    private static boolean visitFactIsOpenAndValid(PatientFact vf) {
        return visitFactIsOpen(vf) && vf.isValid();
    }

    /**
     * Check whether PatientFact's stored_until column is null, indicating that it
     * has not been unstored (deleted). Note: this does not perform time travel (ie.
     * check whether stored_until is null or in the future) Note: the storedness of the
     * underlying properties is not checked
     * @param pf the patient fact to check
     * @return whether fact is stored as of the present moment
     */
    private static boolean factIsStored(OldTemporalCore pf) {
        Instant storedUntil = pf.getStoredUntil();
        return storedUntil == null;
    }

    /**
     * @param encounter the Encounter to search in
     * @param attr      the type to match against
     * @return all open and valid Visit objects of the specified type for the
     * Encounter
     */
    @Transactional
    public static List<PatientFact> getOpenVisitFactWhereVisitType(Encounter encounter, OldAttributeKeyMap attr) {
        return getVisitFactWhere(encounter, vf -> vf.isOfType(attr) && visitFactIsOpenAndValid(vf));
    }

    /**
     * @param encounter the Encounter to search in
     * @return all closed and valid location Visits for the
     * Encounter
     */
    public static List<PatientFact> getClosedLocationVisitFact(Encounter encounter) {
        return getVisitFactWhere(encounter,
                vf -> OldAttributeKeyMap.isLocationVisitType(vf.getFactType()) && !visitFactIsOpen(vf) && vf.isValid());
    }

    /**
     * Determine visit type from the patient class (which ultimately comes from HL7).
     * @param patientClass string from HL7
     * @return the fact type of the visit fact
     * @throws MessageIgnoredException if patient class is not recognised or
     *                                 shouldn't appear in a visit-generating
     *                                 message
     */
    public static OldAttributeKeyMap visitTypeFromPatientClass(String patientClass) throws MessageIgnoredException {
        // For now everything's a bed visit, and we're not using AttributeKeyMap.OUTPATIENT_VISIT.
        // The patient class is also being separately recorded so this can be used if needed.
        return OldAttributeKeyMap.BED_VISIT;
    }

    /**
     * @param encounter  the encounter to add to
     * @param adtMsg     the message details to use
     * @param storedFrom storedFrom value to use for new records
     * @return whether any demographics were actually added/modified
     */
    static boolean addOrUpdateDemographics(Encounter encounter, OldAdtMessage adtMsg, Instant storedFrom) {
        // Compare new demographics with old
        Map<String, PatientFact> newDemographics = InformDbOperations.buildPatientDemographics(adtMsg, storedFrom);
        Map<String, PatientFact> currentDemographics = InformDbOperations.getValidStoredDemographicFacts(encounter).stream()
                .collect(Collectors.toMap(f -> f.getFactType().getShortName(), f -> f));
        return InformDbOperations.updateDemographics(encounter, currentDemographics, newDemographics);
    }

    /**
     * Build the demographics objects from a message but don't actually do anything
     * with them. Include visit related facts like patient class because these are
     * treated very similarly.
     * @param adtMsg     the msg to build demographics from
     * @param storedFrom storedFrom value to use for new records
     * @return Attribute->Fact key-value pairs
     */
    static Map<String, PatientFact> buildPatientDemographics(OldAdtMessage adtMsg, Instant storedFrom) {
        Map<String, PatientFact> demographics = new HashMap<>();
        Instant validFrom = adtMsg.getEventOccurredDateTime();
        if (validFrom == null) {
            // some messages (eg. A08) don't have an event occurred field
            validFrom = adtMsg.getRecordedDateTime();
        }
        PatientFact nameFact = new PatientFact();
        nameFact.setValidFrom(validFrom);
        nameFact.setStoredFrom(storedFrom);
        OldAttribute nameAttr = getCreateAttribute(OldAttributeKeyMap.NAME_FACT);
        nameFact.setFactType(nameAttr);
        addPropertyToFact(nameFact, storedFrom, OldAttributeKeyMap.FIRST_NAME, adtMsg.getPatientGivenName());
        addPropertyToFact(nameFact, storedFrom, OldAttributeKeyMap.MIDDLE_NAMES, adtMsg.getPatientMiddleName());
        addPropertyToFact(nameFact, storedFrom, OldAttributeKeyMap.FAMILY_NAME, adtMsg.getPatientFamilyName());
        demographics.put(nameFact.getFactType().getShortName(), nameFact);

        PatientFact generalDemoFact = new PatientFact();
        generalDemoFact.setValidFrom(validFrom);
        generalDemoFact.setStoredFrom(storedFrom);
        generalDemoFact.setFactType(getCreateAttribute(OldAttributeKeyMap.GENERAL_DEMOGRAPHIC));
        // will we have to worry about Instants and timezones shifting the date?
        addPropertyToFact(generalDemoFact, storedFrom, OldAttributeKeyMap.DOB, adtMsg.getPatientBirthDate());
        String hl7Sex = adtMsg.getPatientSex();
        OldAttribute sexAttrValue = getCreateAttribute(mapSex(hl7Sex));
        addPropertyToFact(generalDemoFact, storedFrom, OldAttributeKeyMap.SEX, sexAttrValue);
        addPropertyToFact(generalDemoFact, storedFrom, OldAttributeKeyMap.NHS_NUMBER, adtMsg.getNhsNumber());
        addPropertyToFact(generalDemoFact, storedFrom, OldAttributeKeyMap.POST_CODE, adtMsg.getPatientZipOrPostalCode());
        demographics.put(generalDemoFact.getFactType().getShortName(), generalDemoFact);

        // death fact
        OldAttribute deathIndicator = getBooleanAttribute(adtMsg.getPatientDeathIndicator());
        PatientFact deathFact = new PatientFact();
        deathFact.setFactType(getCreateAttribute(OldAttributeKeyMap.PATIENT_DEATH_FACT));
        deathFact.setValidFrom(validFrom);
        deathFact.setStoredFrom(storedFrom);
        deathFact.addProperty(
                buildPatientProperty(storedFrom, validFrom, OldAttributeKeyMap.PATIENT_DEATH_INDICATOR, deathIndicator));
        // set death time regardless of whether death boolean is set, sometimes they
        // contradict each other and we need to delegate interpretation of this
        // further down the pipeline :(
        if (adtMsg.getPatientDeathDateTime() != null) {
            deathFact.addProperty(buildPatientProperty(storedFrom, validFrom, OldAttributeKeyMap.PATIENT_DEATH_TIME,
                    adtMsg.getPatientDeathDateTime()));
        }
        demographics.put(deathFact.getFactType().getShortName(), deathFact);

        return demographics;
    }

    /**
     * A little mapping table to convert HL7 sex to Inform-db sex.
     * @param hl7Sex hl7 sex
     * @return Inform-db sex
     */
    private static OldAttributeKeyMap mapSex(String hl7Sex) {
        if (hl7Sex == null) {
            return OldAttributeKeyMap.UNKNOWN;
        }
        switch (hl7Sex) {
            case "M":
                return OldAttributeKeyMap.MALE;
            case "F":
                return OldAttributeKeyMap.FEMALE;
            case "A":
                return OldAttributeKeyMap.OTHER;
            case "O":
                return OldAttributeKeyMap.OTHER;
            case "U":
            default:
                return OldAttributeKeyMap.UNKNOWN;
        }
    }

    /**
     * Add a hospital visit fact to the specified Encounter. This visit is open, ie.
     * ongoing.
     * @param enc            the Encounter to add to
     * @param storedFrom     storedFrom value to use for new records
     * @param visitBeginTime The start time of the visit
     * @param patientClass   the patient class
     * @return the hospital visit fact object
     */
    @Transactional
    static PatientFact addOpenHospitalVisit(Encounter enc, Instant storedFrom, Instant visitBeginTime, String patientClass) {
        PatientFact visitFact = new PatientFact();
        visitFact.setValidFrom(visitBeginTime);
        visitFact.setStoredFrom(storedFrom);
        OldAttribute hosp = getCreateAttribute(OldAttributeKeyMap.HOSPITAL_VISIT);
        visitFact.setFactType(hosp);
        visitFact.addProperty(
                buildPatientProperty(storedFrom, visitBeginTime, OldAttributeKeyMap.ARRIVAL_TIME, visitBeginTime));
        // Patient Class belongs in the hospital visit because it's then easier to query it if needed
        // instead of digging it out of bed visits.
        visitFact.addProperty(
                buildPatientProperty(storedFrom, visitBeginTime, OldAttributeKeyMap.PATIENT_CLASS, patientClass));
        enc.addFact(visitFact);
        return visitFact;
    }

    /**
     * Turn a Boolean into an Emap-Star attribute.
     * @param booleanValue the normal Boolean
     * @return Emap-Star attributes BOOLEAN_TRUE and BOOLEAN_FALSE for true and false, or null for null
     */
    private static OldAttribute getBooleanAttribute(Boolean booleanValue) {
        if (booleanValue == null) {
            return null;
        } else if (booleanValue.booleanValue()) {
            return getCreateAttribute(OldAttributeKeyMap.BOOLEAN_TRUE);
        } else {
            return getCreateAttribute(OldAttributeKeyMap.BOOLEAN_FALSE);
        }
    }

    /**
     * Add a property to a fact if it doesn't already exist with an identical value,
     * otherwise replace it with new version.
     * @param fact    fact to update
     * @param newProp new version of property to use as replacement if necessary
     * @return true iff anything was changed
     */
    public boolean addOrUpdateProperty(PatientFact fact, PatientProperty newProp) {
        OldAttribute propertyType = newProp.getPropertyType();
        List<PatientProperty> currentProps = fact.getPropertyByAttribute(propertyType, PatientProperty::isValid);
        // In the specific case where there is exactly one valid property and it matches the new value, do nothing.
        // Otherwise invalidate all properties and create a new property.
        if (currentProps.size() == 1) {
            PatientProperty onlyProp = currentProps.get(0);
            if (newProp.equals(onlyProp)) {
                return false;
            }
        }
        // there should only be one, but invalidate all just to be sure
        Instant invalidationTime = newProp.getValidFrom();
        for (PatientProperty prop : currentProps) {
            prop.invalidateProperty(newProp.getStoredFrom(), invalidationTime, null);
        }
        fact.addProperty(newProp);
        return true;
    }

    /**
     * Compare function for visits by discharge time. Missing discharge time sorts as "high", ie. it is considered the most recent.
     * @param v1 visit to compare 1
     * @param v2 visit to compare 2
     * @return result of compareTo called on the discharge timestamps, ie. dischV1.compareTo(dischV2)
     */
    public static int sortVisitByDischargeTime(PatientFact v1, PatientFact v2) {
        PatientProperty dischProp1 = getOnlyElement(v1.getPropertyByAttribute(OldAttributeKeyMap.DISCHARGE_TIME, p -> p.isValid()));
        Instant dischV1 = Instant.MAX;
        if (dischProp1 != null) {
            dischV1 = dischProp1.getValueAsDatetime();
        }
        PatientProperty dischProp2 = getOnlyElement(v2.getPropertyByAttribute(OldAttributeKeyMap.DISCHARGE_TIME, p -> p.isValid()));
        Instant dischV2 = Instant.MAX;
        if (dischProp2 != null) {
            dischV2 = dischProp2.getValueAsDatetime();
        }
        return dischV1.compareTo(dischV2);
    }

    private static Map<String, OldAttribute> attributeCache = null;

    /**
     * Return a cached, persisted Attribute object with the given enum value.
     * @param attrKM the enum value of the attribute
     * @return the Attribute object from the cache
     */
    @Transactional
    public static OldAttribute getCreateAttribute(OldAttributeKeyMap attrKM) {
        OldAttribute attribute = attributeCache.get(attrKM.getShortname());
        if (attribute != null) {
            return attribute;
        } else {
            throw new AttributeError("Tried to use attribute but wasn't found in db: " + attrKM.getShortname());
        }
    }

    /**
     * Add a property (key-value pair) to a pre-existing fact, only if its value is non-null.
     * <p>
     * Mainly kept for backwards compatibility, consider using buildPatientProperty directly instead.
     * @param fact         the fact to add to
     * @param storedFrom   storedFrom time to use for new records
     * @param propertyType the property key
     * @param factValue    the property value
     */
    private static void addPropertyToFact(PatientFact fact, Instant storedFrom, OldAttributeKeyMap propertyType, Object factValue) {
        if (factValue != null) {
            fact.addProperty(buildPatientProperty(storedFrom, fact.getValidFrom(), propertyType, factValue));
        }
    }

    /**
     * Filter on a predicate where at most one element should satisfy it.
     * @param <E>  the type of the list elements
     * @param list the list to look in
     * @param pred the predicate to test with
     * @return the only element that satisfies it, or null if there are none that do
     * @throws DuplicateValueException if more than one element satisfies pred
     */
    public static <E> E getOnlyElementWhere(List<E> list, Predicate<? super E> pred) {
        List<E> persons = list.stream().filter(pred).collect(Collectors.toList());
        return getOnlyElement(persons);
    }

    /**
     * @param <E>  the type of the list elements
     * @param list the given list
     * @return the only element in the given list, or null if empty or null, throws if >1
     * @throws DuplicateValueException if >1 element in list
     */
    static <E> E getOnlyElement(List<E> list) {
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
     * Construct an old ADT handler wrapper object.
     * @param adtMsg     the ADT Interchange message
     * @param storedFrom storedFrom time to use for new records
     * @return the newly constructed object
     * @throws MessageIgnoredException if message is being ignored
     */
    private OldAdtOperation oldAdtOperationFactory(OldAdtMessage adtMsg, Instant storedFrom) throws MessageIgnoredException {
        return new OldAdtOperation(this, adtMsg, storedFrom);
    }


    /**
     * If demographics have changed, update them and invalidate the old.
     * @param encounter           the existing encounter that we may need to modify
     *                            demographics of
     * @param currentDemographics existing demographics (eg. from the db)
     * @param newDemographics     new demographics (eg. from the current message)
     * @return whether any demographics were actually added/modified
     */
    static boolean updateDemographics(Encounter encounter, Map<String, PatientFact> currentDemographics,
                                      Map<String, PatientFact> newDemographics) {
        logger.info(String.format("Update demographics (CSN = %s): comparing %d existing demographic facts to %d new facts",
                encounter.getEncounter(), currentDemographics.size(), newDemographics.size()));
        boolean anyChanged = false;
        for (String newKey : newDemographics.keySet()) {
            PatientFact newFact = newDemographics.get(newKey);
            PatientFact currentFact = currentDemographics.get(newKey);
            if (currentFact == null) {
                logger.info("fact does not exist, adding " + newFact.getFactType().getShortName());
                encounter.addFact(newFact);
                anyChanged = true;
            } else {
                if (newFact.equals(currentFact)) {
                    logger.info("fact exists and matches, no action: " + currentFact.getFactType().getShortName());
                } else {
                    // Just invalidate the entire fact and write in the new one.
                    // May try this on a per-property basis in future.
                    Instant invalidationDate = newFact.getValidFrom();
                    logger.info(
                            "fact exists but does not match, replacing: " + currentFact.getFactType().getShortName());
                    currentFact.invalidateAll(newFact.getStoredFrom(), invalidationDate);
                    encounter.addFact(newFact);
                    anyChanged = true;
                }
            }
        }
        return anyChanged;
    }

    /**
     * Convert the simplified data from the pathology message into Inform-db structures,
     * and merge with existing data depending on whether it's a new order or changes to an existing one.
     * @param pathologyOrder the pathology order details, may contain results
     * @param storedFrom     storedFrom time to use for new records
     * @throws MessageIgnoredException    if message can't be processed
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
                            && f.isOfType(OldAttributeKeyMap.PATHOLOGY_TEST_RESULT));
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
                    if (existingResult.equalsPathologyResult(newFact)) {
                        logger.debug(
                                String.format("Ignoring fact, is equal to existing: %s", existingResult.toString()));
                    } else {
                        logger.debug(
                                String.format(
                                        "Fact exists but needs updating.\n    Existing: %s\n    New: %s",
                                        existingResult.toString(), newFact.toString()));
                        updatePathologyResult(existingResult, newFact, storedFrom);
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
     * Crude but it works - just invalidate the whole fact and replace it with a new version.
     * newFact is a replacement result (with no intermediate "cancel" message), so the invalidation
     * time is taken from the start time of the new result.
     * @param existingResult  the result to replace
     * @param newFact         the result to replace it with
     * @param storedFromUntil the time at which this change takes place in the DB
     */
    private void updatePathologyResult(PatientFact existingResult, PatientFact newFact, Instant storedFromUntil) {
        Instant invalidationDate = getOnlyElement(
                newFact.getPropertyByAttribute(OldAttributeKeyMap.PATHOLOGY_RESULT_TIME, PatientProperty::isValid))
                .getValueAsDatetime();
        PatientFact order = existingResult.getParentFact();
        existingResult.invalidateAll(storedFromUntil, invalidationDate);
        logger.warn(String.format("Old fact validity %s -> %s", existingResult.getValidFrom(), invalidationDate));
        order.addChildFact(newFact);
    }

    /**
     * A key for uniquely identifying pathology results *from the database*.
     * Must give equivalent results to method {@link #uniqueKeyFromPathologyResultMessage}.
     * Allows updated results to be compared with existing results.
     * @param pathologyResultFact the pathology result fact
     * @return a key for the result that is unique within the order
     */
    private String uniqueKeyFromPathologyResultFact(PatientFact pathologyResultFact) {
        PatientFact orderFact = pathologyResultFact.getParentFact();
        PatientProperty testCode = getOnlyElement(pathologyResultFact.getPropertyByAttribute(OldAttributeKeyMap.PATHOLOGY_TEST_CODE));
        PatientProperty batteryCode = getOnlyElement(orderFact.getPropertyByAttribute(OldAttributeKeyMap.PATHOLOGY_TEST_BATTERY_CODE));
        PatientProperty isolCode = getOnlyElement(pathologyResultFact.getPropertyByAttribute(OldAttributeKeyMap.PATHOLOGY_ISOLATE_CODE));
        return String.format("%s_%s_%s", batteryCode.getValueAsString(), testCode.getValueAsString(),
                isolCode == null ? "" : isolCode.getValueAsString());
    }

    /**
     * A key for uniquely identifying pathology results *from an interchange message*.
     * Must give equivalent results to method {@link #uniqueKeyFromPathologyResultFact}.
     * Allows updated results to be compared with existing results.
     * @param testBatteryLocalCode   the battery code for the message
     * @param pathologyResultMessage the pathology result message
     * @return a key for the result that is unique within the order
     */
    private String uniqueKeyFromPathologyResultMessage(String testBatteryLocalCode, PathologyResult pathologyResultMessage) {
        return String.format("%s_%s_%s", testBatteryLocalCode, pathologyResultMessage.getTestItemLocalCode(),
                pathologyResultMessage.getIsolateLocalCode());
    }

    /**
     * Convert order details from an Emap-Interchange message to Emap-Star structures.
     * @param order      the pathology order details
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
        pathFact.setFactType(getCreateAttribute(OldAttributeKeyMap.PATHOLOGY_ORDER));
        pathFact.setValidFrom(validFrom);
        pathFact.setStoredFrom(storedFrom);
        pathFact.addProperty(buildPatientProperty(storedFrom, validFrom, OldAttributeKeyMap.PATHOLOGY_ORDER_CONTROL_ID,
                order.getOrderControlId()));
        pathFact.addProperty(buildPatientProperty(storedFrom, validFrom, OldAttributeKeyMap.PATHOLOGY_EPIC_ORDER_NUMBER,
                order.getEpicCareOrderNumber()));
        pathFact.addProperty(buildPatientProperty(storedFrom, validFrom, OldAttributeKeyMap.PATHOLOGY_TEST_BATTERY_CODE,
                order.getTestBatteryLocalCode()));
        pathFact.addProperty(buildPatientProperty(storedFrom, validFrom, OldAttributeKeyMap.PATHOLOGY_LAB_NUMBER,
                order.getLabSpecimenNumber()));
        pathFact.addProperty(buildPatientProperty(storedFrom, validFrom, OldAttributeKeyMap.PATHOLOGY_OCS_NUMBER,
                order.getLabSpecimenNumberOCS()));
        pathFact.addProperty(buildPatientProperty(storedFrom, validFrom, OldAttributeKeyMap.PATHOLOGY_COLLECTION_TIME,
                order.getObservationDateTime()));
        pathFact.addProperty(buildPatientProperty(storedFrom, validFrom, OldAttributeKeyMap.PATHOLOGY_ORDER_TIME,
                order.getOrderDateTime()));
        pathFact.addProperty(buildPatientProperty(storedFrom, validFrom, OldAttributeKeyMap.PATHOLOGY_ORDER_PATIENT_TYPE,
                order.getOrderType()));
        pathFact.addProperty(buildPatientProperty(storedFrom, validFrom, OldAttributeKeyMap.PATHOLOGY_ORDER_ORDER_STATUS,
                order.getOrderStatus()));
        pathFact.addProperty(buildPatientProperty(storedFrom, validFrom, OldAttributeKeyMap.PATHOLOGY_ORDER_RESULT_STATUS,
                order.getResultStatus()));
        pathFact.addProperty(buildPatientProperty(storedFrom, validFrom, OldAttributeKeyMap.PATHOLOGY_LAB_DEPARTMENT_CODE,
                order.getLabDepartment()));

        // Status change time is only given to us once per order/battery result, but we apply it
        // to each result within the order and call it the result time, because results can be returned bit by bit
        // so results within a battery may have different times.
        // Here, we also save it as the generic last status change time.
        pathFact.addProperty(buildPatientProperty(storedFrom, validFrom, OldAttributeKeyMap.PATHOLOGY_STATUS_CHANGE_TIME,
                order.getStatusChangeTime()));

        return pathFact;
    }

    /**
     * Build a patient property given the key/value pair.
     * @param storedFrom the stored from temporal field
     * @param validFrom  the valid from temporal field
     * @param attrKM     the attribute key value
     * @param value      the actual value
     * @return the constructed PatientProperty
     */
    public static PatientProperty buildPatientProperty(Instant storedFrom, Instant validFrom, OldAttributeKeyMap attrKM,
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
     * @param parent               the parent PatientFact, either from the DB or newly constructed
     * @param storedFrom           storedFrom time to use for new records
     * @param pathResults          the pathology results
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
            fact.setFactType(getCreateAttribute(OldAttributeKeyMap.PATHOLOGY_TEST_RESULT));

            String key = uniqueKeyFromPathologyResultMessage(testBatteryLocalCode, pr);

            fact.addProperty(buildPatientProperty(storedFrom, resultTime, OldAttributeKeyMap.PATHOLOGY_TEST_BATTERY_CODE,
                    testBatteryLocalCode));
            fact.addProperty(buildPatientProperty(storedFrom, resultTime, OldAttributeKeyMap.PATHOLOGY_TEST_CODE,
                    pr.getTestItemLocalCode()));
            fact.addProperty(buildPatientProperty(storedFrom, resultTime, OldAttributeKeyMap.PATHOLOGY_ISOLATE_CODE,
                    pr.getIsolateLocalCode()));
            PatientProperty result = buildPatientProperty(storedFrom, resultTime, OldAttributeKeyMap.PATHOLOGY_NUMERIC_VALUE,
                    pr.getNumericValue());
            result.setValueAsString(pr.getStringValue());
            fact.addProperty(result);
            fact.addProperty(
                    buildPatientProperty(storedFrom, resultTime, OldAttributeKeyMap.PATHOLOGY_UNITS, pr.getUnits()));
            fact.addProperty(
                    buildPatientProperty(storedFrom, resultTime, OldAttributeKeyMap.PATHOLOGY_REFERENCE_RANGE, pr.getReferenceRange()));
            fact.addProperty(
                    buildPatientProperty(storedFrom, resultTime, OldAttributeKeyMap.PATHOLOGY_RESULT_TIME, resultTime));
            fact.addProperty(
                    buildPatientProperty(storedFrom, resultTime, OldAttributeKeyMap.PATHOLOGY_RESULT_STATUS, pr.getResultStatus()));
            fact.addProperty(
                    buildPatientProperty(storedFrom, resultTime, OldAttributeKeyMap.RESULT_NOTES, pr.getNotes()));

            // Check for duplicates within the given set of results; warn of their presence
            // and just monitor for differences for the time being.
            // (We know we're seeing duplicates but we don't know if the actual values are
            // ever different).
            PatientFact existing = facts.get(key);
            if (existing != null) {
                String details = "";
                if (!existing.equals(fact)) {
                    details = String.format("\nExisting = %s\nSubsequent = %s", existing, fact);
                }
                logger.warn(String.format("Pathology %s within-message duplicate result not added! Is full duplicate?: %s%s",
                        key, existing.equals(fact), details));
            } else {
                parent.addChildFact(fact);
                facts.put(key, fact);
                // each result can have zero or more sensitivities, which are actually just another type of order
                List<PathologyOrder> pathologySensitivities = pr.getPathologySensitivities();
                for (PathologyOrder sensOrder : pathologySensitivities) {
                    // each sensitivity needs to be built as an order
                    List<? extends PathologyResult> sensResults = sensOrder.getPathologyResults();
                    Map<String, PatientFact> sensFacts = buildPathologyResultsFacts(fact, storedFrom, sensResults,
                            sensOrder.getTestBatteryLocalCode());
                    facts.putAll(sensFacts);
                }
            }
        }
        return facts;
    }

    /**
     * Look up an encounter by an existing order number, or by the encounter number if
     * the order is previously unknown.
     * Move to repo?
     * @param epicCareOrderNumber the Epic order number to search by
     * @param visitNumber         the encounter/visit number to search by
     * @param mrnStr              MRN string to use if an MRN record needs creating
     * @param storedFrom          storedFrom time to use for new records
     * @param backupValidFrom     validFrom time to use if encounter/mrn/person needs creating
     * @return Pair containing the Encounter object that this order is attached to and the PatientFact object that is the root
     * object representing the order, if it exists (else null).
     * @throws MessageIgnoredException    if the Encounter can't be found by any method
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
                    patientFactRepo.findAllPathologyOrdersByOrderNumber(epicCareOrderNumber));
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
                encounter = OldAdtOperation.getCreateEncounter(mrnStr, visitNumber, storedFrom, backupValidFrom, this);
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
}
