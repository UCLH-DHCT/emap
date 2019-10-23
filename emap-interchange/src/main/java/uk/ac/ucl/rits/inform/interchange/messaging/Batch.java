package uk.ac.ucl.rits.inform.interchange.messaging;

import uk.ac.ucl.rits.inform.interchange.EmapOperationMessage;

import java.util.List;

/**
 * Bundles a batch of messages, batchId and callback together.
 *
 * @author Stef Piatek
 */
public class Batch {
    /**
     * @param batchId Unique ID for an entire batch. In most cases this can be the callbackId of the last item in the batch.
     */
    public final String batchId;

    /**
     * @param batch List of paired messages and their callbackIds
     */
    public final List<Pair<EmapOperationMessage, String>> batch;

    /**
     * @param callback Runnable to update processing state after all messages in the queue being successfully published.
     */
    public final Runnable callback;

    public Batch(String batchId, List<Pair<EmapOperationMessage, String>> batch, Runnable callback) {
        if (callback == null) {
            throw new NullPointerException("Runnable is null");
        }
        if (batch == null) {
            throw new NullPointerException("Batch is null");
        }
        if (batch.isEmpty()) {
            throw new IllegalArgumentException("Batch is empty ");
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
