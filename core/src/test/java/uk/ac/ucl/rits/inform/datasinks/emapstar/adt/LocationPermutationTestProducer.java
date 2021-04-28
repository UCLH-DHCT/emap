package uk.ac.ucl.rits.inform.datasinks.emapstar.adt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import uk.ac.ucl.rits.inform.OrderPermutationBase;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LocationVisitAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LocationVisitRepository;
import uk.ac.ucl.rits.inform.informdb.movement.LocationVisit;
import uk.ac.ucl.rits.inform.informdb.movement.LocationVisitAudit;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.adt.AdtMessage;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Component
class LocationPermutationTestProducer extends OrderPermutationBase {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String defaultEncounter = "123412341234";
    @Autowired
    private LocationVisitRepository locationVisitRepository;
    @Autowired
    private LocationVisitAuditRepository locationVisitAuditRepository;
    private String[] locations;
    private String messagePath;
    private Instant initialAdmissionTime;

    /**
     * @param transactionManager Spring transaction manager
     */
    LocationPermutationTestProducer(@Autowired PlatformTransactionManager transactionManager) {
        super(transactionManager);
    }


    void setLocations(String[] locations) {
        this.locations = locations;
    }

    void setMessagePath(String messagePath) {
        this.messagePath = messagePath;
    }

    void setInitialAdmissionTime(Instant initialAdmissionTime) {
        this.initialAdmissionTime = initialAdmissionTime;
    }

    private <T extends AdtMessage> T getLocationAdtMessage(String filename) throws IOException {
        return getMessageFactory().getAdtMessage(String.format("%s/%s.yaml", messagePath, filename));
    }

    private void checkVisit(Instant admissionTime, Instant dischargeTime, String locationString, String messageInformation) {
        // keeping visits and audits for debugging
        List<LocationVisit> visits = StreamSupport.stream(locationVisitRepository.findAll().spliterator(), false).collect(Collectors.toList());
        List<LocationVisitAudit> audits = StreamSupport.stream(locationVisitAuditRepository.findAll().spliterator(), false).collect(Collectors.toList());
        logger.info("Checking visit {}", messageInformation);
        LocationVisit location = locationVisitRepository.findByHospitalVisitIdEncounterAndAdmissionTime(defaultEncounter, admissionTime)
                .orElseThrow(() -> new NoSuchElementException(messageInformation));
        assertEquals(dischargeTime, location.getDischargeTime(), String.format("Discharge time incorrect for %s", messageInformation));
        assertEquals(locationString, location.getLocationId().getLocationString(), String.format("Location incorrect for %s", messageInformation));
    }

    /**
     * Check each location lasts for a hour and is found in the order given.
     */
    @Override
    public void checkFinalState() {
        int adtCheckCount = 0;
        for (String location : locations) {
            checkVisit(
                    initialAdmissionTime.plus(adtCheckCount, ChronoUnit.HOURS),
                    initialAdmissionTime.plus(adtCheckCount + 1, ChronoUnit.HOURS),
                    location,
                    String.format("Location %d (%s)", adtCheckCount, location));
            adtCheckCount += 1;
        }
        List<LocationVisit> allVisits = StreamSupport
                .stream(locationVisitRepository.findAll().spliterator(), false)
                .collect(Collectors.toList());
        assertEquals(locations.length, allVisits.size(), String.format("Visits: %s", allVisits));
    }

    @Override
    protected void processFile(String fileName) throws Exception {
        logger.info("Processing location message: {}", fileName);
        processSingleMessage(getLocationAdtMessage(fileName));
    }

}
