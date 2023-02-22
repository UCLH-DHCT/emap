package uk.ac.ucl.rits.inform.interchange.messaging;

/**
 * Define methods to be run on receipt of a publisher confirmation message back from rabbitmq.
 *
 * @author Stef Piatek
 */
public interface Releasable {
    /**
     * To be run on ack message receipt.
     * @param correlationId  correlationId + ":" + batchId (within the correlationData sent to rabbitmq).
     */
     void finishedSending(String correlationId);

    /**
     * To be run on nack message receipt.
     * @param correlationId  correlationId + ":" + batchId (within the correlationData sent to rabbitmq).
     */
     void failedSending(String correlationId);
}
