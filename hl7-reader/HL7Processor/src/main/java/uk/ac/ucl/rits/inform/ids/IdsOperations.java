package uk.ac.ucl.rits.inform.ids;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@EntityScan("uk.ac.ucl.rits.inform.ids")
public class IdsOperations {

    private SessionFactory idsFactory;
    private boolean idsEmptyOnInit;

    public IdsOperations(
            @Value("${ids.cfg.xml.file}") String idsCfgXml,
            @Autowired Environment environment) {
        String envPrefix = "IDS";
        if (environment.acceptsProfiles("test")) {
            envPrefix = null;
        }
        System.out.println("DBTester() 1 - opening config file " + idsCfgXml);
        idsFactory = makeSessionFactory(idsCfgXml, envPrefix);
        System.out.println("DBTester() 2");
        idsEmptyOnInit = getIdsIsEmpty();
        System.out.println("DBTester() : idsEmptyOnInit = " + idsEmptyOnInit);
    }

    /**
     * You must close the returned Session after you're done
     * @return the Session
     */
    public Session openSession() {
        return idsFactory.openSession();
    }

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
     * Is the IDS currently empty?
     */
    private boolean getIdsIsEmpty() {
        Session idsSession = idsFactory.openSession();
        idsSession.setDefaultReadOnly(true);
        // check is empty
        Query<IdsMaster> qexists = idsSession.createQuery("from IdsMaster", IdsMaster.class);
        qexists.setMaxResults(1);
        boolean idsIsEmpty = qexists.list().isEmpty();
        idsSession.close();
        return idsIsEmpty;
    }

    /**
     * create a session factory from the given config file, overwriting configurable
     * values from the environment, if specified
     *
     * @param configFile the hibernate xml config file
     * @param envPrefix  the prefix for environment variable names, or null if no
     *                   variables should be read from the environment
     *
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

            String url = System.getenv(envVarUrl);
            String uname = System.getenv(envVarUsername);
            String pword = System.getenv(envVarPassword);
            if (url != null) {
                cfg.setProperty("hibernate.connection.url", url);
            }
            if (uname != null) {
                cfg.setProperty("hibernate.connection.username", uname);
            }
            if (pword != null) {
                cfg.setProperty("hibernate.connection.password", pword);
            }
        }

        return cfg.buildSessionFactory();
    }

    public void writeToIds(String hl7message, int id, String triggerEvent, String mrn, String patientClass, String patientLocation) {
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
            idsSession.save(idsrecord);
            tx.commit();
        } finally {
            idsSession.close();
        }
    }

}
