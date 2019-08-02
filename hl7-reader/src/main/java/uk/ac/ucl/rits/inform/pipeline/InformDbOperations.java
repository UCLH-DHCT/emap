package uk.ac.ucl.rits.inform.pipeline;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.hibernate.Session;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v26.message.ORM_O01;
import ca.uhn.hl7v2.model.v26.message.ORU_R01;
import ca.uhn.hl7v2.parser.PipeParser;
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
import uk.ac.ucl.rits.inform.pipeline.exceptions.AttributeError;
import uk.ac.ucl.rits.inform.pipeline.exceptions.DuplicateValueException;
import uk.ac.ucl.rits.inform.pipeline.exceptions.Hl7InconsistencyException;
import uk.ac.ucl.rits.inform.pipeline.exceptions.InformDbIntegrityException;
import uk.ac.ucl.rits.inform.pipeline.exceptions.InvalidMrnException;
import uk.ac.ucl.rits.inform.pipeline.exceptions.MessageIgnoredException;
import uk.ac.ucl.rits.inform.pipeline.hl7.AdtWrap;
import uk.ac.ucl.rits.inform.pipeline.hl7.MSHWrap;
import uk.ac.ucl.rits.inform.pipeline.hl7.PathologyOrder;
import uk.ac.ucl.rits.inform.pipeline.hl7.PathologyResult;
import uk.ac.ucl.rits.inform.pipeline.ids.IdsMaster;
import uk.ac.ucl.rits.inform.pipeline.ids.IdsOperations;
import uk.ac.ucl.rits.inform.pipeline.informdb.AttributeRepository;
import uk.ac.ucl.rits.inform.pipeline.informdb.EncounterRepository;
import uk.ac.ucl.rits.inform.pipeline.informdb.IdsEffectLogging;
import uk.ac.ucl.rits.inform.pipeline.informdb.IdsEffectLoggingRepository;
import uk.ac.ucl.rits.inform.pipeline.informdb.IdsProgress;
import uk.ac.ucl.rits.inform.pipeline.informdb.IdsProgressRepository;
import uk.ac.ucl.rits.inform.pipeline.informdb.MrnRepository;
import uk.ac.ucl.rits.inform.pipeline.informdb.PatientFactRepository;
import uk.ac.ucl.rits.inform.pipeline.informdb.PersonMrnRepository;
import uk.ac.ucl.rits.inform.pipeline.informdb.PersonRepository;

/**
 * All the operations that can be performed on Inform-db.
 */
@Component
@EntityScan({ "uk.ac.ucl.rits.inform.pipeline.informdb", "uk.ac.ucl.rits.inform.informdb" })
public class InformDbOperations {
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
    @Autowired
    private IdsProgressRepository      idsProgressRepository;
    @Autowired
    private IdsEffectLoggingRepository idsEffectLoggingRepository;

    private static final Logger        logger = LoggerFactory.getLogger(InformDbOperations.class);

    @Autowired
    private IdsOperations              idsOperations;

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
     * Wrapper for the entire transaction that performs: - read latest processed ID
     * from Inform-db (ETL metadata) - process the message and write to Inform-db -
     * write the latest processed ID to reflect the above message. Blocks until
     * there are new messages.
     *
     * @param parser        the HAPI parser to be used
     * @param parsingErrors out param for parsing errors encountered
     * @return number of messages processes
     * @throws HL7Exception in some cases where HAPI does
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
        Timestamp messageDatetime = idsMsg.getMessagedatetime();
        Instant messageDatetimeInstant = null;
        if (messageDatetime != null) {
            idsLog.setMessageDatetime(messageDatetime.toInstant());
            messageDatetimeInstant = messageDatetime.toInstant();
        }
        int processed = 0;
        String hl7msg = idsMsg.getHl7message();
        // HL7 is supposed to use \r for line endings, but
        // the IDS uses \n
        hl7msg = hl7msg.replace("\n", "\r");
        Message msgFromIds;
        try {
            msgFromIds = parser.parse(hl7msg);
        } catch (HL7Exception hl7e) {
            String errString = "[" + idsMsg.getUnid() + "]  HL7 parsing error";
            hl7e.printStackTrace();
            // Mark the message as processed even though we couldn't parse it,
            // but record it for later debugging.
            parsingErrors.add(errString);
            logger.info(errString);
            idsLog.setMessage(errString);
            Instant processingEnd = Instant.now();
            idsLog.setProcessingEndTime(processingEnd);
            setLatestProcessedId(idsMsg.getUnid(), messageDatetimeInstant, processingEnd);
            return processed;
        } finally {
            idsLog = idsEffectLoggingRepository.save(idsLog);
        }

