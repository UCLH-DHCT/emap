package uk.ac.ucl.rits.inform.datasources.ids.labs;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.ac.ucl.rits.inform.datasources.ids.exceptions.Hl7MessageIgnoredException;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test LabOrders derived from Winpath
 * @author Stef Piatek
 */
@ActiveProfiles("test")
@SpringBootTest
class TestCoPathOrders {
    @Autowired
    private LabReader labReader;
    private static final String FILE_TEMPLATE = "LabOrders/co_path/%s.txt";

    @Test
    void testOrrR01MessagesThrowException() {
        assertThrows(Hl7MessageIgnoredException.class, () -> labReader.process(FILE_TEMPLATE, "orr_o02"));
    }

    @Test
    void testOrmO01MessagesCpeapThrowException() {
        assertThrows(Hl7MessageIgnoredException.class, () -> labReader.process(FILE_TEMPLATE, "orm_o01_cpeap"));
    }

    @Test
    void testOrmO01MessagesCoPathPlusThrowException() {
        assertThrows(Hl7MessageIgnoredException.class, () -> labReader.process(FILE_TEMPLATE, "orm_o01_copath"));
    }

}
