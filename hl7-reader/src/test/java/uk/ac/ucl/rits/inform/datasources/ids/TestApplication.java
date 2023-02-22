package uk.ac.ucl.rits.inform.datasources.ids;

import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.TimeZone;

@Configuration
public class TestApplication {
    /**
     * Set time zone to be Europe/London to match docker containers
     */
    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/London"));
    }
}
