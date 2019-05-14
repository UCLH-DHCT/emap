package uk.ac.ucl.rits.inform;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hibernate.Session;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v27.message.ADT_A01;
import ca.uhn.hl7v2.parser.PipeParser;
import uk.ac.ucl.rits.inform.hl7.AdtWrap;
import uk.ac.ucl.rits.inform.ids.IdsMaster;
import uk.ac.ucl.rits.inform.ids.IdsOperations;
import uk.ac.ucl.rits.inform.informdb.Attribute;
import uk.ac.ucl.rits.inform.informdb.AttributeKeyMap;
import uk.ac.ucl.rits.inform.informdb.AttributeRepository;
import uk.ac.ucl.rits.inform.informdb.Encounter;
import uk.ac.ucl.rits.inform.informdb.EncounterRepository;
import uk.ac.ucl.rits.inform.informdb.IdsEffectLogging;
import uk.ac.ucl.rits.inform.informdb.IdsEffectLoggingRepository;
import uk.ac.ucl.rits.inform.informdb.IdsProgress;
import uk.ac.ucl.rits.inform.informdb.IdsProgressRepository;
import uk.ac.ucl.rits.inform.informdb.Mrn;
import uk.ac.ucl.rits.inform.informdb.MrnRepository;
import uk.ac.ucl.rits.inform.informdb.PatientDemographicFact;
import uk.ac.ucl.rits.inform.informdb.PatientDemographicFactRepository;
import uk.ac.ucl.rits.inform.informdb.PatientDemographicProperty;
import uk.ac.ucl.rits.inform.informdb.Person;
import uk.ac.ucl.rits.inform.informdb.PersonRepository;
import uk.ac.ucl.rits.inform.informdb.VisitFact;
import uk.ac.ucl.rits.inform.informdb.VisitFactRepository;
import uk.ac.ucl.rits.inform.informdb.VisitProperty;

@Component
@EntityScan("uk.ac.ucl.rits.inform.informdb")
public class InformDbOperations {
    @Autowired
    private AttributeRepository attributeRepository;
    @Autowired
    private PersonRepository personRepo;
    @Autowired
    private MrnRepository mrnRepo;
    @Autowired
    private EncounterRepository encounterRepo;
    @Autowired
    private PatientDemographicFactRepository patientDemographicFactRepository;
    @Autowired
    private VisitFactRepository visitFactRepository;
    @Autowired
    private IdsProgressRepository idsProgressRepository;
    @Autowired
    private IdsEffectLoggingRepository idsEffectLoggingRepository;

    private final static Logger logger = LoggerFactory.getLogger(InformDbOperations.class);

    @Autowired
    private IdsOperations idsOperations;
    
    public void close() {
    }

