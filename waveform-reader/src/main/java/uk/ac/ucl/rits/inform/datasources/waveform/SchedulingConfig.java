package uk.ac.ucl.rits.inform.datasources.waveform;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Scheduling only to be enabled when not running unit tests.
 */
@Configuration
@EnableScheduling
@Profile("!test")
public class SchedulingConfig {
}
