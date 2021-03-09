package uk.ac.ucl.rits.inform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import uk.ac.ucl.rits.inform.datasinks.emapstar.InformDbOperations;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.MessageCancelledException;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessage;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.InterchangeMessageFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class OrderPermutationBase {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private TransactionTemplate transactionTemplate;
    private final InterchangeMessageFactory messageFactory = new InterchangeMessageFactory();
    @Autowired
    protected InformDbOperations dbOps;

    /**
     * @param transactionManager Spring transaction manager
     */
    protected OrderPermutationBase(PlatformTransactionManager transactionManager) {
        transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public InterchangeMessageFactory getMessageFactory() {
        return messageFactory;
    }

    @Transactional
    protected void processSingleMessage(EmapOperationMessage msg) throws EmapOperationMessageProcessingException {
        msg.processMessage(dbOps);
    }


    /**
     * @param fileNames filenames to process
     * @throws EmapOperationMessageProcessingException if error in processing message
     * @throws MessageCancelledException if location visit has been cancelled and then a move message tries to add it
     */
    protected abstract void runTest(List<String> fileNames) throws EmapOperationMessageProcessingException, MessageCancelledException;

    /**
     * Assertions at the end of each permutation test case to ensure that it has run successfully.
     */
    protected abstract void checkFinalState();

    /**
     * Build tests from permutation list
     * @param fileNames filenames to process
     * @throws Exception shouldn't happen
     */
    public void buildTestFromPermutation(List<String> fileNames) throws Exception {
        Exception e = transactionTemplate.execute(status -> {
            status.setRollbackOnly();
            try {
                runTest(fileNames);
            } catch (MessageCancelledException allowed) {
                return null;
            } catch (EmapOperationMessageProcessingException a) {
                return a;
            }
            return null;
        });
        if (e != null) {
            throw e;
        }
    }

}
