package uk.ac.ucl.rits.inform.datasinks.emapstar.dataprocessors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.datasinks.emapstar.controllers.PersonController;
import uk.ac.ucl.rits.inform.datasinks.emapstar.controllers.FormController;
import uk.ac.ucl.rits.inform.datasinks.emapstar.controllers.VisitController;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.RequiredDataMissingException;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.interchange.form.FormMetadataMsg;
import uk.ac.ucl.rits.inform.interchange.form.FormMsg;
import uk.ac.ucl.rits.inform.interchange.form.FormQuestionMetadataMsg;

import java.time.Instant;

/**
 * @author Jeremy Stein
 */
@Component
@Transactional
public class FormProcessor {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final FormController formController;
    private final VisitController visitController;
    private final PersonController personController;

    /**
     * @param formController
     * @param visitController
     * @param personController
     */
    public FormProcessor(
            FormController formController,
            VisitController visitController,
            PersonController personController) {
        this.formController = formController;
        this.visitController = visitController;
        this.personController = personController;
    }

    /**
     * Process a message showing that a Form has been completed. Since form answers are always part of
     * a form, there is no message to process just a form answer.
     * @param formMsg interchange message describing the form instance
     * @param storedFrom stored from timestamp to use when writing
     * @throws RequiredDataMissingException if interchange field missing
     */
    public void processFormMessage(FormMsg formMsg, Instant storedFrom) throws RequiredDataMissingException {
        String srcSystem = formMsg.getSourceSystem();
        Mrn mrn = personController.getOrCreateOnMrnOnly(formMsg.getMrn(), null, srcSystem, formMsg.getFirstFiledDatetime(), storedFrom);
        HospitalVisit hospitalVisit = visitController.getOrCreateMinimalHospitalVisit(
                formMsg.getVisitNumber(),
                mrn,
                srcSystem,
                formMsg.getFirstFiledDatetime(),
                storedFrom);
        formController.processForm(formMsg, storedFrom, hospitalVisit);
    }

    /**
     * Process metadata for a SmartForm and/or SDE.
     * I'm unsure whether to structure this as one message per SmartForm, and risk some
     * repetition of SDEs that may appear in multiple forms. Do repeat much though and does it matter?
     * @param msg
     * @param storedFrom
     */
    public void processMetadataMessage(FormMetadataMsg msg, Instant storedFrom) {
        formController.createOrUpdateFormMetadata(msg, storedFrom);
    }

    public void processQuestionMetadataMessage(FormQuestionMetadataMsg msg, Instant storedFrom) {
        formController.createOrUpdateFormQuestionMetadata(msg, storedFrom);
    }
}
