package uk.ac.ucl.rits.inform.datasinks.emapstar.labs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import uk.ac.ucl.rits.inform.OrderPermutationBase;
import uk.ac.ucl.rits.inform.interchange.lab.LabOrderMsg;

import java.io.IOException;
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

    private List<LabOrderMsg> getLabOrders(String filename) throws IOException {
        return getMessageFactory().getLabOrders(String.format("%s/%s.yaml", messagePath, filename), "message_prefix");
    }


    @Override
    protected void processFile(String fileName) throws Exception {
        if (!fileName.toLowerCase().contains("oru_r01")) {
            logger.info("Parsing order file: {}", fileName);
            processSingleMessage(getMessageFactory().buildLabOrderOverridingDefaults(
                    ormDefaults, String.format("%s/%s.yaml", messagePath, fileName)));
        } else {
            for (LabOrderMsg labOrderMsg : getLabOrders(fileName)) {
                logger.info("Parsing ORU R01 file: {}", fileName);
                processSingleMessage(labOrderMsg);
            }
        }
    }


    /**
     * finalStateChecker should be set with runnable that asserts the final state of the test.
     */
    @Override
    protected void checkFinalState() {
        finalStateChecker.run();
    }

}
