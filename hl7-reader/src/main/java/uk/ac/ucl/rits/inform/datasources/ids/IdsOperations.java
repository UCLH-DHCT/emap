package uk.ac.ucl.rits.inform.datasources.ids;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v26.message.ORM_O01;
import ca.uhn.hl7v2.model.v26.message.ORU_R01;
import ca.uhn.hl7v2.model.v26.segment.MSH;
import ca.uhn.hl7v2.parser.PipeParser;
import ca.uhn.hl7v2.util.Hl7InputStreamMessageIterator;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.datasources.ids.exceptions.Hl7InconsistencyException;
import uk.ac.ucl.rits.inform.datasources.ids.exceptions.Hl7MessageNotImplementedException;
import uk.ac.ucl.rits.inform.datasources.ids.exceptions.ReachedEndException;
import uk.ac.ucl.rits.inform.datasources.idstables.IdsMaster;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessage;
import uk.ac.ucl.rits.inform.interchange.messaging.Publisher;
import uk.ac.ucl.rits.inform.interchange.springconfig.EmapDataSource;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Semaphore;


/**
 * Operations that can be performed on the IDS.
 */
@Component
@EntityScan("uk.ac.ucl.rits.inform.datasources.ids")
public class IdsOperations implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(IdsOperations.class);

    private SessionFactory idsFactory;
    private AdtMessageFactory adtMessageFactory;
    private IdsProgressRepository idsProgressRepository;
    private boolean idsEmptyOnInit;
    private Integer defaultStartUnid;
    private Integer endUnid;

    /**
     * @param idsCfgXml             injected param
     * @param defaultStartDatetime  the start date to use if no progress has been previously recorded in the DB
     * @param endDatetime           the datetime to finish processing messages, regardless of previous progress
     * @param environment           injected param
     * @param adtMessageFactory     injected AdtMessageFactory
     * @param idsProgressRepository injected IdsProgressRepository
     */
    public IdsOperations(
            @Value("${ids.cfg.xml.file}") String idsCfgXml,
            @Value("${ids.cfg.default-start-datetime}") Instant defaultStartDatetime,
            @Value("${ids.cfg.end-datetime}") Instant endDatetime,
            @Autowired Environment environment,
            @Autowired AdtMessageFactory adtMessageFactory,
            @Autowired IdsProgressRepository idsProgressRepository) {
        String envPrefix = "IDS";
        if (environment.acceptsProfiles("test")) {
            envPrefix = null;
        }
        this.adtMessageFactory = adtMessageFactory;
        this.idsProgressRepository = idsProgressRepository;
        logger.info("IdsOperations() opening config file " + idsCfgXml);
        idsFactory = makeSessionFactory(idsCfgXml, envPrefix);
        idsEmptyOnInit = getIdsIsEmpty();
        logger.info("IdsOperations() idsEmptyOnInit = " + idsEmptyOnInit);
        this.defaultStartUnid = getFirstMessageUnidFromDate(defaultStartDatetime);
        this.endUnid = getFirstMessageUnidFromDate(endDatetime);

        // Since progress is stored as the unid (the date info is purely for human convenience),
        // there is no way to translate a future date into a unid.
        // This feature is only intended for processing messages in the past, so that's OK.
        logger.info(String.format(
                "IDS message processing boundaries: Start date = %s, start unid = %d  -->  End date = %s, end unid = %d",
                defaultStartDatetime, this.defaultStartUnid, endDatetime, this.endUnid));
    }


    /**
     * We are writing to the HL7 queue.
     * @return the datasource enum for the hl7 queue
     */
    @Bean
    public EmapDataSource getHl7DataSource() {
        return EmapDataSource.HL7_QUEUE;
    }

    /**
     * Call to close when you're finished with the object.
     */
    @Override
    public void close() {
        if (idsFactory != null) {
            idsFactory.close();
        }
        idsFactory = null;
    }

    /**
     * @return Was the IDS empty when this object was initialised?
     */
    public boolean getIdsEmptyOnInit() {
        return idsEmptyOnInit;
    }

    /**
     * @return Is the IDS currently empty?
     */
    private boolean getIdsIsEmpty() {
        try (Session idsSession = idsFactory.openSession()) {
            idsSession.setDefaultReadOnly(true);
            // check is empty
            Query<IdsMaster> qexists = idsSession.createQuery("select i from IdsMaster i", IdsMaster.class);
            qexists.setMaxResults(1);
            boolean idsIsEmpty = qexists.list().isEmpty();
            return idsIsEmpty;
        }
    }

    /**
     * Find the first message in the IDS that came in at or after a certain
     * timestamp.
     * @param fromDateTime the timestamp to start from, or null for no boundary
     * @return the unid of the first message to be persisted at or after that time,
     * or null if there are no such messages or no bound was requested (fromDateTime == null)
     */
    private Integer getFirstMessageUnidFromDate(Instant fromDateTime) {
        if (fromDateTime == null) {
            // bypass this slow query if no bound was requested
            return null;
        }
        try (Session idsSession = idsFactory.openSession()) {
            idsSession.setDefaultReadOnly(true);
            Query<IdsMaster> qexists = idsSession.createQuery(
                    "select i from IdsMaster i where i.persistdatetime >= :fromDatetime order by i.unid", IdsMaster.class);
            qexists.setParameter("fromDatetime", fromDateTime);
            qexists.setMaxResults(1);
            List<IdsMaster> msgs = qexists.list();
            if (msgs.isEmpty()) {
                logger.warn(String.format("No IDS messages were found beyond the specified date %s, is it in the future?", fromDateTime));
                return null;
            } else {
                return msgs.get(0).getUnid();
            }
        }
    }

    /**
     * Create a session factory from the given config file, overwriting configurable
     * values from the environment, if specified.
     * @param configFile the hibernate xml config file
     * @param envPrefix  the prefix for environment variable names, or null if no
     *                   variables should be read from the environment
     * @return the SessionFactory thus created
     */
    private static SessionFactory makeSessionFactory(String configFile, String envPrefix) {
        Configuration cfg = new Configuration().configure(configFile);
        cfg.addAnnotatedClass(IdsMaster.class);

        if (envPrefix != null) {
            // take the username and password out of the environment
            // so the config file can safely go into source control
            String envVarUrl = envPrefix + "_JDBC_URL";
            String envVarUsername = envPrefix + "_USERNAME";
            String envVarPassword = envPrefix + "_PASSWORD";
            String envVarSchema = envPrefix + "_SCHEMA";

            String url = System.getenv(envVarUrl);
            String uname = System.getenv(envVarUsername);
            String pword = System.getenv(envVarPassword);
            String schema = System.getenv(envVarSchema);
            if (url != null) {
                cfg.setProperty("hibernate.connection.url", url);
            }
            if (uname != null) {
                cfg.setProperty("hibernate.connection.username", uname);
            }
            if (pword != null) {
                cfg.setProperty("hibernate.connection.password", pword);
            }
            if (schema != null) {
                cfg.setProperty("hibernate.default_schema", schema);
            }
        }

        return cfg.buildSessionFactory();
    }

    /**
     * @return the unique ID for the last IDS message we have successfully processed
     */
    @Transactional
    int getLatestProcessedId() {
        IdsProgress onlyRow = idsProgressRepository.findOnlyRow();

        if (onlyRow == null) {
            onlyRow = new IdsProgress();
            // use default start time, if specified
            logger.info(String.format("No progress found, initialising to unid = %d", this.defaultStartUnid));
            if (this.defaultStartUnid != null) {
                // initialise progress as per config, otherwise it'll just stay at 0 (ie. the very beginning)
                onlyRow.setLastProcessedIdsUnid(this.defaultStartUnid);
            }
            onlyRow = idsProgressRepository.save(onlyRow);
        }
        return onlyRow.getLastProcessedIdsUnid();
    }

    /**
     * Record that we have processed all messages up to the specified message.
     * @param lastProcessedIdsUnid the unique ID for the latest IDS message we have
     *                             processed
     * @param messageDatetime      the timestamp of this message
     * @param processingEnd        the time this message was actually processed
     */
    @Transactional
    void setLatestProcessedId(int lastProcessedIdsUnid, Instant messageDatetime, Instant processingEnd) {
        IdsProgress onlyRow = idsProgressRepository.findOnlyRow();
        onlyRow.setLastProcessedIdsUnid(lastProcessedIdsUnid);
        onlyRow.setLastProcessedMessageDatetime(messageDatetime);
        onlyRow.setLastProcessingDatetime(processingEnd);
        onlyRow = idsProgressRepository.save(onlyRow);
    }


    /**
     * Write a message into the IDS. For test IDS instances only!
     * @param hl7message     the HL7 message text
     * @param id             the IDS unique ID
     * @param patientInfoHl7 the parser to get various HL7 fields out of
     * @throws HL7Exception if HAPI does
     */
    private void writeToIds(String hl7message, int id, PatientInfoHl7 patientInfoHl7) throws HL7Exception {
        // To avoid the risk of accidentally attempting to write into the real
        // IDS, check that the IDS was empty when we started. Emptiness strongly
        // suggests that this is a test IDS.
        if (!getIdsEmptyOnInit()) {
            throw new RuntimeException("Cannot write into non-empty IDS, are you sure this is a test?");
        }
        Session idsSession = idsFactory.openSession();
        try {
            Transaction tx = idsSession.beginTransaction();
            IdsMaster idsrecord = new IdsMaster();

            String triggerEvent = patientInfoHl7.getTriggerEvent();
            String mrn = patientInfoHl7.getMrn();
            String patientClass = patientInfoHl7.getPatientClass();
            String patientLocation = patientInfoHl7.getFullLocationString();
            Instant messageTimestamp = patientInfoHl7.getMessageTimestamp();
            String sendingApplication = patientInfoHl7.getSendingApplication();

            // We can't use a sequence to assign ID because it won't exist on the
            // real IDS, so that will cause Hibernate validation to fail.
            // However, since we're starting with an empty IDS and populating it
            // in a single shot, just set the id manually in the client.
            idsrecord.setUnid(id);
            idsrecord.setHl7message(hl7message);
            idsrecord.setMessagetype(triggerEvent);
            idsrecord.setHospitalnumber(mrn);
            idsrecord.setPatientclass(patientClass);
            idsrecord.setPatientlocation(patientLocation);
            idsrecord.setMessagedatetime(messageTimestamp);
            idsrecord.setSenderapplication(sendingApplication);
            idsSession.save(idsrecord);
            tx.commit();
        } finally {
            idsSession.close();
        }
    }

    /**
     * Entry point for populating a test IDS from a file specified on the command
     * line.
     * @return The CommandLineRunner
     */
    @Bean
    @Profile("populate")
    public CommandLineRunner populateIDS() {
        return (args) -> {
            HapiContext context = HL7Utils.initializeHapiContext();
            String hl7fileSource = args[0];
            File file = new File(hl7fileSource);
            logger.info("populating the IDS from file " + file.getAbsolutePath() + " exists = " + file.exists());
            InputStream is = new BufferedInputStream(new FileInputStream(file));
            Hl7InputStreamMessageIterator hl7iter = new Hl7InputStreamMessageIterator(is, context);
            hl7iter.setIgnoreComments(true);
            int count = 0;
            while (hl7iter.hasNext()) {
                count++;
                Message msg = hl7iter.next();
                String singleMessageText = msg.encode();
                PatientInfoHl7 patientInfoHl7 = adtMessageFactory.getPatientInfo(msg);

                this.writeToIds(singleMessageText, count, patientInfoHl7);
            }
            logger.info("Wrote " + count + " messages to IDS");
            context.close();
        };
    }

    /**
     * Get next entry in the IDS, if it exists.
     * @param lastProcessedId the last one we have successfully processed
     * @return the first message that comes after lastProcessedId, or null if there
     * isn't one
     */
    public IdsMaster getNextHL7IdsRecord(int lastProcessedId) {
        // consider changing to "get next N messages" for more efficient database
        // performance
        // when doing large "catch-up" operations
        // (handle the batching in the caller)
        try (Session idsSession = idsFactory.openSession();) {
            idsSession.setDefaultReadOnly(true);
            Query<IdsMaster> qnext =
                    idsSession.createQuery("SELECT i FROM IdsMaster i where i.unid > :lastProcessedId order by i.unid", IdsMaster.class);
            qnext.setParameter("lastProcessedId", lastProcessedId);
            qnext.setMaxResults(1);
            List<IdsMaster> nextMsgOrEmpty = qnext.list();
            if (nextMsgOrEmpty.isEmpty()) {
                return null;
            } else if (nextMsgOrEmpty.size() == 1) {
                return nextMsgOrEmpty.get(0);
            } else {
                throw new InternalError();
            }
        }
    }

    /**
     * Return the next HL7 message in the IDS. If there are no more, block until
     * there are.
     * @param lastProcessedId the latest unique ID that has already been processed
     * @return the next HL7 message record
     */
    public IdsMaster getNextHL7IdsRecordBlocking(int lastProcessedId) {
        long secondsSleep = 10;
        IdsMaster idsMsg = null;
        while (true) {
            idsMsg = getNextHL7IdsRecord(lastProcessedId);
            if (idsMsg == null) {
                logger.info(String.format("No more messages in IDS, retrying in %d seconds", secondsSleep));
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
     * Wrapper for the entire transaction that performs: - read latest processed ID
     * from Inform-db (ETL metadata) - process the message and write to Inform-db -
     * write the latest processed ID to reflect the above message. Blocks until
     * there are new messages.
     * @param publisher the local AMQP handling class
     * @param parser    the HAPI parser to be used
     * @throws AmqpException       if rabbitmq write fails
     * @throws ReachedEndException if we have reached the pre-configured last message
     */
    @Transactional
    public void parseAndSendNextHl7(Publisher publisher, PipeParser parser) throws AmqpException, ReachedEndException {
        int lastProcessedId = getLatestProcessedId();
        logger.info("parseAndSendNextHl7, lastProcessedId = " + lastProcessedId);
        if (this.endUnid != null && lastProcessedId >= this.endUnid) {
            logger.info(String.format("lastProcessedId = %d  >=  endUnid = %d, exiting", lastProcessedId, this.endUnid));
            throw new ReachedEndException();
        }
        IdsMaster idsMsg = getNextHL7IdsRecordBlocking(lastProcessedId);

        Instant messageDatetime = idsMsg.getMessagedatetime();
        String sender = idsMsg.getSenderapplication();

        final Set<String> allowedSenders = new HashSet<>(Arrays.asList("WinPath", "EPIC"));

        try {
            if (!allowedSenders.contains(sender)) {
                logger.warn(String.format("[" + idsMsg.getUnid() + "] Skipping message with senderapplication=\"%s\"", sender));
                return;
            }
            String hl7msg = idsMsg.getHl7message();
            // HL7 is supposed to use \r for line endings, but
            // the IDS uses \n
            hl7msg = hl7msg.replace("\n", "\r");
            Message msgFromIds;
            try {
                msgFromIds = parser.parse(hl7msg);
            } catch (HL7Exception hl7e) {
                StringWriter st = new StringWriter();
                hl7e.printStackTrace(new PrintWriter(st));
                logger.error("[" + idsMsg.getUnid() + "] HL7 parsing error:\n" + st.toString());
                return;
            }

            // One HL7 message can give rise to multiple interchange messages (pathology orders),
            // but failure is only expressed on a per-HL7 message basis.
            try {
                List<? extends EmapOperationMessage> messagesFromHl7Message = messageFromHl7Message(msgFromIds, idsMsg.getUnid());
                int subMessageCount = 0;
                for (EmapOperationMessage msg : messagesFromHl7Message) {
                    subMessageCount++;
                    logger.info(String.format("[%d] sending message (%d/%d) to RabbitMQ ", idsMsg.getUnid(),
                            subMessageCount, messagesFromHl7Message.size()));
                    Semaphore semaphore = new Semaphore(0);
                    publisher.submit(msg, msg.getSourceMessageId(), msg.getSourceMessageId() + "_1", () -> {
                        logger.warn("callback for " + msg.getSourceMessageId());
                        semaphore.release();
                    });
                    semaphore.acquire();
                }
            } catch (HL7Exception | Hl7InconsistencyException | InterruptedException e) {
                String errMsg =
                        "[" + idsMsg.getUnid() + "] Skipping due to " + e.getStackTrace() + " (" + msgFromIds.getClass() + ")";
                logger.error(errMsg);
            }
        } finally {
            Instant processingEnd = Instant.now();
            setLatestProcessedId(idsMsg.getUnid(), messageDatetime, processingEnd);
        }
    }

    /**
     * Using the type+trigger event of the HL7 message, create the correct type of
     * interchange message. One HL7 message can give rise to multiple interchange messages.
     * @param msgFromIds the HL7 message
     * @param idsUnid    the sequential ID number from the IDS (unid)
     * @return list of Emap interchange messages, can be empty if no messages should result
     * @throws HL7Exception              if HAPI does
     * @throws Hl7InconsistencyException if the HL7 message contradicts itself
     */
    public List<? extends EmapOperationMessage> messageFromHl7Message(Message msgFromIds, int idsUnid)
            throws HL7Exception, Hl7InconsistencyException {
        MSH msh = (MSH) msgFromIds.get("MSH");
        String messageType = msh.getMessageType().getMessageCode().getValueOrEmpty();
        String triggerEvent = msh.getMessageType().getTriggerEvent().getValueOrEmpty();
        String sendingFacility = msh.getMsh4_SendingFacility().getHd1_NamespaceID().getValueOrEmpty();
        logger.info(String.format("%s^%s", messageType, triggerEvent));
        String sourceId = String.format("%010d", idsUnid);

        List<EmapOperationMessage> messages = new ArrayList<>();

        switch (messageType) {
            case "ADT":
                buildAndAddAdtMessage(msgFromIds, sourceId, true, messages);
            case "ORU":
                if (triggerEvent.equals("R01")) {
                    if (sendingFacility.equals("Vitals")) {
                        buildAndAddAdtMessage(msgFromIds, sourceId, false, messages);
                        VitalSignBuilder vitalSignBuilder = new VitalSignBuilder(sourceId, (ORU_R01) msgFromIds);
                        messages.addAll(vitalSignBuilder.getMessages());
                    } else {
                        buildAndAddAdtMessage(msgFromIds, sourceId, false, messages);
                        // get all result batteries in the message
                        messages.addAll(PathologyOrderBuilder.buildPathologyOrdersFromResults(sourceId, (ORU_R01) msgFromIds));
                    }
                }
                break;
            case "ORM":
                if (triggerEvent.equals("O01")) {
                    buildAndAddAdtMessage(msgFromIds, sourceId, false, messages);
                    // get all orders in the message
                    messages.addAll(PathologyOrderBuilder.buildPathologyOrders(sourceId, (ORM_O01) msgFromIds));
                }
                break;
            default:
                logger.error(String.format("Could not construct message from unknown type %s/%s", messageType, triggerEvent));

        }
        return messages;
    }

    /**
     * Build an ADT interchange message from HL7 message, if successful, add this to the list of messages.
     * @param msgFromIds    HL7 message
     * @param sourceId      message source ID
     * @param fromAdtStream if from ADT stream, will throw HL7 exception
     * @param messages      interchange messages build from the single HL7 message
     * @throws HL7Exception
     */
    private void buildAndAddAdtMessage(final Message msgFromIds, final String sourceId, final boolean fromAdtStream, List<EmapOperationMessage> messages) throws HL7Exception {
        try {
            messages.add(adtMessageFactory.getAdtMessage(msgFromIds, sourceId));
        } catch (Hl7MessageNotImplementedException | HL7Exception e) {
            if (fromAdtStream && e instanceof HL7Exception) {
                throw (HL7Exception) e;
            } else {
                logger.warn("Ignoring message: " + e.toString());
            }
        }
    }
}