        try {
            processed = processHl7Message(msgFromIds, idsMsg.getUnid(), idsLog, processed);
        } catch (HL7Exception e) {
            String errMsg =
                    "[" + idsMsg.getUnid() + "] Skipping due to HL7Exception " + e + " (" + msgFromIds.getClass() + ")";
            idsLog.setMessage(errMsg);
            logger.warn(errMsg);
        } catch (InvalidMrnException e) {
            String errMsg =
                    "[" + idsMsg.getUnid() + "] Skipping due to invalid Mrn " + e + " (" + msgFromIds.getClass() + ")";
            idsLog.setMessage(errMsg);
            logger.warn(errMsg);
        } catch (MessageIgnoredException e) {
            idsLog.setMessage(e.getClass() + " " + e.getMessage());
        } catch (Hl7InconsistencyException e) {
            idsLog.setMessage(e.getClass() + " " + e.getMessage());
        } finally {
            idsLog = idsEffectLoggingRepository.save(idsLog);
        }
        Instant processingEnd = Instant.now();
        idsLog.setProcessingEndTime(processingEnd);
        idsLog = idsEffectLoggingRepository.save(idsLog);
        setLatestProcessedId(idsMsg.getUnid(), messageDatetimeInstant, processingEnd);

        return processed;
    }

    /**
     * Determine the message type (ADT/ORU/etc) and use the appropriate wrapper
     * class to process the wrapper.
     *
     * @param msgFromIds the message
     * @param idsUnid    the IDS unique ID (or similar unique ID if not from IDS)
     * @param idsLog     the IDS-oriented log, or null if you don't want to log
     * @param processed  the current message processed count
     * @return the updated message processed count
     * @throws HL7Exception if HAPI does
     * @throws Hl7InconsistencyException if there seems to be something wrong in the HL7 stream that should be logged and investigated
     * @throws MessageIgnoredException if message can't be processed
     */
    public int processHl7Message(Message msgFromIds, int idsUnid, IdsEffectLogging idsLog, int processed)
            throws HL7Exception, Hl7InconsistencyException, MessageIgnoredException {
        // it's ok to give any message to an AdtWrap if we're only looking at the MSH
        MSHWrap mshwrap = new AdtWrap(msgFromIds);
        String messageType = mshwrap.getMessageType();
        String triggerEvent = mshwrap.getTriggerEvent();

        logger.info(String.format("[%s] %s^%s", idsUnid, messageType, triggerEvent));

        if (messageType.equals("ADT")) {
            AdtWrap adtWrap = new AdtWrap(msgFromIds);
            if (triggerEvent.equals("A01")) {
                addEncounter(adtWrap);
                processed += 1;
            } else if (triggerEvent.equals("A02")) {
                transferPatient(adtWrap);
                processed += 1;
            } else if (triggerEvent.equals("A03")) {
                dischargePatient(adtWrap);
                processed += 1;
            } else if (triggerEvent.equals("A08")) {
                updatePatientInfo(adtWrap);
                processed += 1;
            } else if (triggerEvent.equals("A13")) {
                cancelDischargePatient(adtWrap);
                processed += 1;
            } else if (triggerEvent.equals("A40")) {
                mergeById(adtWrap);
                processed += 1;
            } else {
                logger.debug("[" + idsUnid + "] Skipping " + triggerEvent + " (" + msgFromIds.getClass() + ")");
                if (idsLog != null) {
                    idsLog.setMessage("Skipping ADT due to message type");
                }
            }
        } else if (messageType.equals("ORU")) {
            if (triggerEvent.equals("R01")) {
                // get all result batteries in the message
                List<PathologyOrder> pathologyOrdersWithResults = PathologyOrder.buildPathologyOrdersFromResults((ORU_R01) msgFromIds);
                for (PathologyOrder order : pathologyOrdersWithResults) {
                    addOrUpdatePathologyOrder(order);
                }
                processed += 1;
            }
        } else if (messageType.equals("ORM")) {
            if (triggerEvent.equals("O01")) {
                // get all orders in the message
                List<PathologyOrder> pathologyOrders = PathologyOrder.buildPathologyOrders((ORM_O01) msgFromIds);
                for (PathologyOrder order : pathologyOrders) {
                    addOrUpdatePathologyOrder(order);
                }
                processed += 1;
            }
        }
        return processed;
    }

    /**
     * @return the unique ID for the last IDS message we have successfully processed
     */
    @Transactional
    private int getLatestProcessedId() {
        IdsProgress onlyRow = idsProgressRepository.findOnlyRow();
        if (onlyRow == null) {
            onlyRow = new IdsProgress();
            // Is it wrong to set in a get?
            onlyRow = idsProgressRepository.save(onlyRow);
        }
        return onlyRow.getLastProcessedIdsUnid();
    }

    /**
     * Record that we have processed all messages up to the specified message.
     *
     * @param lastProcessedIdsUnid the unique ID for the latest IDS message we have
     *                             processed
     * @param messageDatetime      the timestamp of this message
     * @param processingEnd        the time this message was actually processed
     */
    @Transactional
    private void setLatestProcessedId(int lastProcessedIdsUnid, Instant messageDatetime, Instant processingEnd) {
        IdsProgress onlyRow = idsProgressRepository.findOnlyRow();
        onlyRow.setLastProcessedIdsUnid(lastProcessedIdsUnid);
        onlyRow.setLastProcessedMessageDatetime(messageDatetime);
        onlyRow.setLastProcessingDatetime(processingEnd);
        idsProgressRepository.save(onlyRow);
    }

    /**
     * Return the next HL7 message in the IDS. If there are no more, block until
     * there are.
     *
     * @param lastProcessedId the latest unique ID that has already been processed
     * @return the next HL7 message record
     */
    public IdsMaster getNextHL7IdsRecordBlocking(int lastProcessedId) {
        long secondsSleep = 10;
        IdsMaster idsMsg = null;
        while (true) {
            idsMsg = getNextHL7IdsRecord(lastProcessedId);
            if (idsMsg == null) {
                logger.info(String.format("No more messages, retrying in %d seconds", secondsSleep));
                try {
                    Thread.sleep(secondsSleep * 1000);
                } catch (InterruptedException ie) {
                    logger.trace("Sleep was interrupted");
                }
            } else {
                break;
            }
        }
        return idsMsg;
    }

    /**
     * Get next entry in the IDS, if it exists.
     *
     * @param lastProcessedId the last one we have successfully processed
     *
     * @return the first message that comes after lastProcessedId, or null if there
     *         isn't one
     */
    public IdsMaster getNextHL7IdsRecord(int lastProcessedId) {
        // consider changing to "get next N messages" for more efficient database
        // performance
        // when doing large "catch-up" operations
        // (handle the batching in the caller)
        Session idsSession = idsOperations.openSession();
        idsSession.setDefaultReadOnly(true);
        Query<IdsMaster> qnext =
                idsSession.createQuery("from IdsMaster where unid > :lastProcessedId order by unid", IdsMaster.class);
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
    private List<PatientFact> getVisitFactWhere(Encounter encounter, Predicate<? super PatientFact> pred) {
        return getFactWhere(encounter, f -> factIsVisitFact(f) && pred.test(f));
    }

    /**
     * @param encounter the Encounter to search in
     * @param pred      the predicate to check for each PatientFact
     * @return all PatientFact objects in encounter which match predicate pred
     */
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
     * @param encounterDetails contains encounter ID (visit ID) to search for
     * @return the Encounter, existing or newly created
     * @throws HL7Exception if HAPI does
     * @throws MessageIgnoredException if message can't be processed
     */
    private Encounter getCreateEncounter(Mrn mrn, AdtWrap encounterDetails) throws HL7Exception, MessageIgnoredException {
        logger.info("getCreateEncounter");
        String encounter = encounterDetails.getVisitNumber();
        List<Encounter> existingEncs = getEncounterWhere(mrn, encounter);
        if (existingEncs == null || existingEncs.isEmpty()) {
            logger.info("getCreateEncounter CREATING NEW");
            Encounter enc = new Encounter();
            Instant storedFrom = Instant.now();
            enc.setEncounter(encounter);
            Instant validFrom = encounterDetails.getEventOccurred();
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
     * @param encounterDetails msg containing encounter details
     * @return the created Encounter
     * @throws HL7Exception if HAPI does
     * @throws MessageIgnoredException if message can't be processed
     */
    @Transactional
    public Encounter addEncounter(AdtWrap encounterDetails) throws HL7Exception, MessageIgnoredException {
        String mrnStr = encounterDetails.getMrn();
        Instant admissionTime = encounterDetails.getAdmissionDateTime();
        if (mrnStr == null) {
            throw new InvalidMrnException(String.format("Missing mrn in message from %s",
                    encounterDetails.getMSH().getSendingApplication().encode()));
        }
        Mrn newOrExistingMrn = findOrAddMrn(mrnStr, admissionTime, true);
        // Encounter is usually a new one for an A01, but it is
        // possible to get a second A01 if the first admission gets deleted
        // and re-made. (User corrected an error in Epic we assume).
        // Therefore need to reuse the existing encounter and the open visit if it
        // exists.
        // (Better to move the hosp visit creation to the actual "new Encounter"?)
        Encounter enc = getCreateEncounter(newOrExistingMrn, encounterDetails);
        List<PatientFact> allHospitalVisits = getOpenVisitFactWhereVisitType(enc, AttributeKeyMap.HOSPITAL_VISIT);

        // This perhaps belongs in a getCreateHospitalVisit method, with an
        // InformDbDataIntegrity exception
        PatientFact hospitalVisit;
        switch (allHospitalVisits.size()) {
        case 0:
            hospitalVisit = addOpenHospitalVisit(enc, admissionTime);
            addDemographicsToEncounter(enc, encounterDetails);
            // Need to save here so the hospital visit can be created (and thus
            // assigned an ID), so we can refer to that ID in the bed visit.
            // (Bed visits refer to hosp visits explicitly by their IDs).
            break;
        case 1:
            hospitalVisit = allHospitalVisits.get(0);
            // We have received an A01 but there was already an
            // open hospital visit, so invalidate the existing bed visit and its properties
            logger.info("Invalidating previous bed visit");
            List<PatientFact> allOpenBedVisits = getOpenVisitFactWhereVisitType(enc, AttributeKeyMap.BED_VISIT);
            if (allOpenBedVisits.size() != 1) {
                throw new InformDbIntegrityException(
                        "Found an open hospital visit with open bed visit count != 1 - hosp visit = "
                                + hospitalVisit.getFactId());
            }
            // Need to check whether it's the bed visit that corresponds to the existing
            // hospital visit?
            PatientFact openBedVisit = allOpenBedVisits.get(0);
            Instant invalidTime = encounterDetails.getEventOccurred();
            openBedVisit.invalidateAll(invalidTime);
            break;
        default:
            throw new MessageIgnoredException("More than 1 (count = " + allHospitalVisits.size()
                    + ") hospital visits in encounter " + encounterDetails.getVisitNumber());
        }
        // create a new bed visit with the new (or updated) location
        addOpenBedVisit(enc, encounterDetails.getAdmissionDateTime(), hospitalVisit,
                encounterDetails.getFullLocationString());
        enc = encounterRepo.save(enc);
        logger.info("Encounter: " + enc.toString());
        return enc;
    }

    /**
     * @param enc        the encounter to add to
     * @param msgDetails the message details to use
     * @throws HL7Exception if HAPI does
     */
    private void addDemographicsToEncounter(Encounter enc, AdtWrap msgDetails) throws HL7Exception {
        Map<String, PatientFact> demogs = buildPatientDemographics(msgDetails);
        demogs.forEach((k, v) -> enc.addFact(v));
    }

    /**
     * Build the demographics objects from a message but don't actually do anything
     * with them.
     *
     * @param msgDetails the msg to build demographics from
     * @return Attribute->Fact key-value pairs
     * @throws HL7Exception if HAPI does
     */
    private Map<String, PatientFact> buildPatientDemographics(AdtWrap msgDetails) throws HL7Exception {
        Map<String, PatientFact> demographics = new HashMap<>();
        Instant validFrom = msgDetails.getEventOccurred();
        if (validFrom == null) {
            // some messages (eg. A08) don't have an event occurred field
            validFrom = msgDetails.getRecordedDateTime();
        }
        PatientFact nameFact = new PatientFact();
        nameFact.setValidFrom(validFrom);
        nameFact.setStoredFrom(Instant.now());
        Attribute nameAttr = getCreateAttribute(AttributeKeyMap.NAME_FACT);
        nameFact.setFactType(nameAttr);
        addPropertyToFact(nameFact, AttributeKeyMap.FIRST_NAME, msgDetails.getPatientGivenName());
        addPropertyToFact(nameFact, AttributeKeyMap.MIDDLE_NAMES, msgDetails.getPatientMiddleName());
        addPropertyToFact(nameFact, AttributeKeyMap.FAMILY_NAME, msgDetails.getPatientFamilyName());
        demographics.put(AttributeKeyMap.NAME_FACT.getShortname(), nameFact);

        PatientFact generalDemoFact = new PatientFact();
        generalDemoFact.setValidFrom(validFrom);
        generalDemoFact.setStoredFrom(Instant.now());
        generalDemoFact.setFactType(getCreateAttribute(AttributeKeyMap.GENERAL_DEMOGRAPHIC));

        // will we have to worry about Instants and timezones shifting the date?
        addPropertyToFact(generalDemoFact, AttributeKeyMap.DOB, msgDetails.getPatientBirthDate());

        String hl7Sex = msgDetails.getPatientSex();
        Attribute sexAttrValue = getCreateAttribute(mapSex(hl7Sex));
        addPropertyToFact(generalDemoFact, AttributeKeyMap.SEX, sexAttrValue);

        addPropertyToFact(generalDemoFact, AttributeKeyMap.NHS_NUMBER, msgDetails.getNHSNumber());

        addPropertyToFact(generalDemoFact, AttributeKeyMap.POST_CODE, msgDetails.getPatientZipOrPostalCode());

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
        addParentVisitToVisit(visitFact, parentVisit, visitBeginTime);
        enc.addFact(visitFact);
    }

    /**
     * @param visitFact   the visit fact to add to
     * @param parentVisit the parent visit fact to add
     * @param validFrom   the valid from timestamp to use
     */
    private void addParentVisitToVisit(PatientFact visitFact, PatientFact parentVisit, Instant validFrom) {
        Attribute attr = getCreateAttribute(AttributeKeyMap.PARENT_VISIT);
        PatientProperty prop = new PatientProperty();
        prop.setValidFrom(validFrom);
        prop.setStoredFrom(Instant.now());
        prop.setAttribute(attr);
        prop.setValueAsLink(parentVisit.getFactId());
        visitFact.addProperty(prop);
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
     * @param transferDetails usually an A02 message but can be an A08
     * @throws HL7Exception if HAPI does
     * @throws MessageIgnoredException if message can't be processed
     */
    @Transactional
    public void transferPatient(AdtWrap transferDetails) throws HL7Exception, MessageIgnoredException {
        // Docs: "The new patient location should appear in PV1-3 - Assigned Patient
        // Location while the old patient location should appear in PV1-6 - Prior
        // Patient Location."

        // Find the current PatientFact, close it off, and start a new one with its own
        // admit time + location.
        String mrnStr = transferDetails.getMrn();
        String visitNumber = transferDetails.getVisitNumber();
        Encounter encounter = encounterRepo.findEncounterByEncounter(visitNumber);

        if (encounter == null) {
            throw new MessageIgnoredException("Cannot transfer an encounter that doesn't exist: " + visitNumber);
        }

        List<PatientFact> latestOpenBedVisits = getOpenVisitFactWhereVisitType(encounter, AttributeKeyMap.BED_VISIT);
        // The discharge datetime will be null, presumably because the patient hasn't
        // been discharged yet

        // Docs: "EVN-6 Event Occurred (DTM) 01278
        // Definition: This field contains the date/time that the event actually
        // occurred. For example, on a transfer (A02 transfer a patient), this field
        // would contain the date/time the patient was actually transferred."
        Instant eventOccurred = transferDetails.getEventOccurred();
        if (transferDetails.getTriggerEvent().equals("A08")) {
            // A08 doesn't have an event time, so use the recorded time instead
            // Downside: recorded time is later than event time, so subsequent discharge
            // time
            // for this visit can be *earlier* than the arrival time if it's a very short
            // visit
            // or there was a big gap between A08 event + recorded time.
            eventOccurred = transferDetails.getRecordedDateTime();
        }
        if (latestOpenBedVisits.isEmpty()) {
            throw new MessageIgnoredException(
                    "No open bed visit, cannot transfer, did you miss an A13? visit " + visitNumber);
        }
        PatientFact latestOpenBedVisit = latestOpenBedVisits.get(0);
        String newTransferLocation = transferDetails.getFullLocationString();
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

        Instant admissionDateTime = transferDetails.getAdmissionDateTime();
        Instant recordedDateTime = transferDetails.getRecordedDateTime();

        String admitSource = transferDetails.getAdmitSource();
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
     * @throws HL7Exception if HAPI does
     * @throws MessageIgnoredException if message can't be processed
     */
    @Transactional
    public void dischargePatient(AdtWrap adtWrap) throws HL7Exception, MessageIgnoredException {
        String mrnStr = adtWrap.getMrn();
        String visitNumber = adtWrap.getVisitNumber();

        Encounter encounter = encounterRepo.findEncounterByEncounter(visitNumber);
        if (encounter == null) {
            throw new MessageIgnoredException("Cannot discharge for a visit that doesn't exist: " + visitNumber);
        }
        PatientFact latestOpenBedVisit = getOnlyElement(getOpenVisitFactWhereVisitType(encounter, AttributeKeyMap.BED_VISIT));
        if (latestOpenBedVisit == null) {
            throw new MessageIgnoredException(
                    "No open bed visit, cannot discharge, did you miss an A13? visit " + visitNumber);
        }
        Instant eventOccurred = adtWrap.getEventOccurred();
        Instant dischargeDateTime = adtWrap.getDischargeDateTime();
        logger.info("DISCHARGE: MRN " + mrnStr);
        logger.info("A03: eventtime/dischargetime " + eventOccurred + "/" + dischargeDateTime);
        if (dischargeDateTime == null) {
            throw new MessageIgnoredException("Trying to discharge but the discharge date is null");
        } else {
            // Discharge from the bed visit and the hospital visit
            addDischargeToVisit(latestOpenBedVisit, dischargeDateTime);
            PatientFact hospVisit = getOnlyElement(
                    getOpenVisitFactWhereVisitType(latestOpenBedVisit.getEncounter(), AttributeKeyMap.HOSPITAL_VISIT));
            // There *should* be exactly 1...
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
     * @param adtWrap the A13 message detailing the cancel discharge
     * @throws HL7Exception if HAPI does
     * @throws Hl7InconsistencyException if this message can't be matched to an existing discharge
     * @throws MessageIgnoredException if message can't be processed
     */
    @Transactional
    private void cancelDischargePatient(AdtWrap adtWrap) throws HL7Exception, Hl7InconsistencyException, MessageIgnoredException {
        String visitNumber = adtWrap.getVisitNumber();
        // event occurred field seems to be populated despite the Epic example message showing it blank.
        Instant invalidationDate = adtWrap.getEventOccurred();
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
            throw new Hl7InconsistencyException(visitNumber + " Cannot process A13 - most recent bed visit is still open");
        }
        PatientProperty bedDischargeTime = getOnlyElement(
                mostRecentBedVisit.getPropertyByAttribute(AttributeKeyMap.DISCHARGE_TIME, p -> p.isValid()));
        // Find the hospital visit corresponding to the bed visit
        Long hospitalVisitId = getOnlyElement(
                mostRecentBedVisit.getPropertyByAttribute(AttributeKeyMap.PARENT_VISIT, p -> p.isValid()))
                        .getValueAsLink();
        PatientFact hospitalVisit = patientFactRepository.findById(hospitalVisitId).get();
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

    /**
     * Return a persisted Attribute object with the given enum value, creating it
     * first if necessary.
     *
     * @param attrKM the enum value of the attribute
     * @return the Attribute object
     */
    @Transactional
    private Attribute getCreateAttribute(AttributeKeyMap attrKM) {
        Optional<Attribute> attropt = attributeRepo.findByShortName(attrKM.getShortname());
        if (attropt.isPresent()) {
            return attropt.get();
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
     * @param adtWrap the message with the patient info
     * @throws HL7Exception if HAPI does
     * @throws MessageIgnoredException if message can't be processed
     */
    @Transactional
    private void updatePatientInfo(AdtWrap adtWrap) throws HL7Exception, MessageIgnoredException {
        String visitNumber = adtWrap.getVisitNumber();
        String newLocation = adtWrap.getFullLocationString();

        Encounter encounter = encounterRepo.findEncounterByEncounter(visitNumber);
        if (encounter == null) {
            throw new MessageIgnoredException("Cannot find the visit " + visitNumber);
        }

        // Compare new demographics with old
        Map<String, PatientFact> newDemographics = buildPatientDemographics(adtWrap);
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
            logger.warn(String.format("[mrn %s, visit num %s] IMPLICIT TRANSFER IN A08: |%s| -> |%s|", adtWrap.getMrn(),
                    visitNumber, knownlocation.getValueAsString(), newLocation));
            transferPatient(adtWrap);
        }

        encounter = encounterRepo.save(encounter);
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
     * @param adtWrap message containing merge info
     * @throws HL7Exception when HAPI does or merge time in message is blank
     * @throws MessageIgnoredException if message can't be processed
     */
    @Transactional
    private void mergeById(AdtWrap adtWrap) throws HL7Exception, MessageIgnoredException {
        String oldMrnStr = adtWrap.getMergedPatientId();
        String survivingMrnStr = adtWrap.getMrn();
        Instant mergeTime = adtWrap.getRecordedDateTime();
        logger.info(
                "MERGE: surviving mrn " + survivingMrnStr + ", oldMrn = " + oldMrnStr + ", merge time = " + mergeTime);
        if (mergeTime == null) {
            throw new HL7Exception("event occurred null");
        }

        // The non-surviving Mrn is invalidated but still points to the old person
        // (we are recording the fact that between these dates, the hospital believed
        // that the mrn belonged to this person
        Mrn oldMrn = findOrAddMrn(oldMrnStr, null, false);
        Mrn survivingMrn = findOrAddMrn(survivingMrnStr, null, false);
        if (survivingMrn == null || oldMrn == null) {
            throw new InvalidMrnException(String.format("MRNs %s or %s (%s or %s) are not previously known, do nothing",
                    oldMrnStr, survivingMrnStr, oldMrn, survivingMrn));
        }
        Instant now = Instant.now();

        PersonMrn oldPersonMrn = getOnlyElementWhere(oldMrn.getPersons(), pm -> pm.isValidAsOf(now));

        PersonMrn survivingPersonMrn = getOnlyElementWhere(survivingMrn.getPersons(), pm -> pm.isValidAsOf(now));

        if (survivingPersonMrn == null || oldPersonMrn == null) {
            throw new InvalidMrnException(String.format(
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
     * Convert the simplified data from the HL7 message into Inform-db structures,
     * and merge with existing data depending on whether it's a new order or changes to an existing one.
     * @param pathologyOrder the pathology order details, may contain results
     * @throws HL7Exception if HAPI does
     * @throws MessageIgnoredException if message can't be processed
     */
    private void addOrUpdatePathologyOrder(PathologyOrder pathologyOrder) throws HL7Exception, MessageIgnoredException {
        String visitNumber = pathologyOrder.getVisitNumber();
        String epicCareOrderNumber = pathologyOrder.getEpicCareOrderNumber();

        Encounter encounter = getEncounterForOrder(epicCareOrderNumber, visitNumber);

        // build the entire fact hierarchy from the message data
        PatientFact pathologyOrderRootFact = buildPathologyOrderFacts(pathologyOrder);
        // We will need to do some diffing here to check whether we already have some
        // of the results or order details.
        logger.info("new pathology order facts: ");
        logger.info(pathologyOrderRootFact.toString());
        encounter.addFact(pathologyOrderRootFact);
        pathologyOrderRootFact = patientFactRepository.save(pathologyOrderRootFact);
        encounter = encounterRepo.save(encounter);
    }

    /**
     * Convert order details to Inform-db structures.
     * @param order the pathology order details
     * @return a PatientFact object that represents the order
     */
    private PatientFact buildPathologyOrderFacts(PathologyOrder order) {
        Instant storedFrom = Instant.now();
        // the valid from date should be the order time, when this fact became true.
        // however we are currently not getting this time, so use the next earliest
        // time we have, the collection/observation time
        Instant validFrom = order.getObservationDateTime();
        PatientFact pathFact = new PatientFact();
        pathFact.setFactType(getCreateAttribute(AttributeKeyMap.PATHOLOGY_ORDER));
        pathFact.setValidFrom(validFrom);
        pathFact.setStoredFrom(storedFrom);
        pathFact.addProperty(buildPatientProperty(storedFrom, validFrom, AttributeKeyMap.PATHOLOGY_ORDER_CONTROL_ID,
                order.getOrderControlId()));
        pathFact.addProperty(buildPatientProperty(storedFrom, validFrom, AttributeKeyMap.PATHOLOGY_EPIC_ORDER_NUMBER,
                order.getEpicCareOrderNumber()));
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

        // Will be empty if there are no results (eg. this is just an order).
        Map<String, PatientFact> resultFactsFromOrder = buildPathologyResultsFacts(order);
        for (PatientFact child : resultFactsFromOrder.values()) {
            pathFact.addChildFact(child);
        }

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
     * @param orderWithResults the pathology results
     * @return multiple PatientFact objects indexed by a unique identifier
     */
    private Map<String, PatientFact> buildPathologyResultsFacts(PathologyOrder orderWithResults) {
        List<PathologyResult> pathResults = orderWithResults.getPathologyResults();
        Map<String, PatientFact> facts = new HashMap<>();
        Instant storedFrom = Instant.now();
        for (PathologyResult pr : pathResults) {
            Instant resultTime = pr.getResultTime();
            PatientFact fact = new PatientFact();
            fact.setStoredFrom(storedFrom);
            fact.setValidFrom(resultTime);
            fact.setFactType(getCreateAttribute(AttributeKeyMap.PATHOLOGY_TEST_RESULT));

            String key = orderWithResults.getTestBatteryLocalCode() + "_" + pr.getTestItemLocalCode();

            fact.addProperty(buildPatientProperty(storedFrom, resultTime, AttributeKeyMap.PATHOLOGY_TEST_BATTERY_CODE,
                    orderWithResults.getTestBatteryLocalCode()));
            fact.addProperty(buildPatientProperty(storedFrom, resultTime, AttributeKeyMap.PATHOLOGY_TEST_CODE,
                    pr.getTestItemLocalCode()));
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

            facts.put(key, fact);
        }
        return facts;
    }

    /**
     * Look up an encounter by an existing order number, or by the encounter number if
     * the order is previously unknown.
     * Move to repo?
     * @param epicCareOrderNumber the Epic order number to search by
     * @param visitNumber the encounter/visit number to search by
     * @return the Encounter object that this order is attached to
     * @throws MessageIgnoredException if the Encounter can't be found by any method
     */
    private Encounter getEncounterForOrder(String epicCareOrderNumber, String visitNumber) throws MessageIgnoredException {
        PatientFact existingPathologyOrder = getOnlyElement(
                patientFactRepository.findAllPathologyOrdersByOrderNumber(epicCareOrderNumber));
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
            encounter = encounterRepo.findEncounterByEncounter(visitNumber);
            if (encounter == null) {
                throw new MessageIgnoredException("Can't find encounter to attach results to: " + visitNumber);
            }
        }
        if (!encounter.getEncounter().equals(visitNumber)) {
            throw new InformDbIntegrityException("parent encounter of existing order has encounter number "
                    + encounter.getEncounter() + ", expecting " + visitNumber);
        }
        return encounter;
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
