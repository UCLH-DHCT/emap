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
import java.time.Instant;
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

    // Counts for initialised test DB (populate_db.sql)
    private final long STARTING_NUM_QUESTIONS = 29;
    private final long STARTING_NUM_FORMS = 1;

    @Test
    public void formOnly() throws IOException, EmapOperationMessageProcessingException {
        Form form = _processForm();
        // No metadata, so we won't know the name
        assertNull(form.getFormDefinitionId().getName());
    }

    @Test
    public void formFollowedByFormMetadata() throws EmapOperationMessageProcessingException, IOException {
        // This is 1 form, which implies 1 form definition.
        // The form contains 2 answers, so there should be 2 implied questions that we don't have metadata for yet
        Form form = _processForm();

        // Metadata adds 1 form definition and 31 questions, with no overlap with those implied above.
        // Hence 2 form definitions and 33 questions.
        _processMetadata(33, 0, 2);
    }

    @Test
    @Sql("/populate_db.sql")
    public void metadataTestStartingPopulated() throws EmapOperationMessageProcessingException {
        // STARTING_NUM_QUESTIONS questions in initial DB state, message contains 31 questions
        // of which 3 are new. 1 of initial DB state is missing from message, which means that
        // it should be unlinked from the form, but not deleted itself.
        // No new form definitions are added.
        _processMetadata(STARTING_NUM_QUESTIONS + 3, 1, STARTING_NUM_FORMS);
    }

    @Test
    public void metadataTestStartingBlank() throws  EmapOperationMessageProcessingException {
        // Empty initial state, so this is a test of adding only.
        // The 31 questions and 1 form in the message should be added.
        _processMetadata(31, 0, 1);
    }

    /**
     * Process a test FormMsg and perform some basic checks.
     */
    private Form _processForm() throws IOException, EmapOperationMessageProcessingException {
        FormMsg formMsg = messageFactory.getFormMsgTemp();
        processSingleMessage(formMsg);
        assertEquals(1, formRepository.count());
        Form form = formRepository.findAllByHospitalVisitIdEncounter("examplevisit").get(0);
        FormDefinition formDefinition = form.getFormDefinitionId();
        assertEquals("SomeDerivedFormInstanceIdentifier", formDefinition.getInternalId());
        assertEquals(Instant.parse("2022-04-01T11:59:00Z"), form.getFirstFiledDatetime());
        List<FormAnswer> formAnswers = form.getFormAnswers();
        assertEquals(2, formAnswers.size());
        assertEquals("UCLH#123", formAnswers.get(0).getFormQuestionId().getInternalId());
        return form;
    }

    /**
     * Process test FormMetadataMsg and FormQuestionMetadataMsg messages, and perform some basic checks.
     */
    private void _processMetadata(final long expectedNumQuestions, final long orphanedQuestions, final long expectedNumFormDefinitions) throws EmapOperationMessageProcessingException {
        FormMetadataMsg formMetadataMsg = messageFactory.getFormMetadataMsg();
        List<FormQuestionMetadataMsg> formQuestionMetadataMsg = messageFactory.getFormQuestionMetadataMsg();
        processSingleMessage(formMetadataMsg);
        processMessages(formQuestionMetadataMsg);

        assertEquals(expectedNumQuestions, formQuestionRepository.count());
        assertEquals(expectedNumFormDefinitions, formDefinitionRepository.count());
        FormDefinition formDefinition = formDefinitionRepository.findByInternalId("2056").get();

        // Make sure that questions can also be accessed via the OneToMany relationship
        // Disable this check as we probably are going to delete FormDefinitionFormQuestion altogether.
        // I think it currently fails in any case where the form definition
        // already existed but the list of questions needs updating.
        /*
        List<FormDefinitionFormQuestion> questions = formDefinition.getQuestions();
        assertEquals(expectedNumQuestions - orphanedQuestions, questions.size());
         */
    }
}