    /**
     * Wrapper for the entire transaction that performs: - read latest processed ID
     * from Inform-db (ETL metadata) - process the message and write to Inform-db -
     * write the latest processed ID to reflect the above message.
     * Blocks until there are new messages.
     * 
     * @param parser the HAPI parser to be used
     * 
     * @return number of messages processes
     */
    @Transactional(rollbackFor = HL7Exception.class)
    public int processNextHl7(PipeParser parser, List<String> parsingErrors) throws HL7Exception {
        int lastProcessedId = getLatestProcessedId();
        IdsMaster idsMsg = getNextHL7IdsRecordBlocking(lastProcessedId);
        IdsEffectLogging idsLog = new IdsEffectLogging();
        idsLog.setProcessingStartTime(Instant.now());
        idsLog.setIdsUnid(idsMsg.getUnid());
        idsLog.setMrn(idsMsg.getHospitalnumber());
        idsLog.setMessageType(idsMsg.getMessagetype());
        Timestamp messagedatetime = idsMsg.getMessagedatetime();
        if (messagedatetime != null) {
            idsLog.setMessageDatetime(messagedatetime.toInstant());
        }
        int processed = 0;
        String hl7msg = idsMsg.getHl7message();
        // HL7 is supposed to use \r for line endings, but
        // the IDS uses \n
        hl7msg = hl7msg.replace("\n", "\r");
        Message msgFromIds;
        try {
            msgFromIds = parser.parse(hl7msg);
        }
        catch (HL7Exception hl7e) {
            String errString = "[" + idsMsg.getUnid() + "]  HL7 parsing error";
            hl7e.printStackTrace();
            // Mark the message as processed even though we couldn't parse it,
            // but record it for later debugging.
            parsingErrors.add(errString);
            logger.info(errString);
            idsLog.setMessage(errString);
            setLatestProcessedId(idsMsg.getUnid());
            return processed;
        }
        finally {
            idsLog = idsEffectLoggingRepository.save(idsLog);
        }

        try {
            AdtWrap adtWrap = new AdtWrap(msgFromIds);
            if (adtWrap.getTriggerEvent().equals("A01")) {
                logger.info("[" + idsMsg.getUnid() + "] A01, admission ");
                Encounter enc = addEncounter(adtWrap);
                logger.info("[" + idsMsg.getUnid() + "] Done A01, encounter: " + enc.toString());
                processed += 1;
            }
            else if (adtWrap.getTriggerEvent().equals("A02")) {
                logger.info("[" + idsMsg.getUnid() + "] A02, transfer");
                transferPatient(adtWrap);
                processed += 1;
            } else if (adtWrap.getTriggerEvent().equals("A03")) {
                logger.info("[" + idsMsg.getUnid() + "] A03, discharge");
                dischargePatient(adtWrap);
                processed += 1;
            } else if (adtWrap.getTriggerEvent().equals("A08")) {
                logger.info("[" + idsMsg.getUnid() + "] A08, update patient info");
                updatePatientInfo(adtWrap);
                processed += 1;
            } else {
                logger.debug("[" + idsMsg.getUnid() + "] Skipping " + adtWrap.getTriggerEvent() + " ("
                        + msgFromIds.getClass() + ")");
                idsLog.setMessage("Skipping due to message type");
            }
        }
        catch (HL7Exception e) {
            String errMsg = "[" + idsMsg.getUnid() + "] Skipping due to HL7Exception " + e + " (" + msgFromIds.getClass()
                    + ")";
            idsLog.setMessage(errMsg);
            logger.warn(errMsg);
        }
        catch (InvalidMrnException e) {
            String errMsg = "[" + idsMsg.getUnid() + "] Skipping due to invalid Mrn " + e + " (" + msgFromIds.getClass()
            + ")";
            idsLog.setMessage(errMsg);
            logger.warn(errMsg);
        }
        catch (MessageIgnoredException e) {
            idsLog.setMessage(e.getClass() + " " + e.getMessage());
        }
        finally {
            idsLog = idsEffectLoggingRepository.save(idsLog);
        }
        idsLog.setProcessingEndTime(Instant.now());
        idsLog = idsEffectLoggingRepository.save(idsLog);
        setLatestProcessedId(idsMsg.getUnid());

        return processed;
    }

    @Transactional
    public int getLatestProcessedId() {
        IdsProgress onlyRow = idsProgressRepository.findOnlyRow();
        if (onlyRow == null) {
            onlyRow = new IdsProgress();
            // Is it wrong to set in a get?
            onlyRow = idsProgressRepository.save(onlyRow);
        }
        return onlyRow.getLastProcessedIdsUnid();
    }

    @Transactional
    public void setLatestProcessedId(int lastProcessedIdsUnid) {
        IdsProgress onlyRow = idsProgressRepository.findOnlyRow();
        onlyRow.setLastProcessedIdsUnid(lastProcessedIdsUnid);
        idsProgressRepository.save(onlyRow);
    }

    public IdsMaster getNextHL7IdsRecordBlocking(int lastProcessedId) {
        long secondsSleep = 10;
        IdsMaster idsMsg = null;
        while (true) {
            idsMsg = getNextHL7IdsRecord(lastProcessedId);
            if (idsMsg == null) {
                logger.info("No more messages, retrying in " + secondsSleep + " seconds");
                try {
                    Thread.sleep(secondsSleep * 1000);
                }
                catch (InterruptedException ie) {
                }
            }
            else {
                break;
            }
        }
        return idsMsg;
    }

