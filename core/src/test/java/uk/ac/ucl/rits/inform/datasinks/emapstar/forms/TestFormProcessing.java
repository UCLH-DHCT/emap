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
import uk.ac.ucl.rits.inform.informdb.forms.FormQuestion;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.form.FormMetadataMsg;
import uk.ac.ucl.rits.inform.interchange.form.FormQuestionMetadataMsg;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    public void formFollowedByFormMetadata() throws EmapOperationMessageProcessingException, IOException {
        // This is 2 form, which implies 2 form definition.
        // The form contains many answers, so there should be many implied questions that we don't have metadata for yet
        _processForms();

        // implied from forms above (13 answers for 11 distinct questions)
        assertEquals(11, formQuestionRepository.count());
        assertEquals(1, formDefinitionRepository.count());

        // pick just one form instance to inspect in more detail
        Map<String, FormAnswer> answersByIdPreMetadata = getAnswersByConceptId("*116066");
        assertEquals(Instant.parse("2018-11-01T15:39:15Z"), answersByIdPreMetadata.get("UCLH#1167").getFormId().getFirstFiledDatetime());
        Set<String> expectedQuestions = Set.of("UCLH#1167", "UCLH#1205", "FAKE#0001", "FAKE#0003", "FAKE#0004", "FAKE#0005", "FAKE#0006", "FAKE#0007");
        assertEquals(expectedQuestions, answersByIdPreMetadata.keySet());

        // check some values
        assertEquals("abcabc", answersByIdPreMetadata.get("UCLH#1167").getValueAsText());
        assertNull(answersByIdPreMetadata.get("UCLH#1167").getValueAsBoolean());

        assertEquals("1", answersByIdPreMetadata.get("UCLH#1205").getValueAsText());
        assertEquals(true, answersByIdPreMetadata.get("UCLH#1205").getValueAsBoolean());

        assertEquals("2021-02-04", answersByIdPreMetadata.get("FAKE#0003").getValueAsText());
        assertEquals(LocalDate.parse("2021-02-04"), answersByIdPreMetadata.get("FAKE#0003").getValueAsDate());

        assertEquals("2020-04-05T01:02:00.00Z", answersByIdPreMetadata.get("FAKE#0004").getValueAsText());
        assertEquals(Instant.parse("2020-04-05T01:02:00.00Z"), answersByIdPreMetadata.get("FAKE#0004").getValueAsDatetime());

        assertEquals("1.01", answersByIdPreMetadata.get("FAKE#0005").getValueAsText());
        assertEquals(1.01, answersByIdPreMetadata.get("FAKE#0005").getValueAsNumber());

        // all question concept names should be unknown because there was no metadata in the form data
        for (var fa : answersByIdPreMetadata.values()) {
            assertNull(fa.getFormQuestionId().getConceptName());
        }
        // pick any question to reach the form definition
        FormDefinition formDef = answersByIdPreMetadata.get("UCLH#1167").getFormId().getFormDefinitionId();
        // No metadata yet, so we won't know the form name either
        assertNull(formDef.getName());

        // Metadata contains 2 form definition of which we already knew about one;
        // Metadata also contains 29 questions, of which 28 previously unknown.
        // Hence 2 form definitions and 11 + 28 = 39 questions.
        _processMetadata();
        assertEquals(39, formQuestionRepository.count());
        assertEquals(2, formDefinitionRepository.count());

        // also check the audit repos

        // re-fetch and check some of the metadata
        Map<String, FormQuestion> questionsByIdPostMetadata = formQuestionRepository.findAllByInternalIdIn(expectedQuestions).stream().collect(
                Collectors.toUnmodifiableMap(FormQuestion::getInternalId, Function.identity()));
        assertEquals("ICU Discussion", questionsByIdPostMetadata.get("UCLH#1205").getConceptAbbrevName());
        assertEquals("ICU DISCUSSION", questionsByIdPostMetadata.get("UCLH#1205").getConceptName());
        assertEquals("Most SDEs have no description but this one has a two-line description (1/2)\nSecond line of description",
                questionsByIdPostMetadata.get("UCLH#1205").getDescription());

        FormDefinition formDefAfterMetadata  = formDefinitionRepository.findByInternalId("2056").get();
        assertEquals("UCLH ADVANCED TEP", formDefAfterMetadata.getName());
        assertEquals("tep patient friendly name", formDefAfterMetadata.getPatientFriendlyName());
    }

    @Test
    @Sql("/populate_db.sql")
    public void metadataTestStartingPopulated() throws EmapOperationMessageProcessingException, IOException {
        // STARTING_NUM_QUESTIONS questions in initial DB state, metadata messages contain 29 questions
        // of which 0 are new, 2 form definitions of which 1 is new.
        _processMetadata();
        assertEquals(STARTING_NUM_QUESTIONS + 0, formQuestionRepository.count());
        assertEquals(STARTING_NUM_FORMS + 1, formDefinitionRepository.count());
    }

    @Test
    public void metadataTestStartingBlank() throws EmapOperationMessageProcessingException, IOException {
        // Empty initial state, so this is a test of adding only.
        // The 29 questions and 2 form definitions in the message should be added.
        _processMetadata();
        assertEquals(29, formQuestionRepository.count());
        assertEquals(2, formDefinitionRepository.count());
    }

    /**
     * Process a test FormMsg and perform some basic checks.
     */
    private void _processForms() throws IOException, EmapOperationMessageProcessingException {
        processMessages(messageFactory.getFormMsgs("forms1.yaml"));
        assertEquals(2, formRepository.count());
    }

    private Map<String, FormAnswer> getAnswersByConceptId(String visitId) {
        Form form = formRepository.findSingleByHospitalVisitIdEncounter(visitId);
        FormDefinition formDefinition = form.getFormDefinitionId();
        assertEquals("2056", formDefinition.getInternalId());
        Map<String, FormAnswer> answersById = form.getFormAnswers().stream().collect(Collectors.toUnmodifiableMap(
                fa -> fa.getFormQuestionId().getInternalId(), Function.identity()));
        return answersById;
    }

    /**
     * Process test FormMetadataMsg and FormQuestionMetadataMsg messages, and perform some basic checks.
     */
    private void _processMetadata() throws EmapOperationMessageProcessingException, IOException {
        List<FormMetadataMsg> formMetadataMsgs = messageFactory.getFormMetadataMsg("form_metadata1.yaml");
        List<FormQuestionMetadataMsg> formQuestionMetadataMsg = messageFactory.getFormQuestionMetadataMsg("form_question_metadata_full.yaml");
        processMessages(formMetadataMsgs);
        processMessages(formQuestionMetadataMsg);

        FormDefinition formDefinition = formDefinitionRepository.findByInternalId("2056").get();
    }
}
