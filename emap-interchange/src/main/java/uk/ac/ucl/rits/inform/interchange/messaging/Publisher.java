package uk.ac.ucl.rits.inform.interchange.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessage;
import uk.ac.ucl.rits.inform.interchange.springconfig.EmapDataSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;


/**
 * Publishes messages to rabbitmq, resending messages that receive a nack messaged.
 *
 * @ author Stef Piatek
 */
@Component
public class Publisher implements Runnable, Releasable {
    private RabbitTemplate rabbitTemplate;
    private Semaphore semaphore;
    private final BlockingQueue<MessageBatch<? extends EmapOperationMessage>> blockingQueue;
    private Map<String, EmapOperationMessage> waitingMap;
    private Map<String, Pair<Integer, Runnable>> batchWaitingMap;
    private ScheduledThreadPoolExecutor executorService;
    private final int maxInTransit;

    private volatile boolean failedSend = false;

    private Logger logger = LoggerFactory.getLogger(Publisher.class);

    @Autowired
    private EmapDataSource getEmapDataSource;

    /**
     * @param rabbitTemplate rabbitTemplate bean
     * @param maxBatches     Application properties value rabbitmq.max.batches
     *                       Sets the maximum number of batches allowed before blocking
     * @param maxInTransit   Application properties value rabbitmq.max.batches
     *                       Sets the maximum number of messages that can be awaiting an acknowledgement by rabbitmq
     */
    @Autowired
    public Publisher(RabbitTemplate rabbitTemplate, @Value("${rabbitmq.max.batches:1}") int maxBatches,
                     @Value("${rabbitmq.max.intransit:1}") int maxInTransit) {
        semaphore = new Semaphore(maxInTransit, true);
        rabbitTemplate.setConfirmCallback(new MessagesConfirmCallback(this));
        this.rabbitTemplate = rabbitTemplate;
        blockingQueue = new ArrayBlockingQueue<>(maxBatches);
        waitingMap = new ConcurrentHashMap<>();
        batchWaitingMap = new ConcurrentHashMap<>();
        executorService = new ScheduledThreadPoolExecutor(2);
        this.maxInTransit = maxInTransit;
        new Thread(this).start();        // Should tidy up this thread?

    }

    /**
     * Submit single message for publication to rabbitmq queue defined in the configs EmapDataSource bean.
     *
     * @param message       Emap message to be sent.
     * @param correlationId Unique Id for the message. Must not contain a colon character.
     * @param batchId       Unique Id for the batch, in most cases this can be the correlationId.
     *                      Must not contain a colon character.
     * @param callback      To be run on receipt of a successful acknowledgement of publishing from rabbitmq.
     *                      Most likely to update the state of progress.
     */
    public void submit(EmapOperationMessage message, String correlationId, String batchId, Runnable callback) {
        Pair<EmapOperationMessage, String> pair = new Pair<>(message, correlationId);
        List<Pair<EmapOperationMessage, String>> list = new ArrayList<>();
        list.add(pair);
        submit(list, batchId, callback);
    }

    /**
     * Submit batch of messages for publication to rabbitmq queue defined in the configs EmapDataSource bean.
     *
     * @param batch    Batch of messages to be sent (pairs of Emap messages and their unique correlationIds)
     *                 CorrelationIds should be unique within the batch and not contain a colon character.
     * @param batchId  Unique Id for the batch, in most cases this can be the first correlationId of the batch.
     *                 Must not contain a colon character.
     * @param callback To be run on receipt of a successful acknowledgement of publishing all messages in batch from rabbitmq
     *                 Most likely to update the state of progress
     * @param <T>      Any child of EmapOperationMessage so that you can pass in child class directly.
     */
    public <T extends EmapOperationMessage> void submit(List<Pair<T, String>> batch, String batchId, Runnable callback) {
        MessageBatch<T> submitBatch = new MessageBatch<>(batchId, batch, callback);

        // If queue is full for longer than the scan for new messages, then the progress would not have been updated
        // so check ensure that we're not adding a duplicate batchId of one in progress or a waiting batch
        if (batchWaitingMap.containsKey(batchId) | blockingQueue.contains(submitBatch)) {
            logger.warn(String.format("Queue with a batchId of %s already exists", batchId));
            return;
        }
        try {
            blockingQueue.put(submitBatch);
        } catch (InterruptedException e) {
            logger.error("Waiting to submit a batch was interrupted", e);
        }
        logger.info(String.format("BatchId %s was submitted to Publisher batches", batchId));
    }

