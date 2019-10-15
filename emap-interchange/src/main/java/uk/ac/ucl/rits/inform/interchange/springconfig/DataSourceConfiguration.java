package uk.ac.ucl.rits.inform.interchange.springconfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.support.RetryTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Spring configuration for data sources using the AMQP queue.
 *
 * @author Jeremy Stein
 */
@Configuration
public class DataSourceConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(DataSourceConfiguration.class);

    /**
     * @return a converter which ensures Instant objects are handled properly
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        return new Jackson2JsonMessageConverter(mapper);
    }

    @Autowired
    private ConnectionFactory connectionFactory;

    @Autowired
    private EmapDataSource emapDataSource;

    /**
     * @return our rabbit template
     */
    @Bean
    @Profile("default")
    public AmqpTemplate rabbitTemp() {
        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
        String queueName = getEmapDataSource().getQueueName();
        Queue q = new Queue(queueName, true);
        while (true) {
            try {
                rabbitAdmin.declareQueue(q);
                break;
            } catch (AmqpException e) {
                int secondsSleep = 5;
                logger.warn(String.format("Creating RabbitMQ queue \"%s\" failed with exception %s, retrying in %d seconds",
                        queueName, e.toString(), secondsSleep));
                try {
                    Thread.sleep(secondsSleep * 1000);
                } catch (InterruptedException e1) {
                    logger.warn("Sleep interrupted");
                }
                continue;
            }
        }

        RetryTemplate retryTemplate = new RetryTemplate();
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(500);
        backOffPolicy.setMultiplier(10.0);
        backOffPolicy.setMaxInterval(10000);
        retryTemplate.setBackOffPolicy(backOffPolicy);

        RabbitTemplate template = rabbitAdmin.getRabbitTemplate();
        template.setRetryTemplate(retryTemplate);
        template.setMandatory(true);

        logger.info("Created queue " + queueName + ", properties = " + rabbitAdmin.getQueueProperties(queueName));
        return template;
    }

    /**
     * @return the data source
     */
    public EmapDataSource getEmapDataSource() {
        return emapDataSource;
    }

}
