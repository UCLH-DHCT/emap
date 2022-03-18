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
import uk.ac.ucl.rits.inform.interchange.PatientProblem;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test patient problem list parsing from HL7 messages (those with type PPR_PC1, PPR_PC2 and PPR_PC3.
 * Note that the hapi library at the moment only supports PPR_PC1 messages but as they are structurally not different
 * to the other two types, for the time being all problem list messages are cast to PPR_PC1.
 * @author Anika Cawthorn
 */
@ActiveProfiles("test")
@SpringBootTest
public class TestProblemLists extends TestHl7MessageStream {
    private static final String FILE_TEMPLATE = "ProblemList/%s.txt";
    private static final String MRN = "8DcEwvqa8Q3";
    private static final String PROBLEM_CODE = "K64.9";
    private static final Instant PROBLEM_ADDED = Instant.parse("2020-03-02T00:00:00Z");
    private static final Instant PROBLEM_UPDATE = Instant.parse("2020-03-02T21:01:22Z");
    private static final String EPIC = "EPIC";
    @Autowired
    PatientProblemService patientProblemService;

    @BeforeEach
    private void resetProblemProgress(@Value("${ids.cfg.default-start-datetime}") Instant serviceStart) {
        patientProblemService.setProblemListProgress(serviceStart);
    }

    List<PatientProblem> getAllProblems(String fileName) throws Exception {
        List<? extends EmapOperationMessage> msgs = null;
        try {
            msgs = processSingleMessage(String.format(FILE_TEMPLATE, fileName));
        } catch (Exception e) {
            throw e;
        }

        assert msgs != null;
        // filter out any implied ADT messages
        return msgs.stream()
                .filter(msg -> (msg instanceof PatientProblem))
                .map(o -> (PatientProblem) o)
                .collect(Collectors.toList());
    }

    /**
     * Minimal information from HL7 should be parsed.
     * @throws Exception shouldn't happen
     */
    @Test
    void testSingleProblemListParsed() throws Exception {
        List<PatientProblem> problems = getAllProblems("minimal_problem_list_inpatient");
        assertEquals(1, problems.size());
        PatientProblem problem = problems.get(0);
        assertEquals(MRN, problem.getMrn());
        assertEquals(EPIC, problem.getSourceSystem());
        assertEquals(PROBLEM_CODE, problem.getProblemCode());
        assertEquals(PROBLEM_ADDED, problem.getProblemAdded());
        assertEquals(PROBLEM_UPDATE, problem.getUpdatedDateTime());
    }

    /**
     * Patient problem with resolved date is parsed correctly.
     * @throws Exception shouldn't happen
     */
    @Test
    void testProblemResolvedTime() throws Exception {
        List<PatientProblem> problems = getAllProblems("problem_list_resolved");
        assertEquals(1, problems.size());
        PatientProblem problem = problems.get(0);
        assertEquals(InterchangeValue.buildFromHl7(PROBLEM_UPDATE), problem.getProblemResolved());
    }

//    /**
//     * A05 with two infections in ZIF segment should produce two patient infections.
//     * @throws Exception shouldn't happen
//     */
//    @Test
//    void testMultipleInfectionsParsed() throws Exception {
//        List<PatientInfection> infections = getAllInfections("multiple_infections");
//        assertEquals(2, infections.size());
//    }
//
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