    /**
     * Get next entry in the IDS, if it exists
     * @param lastProcessedId the last one we have successfully processed
     *
     * @return the first message that comes after lastProcessedId, or null if there isn't one
     */
    public IdsMaster getNextHL7IdsRecord(int lastProcessedId) {
        // consider changing to "get next N messages" for more efficient database performance
        // when doing large "catch-up" operations
        // (handle the batching in the caller)
        Session idsSession = idsOperations.openSession();
        idsSession.setDefaultReadOnly(true);
        Query<IdsMaster> qnext = idsSession.createQuery("from IdsMaster where unid > :lastProcessedId order by unid",
                IdsMaster.class);
        qnext.setParameter("lastProcessedId", lastProcessedId);
        qnext.setMaxResults(1);
        List<IdsMaster> nextMsgOrEmpty = qnext.list();
        idsSession.close();
        if (nextMsgOrEmpty.isEmpty()) {
            return null;
        } else if (nextMsgOrEmpty.size() == 1) {
            return nextMsgOrEmpty.get(0);
        } else {
            throw new InternalError();
        }
    }

    // Search through encounters in memory with a given encounter number
    private List<Encounter> getEncounterWhere(Mrn mrn, String encounter) {
        List<Encounter> existingEncs = mrn.getEncounters();
        if (existingEncs == null) {
            return null;
        }
        List<Encounter> matchingEncs = existingEncs
                .stream()
                .filter(e -> encounter.equals(e.getEncounter()))
                .collect(Collectors.toList());
        return matchingEncs;
    }
    
    private List<VisitFact> getVisitFactWhere(Encounter encounter, Predicate<? super VisitFact> pred) {
        List<VisitFact> visits = encounter.getVisits();
        if (visits == null) {
            return new ArrayList<VisitFact>();
        }
        List<VisitFact> matchingVisits = visits
                .stream()
                .filter(pred)
                .collect(Collectors.toList());
        return matchingVisits;
    }
    
    /**
     * Check whether VisitFact has no discharge time property, indicating it's still open,
     * and its valid until column is null, indicating that it has never been
     * invalidated.
     * Note: this does not perform time travel (ie. check whether validuntil is null or in the future)
     * Note: the validity of the underlying visit properties is not checked - what would it mean to have
     * a mismatch in validity between a Fact and its underlying properties? 
     * @param vf
     * @return
     */
    private boolean visitFactIsOpenAndValid(VisitFact vf) {
        List<VisitProperty> vpEnd = vf.getPropertyByAttribute(AttributeKeyMap.DISCHARGE_TIME);
        Instant validUntil = vf.getValidUntil();
        return vpEnd.isEmpty() && (validUntil == null);
    }

    private boolean visitFactIsOfType(VisitFact vf, AttributeKeyMap visitTypeAttr) {
        return visitTypeAttr.getShortname().equals(vf.getVisitType().getShortName());
    }
    /**
     * Get all VisitFact objects on this encounter with the given visit type, or null if none
     * @param encounter where to look for VisitFact objects
     * @param attr the visit type (as an attribute)
     * @return
     */
    private List<VisitFact> getVisitFactWhereVisitType(Encounter encounter, AttributeKeyMap attr) {
        return getVisitFactWhere(encounter, vf -> visitFactIsOfType(vf, attr));
    }


    /**
     * Get all open and valid Visit
     * @param encounter
     * @param attr
     * @return
     */
    private List<VisitFact> getOpenVisitFactWhereVisitType(Encounter encounter, AttributeKeyMap attr) {
        logger.info("getOpenVisitFactWhereVisitType: " + encounter + " " + attr);
        return getVisitFactWhere(encounter,
                vf -> visitFactIsOfType(vf, attr)
                && visitFactIsOpenAndValid(vf)
                );
    }
    
    /**
     * Get existing encounter or create a new one if it doesn't exist
     * @param mrn the MRN to search in
     * @param encounterDetails contains encounter ID (visit ID) to search for
     * @return the Encounter, existing or newly created
     * @throws HL7Exception 
     */
    private Encounter getCreateEncounter(Mrn mrn, AdtWrap encounterDetails) throws HL7Exception {
        logger.info("getCreateEncounter");
        String encounter = encounterDetails.getVisitNumber();
        List<Encounter> existingEncs = getEncounterWhere(mrn, encounter);
        if (existingEncs == null || existingEncs.isEmpty()) {
            logger.info("getCreateEncounter CREATING NEW");
            Encounter enc = new Encounter();
            enc.setStoredFrom(Instant.now());
            enc.setEncounter(encounter);
            enc.setValidFrom(encounterDetails.getEventOccurred());
            enc.setMrn(mrn);
            return enc;
        }
        else if (existingEncs.size() > 1) {
            throw new MessageIgnoredException("More than one encounter with this ID, not sure how to handle this yet: " + encounter);
        }
        else {
            // return the only element
            logger.info("getCreateEncounter RETURNING EXISTING");
            return existingEncs.get(0);
        }
    }

