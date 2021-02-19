package uk.ac.ucl.rits.inform.datasources.ids.labs;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.ac.ucl.rits.inform.datasources.ids.TestHl7MessageStream;
import uk.ac.ucl.rits.inform.datasources.ids.exceptions.Hl7InconsistencyException;
import uk.ac.ucl.rits.inform.datasources.ids.exceptions.Hl7MessageIgnoredException;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessage;

import java.util.List;

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

    void process(String fileTemplate, String fileName) throws Hl7MessageIgnoredException, Hl7InconsistencyException {
        List<? extends EmapOperationMessage> msgs = null;
        try {
            msgs = processSingleMessage(String.format(fileTemplate, fileName));
        } catch (Hl7MessageIgnoredException | Hl7InconsistencyException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
        }
        // add more to once have an interchange format
    }

    @Test
    void testOrmO01MessagesThrowException() {
        assertThrows(Hl7MessageIgnoredException.class, () -> process(FILE_TEMPLATE, "oru_r01"));
    }

}
