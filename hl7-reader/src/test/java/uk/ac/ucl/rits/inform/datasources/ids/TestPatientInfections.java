package uk.ac.ucl.rits.inform.datasources.ids;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessage;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.PatientInfection;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test EPIC Patient infection parsing
 * @author Stef Piatek
 */
@ActiveProfiles("test")
@SpringBootTest
class TestPatientInfections extends TestHl7MessageStream {
    private static final String FILE_TEMPLATE = "PatientInfection/%s.txt";
    private static final String MRN = "8DcEwvqa8Q3";
    private static final String MUMPS_INFECTION = "Mumps";
    private static final Instant MUMPS_ADD = Instant.parse("2019-06-02T10:31:05Z");
    private static final Instant MUMPS_UPDATE = Instant.parse("2019-06-07T11:32:00Z");
    private static final String EPIC = "EPIC";



    List<PatientInfection> getAllInfections(String fileName) throws Exception {
        List<? extends EmapOperationMessage> msgs = null;
        try {
            msgs = processSingleMessage(String.format(FILE_TEMPLATE, fileName));
        } catch (Exception e) {
            throw e;
        }

        assert msgs != null;
        // filter out any implied ADT messages
        return msgs.stream()
                .filter(msg -> (msg instanceof PatientInfection))
                .map(o -> (PatientInfection) o)
                .collect(Collectors.toList());
    }

    /**
     * Minimal information from HL7 should be parsed.
     * @throws Exception shouldn't happen
     */
    @Test
    void testSingleInfectionParsed() throws Exception {
        List<PatientInfection> infections = getAllInfections("a05");
        assertEquals(1, infections.size());
        PatientInfection mumps = infections.get(0);
        assertEquals(MRN, mumps.getMrn());
        assertEquals(EPIC, mumps.getSourceSystem());
        assertEquals(MUMPS_INFECTION, mumps.getInfectionCode());
        assertEquals(MUMPS_ADD, mumps.getInfectionAdded());
        assertEquals(MUMPS_UPDATE, mumps.getUpdatedDateTime());
    }

    /**
     * Patient infection with resolved date time should be parsed.
     * @throws Exception shouldn't happen
     */
    @Test
    void testInfectionResolvedTime() throws Exception { List<PatientInfection> infections = getAllInfections("mumps_resolved");
        assertEquals(1, infections.size());
        PatientInfection mumps = infections.get(0);
        assertEquals(InterchangeValue.buildFromHl7(MUMPS_UPDATE), mumps.getInfectionResolved());
    }

    /**
     * A05 with two infections in ZIF segment should produce two patient infections.
     * @throws Exception shouldn't happen
     */
    @Test
    void testMultipleInfectionsParsed() throws Exception {
        List<PatientInfection> infections = getAllInfections("multiple_infections");
        assertEquals(2, infections.size());
    }

    /**
     * A05 with no ZIF segment for patient infections.
     * Should not throw an error, but doesn't return a patient infection message.
     * @throws Exception shouldn't happen
     */
    @Test
    void testNoPatientInfectionsInA05() throws Exception { List<PatientInfection> infections = getAllInfections("no_infections");
        assertTrue(infections.isEmpty());
    }

    @Test
    void testNoInfectionAddedTime() throws Exception {
        List<PatientInfection> infections = getAllInfections("mumps_no_add_time");
        assertEquals(1, infections.size());
        assertNull(infections.get(0).getInfectionAdded());
    }
}
