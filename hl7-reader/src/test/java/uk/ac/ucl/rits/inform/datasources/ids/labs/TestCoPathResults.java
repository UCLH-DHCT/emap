package uk.ac.ucl.rits.inform.datasources.ids.labs;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.ac.ucl.rits.inform.datasources.ids.exceptions.Hl7MessageIgnoredException;

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
    void testOrmO01MessagesCoPathThrowException() {
        assertThrows(Hl7MessageIgnoredException.class, () -> labReader.process(FILE_TEMPLATE, "oru_r01_copath"));
    }

    @Test
    void testOrmO01MessagesCoPathPlusThrowException() {
        assertThrows(Hl7MessageIgnoredException.class, () -> labReader.process(FILE_TEMPLATE, "oru_r01_copathplus"));
    }

}