    /**
     * Create a new encounter using the details given in the A01 message. This may
     * also entail creating a new Mrn and Person if these don't already exist.
     * 
     * @param encounterDetails
     * @throws HL7Exception 
     */
    @Transactional
    public Encounter addEncounter(AdtWrap encounterDetails) throws HL7Exception {
        String mrnStr = encounterDetails.getMrn();
        if (mrnStr == null) {
            throw new InvalidMrnException();
        }
        Mrn newOrExistingMrn = findOrAddMrn(mrnStr, true);
        // Encounter is usually a new one for an A01, but it is
        // possible to get a second A01 if the first admission gets deleted
        // and re-made. (User corrected an error in Epic we assume).
        // Therefore need to reuse the existing encounter and the open visit if it exists.
        // (Better to move the hosp visit creation to the actual "new Encounter"?)
        Encounter enc = getCreateEncounter(newOrExistingMrn, encounterDetails);
        List<VisitFact> allHospitalVisits = getOpenVisitFactWhereVisitType(enc, AttributeKeyMap.HOSPITAL_VISIT);

        // This perhaps belongs in a getCreateHospitalVisit method, with an InformDbDataIntegrity exception
        VisitFact hospitalVisit;
        switch (allHospitalVisits.size()) {
        case 0:
            hospitalVisit = addOpenHospitalVisit(enc, encounterDetails.getAdmissionDateTime());
            addDemographicsToEncounter(enc, encounterDetails);
            // Need to save here so the hospital visit can be created (and thus assigned an ID),
            // so we can refer to that ID in the bed visit.
            // (Bed visits refer to hosp visits explicitly by their IDs).
            enc = encounterRepo.save(enc);
            break;
        case 1:
            hospitalVisit = allHospitalVisits.get(0);
            // We have received an A01 but there was already an
            // open hospital visit, so invalidate the existing bed visit and its properties
            logger.info("Invalidating previoud bed visit");
            List<VisitFact> allOpenBedVisits = getOpenVisitFactWhereVisitType(enc, AttributeKeyMap.BED_VISIT);
            if (allOpenBedVisits.size() != 1) {
                throw new InformDbIntegrityException(
                        "Found an open hospital visit with open bed visit count != 1 - hosp visit = "
                                + hospitalVisit.getVisitId());
            }
            // Need to check whether it's the bed visit that corresponds to the existing hospital visit?
            VisitFact openBedVisit = allOpenBedVisits.get(0);
            Instant invalidTime = encounterDetails.getEventOccurred();
            openBedVisit.invalidateAll(invalidTime);
            break;
        default:
            throw new MessageIgnoredException("More than 1 (count = " + allHospitalVisits.size()
                    + ") hospital visits in encounter " + encounterDetails.getVisitNumber());
        }
        // create a new bed visit with the new (or updated) location
        addOpenBedVisit(enc, encounterDetails.getAdmissionDateTime(),
                hospitalVisit,
                encounterDetails.getFullLocationString());
        enc = encounterRepo.save(enc);
        return enc;
    }

    /**
     * @param enc the encounter to add to
     * @param msgDetails the message details to use
     * @throws HL7Exception 
     */
    private void addDemographicsToEncounter(Encounter enc, AdtWrap msgDetails) throws HL7Exception {
        HashMap<String,PatientDemographicFact> demogs = buildDemographics(msgDetails);
        demogs.forEach((k, v) -> enc.addDemographic(v));
    }

