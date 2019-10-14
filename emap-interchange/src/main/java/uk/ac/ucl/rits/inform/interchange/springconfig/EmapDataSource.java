package uk.ac.ucl.rits.inform.interchange.springconfig;

public enum EmapDataSource {
    HL7_QUEUE("hl7Queue")
    ;

    private String queueName;

    /**
     * @param queueName name of the AMQP queue for this data source
     */
    EmapDataSource(String queueName) {
        this.queueName = queueName;
    }

    /**
     * @return AMQP queue name
     */
    public String getQueueName() {
        return queueName;
    }
}
