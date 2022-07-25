package uk.ac.ucl.rits.inform.datasinks.emapstar.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.FormAnswerRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.FormDefinitionRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.FormQuestionRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.FormRepository;
import uk.ac.ucl.rits.inform.informdb.TemporalFrom;
import uk.ac.ucl.rits.inform.informdb.forms.Form;
import uk.ac.ucl.rits.inform.informdb.forms.FormAnswer;
import uk.ac.ucl.rits.inform.informdb.forms.FormDefinition;
import uk.ac.ucl.rits.inform.informdb.forms.FormDefinitionFormQuestion;
import uk.ac.ucl.rits.inform.informdb.forms.FormQuestion;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.interchange.form.FormAnswerMsg;
import uk.ac.ucl.rits.inform.interchange.form.FormMetadataMsg;
import uk.ac.ucl.rits.inform.interchange.form.FormMsg;
import uk.ac.ucl.rits.inform.interchange.form.FormQuestionMetadataMsg;

import java.time.Instant;
import java.util.Optional;

/**
 * @author Jeremy Stein
 */
@Component
@Transactional
public class FormController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final FormRepository formRepository;
    private final FormAnswerRepository formAnswerRepository;
    private final FormDefinitionRepository formDefinitionRepository;
    private final FormQuestionRepository formQuestionRepository;

    /**
     * @param formRepository
     * @param formAnswerRepository
     * @param formDefinitionRepository
     * @param formQuestionRepository
     */
    public FormController(
            FormRepository formRepository,
            FormAnswerRepository formAnswerRepository,
            FormDefinitionRepository formDefinitionRepository,
            FormQuestionRepository formQuestionRepository) {
        this.formRepository = formRepository;
        this.formAnswerRepository = formAnswerRepository;
        this.formDefinitionRepository = formDefinitionRepository;
        this.formQuestionRepository = formQuestionRepository;
    }

    /**
     * A new (instance of a) form has been completed.
     * @param formMsg the form message, containing all answers
     * @param storedFrom stored from timestamp
     * @param hospitalVisit the hospital visit to associate this form with
     */
    public void processForm(FormMsg formMsg, Instant storedFrom, HospitalVisit hospitalVisit) {
        /* This value will only be used if formMsg contains a reference to a form or question
         * that we don't have metadata for so a placeholder metadata entry needs to be created.
         * In this case, there is not very much data at all, so the importance of a "valid from"
         * date seems quite low, so just pick it to match the current time that we're writing this.
         */
        Instant metadataValidFrom = storedFrom;

        // get existing or create a new, minimal, metadata entry
        FormDefinition formDefinition = getOrCreateFormDefinition(formMsg.getSourceMessageId(), storedFrom, metadataValidFrom);

        Form form = new Form();
        form.setFormDefinitionId(formDefinition);
        form.setStoredFrom(storedFrom);
        form.setValidFrom(formMsg.getFirstFiledDatetime());
        form.setHospitalVisitId(hospitalVisit);

        form.setFirstFiledDatetime(formMsg.getFirstFiledDatetime());
        for (FormAnswerMsg answerMsg : formMsg.getFormAnswerMsgs()) {
            // TODO: tidy up differences between different epic IDs
            String epicElementId = answerMsg.getQuestionId();
            String sdeName = answerMsg.getQuestionId();
            String sdeStringValue = answerMsg.getStringValue().get();
            FormAnswer formAnswer = new FormAnswer();
            formAnswer.setStoredFrom(storedFrom);
            formAnswer.setValidFrom(formMsg.getFirstFiledDatetime());
            formAnswer.setValueAsString(sdeStringValue);
            formAnswer.setInternalId(answerMsg.getSourceMessageId());
            form.addFormAnswer(formAnswer);
            FormQuestion formQuestion = getOrCreateFormQuestion(epicElementId, storedFrom, metadataValidFrom);
            formAnswer.setFormQuestionId(formQuestion);
            formAnswer = formAnswerRepository.save(formAnswer);
        }

        form = formRepository.save(form);
    }

    private FormDefinition getOrCreateFormDefinition(String formSourceId, Instant storedFrom, Instant validFrom) {
        Optional<FormDefinition> existing = formDefinitionRepository.findByInternalId(formSourceId);
        if (existing.isPresent()) {
            return existing.get();
        } else {
            FormDefinition newFormDefinition = new FormDefinition();
            newFormDefinition.setStoredFrom(storedFrom);
            newFormDefinition.setValidFrom(validFrom);
            newFormDefinition.setInternalId(formSourceId);
            return formDefinitionRepository.save(newFormDefinition);
        }
    }

    private FormQuestion getOrCreateFormQuestion(String formQuestionId, Instant storedFrom, Instant validFrom) {
        Optional<FormQuestion> existing = formQuestionRepository.findByInternalId(formQuestionId);
        if (existing.isPresent()) {
            return existing.get();
        } else {
            FormQuestion newFormQuestion = new FormQuestion();
            newFormQuestion.setStoredFrom(storedFrom);
            newFormQuestion.setValidFrom(validFrom);
            newFormQuestion.setInternalId(formQuestionId);
            return formQuestionRepository.save(newFormQuestion);
        }
    }

    /**
     * Create new, or update existing form metadata entry.
     * @param formMetadataMsg metadata to update with
     * @param storedFrom stored from if new rows are created
     */
    public void createOrUpdateFormMetadata(FormMetadataMsg formMetadataMsg, Instant storedFrom) {
        Instant validFrom = formMetadataMsg.getValidFrom();
        TemporalFrom temporalFrom = new TemporalFrom(validFrom, storedFrom);

        FormDefinition formDefinition = getOrCreateFormDefinition(formMetadataMsg.getSourceMessageId(), storedFrom, validFrom);
        // TODO: need to use a RowState thingy then update all the fields
        formDefinition.setName(formMetadataMsg.getFormName());
        formDefinition.setPatientFriendlyName(formMetadataMsg.getFormPatientFriendlyName());

        for (String questionId : formMetadataMsg.getQuestionIds()) {
            // We only have the question IDs at this point
            // so ensure that at minimum a placeholder entry exists for the questions,
            // then associate them with the form (what if they already exist?)
            FormQuestion formQuestion = getOrCreateFormQuestion(questionId, storedFrom, validFrom);
            FormDefinitionFormQuestion.newLink(formDefinition, formQuestion, temporalFrom);
        }
    }

    /**
     * Create new, or update an existing question metadata entry.
     * @param formQuestionMetadataMsg metadata message
     * @param storedFrom stored from if new rows are created
     */
    public void createOrUpdateFormQuestionMetadata(FormQuestionMetadataMsg formQuestionMetadataMsg, Instant storedFrom) {
        Instant validFrom = formQuestionMetadataMsg.getValidFrom();
        FormQuestion formQuestion = getOrCreateFormQuestion(formQuestionMetadataMsg.getSourceMessageId(), storedFrom, validFrom);
        // TODO: need to use a RowState thingy then update all the fields
        formQuestion.setConceptName(formQuestionMetadataMsg.getName());
        formQuestion.setInternalId(formQuestionMetadataMsg.getSourceMessageId());
    }
}