    /**
     * Build the demographics objects from a message but don't actually do
     * anything with them.
     * @param msgDetails
     * @return Attribute->Fact key-value pairs
     * @throws HL7Exception 
     */
    private HashMap<String,PatientDemographicFact> buildDemographics(AdtWrap msgDetails) throws HL7Exception {
        HashMap<String, PatientDemographicFact> demographics = new HashMap<String, PatientDemographicFact>();
        {
            PatientDemographicFact fact = new PatientDemographicFact();
            fact.setValidFrom(msgDetails.getEventOccurred());
            fact.setStoredFrom(Instant.now());
            Attribute attr = getCreateAttribute(AttributeKeyMap.NAME_FACT);
            fact.setFactType(attr);
            addPropertyToFact(fact, AttributeKeyMap.FIRST_NAME, msgDetails.getGivenName());
            addPropertyToFact(fact, AttributeKeyMap.MIDDLE_NAMES, msgDetails.getMiddleName());
            addPropertyToFact(fact, AttributeKeyMap.FAMILY_NAME, msgDetails.getFamilyName());
            demographics.put(AttributeKeyMap.NAME_FACT.getShortname(), fact);
        }
        {
            PatientDemographicFact fact = new PatientDemographicFact();
            fact.setValidFrom(msgDetails.getEventOccurred());
            fact.setStoredFrom(Instant.now());
            fact.setFactType(getCreateAttribute(AttributeKeyMap.GENERAL_DEMOGRAPHIC));
            
            // will we have to worry about Instants and timezones shifting the date?
            addPropertyToFact(fact, AttributeKeyMap.DOB, msgDetails.getDob());
            
            String hl7Sex = msgDetails.getAdministrativeSex();
            Attribute sexAttrValue = getCreateAttribute(mapSex(hl7Sex));
            addPropertyToFact(fact, AttributeKeyMap.SEX, sexAttrValue);
            demographics.put(AttributeKeyMap.GENERAL_DEMOGRAPHIC.getShortname(), fact);
        }
        return demographics;
    }
    
    /**
     * A little mapping table to convert HL7 sex to Inform-db sex
     * @param hl7Sex
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

    private VisitFact addOpenHospitalVisit(Encounter enc, Instant visitBeginTime) {
        VisitFact visitFact = new VisitFact();
        visitFact.setValidFrom(visitBeginTime);
        visitFact.setStoredFrom(Instant.now());
        Attribute hosp = getCreateAttribute(AttributeKeyMap.HOSPITAL_VISIT);
        visitFact.setVisitType(hosp);
        addArrivalTimeToVisit(visitFact, visitBeginTime);
        enc.addVisit(visitFact);
        return visitFact;
    }
    
    /**
     * @param enc the encounter to add the Visit to
     * @param visitBeginTime when the Visit began, which could be an admission
     * or a transfer time
     * @param currentBed
     */
    @Transactional
    private void addOpenBedVisit(Encounter enc, Instant visitBeginTime, VisitFact parentVisit, String currentBed) {
        VisitFact visitFact = new VisitFact();
        visitFact.setStoredFrom(Instant.now());
        visitFact.setValidFrom(visitBeginTime);
        Attribute hosp = getCreateAttribute(AttributeKeyMap.BED_VISIT);
        visitFact.setVisitType(hosp);
        addArrivalTimeToVisit(visitFact, visitBeginTime);
        addLocationToVisit(visitFact, currentBed, visitBeginTime);
        addParentVisitToVisit(visitFact, parentVisit, visitBeginTime);
        enc.addVisit(visitFact);
    }

    /**
     * @param visitFact
     * @param currentBed
     */
    private void addParentVisitToVisit(VisitFact visitFact, VisitFact parentVisit, Instant validFrom) {
        Attribute attr = getCreateAttribute(AttributeKeyMap.PARENT_VISIT);
        VisitProperty prop = new VisitProperty();
        prop.setValidFrom(validFrom);
        prop.setStoredFrom(Instant.now());
        prop.setAttribute(attr);
        prop.setValueAsLink(parentVisit.getVisitId());
        visitFact.addProperty(prop);
    }

    
    /**
     * @param visitFact
     * @param currentBed
     */
    private void addLocationToVisit(VisitFact visitFact, String currentBed, Instant validFrom) {
        Attribute location = getCreateAttribute(AttributeKeyMap.LOCATION);
        VisitProperty locVisProp = new VisitProperty();
        locVisProp.setValidFrom(validFrom);
        locVisProp.setStoredFrom(Instant.now());
        locVisProp.setAttribute(location);
        locVisProp.setValueAsString(currentBed);
        visitFact.addProperty(locVisProp);
    }

    /**
     * @param visitFact
     * @param visitBeginTime
     */
    private void addArrivalTimeToVisit(VisitFact visitFact, Instant visitBeginTime) {
        Attribute arrivalTime = getCreateAttribute(AttributeKeyMap.ARRIVAL_TIME);
        VisitProperty arrVisProp = new VisitProperty();
        arrVisProp.setValidFrom(visitBeginTime);
        arrVisProp.setStoredFrom(Instant.now());
        arrVisProp.setValueAsDatetime(visitBeginTime);
        arrVisProp.setAttribute(arrivalTime);
        visitFact.addProperty(arrVisProp);
    }
    
