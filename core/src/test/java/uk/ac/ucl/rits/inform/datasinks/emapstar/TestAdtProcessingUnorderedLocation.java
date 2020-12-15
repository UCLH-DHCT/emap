package uk.ac.ucl.rits.inform.datasinks.emapstar;

import com.google.common.collect.Collections2;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LocationVisitAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LocationVisitRepository;
import uk.ac.ucl.rits.inform.informdb.movement.LocationVisit;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.adt.AdtMessage;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

class TestAdtProcessingUnorderedLocation extends MessageProcessingBase {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private LocationVisitRepository locationVisitRepository;
    @Autowired
    private LocationVisitAuditRepository locationVisitAuditRepository;
    private TransactionTemplate transactionTemplate;

    /***
     * @param transactionManager Spring transaction manager
     */
    public TestAdtProcessingUnorderedLocation(@Autowired PlatformTransactionManager transactionManager) {
        transactionTemplate = new TransactionTemplate(transactionManager);
    }

    private String[] adtFilenames = {"02_A01", "03_A02", "04_A02", "05_A02", "06_A02", "07_A06", "08_A03"};
    private String[] locations = {
            "ED^UCHED RAT CHAIR^RAT-CHAIR",
            "ED^NON COVID MAJORS 05^05-NON COVID MAJORS",
            "ED^NON COVID MAJORS 04^04-NON COVID MAJORS",
            "ED^NON COVID MAJORS 05^05-NON COVID MAJORS",
            "ED^NON COVID MAJORS 04^04-NON COVID MAJORS",
            "EAU^UCH T00 EAU BY02^BY02-08",
    };

    private <T extends AdtMessage> T getLocationAdtMessage(String filename) {
        return messageFactory.getAdtMessage(String.format("location/%s.yaml", filename));
    }

    void checkVisit(Instant admissionTime, Instant dischargeTime, String locationString, String adtMessage) {
        LocationVisit location = locationVisitRepository.findByHospitalVisitIdEncounterAndAdmissionTime(defaultEncounter, admissionTime)
                .orElseThrow(() -> new NoSuchElementException(adtMessage));
        Assertions.assertEquals(dischargeTime, location.getDischargeTime(), String.format("Discharge time incorrect for %s", adtMessage));
        Assertions.assertEquals(locationString, location.getLocationId().getLocationString(), String.format("Location incorrect for %s", adtMessage));
    }

    private void checkAllVisits() {
        // From A02
        Instant admissionInstant = Instant.parse("2013-02-11T11:00:52Z");

        int adtCheckCount = 0;
        for (String location : locations) {
            checkVisit(
                    admissionInstant.plus(adtCheckCount, ChronoUnit.HOURS),
                    admissionInstant.plus(adtCheckCount + 1, ChronoUnit.HOURS),
                    location,
                    adtFilenames[adtCheckCount]);
            adtCheckCount += 1;
        }
    }

    /**
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    void testOrderedMessages() throws EmapOperationMessageProcessingException {
        runTest(List.of(adtFilenames));
    }

    void runTest(List<String> fileNames) throws EmapOperationMessageProcessingException {
        for (String filename : fileNames) {
            logger.info("Processing location message: {}", filename);
            processSingleMessage(getLocationAdtMessage(filename));
        }
        checkAllVisits();
    }

    /**
     * Create all the tests.
     * @return A stream of all the possible valid orderings.
     */
    @TestFactory
    public Stream<DynamicTest> testUnorderedMessages() {
        Collection<List<String>> fullMessages = Collections2.orderedPermutations(List.of(adtFilenames));

        return fullMessages.stream().map(l -> DynamicTest.dynamicTest("Test " + l.toString(), () -> {
            Exception e = transactionTemplate.execute(status -> {
                status.setRollbackOnly();
                try {
                    runTest(l);
                } catch (EmapOperationMessageProcessingException a) {
                    return a;
                }
                return null;
            });
            if (e != null) {
                throw e;
            }
        }));
    }


}
