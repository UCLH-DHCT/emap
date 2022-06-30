package uk.ac.ucl.rits.inform.datasources.ids;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.PatientProblem;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Test patient problem list parsing from HL7 messages (those with type PPR_PC1, PPR_PC2 and PPR_PC3.
 * Note that the hapi library at the moment only supports PPR_PC1 messages but as they are structurally not different
 * to the other two types, for the time being all problem list messages are cast to PPR_PC1.
 * @author Anika Cawthorn
 */
@ActiveProfiles("test")
@SpringBootTest
public class TestPatientProblems extends TestHl7MessageStream {
    private static final String FILE_TEMPLATE = "ProblemList/%s.txt";
    private static final String MRN = "8DcEwvqa8Q3";
    private static final String VISIT_NUMBER = "123412341234";
    private static final String PROBLEM_CODE = "K64.9";
    private static final LocalDate PROBLEM_ADDED = LocalDate.parse("2019-06-01");
    private static final String PROBLEM_NAME = "Haemorrhoids, unspecified";
    private static final LocalDate PROBLEM_ONSET = LocalDate.parse("2019-05-31");
    private static final Instant PROBLEM_UPDATE = Instant.parse("2020-03-02T21:01:22Z");
    private static final LocalDate PROBLEM_RESOLVED = LocalDate.parse("2020-03-02");
    private static final String EPIC = "EPIC";
    private static final String COMMENT = " - Screened and started on meds and some other meds - CRP 0.6-->4.9-->4.1 " +
            "- Blood cultures negative";
    private static final InterchangeValue<Long> EPIC_ID = InterchangeValue.buildFromHl7(1333555L);
    @Autowired
    PatientProblemService patientProblemService;

    List<PatientProblem> getAllProblems(String fileName) throws Exception {
        var msgs = processSingleMessage(String.format(FILE_TEMPLATE, fileName));
        assertNotNull(msgs);

        // filter out any implied ADT messages
        return msgs.stream()
                .filter(msg -> (msg instanceof PatientProblem))
                .map(o -> (PatientProblem) o)
                .collect(Collectors.toList());
    }

    /**
     * Given that no problem list has been assigned to a patient before
     * When a new HL7 problem list message arrives
     * Then it is added to the patient and the details of the problem are propagated correctly.
     * @throws Exception shouldn't happen
     */
    @Test
    void testSingleProblemListParsed() throws Exception {
        List<PatientProblem> problems = getAllProblems("minimal_problem_list_inpatient");
        assertEquals(1, problems.size());
        PatientProblem problem = problems.get(0);
        assertEquals(MRN, problem.getMrn());
        assertEquals(EPIC, problem.getSourceSystem());
        assertEquals(PROBLEM_CODE, problem.getConditionCode());
        assertEquals(PROBLEM_NAME, problem.getConditionName().get());
        assertEquals(PROBLEM_ADDED, problem.getAddedDate());
        assertEquals(PROBLEM_UPDATE, problem.getUpdatedDateTime());
        assertEquals(EPIC_ID, problem.getEpicConditionId());
    }

    /**
     * Given that a problem list HL7 message has a resolved status
     * When the message is processed
     * Then the resolved time is added to the problem interchange message.
     * @throws Exception shouldn't happen
     */
    @Test
    void testProblemResolvedTime() throws Exception {
        List<PatientProblem> problems = getAllProblems("problem_list_resolved");
        assertEquals(1, problems.size());
        PatientProblem problem = problems.get(0);
        assertEquals(InterchangeValue.buildFromHl7(PROBLEM_RESOLVED), problem.getResolvedDate());
    }

    /**
     * Given that a problem list HL7 message has multiple PRB segments
     * When the message is processed
     * Then all PRB segments are parsed into individual problem interchange messages.
     * @throws Exception shouldn't happen
     */
    @Test
    void testMultipleProblemsParsed() throws Exception {
        List<PatientProblem> problems = getAllProblems("multiple_problem_lists");
        assertEquals(3, problems.size());
    }

    /**
     * Given that no problem lists have been parsed before
     * When a problem list HL7 message without PRB segments arrives
     * Then no problem list interchange message is generated
     * @throws Exception shouldn't happen
     */
    @Test
    void testNoPatientProblemInPC1() throws Exception {
        List<PatientProblem> problems = getAllProblems("no_problem_lists");
        assertTrue(problems.isEmpty());
    }

    /**
     * Given a problem with an onset time and an associated PV1 visit section in the message
     * When it is processed
     * Then the message should contain the set onset date and the visit number
     * @throws Exception should not happen
     */
    @Test
    void testProblemOnsetDateWithVisit() throws Exception{

        var problem = getAllProblems("problem_list_onset").get(0);
        assertEquals(VISIT_NUMBER, problem.getVisitNumber().get());
        assertEquals(PROBLEM_ONSET, problem.getOnsetTime().get());
    }

    /**
     * Given a problem with a couple of note segments in the end
     * When it is processed
     * Then the message should contain the set the concatenated note segments as comment
     * @throws Exception should not happen
     */
    @Test
    void testProblemNoteSegment() throws Exception{

        var problem = getAllProblems("problem_list_notes").get(0);
        assertEquals(InterchangeValue.buildFromHl7(COMMENT), problem.getComment());
    }
}
