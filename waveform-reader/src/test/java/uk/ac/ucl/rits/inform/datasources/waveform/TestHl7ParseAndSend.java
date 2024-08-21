package uk.ac.ucl.rits.inform.datasources.waveform;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import uk.ac.ucl.rits.inform.datasources.waveform.hl7parse.Hl7ParseException;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.visit_observations.WaveformMessage;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringJUnitConfig
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@SpringBootTest
@ActiveProfiles("test")
class TestHl7ParseAndSend {
    @Autowired
    private Hl7ParseAndSend hl7ParseAndSend;

    @Test
    void goodMessage() throws IOException, URISyntaxException, Hl7ParseException {
        String hl7String = readHl7FromResource("hl7/test1.hl7");
        List<WaveformMessage> msgs = hl7ParseAndSend.parseHl7(hl7String);
        assertEquals(5, msgs.size());
        assertTrue(msgs.stream().allMatch(m -> m.getSourceLocationString().equals("UCHT03TEST")));
        assertEquals(
                List.of("59912", "59913", "59914", "59915", "59916"),
                msgs.stream().map(m -> m.getSourceStreamId()).toList());
        List<String> distinctMessageIds = msgs.stream().map(m -> m.getSourceMessageId()).distinct().toList();
        assertEquals(msgs.size(), distinctMessageIds.size());
        var expectedValues = List.of(
                List.of(42.10),
                List.of(42.20),
                List.of(42.30, 43.30, 44.30),
                List.of(42.40, 43.40, 44.40, 45.40),
                List.of(42.50, 43.50, 44.5, 45.5, 46.5));

        for (int i = 0; i < msgs.size(); i++) {
            WaveformMessage m = msgs.get(i);
            InterchangeValue<List<Double>> numericValues = m.getNumericValues();
            assertTrue(numericValues.isSave());
            List<Double> expected = expectedValues.get(i);
            assertEquals(expected, numericValues.get());
        }
    }

    @Test
    void messageWithMoreThanOneRepeat() throws IOException, URISyntaxException {
        String hl7String = readHl7FromResource("hl7/test1.hl7");
        String hl7WithReps = hl7String.replace("42.50^", "42.50~");
        Hl7ParseException e = assertThrows(Hl7ParseException.class, () -> hl7ParseAndSend.parseHl7(hl7WithReps));
        assertTrue(e.getMessage().contains("only be 1 repeat"));
    }

    @Test
    void messageWithConflictingLocation() throws IOException, URISyntaxException {
        String hl7String = readHl7FromResource("hl7/test1.hl7");
        String hl7WithReps = hl7String.replace("PV1||I|UCHT03TEST|", "PV1||I|UCHT03TESTXXX|");
        Hl7ParseException e = assertThrows(Hl7ParseException.class, () -> hl7ParseAndSend.parseHl7(hl7WithReps));
        assertTrue(e.getMessage().contains("Unexpected location"));
    }

    private String readHl7FromResource(String relativeResourceFilePath) throws IOException, URISyntaxException {
        ClassLoader classLoader = this.getClass().getClassLoader();
        URI uri = classLoader.getResource(relativeResourceFilePath).toURI();
        List<String> readAllLines = Files.readAllLines(Path.of(uri));
        return String.join("\r", readAllLines) + "\r";
    }

}