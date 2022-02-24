package uk.ac.ucl.rits.inform.datasinks.emapstar.labs;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import uk.ac.ucl.rits.inform.datasinks.emapstar.MessageProcessingBase;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.IncompatibleDatabaseStateException;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.ClimbSequenceAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.ClimbSequenceRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabSampleRepository;
import uk.ac.ucl.rits.inform.informdb.labs.ClimbSequence;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.lab.ClimbSequenceMsg;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestClimbSequenceProcessing extends MessageProcessingBase {
    private ClimbSequenceMsg focalSequence;
    private ClimbSequenceMsg communitySequence;


    @Autowired
    LabSampleRepository labSampleRepository;
    @Autowired
    ClimbSequenceRepository climbSequenceRepository;
    @Autowired
    ClimbSequenceAuditRepository climbSequenceAuditRepository;

    private static String LAB_SAMPLE = "22U113534";

    private ClimbSequence getSingleClimbSequence() {
        List<ClimbSequence> climbSequences = getAllEntities(climbSequenceRepository);
        assertEquals(1, climbSequences.size());
        return climbSequences.get(0);
    }


    TestClimbSequenceProcessing() throws IOException {
        focalSequence = messageFactory.getClimbSequenceMsg("focal.yaml");
        communitySequence = messageFactory.getClimbSequenceMsg("community.yaml");
    }

    /**
     * Given that no lab samples exist
     * When a focal ClimbSequenceMessage is processed
     * Then an exception should be thrown as the lab sample should always exist beforehand
     */
    @Test
    void testNonExistentLabSampleThrows() {
        assertThrows(IncompatibleDatabaseStateException.class, () -> processSingleMessage(focalSequence));
    }

    /**
     * Given that no lab samples exist
     * When a community ClimbSequenceMsg (has no lab sample) is processed
     * Then A ClimbSequence is created without linking it to a lab sample
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    void testCommunitySequenceCreated() throws EmapOperationMessageProcessingException {
        processSingleMessage(communitySequence);

        assertNull(getSingleClimbSequence().getLabSampleId());
    }

    /**
     * Given that a lab sample exists with no ClimbSequences
     * When a focal ClimbSequenceMsg is processed that uses the existing lab Sample
     * Then A ClimbSequence is created for the lab sample
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    @Sql("/populate_db.sql")
    void testClimbFocalSequenceCreated() throws EmapOperationMessageProcessingException {
        processSingleMessage(focalSequence);

        assertEquals(LAB_SAMPLE, getSingleClimbSequence().getLabSampleId().getExternalLabNumber());
    }

    /**
     * Given that a focal climb sequence has been processed
     * When the same cogId has newer and different data
     * Then the persisted climb sequence should be updated
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    @Sql("/populate_db.sql")
    void testNewClimbSequenceUpdates() throws EmapOperationMessageProcessingException {
        Instant originalValidFrom = focalSequence.getSequenceValidFrom();
        String newSequence = "AAAAAACCGGTTAAA";

        processSingleMessage(focalSequence);

        focalSequence.setSequenceValidFrom(originalValidFrom.plusSeconds(1));
        focalSequence.setSequence(newSequence);
        processSingleMessage(focalSequence);

        ClimbSequence outputSequence = getSingleClimbSequence();
        assertTrue(outputSequence.getValidFrom().isAfter(originalValidFrom));
        assertEquals(newSequence, outputSequence.getSequence());
    }

    /**
     * Given that a focal climb sequence has been processed
     * When the same cogId has older and different data
     * Then the persisted climb sequence shouldn't be updated
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    @Sql("/populate_db.sql")
    void testOldClimbSequenceDoesntUpdates() throws EmapOperationMessageProcessingException {
        Instant originalValidFrom = focalSequence.getSequenceValidFrom();
        String newSequence = "AAAAAACCGGTTAAA";

        processSingleMessage(focalSequence);

        focalSequence.setSequenceValidFrom(originalValidFrom.minusSeconds(1));
        focalSequence.setSequence(newSequence);
        processSingleMessage(focalSequence);

        ClimbSequence outputSequence = getSingleClimbSequence();
        assertEquals(originalValidFrom, outputSequence.getValidFrom());
        assertNotEquals(newSequence, outputSequence.getSequence());
    }


}
