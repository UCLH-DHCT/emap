package uk.ac.ucl.rits.inform.datasinks.emapstar;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.decisions.AdvancedDecisionRepository;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;

/**
 * Test cases to assert correct processing of Advanced Decision Messages.
 * @author Anika Cawthorn
 */
public class TestAdvancedDecisionProcessing extends MessageProcessingBase {
    @Autowired
    AdvancedDecisionRepository advancedDecisionRepo;

    @Test
    void testMinimalAdvancedDecisionCreated() throws EmapOperationMessageProcessingException {
        System.out.println("tst");
    }
}
