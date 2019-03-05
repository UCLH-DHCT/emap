package uk.ac.ucl.rits.inform;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang.NotImplementedException;
import org.hibernate.query.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v27.message.ADT_A01;
import ca.uhn.hl7v2.parser.PipeParser;
import uk.ac.ucl.rits.inform.ids.IdsMaster;
import uk.ac.ucl.rits.inform.informdb.Attribute;
import uk.ac.ucl.rits.inform.informdb.AttributeRepository;
import uk.ac.ucl.rits.inform.informdb.Encounter;
import uk.ac.ucl.rits.inform.informdb.EncounterRepository;
import uk.ac.ucl.rits.inform.informdb.Mrn;
import uk.ac.ucl.rits.inform.informdb.MrnRepository;
import uk.ac.ucl.rits.inform.informdb.PatientDemographicFact;
import uk.ac.ucl.rits.inform.informdb.PatientDemographicFactRepository;
import uk.ac.ucl.rits.inform.informdb.Person;
import uk.ac.ucl.rits.inform.informdb.PersonRepository;

@Component
public class DBTester {
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
    private IdsProgressRepository idsProgressRepository;

    private final static Logger logger = LoggerFactory.getLogger(DBTester.class);

    private String idsCfgXml;

    private SessionFactory idsFactory;
    private Session idsSession;

    public DBTester(@Value("${ids.cfg.xml.file}") String idsCfgXml) {
        this.idsCfgXml = idsCfgXml;
        System.out.println("DBTester() 1 - opening config file " + idsCfgXml);
        idsFactory = makeSessionFactory(idsCfgXml, "IDS");
        System.out.println("DBTester() 2");
    }

    public void close() {
        if (idsFactory != null) {
            idsFactory.close();
        }
        idsFactory = null;
    }

    /**
     * Wrapper for the entire transaction that performs: - read latest processed ID
     * from Inform-db (ETL metadata) - process the message and write to Inform-db -
     * write the latest processed ID to reflect the above message
     * 
     * @param parser the HAPI parser to be used
     * 
     * @return true if a message was processed, false if there were no messages to
     *         process
     */
    @Transactional(rollbackFor = HL7Exception.class)
    public boolean processNextHl7(PipeParser parser) throws HL7Exception {
        System.out.println("hello there1a");
        int lastProcessedId = getLatestProcessedId();

        System.out.println("hello there1b");
        IdsMaster idsMsg = getNextHL7IdsRecord(lastProcessedId);
        if (idsMsg == null) {
            return false;
        }
        System.out.println("hello there2, msg = " + idsMsg);
        Message msgFromIds = null;
        msgFromIds = parser.parse(idsMsg.getHl7message());
        System.out.println("version is " + msgFromIds.getVersion());
        if (msgFromIds instanceof ADT_A01) {
            ADT_A01 adt_01 = (ADT_A01) msgFromIds;
            System.out.println("hello there4");
            Encounter enc = addEncounter(new A01Wrap(adt_01));
            System.out.println("Added from IDS: " + enc.toString());
        } else {
            System.out.println("Not an A01, skipping " + msgFromIds.getClass());
        }
        setLatestProcessedId(idsMsg.getUnid());

        return true;
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

    public String getNextHL7IdsMsg(int lastProcessedId) {
        IdsMaster next = getNextHL7IdsRecord(lastProcessedId);
        if (next == null) {
            return null;
        } else {
            System.out.println("Got message with unid " + next.getUnid());
            return next.getHl7message();
        }
    }

    public IdsMaster getNextHL7IdsRecord(int lastProcessedId) {
        idsSession = idsFactory.openSession();
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

    private static SessionFactory makeSessionFactory(String configFile, String label) {
        Configuration cfg = new Configuration().configure(configFile);
        cfg.addAnnotatedClass(IdsMaster.class);

        // take the username and password out of the environment
        // so the config file can safely go into source control
        String envVarUsername = label + "_USERNAME";
        String envVarPassword = label + "_PASSWORD";

        String uname = System.getenv(envVarUsername);
        String pword = System.getenv(envVarPassword);
        if (uname != null) {
            cfg.setProperty("hibernate.connection.username", uname);
        }
        if (pword != null) {
            cfg.setProperty("hibernate.connection.password", pword);
        }

        return cfg.buildSessionFactory();
    }

    /**
     * Create a new encounter using the details given in the A01 message. This may
     * also entail creating a new Mrn and Person if these don't already exist.
     * 
     * @param encounterDetails
     */
    @Transactional
    public Encounter addEncounter(A01Wrap encounterDetails) {
        String mrnStr = encounterDetails.getMrn();
        Mrn newOrExistingMrn = findOrAddMrn(mrnStr);
        // Encounter is always a new one for an A01
        Encounter enc = new Encounter();
        enc.setStore_datetime(Timestamp.from(Instant.now()));
        enc.setEncounter(encounterDetails.getVisitNumber());
        enc.setEvent_time(encounterDetails.getEventTime());
        enc.setMrn(newOrExistingMrn);
        enc = encounterRepo.save(enc);
        PatientDemographicFact fact = new PatientDemographicFact();
        fact.setEncounter(enc);
        // need to make an attribute repo with a find by attr ID method??
        // fact.setKeyValueProp(Attribute.AttributeId.FAMILY_NAME,
        // encounterDetails.getFamilyName());
        Optional<Attribute> attropt = attributeRepository.findById(Attribute.AttributeId.FAMILY_NAME);
        Attribute attr;
        if (attropt.isPresent()) {
            attr = attropt.get();
        } else {
            // TODO: The correct way would be to pre-populate all attrs on startup
            attr = new Attribute();
            attr.setAttribute_id(Attribute.AttributeId.FAMILY_NAME);
            attr.setDescription("Family Name");
            attr = attributeRepository.save(attr);
        }
        fact.setKeyValueProp(attr, encounterDetails.getFamilyName());
        fact = patientDemographicFactRepository.save(fact);

        return enc;
    }

    public long countEncounters() {
        return encounterRepo.count();
    }

    // Find an existing Mrn by its string representation, or create a new
    // Mrn record if it doesn't exist.
    private Mrn findOrAddMrn(String mrnStr) {
        List<Mrn> allMrns = mrnRepo.findByMrnString(mrnStr);
        Mrn mrn;
        if (allMrns.isEmpty()) {
            /*
             * If it's a new MRN then assume that it's also a new person (or at least we
             * don't know which person it is yet, and we'll have to wait for the merge
             * before we find out, so we'll have to create a new person for now)
             */
            mrn = new Mrn();
            mrn.setMrn(mrnStr);
            Person pers = new Person();
            personRepo.save(pers);
            mrn.setPerson(pers);
        } else if (allMrns.size() > 1) {
            throw new NotImplementedException("Does this even make sense?");
        } else {
            mrn = allMrns.get(0);
        }
        mrn.setStore_datetime(Timestamp.from(Instant.now()));
        mrn = mrnRepo.save(mrn);
        return mrn;
    }

    private Person findOrAddPerson() {
        Optional<Person> pers = personRepo.findById(42);
        if (pers.isPresent()) {
            Person pgot = pers.get();
            System.out.println(pgot.toString());
            return pgot;

        } else {
            Person pnew = personRepo.save(new Person());
            System.out.println(pnew.toString());
            return pnew;
        }
    }

}