    /**
     * If a semaphore is available, adds message to the waitingMap and publishes message to rabbitmq.
     *
     * @param message       Emap message to be sent.
     * @param correlationId Unique Id for the message. Must not contain a colon character.
     * @param batchId       Unique Id for the batch, in most cases this can be the correlationId.
     *                      Must not contain a colon character.
     */
    private void publish(EmapOperationMessage message, String correlationId, String batchId) {
        logger.info("Sending message to RabbitMQ");

        CorrelationData correlationData = new CorrelationData(correlationId + ":" + batchId);
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            logger.error("Waiting to send message to rabbitmq was interrupted", e);
        }
        waitingMap.put(correlationId, message);
        rabbitTemplate.convertAndSend(getEmapDataSource.getQueueName(), message, correlationData);
    }

    /**
     *
     */
    public void run() {
        while (true) {
            try {
                MessageBatch<? extends EmapOperationMessage> messageBatch = blockingQueue.take();
                batchWaitingMap.put(messageBatch.batchId, new Pair<>(messageBatch.batch.size(), messageBatch.callback));
                for (Pair<? extends EmapOperationMessage, String> pair : messageBatch.batch) {
                    publish(pair.first, pair.second, messageBatch.batchId);
                }
                // remove the wait here?
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                logger.error("Publisher thread interrupted", e);
            }
        }
    }

    @Override
    public void finishedSending(String correlationId) {
        String[] ids = correlationId.split(":");
        if (ids.length != 2) {
            logger.error("Colon was present in correlationId or batchId provided");
            throw new IllegalStateException("Colon was present in correlationId or batchId provided");
        }

        // Synchronised to make sure that only one thread is releasing semaphores if there was a previous nack
        synchronized (waitingMap) {
            waitingMap.remove(ids[0]);
            if (failedSend) {
                if (waitingMap.isEmpty()) {
                    failedSend = false;
                    semaphore.release(maxInTransit);
                }
            } else {
                semaphore.release();
            }
        }
        synchronized (batchWaitingMap) {
            Pair<Integer, Runnable> batchState = batchWaitingMap.get(ids[1]);
            int countOfWaitingMessages = batchState.first - 1;
            if (countOfWaitingMessages == 0) {
                batchWaitingMap.remove(ids[1]);
                // Real work done in a separate thread so that it doesn't block the event thread
                executorService.execute(batchState.second);
            } else {
                batchWaitingMap.put(ids[1], new Pair<>(countOfWaitingMessages, batchState.second));
            }
        }
        logger.info(String.format("Sent %s", correlationId));

    }

    @Override
    public void failedSending(final String correlationId) {
        // On first Nack, drain all permits so only retries will be republished
        semaphore.drainPermits();
        failedSend = true;

        String[] ids = correlationId.split(":");
        if (ids.length != 2) {
            logger.error("Colon was present in correlationId or batchId provided");
            throw new IllegalStateException("Colon was present in correlationId or batchId provided");
        }
        final EmapOperationMessage message = waitingMap.get(ids[0]);
        final CorrelationData correlationData = new CorrelationData(correlationId);

        executorService.schedule(new Runnable() {
            @Override
            public void run() {
                logger.info(String.format("Resending message with correlationData: %s", correlationData));
                rabbitTemplate.convertAndSend(getEmapDataSource.getQueueName(), message, correlationData);
            }
        }, 5, TimeUnit.SECONDS); // parameterise the delay?
    }
}

