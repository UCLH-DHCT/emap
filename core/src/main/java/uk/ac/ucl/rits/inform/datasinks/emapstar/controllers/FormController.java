package uk.ac.ucl.rits.inform.datasinks.emapstar.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.datasinks.emapstar.RowState;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.FormAnswerRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.FormDefinitionRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.FormQuestionRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.FormRepository;
import uk.ac.ucl.rits.inform.informdb.TemporalFrom;
import uk.ac.ucl.rits.inform.informdb.forms.Form;
import uk.ac.ucl.rits.inform.informdb.forms.FormAnswer;
import uk.ac.ucl.rits.inform.informdb.forms.FormDefinition;
import uk.ac.ucl.rits.inform.informdb.forms.FormDefinitionAudit;
import uk.ac.ucl.rits.inform.informdb.forms.FormDefinitionFormQuestion;
import uk.ac.ucl.rits.inform.informdb.forms.FormQuestion;
import uk.ac.ucl.rits.inform.informdb.forms.FormQuestionAudit;
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
        RowState<FormDefinition, FormDefinitionAudit> formDefinition = getOrCreateFormDefinition(
                formMsg.getSourceMessageId(), storedFrom, metadataValidFrom);

        Form form = new Form(new TemporalFrom(formMsg.getFirstFiledDatetime(), storedFrom), formDefinition.getEntity());
        form.setHospitalVisitId(hospitalVisit);

        form.setFirstFiledDatetime(formMsg.getFirstFiledDatetime());
        form = formRepository.save(form);
        for (FormAnswerMsg answerMsg : formMsg.getFormAnswerMsgs()) {
            String questionId = answerMsg.getQuestionId();
            String answerStringValue = answerMsg.getStringValue().get();
            RowState<FormQuestion, FormQuestionAudit> formQuestion = getOrCreateFormQuestion(questionId, storedFrom, metadataValidFrom);
            FormAnswer formAnswer = new FormAnswer(
                    new TemporalFrom(formMsg.getFirstFiledDatetime(), storedFrom),
                    form,
                    formQuestion.getEntity());
            formAnswer.setValueAsText(answerStringValue);
            formAnswer.setInternalId(answerMsg.getSourceMessageId());
            formAnswer = formAnswerRepository.save(formAnswer);
        }
    }

    private RowState<FormDefinition, FormDefinitionAudit> getOrCreateFormDefinition(String formSourceId, Instant storedFrom, Instant validFrom) {
        Optional<FormDefinition> existing = formDefinitionRepository.findByInternalId(formSourceId);
        if (existing.isPresent()) {
            return new RowState<>(existing.get(), validFrom, storedFrom, true);
        } else {
            FormDefinition newFormDefinition = new FormDefinition(new TemporalFrom(validFrom, storedFrom), formSourceId);
            newFormDefinition = formDefinitionRepository.save(newFormDefinition);
            return new RowState<>(newFormDefinition, validFrom, storedFrom, true);
        }
    }

    private RowState<FormQuestion, FormQuestionAudit> getOrCreateFormQuestion(String formQuestionId, Instant storedFrom, Instant validFrom) {
        Optional<FormQuestion> existing = formQuestionRepository.findByInternalId(formQuestionId);
        if (existing.isPresent()) {
            return new RowState<>(existing.get(), validFrom, storedFrom, true);
        } else {
            FormQuestion newFormQuestion = new FormQuestion(new TemporalFrom(validFrom, storedFrom), formQuestionId);
            newFormQuestion = formQuestionRepository.save(newFormQuestion);
            return new RowState<>(newFormQuestion, validFrom, storedFrom, true);
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

        RowState<FormDefinition, FormDefinitionAudit> formDefinition = getOrCreateFormDefinition(
                formMetadataMsg.getSourceMessageId(), storedFrom, validFrom);
        formDefinition.assignIfDifferent(
                formMetadataMsg.getFormName(),
                formDefinition.getEntity().getName(),
                formDefinition.getEntity()::setName);
        formDefinition.assignIfDifferent(
                formMetadataMsg.getFormPatientFriendlyName(),
                formDefinition.getEntity().getPatientFriendlyName(),
                formDefinition.getEntity()::setPatientFriendlyName);

        for (String questionId : formMetadataMsg.getQuestionIds()) {
            // We only have the question IDs at this point
            // so ensure that at minimum a placeholder entry exists for the questions,
            // then associate them with the form (what if they already exist?)
            RowState<FormQuestion, FormQuestionAudit> formQuestion = getOrCreateFormQuestion(questionId, storedFrom, validFrom);
            FormQuestion formQuestionEntity = formQuestion.getEntity();
            new FormDefinitionFormQuestion(temporalFrom, formDefinition.getEntity(), formQuestionEntity);
        }
    }

    /**
     * Create new, or update an existing question metadata entry.
     * @param formQuestionMetadataMsg metadata message
     * @param storedFrom stored from if new rows are created
     */
    public void createOrUpdateFormQuestionMetadata(FormQuestionMetadataMsg formQuestionMetadataMsg, Instant storedFrom) {
        Instant validFrom = formQuestionMetadataMsg.getValidFrom();
        RowState<FormQuestion, FormQuestionAudit> formQuestion = getOrCreateFormQuestion(
                formQuestionMetadataMsg.getSourceMessageId(), storedFrom, validFrom);
        formQuestion.assignIfDifferent(
                formQuestionMetadataMsg.getName(),
                formQuestion.getEntity().getConceptName(),
                formQuestion.getEntity()::setConceptName);
        formQuestion.assignIfDifferent(
                formQuestionMetadataMsg.getSourceMessageId(),
                formQuestion.getEntity().getInternalId(),
                formQuestion.getEntity()::setInternalId);
    }
}
