# Emap Interchange format

This is the interchange format for sending data to the Emap core processor.

# Connecting to RabbitMQ

You need to add the springconfig package to your Spring app's component scan path. eg.

```java
@SpringBootApplication(scanBasePackages = {
        "uk.ac.ucl.rits.inform.datasources.ids",
        "uk.ac.ucl.rits.inform.informdb",
        "uk.ac.ucl.rits.inform.interchange.springconfig"})  //  <-------------------
public class AppHl7 {
...
```

Then it will find and use this config. But you also need to supply a bean yourself that defines which queue you want to use.
I've defined an enum (`EmapDataSource`) which lists all the queues. It currently only has the HL7 queue in it. The bean returns
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