    /**
     * Close off the existing Visit and open a new one
     * @param transferDetails usually an A02 message but can be an A08
     * @throws HL7Exception 
     */
    @Transactional
    public void transferPatient(AdtWrap transferDetails) throws HL7Exception {
        // Docs: "The new patient location should appear in PV1-3 - Assigned Patient
        // Location while the old patient location should appear in PV1-6 - Prior
        // Patient Location."
        
        // Find the current VisitFact, close it off, and start a new one with its own
        // admit time + location.
        String mrnStr = transferDetails.getMrn();
        String visitNumber = transferDetails.getVisitNumber();
        Encounter encounter = encounterRepo.findEncounterByEncounter(visitNumber);
        
        if (encounter == null) {
            throw new MessageIgnoredException("Cannot transfer an encounter that doesn't exist: " + visitNumber);
        }
        
        List<VisitFact> latestOpenBedVisits = getOpenVisitFactWhereVisitType(encounter, AttributeKeyMap.BED_VISIT);
        // The discharge datetime will be null, presumably because the patient hasn't
        // been discharged yet

        // Docs: "EVN-6 Event Occurred (DTM) 01278
        // Definition: This field contains the date/time that the event actually
        // occurred. For example, on a transfer (A02 transfer a patient), this field
        // would contain the date/time the patient was actually transferred."
        Instant eventOccurred = transferDetails.getEventOccurred();
        if (transferDetails.getTriggerEvent().equals("A08")) {
            // A08 doesn't have an event time, so use the recorded time instead
            // Downside: recorded time is later than event time, so subsequent discharge time
            // for this visit can be *earlier* than the arrival time if it's a very short visit
            // or there was a big gap between A08 event + recorded time.
            eventOccurred = transferDetails.getRecordedDateTime();
        }
        if (latestOpenBedVisits.isEmpty()) {
            throw new MessageIgnoredException("No open bed visit, cannot transfer, did you miss an A13? visit " + visitNumber);
        }
        VisitFact latestOpenBedVisit = latestOpenBedVisits.get(0);
        String newTransferLocation = transferDetails.getFullLocationString();
        String currentKnownLocation = getOnlyElement(
                latestOpenBedVisit.getPropertyByAttribute(AttributeKeyMap.LOCATION)).getValueAsString();
        if (newTransferLocation.equals(currentKnownLocation)) {
            // If we get an A02 with a new location that matches where we already thought the patient was,
            // don't perform an actual transfer. In the test data, this happens in a minority of cases
            // following an A08 implied transfer. Let's see what it does in the real data...
            String err = "[mrn " + mrnStr + "] REDUNDANT transfer, location has not changed: " + currentKnownLocation;
            logger.warn(err);
            throw new MessageIgnoredException(err);
        }
        addDischargeToVisit(latestOpenBedVisit, eventOccurred);
        
        Instant admissionDateTime = transferDetails.getAdmissionDateTime();
        Instant recordedDateTime = transferDetails.getRecordedDateTime();
        
        String admitSource = transferDetails.getAdmitSource();
        logger.info("TRANSFERRING: MRN = " + mrnStr);
        logger.info("    A02 details: adm " + admissionDateTime);
        logger.info("    A02 details: admitsrc/event/recorded " + admitSource + "/" + eventOccurred + "/" + recordedDateTime);

        // add a new visit to the current encounter
        Encounter encounterDoubleCheck = latestOpenBedVisit.getEncounter();
        if (encounter != encounterDoubleCheck) {
            throw new MessageIgnoredException("Different encounter: " + encounter + " | " + encounterDoubleCheck);
        }
        List<VisitFact> hospitalVisit = getOpenVisitFactWhereVisitType(encounter, AttributeKeyMap.HOSPITAL_VISIT);
        // link the bed visit to the parent (hospital) visit
        addOpenBedVisit(encounter, eventOccurred, hospitalVisit.get(0), newTransferLocation);
    }

