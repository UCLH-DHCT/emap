package uk.ac.ucl.rits.inform.interchange.messaging;

import org.apache.commons.lang3.tuple.ImmutablePair;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessage;

import java.util.List;

/**
 * Bundles a batch of messages, batchId and callback together.
 * @param <T> Any child of EmapOperationMessage so that you can pass in child class directly.
 * @author Stef Piatek
 */
public class MessageBatch<T extends EmapOperationMessage> {
    /**
     * @param batchId Unique ID for an entire batch. In most cases this can be the correlationIDs of the last item in the batch.
     */
    public final String batchId;

    /**
     * @param batch List of paired messages and their correlationIDs
     */
    public final List<ImmutablePair<T, String>> batch;

    /**
     * @param callback Runnable to update processing state after all messages in the batch being successfully published.
     */
    public final Runnable callback;

    /**
     * @param batchId  Unique Id for the batch, in most cases this can be the correlationId.
     *                 Must not contain a colon character.
     * @param batch    Batch of messages to be sent (pairs of Emap messages and their unique correlationIds)
     *                 CorrelationIds should be unique within the batch and not contain a colon character.
     * @param callback To be run on receipt of a successful acknowledgement of publishing from rabbitmq.
     *                 Most likely to update the state of progress.
     * @throws NullPointerException     callback or batch is null
     * @throws IllegalArgumentException empty batch or batchId contains a colon character
     */
    public MessageBatch(String batchId, List<ImmutablePair<T, String>> batch, Runnable callback) {
        if (callback == null) {
            throw new NullPointerException("Runnable is null");
        }
        if (batch == null) {
            throw new NullPointerException("Batch is null");
        }
        if (batch.isEmpty()) {
            throw new IllegalArgumentException("Batch is empty");
        }
        if (batchId.contains(":")) {
            throw new IllegalArgumentException("batchId contains a colon character");
        }
        this.batchId = batchId;
        this.batch = batch;
        this.callback = callback;
    }

    @Override
    public String toString() {
        return String.format("Batch{batchId=%s, batchSize=%d}", batchId, batch.size());
    }
}
