package uk.ac.ucl.rits.inform.datasinks.emapstar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.MrnRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.MrnToLiveRepository;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.ResearchOptOut;
import uk.ac.ucl.rits.inform.interchange.adt.MergePatient;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Test cases to ensure that processing NHS research opt out is done correctly.
 * <p>
 * This also includes cascading research opt out during merges, which I've put here, even if this is the outcome of an ADT message for merge MRNs.
 */
class TestResearchOptOutProcessing extends MessageProcessingBase {
    @Autowired
    private MrnRepository mrnRepository;
    @Autowired
    private MrnToLiveRepository mrnToLiveRepository;

    private List<ResearchOptOut> optOutMessages;
    private MergePatient mergeMessage;

    private static final String EXISTING_MRN = "60600000";
    private static final String RETIRING_MERGE_MRN = "40800000";
    private static final String SURVIVING_MERGE_MRN = "40800001";

    @BeforeEach
    private void setUp() throws IOException {
        optOutMessages = messageFactory.getResearchOptOuts("all_opt_out.yaml");
        mergeMessage = messageFactory.getAdtMessage("generic/A40.yaml");
    }

    /**
     * Given that no patients exist in the database,
     * when 5 research opt out messages are processed,
     * then each patient is created with an opt-out.
     */
    @Test
    void testOptOutCreatesNewPatients() throws EmapOperationMessageProcessingException {
        processMessages(optOutMessages);

        var mrns = getAllMrns();
        assertEquals(5, mrns.size());
        mrns.forEach(mrn -> assertTrue(mrn.isResearchOptOut()));
    }

    /**
     * Given that a merged patient exists in star,
     * When a research opt out message is processed for the merged patient,
     * Then the Mrn and the live Mrn should both be marked as opted out.
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    @Sql("/populate_db.sql")
    void testOptOutUpdatesExistingLive() throws EmapOperationMessageProcessingException {
        var optOutForMergedMrn = optOutMessages.stream().filter(mrn -> EXISTING_MRN.equals(mrn.getMrn())).findFirst().orElseThrow();
        processSingleMessage(optOutForMergedMrn);

        var mrn = mrnRepository.findByMrnEquals(EXISTING_MRN).orElseThrow();
        assertTrue(mrn.isResearchOptOut());
        var liveMrn = mrnToLiveRepository.getByMrnIdEquals(mrn).getLiveMrnId();
        assertTrue(liveMrn.isResearchOptOut());
    }

    /**
     * Given that an existing patient exists in star and has opted out of research,
     * When a merge message is processed for the patient,
     * Then both the retired and the surviving patient should be marked as opted out.
     * <p>
     * The output should be the same if the surviving or retiriing patient has opted out before the merge.
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @ParameterizedTest
    @ValueSource(strings = {SURVIVING_MERGE_MRN, RETIRING_MERGE_MRN})
    void testMergePatientCarriesOptOutToLive(String optOutIdentifier) throws EmapOperationMessageProcessingException {
        var optOutMsg = optOutMessages.get(0);
        optOutMsg.setMrn(optOutIdentifier);
        processSingleMessage(optOutMsg);

        processSingleMessage(mergeMessage);

        var retiredMrn = mrnRepository.findByMrnEquals(RETIRING_MERGE_MRN).orElseThrow();
        assertTrue(retiredMrn.isResearchOptOut());
        var survivingMrn = mrnRepository.findByMrnEquals(SURVIVING_MERGE_MRN).orElseThrow();
        assertTrue(survivingMrn.isResearchOptOut());
    }

}