    /**
     * @param mrnStr
     * @return the most recent VisitFact associated with the given MRN,
     * or null if there aren't any
     */
    private VisitFact findLatestVisitByMrn(String mrnStr) {
        List<VisitFact> latestVisitsByMrn = visitFactRepository.findLatestVisitsByMrn(mrnStr, AttributeKeyMap.ARRIVAL_TIME.getShortname());

        if (latestVisitsByMrn.isEmpty()) {
            return null;
        }
        VisitFact latestVisit = latestVisitsByMrn.get(0);
        logger.info("Latest visit: " + latestVisit.getVisitId());
        return latestVisit;
    }

    /**
     * Mark the patient's most recent Visit as finished
     * @param adtWrap the A03 message detailing the discharge
     * @throws HL7Exception 
     */
    @Transactional
    public void dischargePatient(AdtWrap adtWrap) throws HL7Exception {
        String mrnStr = adtWrap.getMrn();
        String visitNumber = adtWrap.getVisitNumber();
        
        Encounter encounter = encounterRepo.findEncounterByEncounter(visitNumber);
        if (encounter == null) {
            throw new MessageIgnoredException("Cannot discharge for a visit that doesn't exist: " + visitNumber);
        }
        List<VisitFact> latestOpenBedVisits = getOpenVisitFactWhereVisitType(encounter, AttributeKeyMap.BED_VISIT);
        if (latestOpenBedVisits.isEmpty()) {
            throw new MessageIgnoredException("No open bed visit, cannot transfer, did you miss an A13? visit " + visitNumber);
        }
        Instant eventOccurred = adtWrap.getEventOccurred();
        Instant dischargeDateTime = adtWrap.getDischargeDateTime();
        logger.info("DISCHARGE: MRN " + mrnStr);
        logger.info("A03: eventtime/dischargetime " + eventOccurred + "/" + dischargeDateTime);
        if (dischargeDateTime == null) {
            throw new MessageIgnoredException("Trying to discharge but the discharge date is null");
        }
        else {
            VisitFact latestOpenBedVisit = latestOpenBedVisits.get(0);
            // Discharge from the bed visit and the hospital visit
            addDischargeToVisit(latestOpenBedVisit, dischargeDateTime);
            List<VisitFact> hospVisit = getOpenVisitFactWhereVisitType(latestOpenBedVisit.getEncounter(), AttributeKeyMap.HOSPITAL_VISIT);
            // There *should* be exactly 1...
            addDischargeToVisit(hospVisit.get(0), dischargeDateTime);
        }
    }
    
    /**
     * Mark a Visit as finished, which can happen either when transferring or
     * discharging a patient.
     * @param visit the visit to mark as finished
     * @param dischargeDateTime the discharge/transfer time
     */
    private void addDischargeToVisit(VisitFact visit, Instant dischargeDateTime) {
        Attribute dischargeTime = getCreateAttribute(AttributeKeyMap.DISCHARGE_TIME);
        VisitProperty visProp = new VisitProperty();
        visProp.setValidFrom(dischargeDateTime);
        visProp.setStoredFrom(Instant.now());
        visProp.setValueAsDatetime(dischargeDateTime);
        visProp.setAttribute(dischargeTime);
        visit.addProperty(visProp);
    }
    
    
    private Attribute getCreateAttribute(AttributeKeyMap attrKM) {
        Optional<Attribute> attropt = attributeRepository.findByShortName(attrKM.getShortname());
        if (attropt.isPresent()) {
            return attropt.get();
        } else {
            // In future we will have a more orderly list of Attributes, but am
            // creating them on the fly for now
            Attribute attr = new Attribute();
            attr.setShortName(attrKM.getShortname());
            attr.setDescription(attrKM.toString()); // just assume a description from the name for now
            attr = attributeRepository.save(attr);
            return attr;
        }
    }

    private void addPropertyToFact(PatientDemographicFact fact, AttributeKeyMap attrKM, Object factValue) {
        if (factValue != null) {
            Attribute attr = getCreateAttribute(attrKM);
            PatientDemographicProperty prop = new PatientDemographicProperty();
            prop.setValidFrom(fact.getValidFrom());
            prop.setStoredFrom(Instant.now());
            prop.setAttribute(attr);
            prop.setValue(factValue);
            fact.addProperty(prop);
        }
    }

    private <E> E getOnlyElement(List<E> list) {
        switch (list.size()) {
        case 0:
            return null;
        case 1:
            return list.get(0);
        default:
            throw new DuplicateValueException();
        }
    }

