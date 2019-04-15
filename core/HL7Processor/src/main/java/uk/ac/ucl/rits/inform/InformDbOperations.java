package uk.ac.ucl.rits.inform;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

    private final static Logger logger = LoggerFactory.getLogger(InformDbOperations.class);

    private IdsOperations idsOperations;
    
    public InformDbOperations(
            @Value("${ids.cfg.xml.file}") String idsCfgXml,
            @Autowired Environment environment
        ) {
        idsOperations = new IdsOperations(idsCfgXml, environment);
    }

    public void close() {
       idsOperations.close();
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
            // Will need a more sophisticated way of logging these errors. Do
            // it in the destination database?
            parsingErrors.add(errString);
            logger.info(errString);
            setLatestProcessedId(idsMsg.getUnid());
            return processed;
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
            } else {
                logger.debug("[" + idsMsg.getUnid() + "] Skipping " + adtWrap.getTriggerEvent() + " ("
                        + msgFromIds.getClass() + ")");
            }
        }
        catch (HL7Exception e) {
            logger.warn("[" + idsMsg.getUnid() + "] Skipping due to HL7Exception " + e + " (" + msgFromIds.getClass()
                    + ")");
        }
        catch (InvalidMrnException e) {
            logger.warn("[" + idsMsg.getUnid() + "] Skipping due to invalid Mrn " + e + " (" + msgFromIds.getClass()
            + ")");
        }
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
            return null;
        }
        List<VisitFact> matchingVisits = visits
                .stream()
                .filter(pred)
                .collect(Collectors.toList());
        return matchingVisits;
    }
    
    private boolean visitFactIsOpen(VisitFact vf) {
        List<VisitProperty> vpEnd = getPropertyByAttribute(vf, AttributeKeyMap.DISCHARGE_TIME);
        return vpEnd.isEmpty();
    }
    
    private List<VisitProperty> getPropertyByAttribute(VisitFact vf, AttributeKeyMap dischargeTime) {
        List<VisitProperty> props = vf.getVisitProperties();
        if (props == null) {
            return new ArrayList<VisitProperty>();
        }
        List<VisitProperty> propsWithAttr = props.stream()
                .filter(prop -> prop.getAttribute().getShortName().equals(dischargeTime.getShortname()))
                .collect(Collectors.toList());
        return propsWithAttr;
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


    private List<VisitFact> getOpenVisitFactWhereVisitType(Encounter encounter, AttributeKeyMap attr) {
        logger.info("getOpenVisitFactWhereVisitType: " + encounter + " " + attr);
        return getVisitFactWhere(encounter,
                vf -> visitFactIsOfType(vf, attr)
                && visitFactIsOpen(vf)
                );
    }
    
    /**
     * Get existing encounter or create a new one if it doesn't exist
     * @param mrn the MRN to search in
     * @param encounterDetails contains encounter ID (visit ID) to search for
     * @return the Encounter, existing or newly created
     */
    private Encounter getCreateEncounter(Mrn mrn, AdtWrap encounterDetails) {
        logger.info("getCreateEncounter");
        String encounter = encounterDetails.getVisitNumber();
        List<Encounter> existingEncs = getEncounterWhere(mrn, encounter);
        if (existingEncs == null || existingEncs.isEmpty()) {
            logger.info("getCreateEncounter CREATING NEW");
            Encounter enc = new Encounter();
            enc.setStoredFrom(Instant.now());
            enc.setEncounter(encounter);
            enc.setValidFrom(encounterDetails.getEventTime());
            enc.setMrn(mrn);
            return enc;
        }
        else if (existingEncs.size() > 1) {
            throw new RuntimeException("More than one encounter with this ID, not sure how to handle this yet: " + encounter);
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
        Encounter enc = getCreateEncounter(newOrExistingMrn, encounterDetails);
      
        PatientDemographicFact fact = new PatientDemographicFact();
        Attribute attr = getCreateAttribute(AttributeKeyMap.NAME_FACT);
        fact.setFactType(attr);
        addPropertyToFact(fact, AttributeKeyMap.FIRST_NAME, encounterDetails.getGivenName());
        addPropertyToFact(fact, AttributeKeyMap.MIDDLE_NAMES, encounterDetails.getMiddleName());
        addPropertyToFact(fact, AttributeKeyMap.FAMILY_NAME, encounterDetails.getFamilyName());
        enc.addDemographic(fact);

        VisitFact hospitalVisit = addOpenHospitalVisit(enc, encounterDetails.getPV1Wrap().getAdmissionDateTime());
        // Need to save here so the hospital visit can be created (and thus assigned an ID),
        // so we can refer to that ID in the bed visit.
        // (Bed visits refer to hosp visits explicitly by their IDs).
        enc = encounterRepo.save(enc);
        addOpenBedVisit(enc, encounterDetails.getPV1Wrap().getAdmissionDateTime(),
                hospitalVisit,
                encounterDetails.getPV1Wrap().getCurrentBed());
        enc = encounterRepo.save(enc);
        return enc;
    }

    private VisitFact addOpenHospitalVisit(Encounter enc, Instant visitBeginTime) {
        VisitFact visitFact = new VisitFact();
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
        Attribute hosp = getCreateAttribute(AttributeKeyMap.BED_VISIT);
        visitFact.setVisitType(hosp);
        addArrivalTimeToVisit(visitFact, visitBeginTime);
        addLocationToVisit(visitFact, currentBed);
        addParentVisitToVisit(visitFact, parentVisit);
        enc.addVisit(visitFact);
    }

    /**
     * @param visitFact
     * @param currentBed
     */
    private void addParentVisitToVisit(VisitFact visitFact, VisitFact parentVisit) {
        Attribute attr = getCreateAttribute(AttributeKeyMap.PARENT_VISIT);
        VisitProperty prop = new VisitProperty();
        prop.setStoredFrom(Instant.now());
        prop.setAttribute(attr);
        prop.setValueAsLink(parentVisit.getVisitId());
        visitFact.addProperty(prop);
    }

    
    /**
     * @param visitFact
     * @param currentBed
     */
    private void addLocationToVisit(VisitFact visitFact, String currentBed) {
        Attribute location = getCreateAttribute(AttributeKeyMap.LOCATION);
        VisitProperty locVisProp = new VisitProperty();
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
        arrVisProp.setStoredFrom(Instant.now());
        arrVisProp.setValueAsDatetime(visitBeginTime);
        arrVisProp.setAttribute(arrivalTime);
        visitFact.addProperty(arrVisProp);
    }
    
    /**
     * Close off the existing Visit and open a new one
     * @param transferDetails
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
            logger.warn("Cannot transfer an encounter that doesn't exist: " + visitNumber);
            return;
        }
        
        List<VisitFact> latestOpenBedVisits = getOpenVisitFactWhereVisitType(encounter, AttributeKeyMap.BED_VISIT);
        // The discharge datetime will be null, presumably because the patient hasn't
        // been discharged yet

        // Docs: "EVN-6 Event Occurred (DTM) 01278
        // Definition: This field contains the date/time that the event actually
        // occurred. For example, on a transfer (A02 transfer a patient), this field
        // would contain the date/time the patient was actually transferred."
        Instant eventOccurred = transferDetails.getEVNWrap().getEventOccurred();
        if (latestOpenBedVisits.isEmpty()) {
            logger.error("There is no open bed visit, cannot transfer, was there an A13 message you didn't process? vis num " + visitNumber);
            return;
        }
        VisitFact latestOpenBedVisit = latestOpenBedVisits.get(0);
        addDischargeToVisit(latestOpenBedVisit, eventOccurred);
        
        Instant admissionDateTime = transferDetails.getPV1Wrap().getAdmissionDateTime();
        Instant recordedDateTime = transferDetails.getEVNWrap().getRecordedDateTime();
        
        String admitSource = transferDetails.getPV1Wrap().getAdmitSource();
        logger.info("TRANSFERRING: MRN = " + mrnStr);
        logger.info("    A02 details: adm " + admissionDateTime);
        logger.info("    A02 details: admitsrc/event/recorded " + admitSource + "/" + eventOccurred + "/" + recordedDateTime);

        // add a new visit to the current encounter
        Encounter encounterDoubleCheck = latestOpenBedVisit.getEncounter();
        if (encounter != encounterDoubleCheck) {
            logger.error("Different encounter: " + encounter + " | " + encounterDoubleCheck);
            throw new RuntimeException("waaa");
        }
        List<VisitFact> hospitalVisit = getVisitFactWhereVisitType(encounter, AttributeKeyMap.HOSPITAL_VISIT);
        // link the bed visit to the parent (hospital) visit
        addOpenBedVisit(encounter, eventOccurred, hospitalVisit.get(0), transferDetails.getPV1Wrap().getCurrentBed());
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
            logger.warn("Cannot discharge for a visit that doesn't exist: " + visitNumber);
            return;
        }
        List<VisitFact> latestOpenBedVisits = getOpenVisitFactWhereVisitType(encounter, AttributeKeyMap.BED_VISIT);
        if (latestOpenBedVisits.isEmpty()) {
            logger.error("There is no open bed visit, cannot discharge, was there an A13 message you didn't process? vis num " + visitNumber);
            return;
        }
        Instant eventOccurred = adtWrap.getEVNWrap().getEventOccurred();
        Instant dischargeDateTime = adtWrap.getPV1Wrap().getDischargeDateTime();
        logger.info("DISCHARGE: MRN " + mrnStr);
        logger.info("A03: eventtime/dischargetime " + eventOccurred + "/" + dischargeDateTime);
        if (eventOccurred == null) {
            logger.warn("Trying to discharge but the event occurred date is null. Is this a dupe message?");
        }
        else {
            VisitFact latestOpenBedVisit = latestOpenBedVisits.get(0);
            // Discharge from the bed visit and the hospital visit
            addDischargeToVisit(latestOpenBedVisit, dischargeDateTime);
            List<VisitFact> hospVisit = getVisitFactWhereVisitType(latestOpenBedVisit.getEncounter(), AttributeKeyMap.HOSPITAL_VISIT);
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

    private void addPropertyToFact(PatientDemographicFact fact, AttributeKeyMap attrKM, String factValue) {
        if (factValue != null) {
            Attribute attr = getCreateAttribute(attrKM);
            PatientDemographicProperty prop = new PatientDemographicProperty();
            prop.setStoredFrom(Instant.now());
            prop.setAttribute(attr);
            prop.setValueAsString(factValue);
            fact.addProperty(prop);
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
        mrn.setStoredFrom(Instant.now());
        mrn = mrnRepo.save(mrn);
        return mrn;
    }

    public void writeToIds(String hl7message, int id, String triggerEvent, String mrn) {
        idsOperations.writeToIds(hl7message, id, triggerEvent, mrn);
    }

}
