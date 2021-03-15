package uk.ac.ucl.rits.inform.datasources.ids.labs;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.ac.ucl.rits.inform.interchange.lab.LabResultMsg;

import java.io.File;
import java.io.FileInputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * Test Lab results derived from Winpath
 * @author Stef Piatek
 */
@ActiveProfiles("test")
@SpringBootTest
class TestCoPathResults {
    @Autowired
    private LabReader labReader;
    private static final String FILE_TEMPLATE = "LabOrders/co_path/%s.txt";

    @Test
    void testValueAsByteParsed() throws Exception {
        File reportFile = new File(getClass().getResource("/LabOrders/co_path/report.pdf").getFile());
        byte[] expectedBytes;
        try (FileInputStream inputStream = new FileInputStream(reportFile)) {
            expectedBytes = inputStream.readAllBytes();
        }

        LabResultMsg result = labReader.process(FILE_TEMPLATE, "oru_r01_copath")
                .getLabResultMsgs().stream()
                .filter(r -> r.getByteValue().isSave())
                .findFirst().orElseThrow();
        assertArrayEquals(expectedBytes, result.getByteValue().get());
    }

}
