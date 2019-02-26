package uk.ac.ucl.rits.inform;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang.NotImplementedException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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

    private final static Logger logger = LoggerFactory.getLogger(DBTester.class);

    private SessionFactory idsFactory;
    private Session idsSession;

    public DBTester() {
        idsFactory = makeSessionFactory("ids.cfg.xml", "IDS");
        idsSession = idsFactory.openSession();
        IdsMaster ids = null;
        try {
            ids = idsSession.find(IdsMaster.class, 0);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }

        System.out.println("ids record = " + ids);

        idsSession.close();
        idsFactory.close();
    }

    private static SessionFactory makeSessionFactory(String configFile, String label) {
        Configuration cfg = new Configuration().configure(configFile);
        cfg.addAnnotatedClass(IdsMaster.class);

        // take the username and password out of the environment
        // so the config file can safely go into source control
        String envVarUsername = label + "_USERNAME";
        String envVarPassword = label + "_PASSWORD";

        cfg.setProperty("hibernate.connection.username", System.getenv(envVarUsername));
        cfg.setProperty("hibernate.connection.password", System.getenv(envVarPassword));

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
