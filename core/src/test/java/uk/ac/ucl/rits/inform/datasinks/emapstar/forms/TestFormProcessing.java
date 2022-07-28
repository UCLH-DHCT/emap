package uk.ac.ucl.rits.inform.datasinks.emapstar.forms;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.datasinks.emapstar.MessageProcessingBase;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.FormAnswerRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.FormDefinitionRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.FormQuestionRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.FormRepository;
import uk.ac.ucl.rits.inform.informdb.forms.Form;
import uk.ac.ucl.rits.inform.informdb.forms.FormAnswer;
import uk.ac.ucl.rits.inform.informdb.forms.FormDefinition;
import uk.ac.ucl.rits.inform.informdb.forms.FormDefinitionFormQuestion;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.form.FormMetadataMsg;
import uk.ac.ucl.rits.inform.interchange.form.FormMsg;
import uk.ac.ucl.rits.inform.interchange.form.FormQuestionMetadataMsg;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Jeremy Stein
 * Test SmartForm and Smart Data Element processing.
 */
@Transactional
public class TestFormProcessing extends MessageProcessingBase {
    @Autowired
    private FormAnswerRepository formAnswerRepository;
    @Autowired
    private FormRepository formRepository;
    @Autowired
    private FormDefinitionRepository formDefinitionRepository;
    @Autowired
    private FormQuestionRepository formQuestionRepository;

    private final long STARTING_NUM_QUESTIONS = 29;
    private final long STARTING_NUM_FORMS = 1;

    @Test
    public void basicFormTest() throws IOException, EmapOperationMessageProcessingException {
        FormMsg formMsg = messageFactory.getFormMsgTemp();
        processSingleMessage(formMsg);
        assertEquals(1, formRepository.count());
        Form form = formRepository.findAllByHospitalVisitIdEncounter("examplevisit").get(0);
        FormDefinition formDefinition = form.getFormDefinitionId();
        assertEquals("SomeDerivedFormInstanceIdentifier", formDefinition.getInternalId());
        // we don't have the name yet
        assertNull(formDefinition.getName());
        List<FormAnswer> formAnswers = form.getFormAnswers();
        assertEquals(2, formAnswers.size());
        assertEquals("UCLH#123", formAnswers.get(0).getFormQuestionId().getInternalId());
    }

    @Test
    @Sql("/populate_db.sql")
    public void sqlTest() {
        return;
    }

    @Test
    @Sql("/populate_db.sql")
    public void metadataTestStartingPopulated() throws EmapOperationMessageProcessingException {
        // STARTING_NUM_QUESTIONS questions in initial DB state, message contains 31 questions
        // of which 3 are new. 1 of initial DB state is missing from message, which means that
        // it should be unlinked from the form, but not deleted itself.
        // No new forms are added.
        _basicMetadataTest(STARTING_NUM_QUESTIONS + 3, 1, STARTING_NUM_FORMS);
    }

    @Test
    public void metadataTestStartingBlank() throws  EmapOperationMessageProcessingException {
        // Empty initial state, so the 31 questions and 1 form in the message should be added
        _basicMetadataTest(31, 0, 1);
    }

    private void _basicMetadataTest(final long expectedNumQuestions, final long orphanedQuestions, final long expectedNumForms) throws EmapOperationMessageProcessingException {
        FormMetadataMsg formMetadataMsg = messageFactory.getFormMetadataMsg();
        List<FormQuestionMetadataMsg> formQuestionMetadataMsg = messageFactory.getFormQuestionMetadataMsg();
        processSingleMessage(formMetadataMsg);
        processMessages(formQuestionMetadataMsg);

        assertEquals(expectedNumQuestions, formQuestionRepository.count());
        assertEquals(expectedNumForms, formDefinitionRepository.count());
        FormDefinition form = formDefinitionRepository.findByInternalId("2056").get();

        // Make sure that questions can also be accessed via the OneToMany relationship
        List<FormDefinitionFormQuestion> questions = form.getQuestions();
        assertEquals(expectedNumQuestions - orphanedQuestions, questions.size());
    }
}
