package uk.ac.ucl.rits.inform;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import uk.ac.ucl.rits.inform.datasinks.emapstar.InformDbOperations;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.IncompatibleDatabaseStateException;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.MessageCancelledException;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessage;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.InterchangeMessageFactory;

import java.util.Collection;

public abstract class OrderPermutationBase {
    private final TransactionTemplate transactionTemplate;
    private final InterchangeMessageFactory messageFactory = new InterchangeMessageFactory();
    @Autowired
    private InformDbOperations dbOps;
    @Autowired
    private CacheManager cacheManager;

    /**
     * @param transactionManager Spring transaction manager
     */
    protected OrderPermutationBase(PlatformTransactionManager transactionManager) {
        transactionTemplate = new TransactionTemplate(transactionManager);
    }

    protected InterchangeMessageFactory getMessageFactory() {
        return messageFactory;
    }

    @Transactional
    protected void processSingleMessage(EmapOperationMessage msg) throws EmapOperationMessageProcessingException {
        msg.processMessage(dbOps);
    }


    /**
     * Define the the processing of a single interchange file.
     * @param fileName fileName to process
     * @throws EmapOperationMessageProcessingException if error in processing message
     * @throws MessageCancelledException               if visit has been previouslty cancelled
     */
    protected abstract void processFile(String fileName) throws EmapOperationMessageProcessingException, MessageCancelledException, Exception;

    /**
     * Assertions at the end of each permutation test case to ensure that it has run successfully.
     */
    protected abstract void checkFinalState();

    /**
     * Build tests from permutation list
     * @param fileNames filenames to process
     * @throws Exception shouldn't happen
     */
    public void buildTestFromPermutation(Iterable<String> fileNames) throws Exception {
        Exception e = transactionTemplate.execute(status -> {
            status.setRollbackOnly();
            clearCache();
            try {
                runTest(fileNames);
            } catch (MessageCancelledException | IncompatibleDatabaseStateException allowed) {
                return null;
            } catch (Exception a) {
                return a;
            }
            return null;
        });
        if (e != null) {
            throw e;
        }
    }

    /**
     * Manually clear all cache.
     */
    private void clearCache() {
        Collection<String> caches =  cacheManager.getCacheNames();
        for (String cache: caches) {
            cacheManager.getCache(cache).clear();
        }
    }

    private void runTest(Iterable<String> fileNames) throws Exception {
        // processing orders and results using different methods
        for (String filename : fileNames) {
            processFile(filename);
        }
        checkFinalState();
    }

}
