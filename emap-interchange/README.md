# Emap Interchange format

This is the interchange format for sending data to the Emap core processor.

# Connecting to RabbitMQ

You need to add the interchange package to your Spring app's component scan path. eg.

```java
@SpringBootApplication(scanBasePackages = {
        "uk.ac.ucl.rits.inform.datasources.ids",
        "uk.ac.ucl.rits.inform.informdb",
        "uk.ac.ucl.rits.inform.interchange"})  //  <-------------------
public class AppHl7 {
...
```

Then it will find and use this config along with the Publisher. But you also need to supply a bean yourself that defines which queue you want to use.
I've defined an enum (`EmapDataSource`) which lists all the queues. It currently only has the HL7 and Caboodle Vitalsigns queue in it. The bean should return
the enum value corresponding to the queue you want to use. Here is the version in the HL7 data source. The method name is not important.

```
/**
 * We are writing to the HL7 queue.
 * @return the datasource enum for the hl7 queue
 */
@Bean
public EmapDataSource getHl7DataSource() {
    return EmapDataSource.HL7_QUEUE;
}
```

Spring AMQP reads its user-settable config from the Spring properties, which can be set via environment variables as in the following example:

```bash
SPRING_RABBITMQ_HOST=rabbitmq
SPRING_RABBITMQ_PORT=8672
SPRING_RABBITMQ_USERNAME=someuser
SPRING_RABBITMQ_PASSWORD=redacted
```

# Managing publishing of messages to rabbitmq

The messaging package in the interchange is used to publish messages to rabbitmq. 

- The publisher will accept a maximum number of batches of messages to be submitted before
  it blocks the thread. 
- After rabbitmq has confirmed receipt of all messages in a batch, the publisher will release the 
  space from the maximum number of batches to allow another batch to be added.
- A runnable callback is submitted along with the batch, to allow the updating of progress upon
  confirmation from rabbitmq that the message has been received.
    - This is run within a separate thread pool to ensure that it does not block
      the event thread. 

## Configuration

You will need to add some variables to your application properties in order to configure the 
rabbitmq queue and how publisher handles batches of messages:

```bash
rabbitmq.queue.length=100000
rabbitmq.max.batches=5
rabbitmq.max.intransit=1
```

- The `rabbitmq.queue.length` is the maximum message limit for the rabbitmq queue.
- The `rabbitmq.max.batches` is the maximum number of batches that can be submitted to the publisher 
  before it blocks the thread.
- The `rabbitmq.max.intransit` is the maximum number of messages that can be sent that do not
  have an acknowledgement from rabbitmq of successful receipt. If this is 1 then order will 
  be preserved, otherwise it is the maximum number of messages out of order.
 
  
## Submitting messages to be published

### Submitting a batch of messages

If messages are processed in batches, they can be submitted as a list of Pair of messages and correlationIds, 
along with a batchId and a runnable class that will update the progress upon acknowledgement for publishing
every message in the batch. 

- Autowire in the publisher 
- Create your batch 
- correlationId must be unique within the batch and must not contain a colon character
- batchId must be unique within the system and must not contain a colon character. 
    - If this batchId has already been submitted to the publisher and is awaiting publishing to rabbitmq
      then it will not be submitted in duplicate to the publisher. 
    - This can often just be the last correlationId of the batch

```java
class CaboodleOperations {
    @Autowired
    private Publisher publisher;

    private void submitCaboodleRows(Tuple caboodleRows) {
        Tuple lastCaboodleRow = null;
        List<Pair<VitalSigns, String>> batch = new ArrayList<>();

        for (Tuple caboodleRow : caboodleRows) {
            VitalSigns vitalSign = processCaboodleRow(caboodleRow);
            String correlationId = caboodleRow.get("uniqueId");
            Pair<VitalSigns, String> pair = new Pair<>(vitalSign, correlationId);
            batch.add(pair);

            lastCaboodleRow = caboodleRow;
        }
        if (lastCaboodleRow != null) {
            // submit queue if there are any new items
            ProgressUpdater progressUpdater = new ProgressUpdater(
                    lastCaboodleRow.get("lastUpdatedDate"),
                    lastCaboodleRow.get("measurementType"),
                    correlationId);
            publisher.submit(batch, correlationId, progressUpdater);
        }
    }
}
```

### Submitting a single message
There is a submit method for submitting a single message instead of a batch and this requires similar
inputs. This method makes a batch of length 1 from your message and submits it. 


