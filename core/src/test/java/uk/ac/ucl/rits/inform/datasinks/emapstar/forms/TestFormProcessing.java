package uk.ac.ucl.rits.inform.datasinks.emapstar.forms;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import uk.ac.ucl.rits.inform.datasinks.emapstar.MessageProcessingBase;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.FormAnswerRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.FormRepository;
import uk.ac.ucl.rits.inform.informdb.forms.Form;
import uk.ac.ucl.rits.inform.informdb.forms.FormAnswer;
import uk.ac.ucl.rits.inform.informdb.forms.FormDefinition;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.form.FormMsg;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Jeremy Stein
 * Test SmartForm and Smart Data Element processing.
 */
public class TestFormProcessing extends MessageProcessingBase {
    @Autowired
    private FormAnswerRepository formAnswerRepository;
    @Autowired
    private FormRepository formRepository;

    @Test
    public void basicFormTest() throws IOException, EmapOperationMessageProcessingException {
        FormMsg formMsg = messageFactory.getFormMsgTemp();
        processSingleMessage(formMsg);
        Form form = formRepository.findAll().iterator().next();
        FormDefinition formDefinition = form.getFormDefinitionId();
        assertEquals("SmartForm1234", formDefinition.getFormName());
        List<FormAnswer> formAnswers = form.getFormAnswers();
        assertEquals(2, formAnswers.size());
        assertEquals("UCLH#123", formAnswers.get(0).getFormQuestionId().getSmartDataElementIdString());
    }

    @Test
    @Sql("/populate_db.sql")
    public void sqlTest() {
        return;
    }
}
