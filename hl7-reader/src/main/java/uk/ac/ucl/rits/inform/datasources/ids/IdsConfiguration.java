package uk.ac.ucl.rits.inform.datasources.ids;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import uk.ac.ucl.rits.inform.datasources.idstables.IdsMaster;

import java.time.Instant;

/**
 * Allows access to the IDS configuration for building session factory and start and end date.
 * @author Stef Piatek
 */
@Component
public class IdsConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(IdsConfiguration.class);


    private Instant startDateTime;
    private Instant endDatetime;
    private SessionFactory sessionFactory;

    /**
     * @param idsCfgXml             IDS config filename to use
     * @param serviceStartDatetime  the start date to use if no progress has been previously recorded in the DB
     * @param endDatetime           the datetime to finish processing messages, regardless of previous progress
     * @param startFromLastId       start processing from the previous progress if it exists
     * @param environment           autowired
     * @param idsProgressRepository autowired
     */
    public IdsConfiguration(
            @Value("${ids.cfg.xml.file}") String idsCfgXml,
            @Value("${ids.cfg.default-start-datetime}") Instant serviceStartDatetime,
            @Value("${ids.cfg.end-datetime}") Instant endDatetime,
            @Value("${ids.cfg.start-from-last-id}") boolean startFromLastId,
            Environment environment,
            IdsProgressRepository idsProgressRepository) {
        this.endDatetime = endDatetime;
        setStartDate(serviceStartDatetime, startFromLastId, idsProgressRepository);
        sessionFactory = makeSessionFactory(idsCfgXml, environment);
    }

    public Instant getStartDateTime() {
        return startDateTime;
    }

    public Instant getEndDatetime() {
        return endDatetime;
    }

    SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    /**
     * Set start date from service start or previous progress if it exists and configured to do so.
     * @param serviceStartDatetime  the start date to use if no progress has been previously recorded in the DB
     * @param startFromLastId       start processing from the previous progress if it exists
     * @param idsProgressRepository autowired
     */
    private void setStartDate(Instant serviceStartDatetime, boolean startFromLastId, IdsProgressRepository idsProgressRepository) {
        IdsProgress idsProgress = idsProgressRepository.findOnlyRow();
        if (startFromProgressAndProgressAfterStartDate(serviceStartDatetime, startFromLastId, idsProgress)) {
            logger.info("Using the datetime of the last-processed row in the IDS as the start datetime");
            startDateTime = idsProgress.getLastProcessingDatetime();
        } else {
            logger.info("Using the service start datetime as the start datetime");
            startDateTime = serviceStartDatetime;
        }
    }

    private boolean startFromProgressAndProgressAfterStartDate(Instant serviceStartDatetime, boolean startFromLastId, IdsProgress idsProgress) {
        return startFromLastId && idsProgress != null && idsProgress.getLastProcessedMessageDatetime().isAfter(serviceStartDatetime);
    }

    /**
     * Create a session factory from the class' IDS configuration file, overwriting configurable values from the environment, if specified.
     * @param idsCfgXml   IDS config filename to use
     * @param environment autowired
     * @return the SessionFactory thus created
     */
    private SessionFactory makeSessionFactory(String idsCfgXml, Environment environment) {
        String envPrefix = "IDS";
        if (environment.acceptsProfiles("test")) {
            envPrefix = null;
        }
        logger.info("Reading IDS config file " + idsCfgXml);
        Configuration cfg = new Configuration().configure(idsCfgXml);
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
}
