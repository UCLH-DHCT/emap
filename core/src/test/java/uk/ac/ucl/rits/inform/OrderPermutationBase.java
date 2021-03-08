package uk.ac.ucl.rits.inform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import uk.ac.ucl.rits.inform.datasinks.emapstar.InformDbOperations;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.MessageLocationCancelledException;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LocationVisitAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LocationVisitRepository;
import uk.ac.ucl.rits.inform.informdb.movement.LocationVisit;
import uk.ac.ucl.rits.inform.informdb.movement.LocationVisitAudit;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessage;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.InterchangeMessageFactory;
import uk.ac.ucl.rits.inform.interchange.adt.AdtMessage;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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
     * @throws MessageLocationCancelledException if location visit has been cancelled and then a move message tries to add it
     */
    protected abstract void runTest(List<String> fileNames) throws EmapOperationMessageProcessingException, MessageLocationCancelledException;

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
            } catch (MessageLocationCancelledException allowed) {
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
