package uk.ac.ucl.rits.inform.datasinks.emapstar.labs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import uk.ac.ucl.rits.inform.OrderPermutationBase;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.MessageLocationCancelledException;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.lab.LabOrderMsg;

import java.util.List;

@Component
class LabsPermutationTestProducer extends OrderPermutationBase {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private String messagePath;
    private Runnable finalStateChecker;
    private String ormDefaults;

    /**
     * @param transactionManager Spring transaction manager
     */
    LabsPermutationTestProducer(@Autowired PlatformTransactionManager transactionManager) {
        super(transactionManager);
    }


    void setMessagePathAndORMDefaults(String messagePath) {
        this.messagePath = messagePath;
        this.ormDefaults = String.format("%s/orm_defaults.yaml", messagePath);
    }

    public void setFinalStateChecker(Runnable finalStateChecker) {
        this.finalStateChecker = finalStateChecker;
    }

    private List<LabOrderMsg> getLabOrders(String filename) {
        return getMessageFactory().getLabOrders(String.format("%s/%s.yaml", messagePath, filename), "message_prefix");
    }

    @Override
    public void runTest(List<String> fileNames) throws EmapOperationMessageProcessingException, MessageLocationCancelledException {
        // processing orders and results using different methods
        for (String filename : fileNames) {
            if (!filename.toLowerCase().contains("oru_r01")) {
                logger.info("Parsing order file: {}", filename);
                processSingleMessage(getMessageFactory().buildLabOrderOverridingDefaults(
                        ormDefaults, String.format("%s/%s.yaml", messagePath, filename)));
            } else {
                for (LabOrderMsg labOrderMsg : getLabOrders(filename)) {
                    logger.info("Parsing ORU R01 file: {}", filename);
                    processSingleMessage(labOrderMsg);
                }
            }
        }
        checkFinalState();
    }


    /**
     * finalStateChecker should be set with runnable that asserts the final state of the test.
     */
    @Override
    protected void checkFinalState() {
        finalStateChecker.run();
    }

}
