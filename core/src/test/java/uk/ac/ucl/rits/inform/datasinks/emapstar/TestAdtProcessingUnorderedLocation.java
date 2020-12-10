package uk.ac.ucl.rits.inform.datasinks.emapstar;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LocationVisitRepository;
import uk.ac.ucl.rits.inform.informdb.movement.LocationVisit;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.adt.AdtMessage;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.NoSuchElementException;

class TestAdtProcessingUnorderedLocation extends MessageProcessingBase {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private LocationVisitRepository locationVisitRepository;

    private String[] adtFilenames = {"01_A04", "02_A01", "03_A02", "04_A02", "05_A02", "06_A02", "07_A02", "08_A06", "09_A03"};
    private String[] locations = {
            "ED^null^null",
            "ED^UCHED RAT CHAIR^RAT-CHAIR",
            "ED^NON COVID MAJORS 05^05-NON COVID MAJORS",
            "ED^NON COVID MAJORS 04^04-NON COVID MAJORS",
            "ED^NON COVID MAJORS 05^05-NON COVID MAJORS",
            "ED^NON COVID MAJORS 04^04-NON COVID MAJORS",
            "ED^UCHED OTF POOL^OTF",
            "EAU^UCH T00 EAU BY02^BY02-08"
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
        Instant admissionInstant = Instant.parse("2013-02-11T10:00:52Z");

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
        for (String filename : adtFilenames) {
            logger.info("Processing location message: {}", filename);
            processSingleMessage(getLocationAdtMessage(filename));
        }
        checkAllVisits();
    }


}
