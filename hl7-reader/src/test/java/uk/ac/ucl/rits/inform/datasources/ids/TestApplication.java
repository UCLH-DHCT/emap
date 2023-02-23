package uk.ac.ucl.rits.inform.datasources.ids;

import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.TimeZone;

@Configuration
public class TestApplication {
    /**
     * Hardcode timezone to ensure that instant conversion will be consistent for test assertions
     */
    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/London"));
    }
}
