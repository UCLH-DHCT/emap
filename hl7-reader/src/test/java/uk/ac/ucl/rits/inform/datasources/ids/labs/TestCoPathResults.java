package uk.ac.ucl.rits.inform.datasources.ids.labs;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.ac.ucl.rits.inform.datasources.ids.exceptions.Hl7InconsistencyException;
import uk.ac.ucl.rits.inform.interchange.lab.LabResultMsg;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    @TestFactory
    Stream<DynamicTest> testHl7Inconsistencies(){
        return List.of(
                "oru_r01_id_change", "oru_r01_sub_id_change", "oru_r01_multiple_value_reps", "oru_r01_unrecognised_data_type",
                "oru_r01_report_coding_unexpected"
        )
                .stream()
                .map(filename -> DynamicTest.dynamicTest(
                        filename,
                        () -> assertThrows(Hl7InconsistencyException.class, () -> labReader.process(FILE_TEMPLATE, filename)))
                );
    }

}
