package uk.ac.ucl.rits.inform.datasources.ids;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessage;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
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
        assertEquals(ALLERGEN_TYPE, allergy.getAllergenType());
        assertEquals(ALLERGY_SEVERITY, allergy.getSeverity());
        assertEquals(ALLERGY_ADD, allergy.getAllergyAdded());
        assertEquals(ALLERGY_UPDATE, allergy.getUpdatedDateTime());
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
     * ADT_A60 with two IAM segments should produce two patient allergies.
     * @throws Exception shouldn't happen
     */
    @Test
    void testMultipleInfectionsParsed() throws Exception {
        List<PatientAllergy> allergies = getAllAllergies("2019_05_multiple_allergies");
        assertEquals(2, allergies.size());
    }

//    /**
//     * A05 with no ZIF segment for patient infections.
//     * Should not throw an error, but doesn't return a patient infection message.
//     * @throws Exception shouldn't happen
//     */
//    @Test
//    void testNoPatientInfectionsInA05() throws Exception {
//        List<PatientInfection> infections = getAllInfections("no_infections");
//        assertTrue(infections.isEmpty());
//    }
//
//    /**
//     * Given that a patient infection doesn't have an added date time
//     * When the message is processed
//     * Then the infection should not be added
//     * <p>
//     * The hoover should deal with messages with no added datetime
//     * @throws Exception shouldn't happen
//     */
//    @Test
//    void testNoInfectionAddedTime() throws Exception {
//        List<PatientInfection> infections = getAllInfections("mumps_no_add_time");
//        assertTrue(infections.isEmpty());
//    }
//
//    /**
//     * Given that patient infection added date time is earlier than service start
//     * When the message is processed
//     * Then the infection should not be added
//     */
//    @Test
//    void testNoInfectionsBeforeServiceStart() throws Exception {
//        List<PatientInfection> infections = getAllInfections("earlier_infection");
//        assertTrue(infections.isEmpty());
//    }
//
//    /**
//     * Ensure that only patient infections are not processed if they have an earlier added datetime than the current progress.
//     * @param setupFile    test setup processing, used to set the current progress
//     * @param testedFile   file where the actual output is tested
//     * @param expectedSize expected number of messages from the output file
//     * @throws Exception shouldn't happen
//     */
//    @ParameterizedTest
//    @CsvSource({
//            "2019_05_infection, 2019_05_infection, 1", // same date as existing progress is parsed
//            "2019_05_infection, 2019_06_infection, 1", // later date as existing progress is parsed
//            "2019_06_infection, 2019_05_infection, 0",  // earlier date as progress is parsed
//            "2019_06_infection, 2019_06_infection, 1",  // same date as existing progress is parsed
//    })
//    void earlierInfectionsSkipped(String setupFile, String testedFile, Long expectedSize) throws Exception {
//        getAllInfections(setupFile);
//
//        List<PatientInfection> infections = getAllInfections(testedFile);
//        assertEquals(expectedSize, infections.size());
//    }
}
