package uk.ac.ucl.rits.inform.datasources.ids;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.ac.ucl.rits.inform.interchange.ConditionAction;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessage;
import uk.ac.ucl.rits.inform.interchange.PatientAllergy;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test EPIC Patient allergy parsing.
 * @author Anika Cawthorn
 */
@ActiveProfiles("test")
@SpringBootTest
class TestPatientAllergies extends TestHl7MessageStream {
    private static final String FILE_TEMPLATE = "PatientAllergies/%s.txt";
    private static final String MRN = "8DcEwvqa8Q3";
    private static final String ALLERGEN_TYPE = "Environ";
    private static final String ALLERGY_SEVERITY = "High";
    private static final Instant ALLERGY_ADD = Instant.parse("2019-05-15T08:40:05Z");
    private static final Instant ALLERGY_UPDATE = Instant.parse("2019-05-15T08:40:05Z");
    private static final ConditionAction ALLERGY_ACTION = ConditionAction.ADD;
    private static final String EPIC = "EPIC";
    @Autowired
    PatientAllergyService patientAllergyService;

    @BeforeEach
    private void resetInfectionProgress(@Value("${ids.cfg.default-start-datetime}") Instant serviceStart) {
        patientAllergyService.setAllergiesProgress(serviceStart);
    }

    List<PatientAllergy> getAllAllergies(String fileName) throws Exception {
        List<? extends EmapOperationMessage> msgs = null;
        try {
            msgs = processSingleMessage(String.format(FILE_TEMPLATE, fileName));
        } catch (Exception e) {
            throw e;
        }

        assert msgs != null;
        // filter out any implied ADT messages
        return msgs.stream()
                .filter(msg -> (msg instanceof PatientAllergy))
                .map(o -> (PatientAllergy) o)
                .collect(Collectors.toList());
    }

    /**
     * Given that ADT_A60 message processing has been activated
     * When a single HL7 message with one IAM segment arrives
     * Then a Collection of the length one should be generated containing one PatientAllergy Interchange message
     * @throws Exception shouldn't happen
     */
    @Test
    void testSingleAllergyParsed() throws Exception {
        List<PatientAllergy> allergies = getAllAllergies("2019_05_allergy");
        assertEquals(1, allergies.size());
        PatientAllergy allergy = allergies.get(0);
        assertEquals(MRN, allergy.getMrn());
        assertEquals(EPIC, allergy.getSourceSystem());
        assertEquals(ALLERGEN_TYPE, allergy.getSubType().get());
        assertEquals(ALLERGY_SEVERITY, allergy.getSeverity().get());
        assertEquals(ALLERGY_ADD, allergy.getAddedDatetime());
        assertEquals(ALLERGY_UPDATE, allergy.getUpdatedDateTime());
        assertEquals(ALLERGY_ACTION, allergy.getAction());
    }

    /**
     * Given that ADT_A60 message processing has been activated
     * When a single HL7 message with one IAM segment but without added time arrives
     * Then an empty collection should be generated
     * @throws Exception shouldn't happen
     */
    @Test
    void testAllergyNoAddTime() throws Exception {
        List<PatientAllergy> allergies = getAllAllergies("2019_05_allergy_noaddtime");
        assertEquals(0, allergies.size());
    }

    /**
     * Given that ADT_A60 message processing has been activated
     * When an ADT_A60 HL7 message with two IAM segments should produce two patient allergies.
     * Then a Collection of two PatientAllergy Interchange messages should be created
     * @throws Exception shouldn't happen
     */
    @Test
    void testMultipleAllergiesParsed() throws Exception {
        List<PatientAllergy> allergies = getAllAllergies("2019_05_multiple_allergies");
        assertEquals(2, allergies.size());
    }

    /**
     * Given that ADT_A60 message processing has been activated
     * When an ADT_A60 message without an IAM segment arrives
     * Then no error should be raised, but no allergy interchange messages should be created either
     * @throws Exception shouldn't happen
     */
    @Test
    void testNoAllergySegmentInA60() throws Exception {
        List<PatientAllergy> allergies = getAllAllergies("2019_05_allergy_noIAM");
        assertTrue(allergies.isEmpty());
    }

    /**
     * Given that ADT_A60 message processing has been activated
     * When an ADT_A60 message with a reported time earlier than the service start date arrives
     * Then the allergy interchange message should not be added
     */
    @Test
    void testNoAllergiesBeforeServiceStart() throws Exception {
        List<PatientAllergy> allergies = getAllAllergies("earlier_allergy");
        assertTrue(allergies.isEmpty());
    }

    /**
     * Given that allergy message processing has been activated
     * When multiple messages are processed some falling before and some after service start datetime
     * Then only those messages are transferred into Interchange messages that have a reported time after the service
     *    start date
     * @param setupFile    test setup processing, used to set the current progress
     * @param testedFile   file where the actual output is tested
     * @param expectedSize expected number of messages from the output file
     * @throws Exception shouldn't happen
     */
    @ParameterizedTest
    @CsvSource({
            "2019_05_allergy, 2019_05_allergy, 1", // same date as existing progress is parsed
            "2019_05_allergy, 2019_06_allergy, 1", // later date than existing progress is parsed
            "2019_06_allergy, 2019_05_allergy, 0",  // earlier date than progress is parsed
            "2019_06_allergy, 2019_06_allergy, 1",  // same date as existing progress is parsed
    })
    void earlierInfectionsSkipped(String setupFile, String testedFile, Long expectedSize) throws Exception {
        getAllAllergies(setupFile);

        List<PatientAllergy> allergies = getAllAllergies(testedFile);
        assertEquals(expectedSize, allergies.size());
    }
}
