package uk.ac.ucl.rits.inform.datasources.waveform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import uk.ac.ucl.rits.inform.interchange.messaging.Publisher;
import uk.ac.ucl.rits.inform.interchange.visit_observations.WaveformMessage;

@Component
@Profile("hl7reader")
public class WaveformOperations {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Publisher publisher;

    public WaveformOperations(Publisher publisher) {
        this.publisher = publisher;
    }


    private void publishMessage(Publisher publisher, String messageId, WaveformMessage m) throws InterruptedException {
        //                    logger.debug("Message = {}", m.toString());
        publisher.submit(m, messageId, messageId, () -> {
            logger.debug("Successful ACK for message with ID {}", messageId);
        });
    }


    public void sendMessage(WaveformMessage msg) throws InterruptedException {
        publishMessage(publisher, msg.getSourceMessageId(), msg);
    }
}
