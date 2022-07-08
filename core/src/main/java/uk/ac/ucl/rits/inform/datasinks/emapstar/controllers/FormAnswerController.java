package uk.ac.ucl.rits.inform.datasinks.emapstar.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.FormAnswerRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.FormDefinitionRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.FormQuestionRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.FormRepository;
import uk.ac.ucl.rits.inform.informdb.forms.Form;
import uk.ac.ucl.rits.inform.informdb.forms.FormAnswer;
import uk.ac.ucl.rits.inform.informdb.forms.FormDefinition;
import uk.ac.ucl.rits.inform.informdb.forms.FormQuestion;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.interchange.form.FormAnswerMsg;
import uk.ac.ucl.rits.inform.interchange.form.FormMsg;

import java.time.Instant;

/**
 * @author Jeremy Stein
 */
@Component
@Transactional
public class FormAnswerController {
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
    public FormAnswerController(
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
    public void processSmartForm(FormMsg formMsg, Instant storedFrom, HospitalVisit hospitalVisit) {
        // need to get existing

        // this will eventually come from the concept table
        Instant metadataValidFrom = Instant.now();

        FormDefinition formDefinition = new FormDefinition();
        formDefinition.setValidFrom(metadataValidFrom);
        formDefinition.setStoredFrom(storedFrom);
        formDefinition.setFormName(formMsg.getSourceSystemFormId());
        formDefinition.setDescription("example smartform description");
        formDefinition = formDefinitionRepository.save(formDefinition);

        Form form = new Form();
        form.setFormDefinitionId(formDefinition);
        form.setStoredFrom(storedFrom);
        form.setValidFrom(formMsg.getFormFilingDatetime());
        form.setHospitalVisitId(hospitalVisit);

        form.setFormFilingDatetime(formMsg.getFormFilingDatetime());
        for (FormAnswerMsg answerMsg : formMsg.getFormAnswerMsgs()) {
            String epicElementId = answerMsg.getEpicElementId();
            String sdeName = answerMsg.getElementName();
            String sdeValue = answerMsg.getElementValue();
            FormAnswer formAnswer = new FormAnswer();
            formAnswer.setStoredFrom(storedFrom);
            formAnswer.setValidFrom(formMsg.getFormFilingDatetime());
            formAnswer.setValueAsString(sdeValue);
            FormQuestion formQuestion = new FormQuestion();
            formQuestion.setValidFrom(metadataValidFrom);
            formQuestion.setStoredFrom(storedFrom);
            formQuestion.setFormQuestionConceptName("example concept name");
            formQuestion.setFormQuestionConceptAbbrevName("xmpl cncpt nm");
            formQuestion.setSmartDataElementIdString(answerMsg.getEpicElementId());
            formQuestion = formQuestionRepository.save(formQuestion);
            formAnswer.setFormQuestionId(formQuestion);
            formAnswer = formAnswerRepository.save(formAnswer);
            form.addFormAnswer(formAnswer);
        }

        form = formRepository.save(form);
    }
}
