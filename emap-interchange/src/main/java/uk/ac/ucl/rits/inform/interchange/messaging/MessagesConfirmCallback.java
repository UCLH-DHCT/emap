package uk.ac.ucl.rits.inform.interchange.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * Receives callback acknowledgement and runs correct releasable method based on the ack.
 *
 * @author Stef Piatek
 */
public class MessagesConfirmCallback implements RabbitTemplate.ConfirmCallback {
    private Releasable releasable;
    private Logger logger = LoggerFactory.getLogger(MessagesConfirmCallback.class);

    /**
     * @param releasable Implementation of releasable
     */
    MessagesConfirmCallback(Releasable releasable) {
        this.releasable = releasable;
    }


    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String cause) {
        if (ack) {
            releasable.finishedSending(correlationData.getId());
        } else {
            logger.info(String.format("Message nack received cause: %s, %s", correlationData, cause));
            releasable.failedSending(correlationData.getId());
        }
    }
}
