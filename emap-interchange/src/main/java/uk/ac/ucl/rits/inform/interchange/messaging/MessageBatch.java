package uk.ac.ucl.rits.inform.interchange.messaging;

import lombok.Getter;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessage;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Bundles a batch of messages, batchId and callback together.
 * @param <T> Any child of EmapOperationMessage so that you can pass in child class directly.
 * @author Stef Piatek
 */
@Getter
public class MessageBatch<T extends EmapOperationMessage> {
    /**
     * Unique ID for an entire batch. In most cases this can be the correlationIDs of the last item in the batch.
     */
    private final String batchId;

    /**
     * List of paired messages and their correlationIDs.
     */
    private final List<ImmutablePair<T, String>> batch;

    /**
     * Runnable to update processing state after all messages in the batch being successfully published.
     */
    private final Runnable callback;

    /**
     * @param batchId  Unique Id for the batch, in most cases this can be the correlationId.
     *                 Must not contain a colon character.
     * @param batch    Batch of messages to be sent (pairs of Emap messages and their unique correlationIds)
     *                 CorrelationIds should be unique within the batch and not contain a colon character.
     * @param callback To be run on receipt of a successful acknowledgement of publishing from rabbitmq.
     *                 Most likely to update the state of progress.
     * @throws NullPointerException     callback or batch is null
     * @throws IllegalArgumentException empty batch or batchId contains a colon character, or duplicate correlationIds in the batch
     */
    MessageBatch(String batchId, List<ImmutablePair<T, String>> batch, Runnable callback) {
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
        Set<String> uniqueCorrelationIds = batch.stream().map(Pair::getRight).collect(Collectors.toSet());
        if (uniqueCorrelationIds.size() != batch.size()) {
            throw new IllegalArgumentException(String.format("Batch %s has non-unique correlationIds so would block publishing", batchId));
        }

        this.batchId = batchId;
        this.batch = batch;
        this.callback = callback;
    }

    int getNumberOfMessages() {
        return batch.size();
    }

    @Override
    public String toString() {
        return String.format("Batch{batchId=%s, batchSize=%d}", batchId, batch.size());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        MessageBatch<?> that = (MessageBatch<?>) obj;
        return batchId.equals(that.batchId);
    }

    @Override
    public int hashCode() {
        return batchId.hashCode();
    }
}
