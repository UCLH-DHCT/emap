package uk.ac.ucl.rits.inform;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

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
                Encounter enc = addEncounter(adtWrap);
                logger.info("[" + idsMsg.getUnid() + "] A01, add new encounter: " + enc.toString());
                processed += 1;
            }
            else if (adtWrap.getTriggerEvent().equals("A02")) {
                transferPatient(adtWrap);
                logger.info("[" + idsMsg.getUnid() + "] A02, transfer: ....");
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


    /**
     * Create a new encounter using the details given in the A01 message. This may
     * also entail creating a new Mrn and Person if these don't already exist.
     * 
     * @param encounterDetails
     */
    @Transactional
    public Encounter addEncounter(AdtWrap encounterDetails) {
        String mrnStr = encounterDetails.getMrn();
        Mrn newOrExistingMrn = findOrAddMrn(mrnStr, true);
        // Encounter is always a new one for an A01
        Encounter enc = new Encounter();
        enc.setStoredFrom(Instant.now());
        enc.setEncounter(encounterDetails.getVisitNumber());
        enc.setValidFrom(encounterDetails.getEventTime());
        enc.setMrn(newOrExistingMrn);

        PatientDemographicFact fact = new PatientDemographicFact();
        Attribute attr = getCreateAttribute(AttributeKeyMap.NAME_FACT);
        fact.setFactType(attr);
        addPropertyToFact(fact, AttributeKeyMap.FIRST_NAME, encounterDetails.getGivenName());
        addPropertyToFact(fact, AttributeKeyMap.MIDDLE_NAMES, encounterDetails.getMiddleName());
        addPropertyToFact(fact, AttributeKeyMap.FAMILY_NAME, encounterDetails.getFamilyName());
        enc.addDemographic(fact);

        addOpenVisit(enc, encounterDetails);
        enc = encounterRepo.save(enc);
        return enc;
    }

    /**
     * @param enc the encounter to add the Visit to
     * @param adtDetails where to find the data to add
     */
    @Transactional
    private void addOpenVisit(Encounter enc, AdtWrap adtDetails) {
        VisitFact visitFact = new VisitFact();
        Attribute hosp = getCreateAttribute(AttributeKeyMap.HOSPITAL_VISIT);
        visitFact.setVisitType(hosp);
        try {
            Attribute arrivalTime = getCreateAttribute(AttributeKeyMap.ARRIVAL_TIME);
            Instant admissionDateTime = adtDetails.getPV1Wrap().getAdmissionDateTime();
            VisitProperty visProp = new VisitProperty();
            visProp.setValueAsDatetime(admissionDateTime);
            visProp.setAttribute(arrivalTime);
            visitFact.addProperty(visProp);
        } catch (HL7Exception e) {
        }
        try {
            Attribute location = getCreateAttribute(AttributeKeyMap.LOCATION);
            String bed = adtDetails.getPV1Wrap().getCurrentBed();
            VisitProperty visProp = new VisitProperty();
            visProp.setAttribute(location);
            visProp.setValueAsString(bed);
            visitFact.addProperty(visProp);
        }
        catch (HL7Exception e) {
        }

        enc.addVisit(visitFact);
    }
    
    /**
     * Close off the existing Visit and open a new one
     * @param transferDetails
     * @throws HL7Exception 
     */
    @Transactional
    public void transferPatient(AdtWrap transferDetails) throws HL7Exception {
        // Find the current VisitFact.
        // Close it off.
        // Start a new one with its own admit time + location.
        String mrnStr = transferDetails.getMrn();
        
        logger.info("Finding MRN " + mrnStr);
        List<VisitFact> latestVisitsByMrn = visitFactRepository.findLatestVisitsByMrn(mrnStr, AttributeKeyMap.ARRIVAL_TIME.getShortname());

        if (latestVisitsByMrn.isEmpty()) {
            logger.warn("Cannot transfer an MRN that doesn't exist: " + mrnStr);
            return;
        }
        VisitFact latestVisit = latestVisitsByMrn.get(0);
        logger.info("Latest visit: " + latestVisit.toString());
        
        Instant dischargeDateTime = transferDetails.getPV1Wrap().getDischargeDateTime();

        Attribute dischargeTime = getCreateAttribute(AttributeKeyMap.DISCHARGE_TIME);
        VisitProperty visProp = new VisitProperty();
        visProp.setValueAsDatetime(dischargeDateTime);
        visProp.setAttribute(dischargeTime);
        latestVisit.addProperty(visProp);
        
        Instant admissionDateTime = transferDetails.getPV1Wrap().getAdmissionDateTime();
        // these are different by one second in one example - is one Epic only?
        // Hmmm.
        String recordedDateTime = transferDetails.getEVNWrap().getRecordedDateTime();
        String eventOccurred = transferDetails.getEVNWrap().getEventOccurred();
        
        String admitSource = transferDetails.getPV1Wrap().getAdmitSource();
        logger.info("TRANSFERRING: MRN = " + mrnStr);
        logger.info("    A02 details: dis/adm " + dischargeDateTime + "/" + admissionDateTime);
        logger.info("    A02 details: admitsrc/event/recorded " + admitSource + "/" + eventOccurred + "/" + recordedDateTime);

        // add a new visit to the current encounter
        Encounter encounter = latestVisit.getEncounter();
        addOpenVisit(encounter, transferDetails);
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

    public void writeToIds(String hl7message, int id) {
        idsOperations.writeToIds(hl7message, id);
    }

}
