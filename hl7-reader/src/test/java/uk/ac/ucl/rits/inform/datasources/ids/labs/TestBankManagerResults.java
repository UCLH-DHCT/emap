package uk.ac.ucl.rits.inform.datasources.ids.labs;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.ac.ucl.rits.inform.datasources.ids.TestHl7MessageStream;
import uk.ac.ucl.rits.inform.datasources.ids.exceptions.Hl7MessageIgnoredException;
import uk.ac.ucl.rits.inform.interchange.lab.LabOrderMsg;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test LabResults derived from Bank Manager
 * @author Stef Piatek
 */
@ActiveProfiles("test")
@SpringBootTest
class TestBankManagerResults extends TestHl7MessageStream {
    @Autowired
    private static final String FILE_TEMPLATE = "LabOrders/bank_manager/%s.txt";

    @Autowired
    private LabReader labReader;

    @Test
    void testOrmO01MessagesThrowException() throws Exception {
        LabOrderMsg order = labReader.getFirstOrder(FILE_TEMPLATE, "oru_r01_result");

    }

}