    /**
     * Handle an A08 message. This is supposed to be about patient info changes (ie. demographics,
     * but we also see changes to location (ie. transfers) communicated only via an A08)
     *
     * @param adtWrap
     * @throws HL7Exception
     */
    @Transactional
    private void updatePatientInfo(AdtWrap adtWrap) throws HL7Exception {
        String visitNumber = adtWrap.getVisitNumber();
        String newLocation = adtWrap.getFullLocationString();

        Encounter encounter = encounterRepo.findEncounterByEncounter(visitNumber);
        if (encounter == null) {
            throw new MessageIgnoredException("Cannot find the visit " + visitNumber);
        }

        // Compare new demographics with old
        HashMap<String,PatientDemographicFact> newDemographics = buildDemographics(adtWrap);
        HashMap<String, PatientDemographicFact> currentDemographics = encounter.getDemographicsAsHashMap();
        updateDemographics(encounter, currentDemographics, newDemographics);

        // Visits, just detect changes for now until I work out what to do
        List<VisitFact> latestOpenBedVisits = getOpenVisitFactWhereVisitType(encounter, AttributeKeyMap.BED_VISIT);
        VisitFact onlyOpenBedVisit = getOnlyElement(latestOpenBedVisits);
        if (onlyOpenBedVisit == null) {
            throw new MessageIgnoredException("Got A08 but no open bed visit for visit " + visitNumber);
        }
        VisitProperty knownlocation = getOnlyElement(onlyOpenBedVisit.getPropertyByAttribute(AttributeKeyMap.LOCATION));
        if (!newLocation.equals(knownlocation.getValueAsString())) {
            logger.warn(String.format("[mrn %s, visit num %s] IMPLICIT TRANSFER IN A08: |%s| -> |%s|",
                    adtWrap.getMrn(), visitNumber, knownlocation.getValueAsString(), newLocation));
            transferPatient(adtWrap);
        }

        encounter = encounterRepo.save(encounter);
    }

    /**
     * If demographics have changed, update them and invalidate the old.
     * @param encounter the existing encounter that we may need to modify demographics of
     * @param currentDemographics existing demographics (eg. from the db)
     * @param newDemographics new demographics (eg. from the current message)
     */
    private void updateDemographics(
            Encounter encounter,
            HashMap<String, PatientDemographicFact> currentDemographics,
            HashMap<String, PatientDemographicFact> newDemographics) {
        logger.info(String.format("A08 comparing %d existing demographic facts to %s new facts",
                currentDemographics.size(), newDemographics.size()));
        for (String newKey : newDemographics.keySet()) {
            PatientDemographicFact newFact = newDemographics.get(newKey);
            PatientDemographicFact currentFact = currentDemographics.get(newKey);
            if (currentFact == null) {
                logger.info("fact does not exist, adding " + newFact.getFactType().getShortName());
                encounter.addDemographic(newFact);
            }
            else {
                if (newFact.equals(currentFact)) {
                    logger.info("fact exists and matches, no action: " + currentFact.getFactType().getShortName());
                }
                else {
                    // Just invalidate the entire fact and write in the new one.
                    // May try this on a per-property basis in future.
                    Instant invalidationDate = newFact.getValidFrom();
                    logger.info("fact exists but does not match, replacing: " + currentFact.getFactType().getShortName());
                    currentFact.invalidateAll(invalidationDate);
                    encounter.addDemographic(newFact);
                }
            }
        }
    }

    public long countEncounters() {
        return encounterRepo.count();
    }

    // Find an existing Mrn by its string representation, or create a new
    // Mrn record if it doesn't exist.
    private Mrn findOrAddMrn(String mrnStr, boolean createIfNotExist) {
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
            mrn.setMrn(mrnStr);
            mrn.setStoredFrom(Instant.now());
            Person pers = new Person();
            pers.setCreateDatetime(Instant.now());
            pers.addMrn(mrn);
            pers = personRepo.save(pers);
            mrn.setPerson(pers);
        } else if (allMrns.size() > 1) {
            throw new NotYetImplementedException("Does this even make sense?");
        } else {
            logger.info("Reusing an existing MRN");
            mrn = allMrns.get(0);
        }
        mrn = mrnRepo.save(mrn);
        return mrn;
    }

}
