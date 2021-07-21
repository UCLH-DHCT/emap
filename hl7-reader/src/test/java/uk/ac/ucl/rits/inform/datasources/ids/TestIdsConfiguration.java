package uk.ac.ucl.rits.inform.datasources.ids;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;


@ActiveProfiles("test")
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class TestIdsConfiguration {
    @Autowired
    private IdsConfiguration idsConfiguration;
    @Autowired
    private IdsProgressRepository idsProgressRepository;

    private static final Instant SERVICE_START = Instant.parse("2019-04-01T00:00:00Z");
    private static final Instant BEFORE_SERVICE_START = SERVICE_START.minusSeconds(10);
    private static final Instant AFTER_SERVICE_START = SERVICE_START.plusSeconds(10);

    private void saveProgressWithMessageDatetime(Instant messageDatetime) {
        IdsProgress idsProgress = idsProgressRepository.findById(0)
                .orElseGet(IdsProgress::new);
        idsProgress.setLastProcessedMessageDatetime(messageDatetime);
        idsProgressRepository.save(idsProgress);
    }

    @BeforeEach
    void setUp() {
        idsConfiguration.setServiceStartDatetime(SERVICE_START);
    }

    /**
     * Given that no previous progress
     * When configured to start from last Id
     * Then start date should be the service start date
     */
    @Test
    void testUsesServiceStartWhenNoProgress() {
        idsProgressRepository.deleteAll();

        idsConfiguration.setStartFromLastId(true);

        assertEquals(SERVICE_START, idsConfiguration.getStartDateTime());
    }

    /**
     * Given that previous progress is before the service start
     * When configured to start from last Id
     * Then start date should be the service start date
     */
    @Test
    void testUsesServiceStartWhenProgressIsBeforeServiceStart() {
        saveProgressWithMessageDatetime(BEFORE_SERVICE_START);

        idsConfiguration.setStartFromLastId(true);

        assertEquals(SERVICE_START, idsConfiguration.getStartDateTime());
    }


    /**
     * Given that previous progress is after the service start
     * When configured to start from last Id
     * Then start date should be the service start date
     */
    @Test
    void testUsesPreviousProgressWhenAfterServiceStart() {
        saveProgressWithMessageDatetime(AFTER_SERVICE_START);

        idsConfiguration.setStartFromLastId(true);

        assertEquals(AFTER_SERVICE_START, idsConfiguration.getStartDateTime());
    }

    /**
     * Given that previous progress is after the service start
     * When configured NOT to start from last Id
     * Then start date should be the service start date
     */
    @Test
    void testUsesServiceStartWhenNotSetToStartFromLastId() {
        saveProgressWithMessageDatetime(AFTER_SERVICE_START);

        idsConfiguration.setStartFromLastId(false);

        assertEquals(SERVICE_START, idsConfiguration.getStartDateTime());
    }
}
