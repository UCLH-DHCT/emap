package uk.ac.ucl.rits.inform.datasinks.emapstar.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.datasinks.emapstar.RowState;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.ConsultationRequestRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.ConsultationRequestAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.ConsultationRequestTypeRepository;
import uk.ac.ucl.rits.inform.informdb.consults.ConsultationRequestAudit;
import uk.ac.ucl.rits.inform.informdb.consults.ConsultationType;
import uk.ac.ucl.rits.inform.informdb.consults.ConsultationRequest;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.ConsultRequest;

import java.time.Instant;

/**
 * Functionality to create consultation requests for patients.
 * @author Anika Cawthorn
 */
@Component
public class ConsultationRequestController {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ConsultationRequestRepository consultationRequestRepo;
    private final ConsultationRequestTypeRepository consultationRequestTypeRepo;
    private final ConsultationRequestAuditRepository consultationRequestAuditRepo;
    private final QuestionController questionController;

    /**
     * Setting repositories holding information on patient states.
     * @param consultationRequestRepo        Consultation request repo
     * @param consultationRequestTypeRepo    Consultation request type repo
     * @param consultationRequestAuditRepo   Consultation request audit type repo
     * @param questionController             Question controller for questions in relation to consultation requests
     */
    public ConsultationRequestController(
            ConsultationRequestRepository consultationRequestRepo,
            ConsultationRequestTypeRepository consultationRequestTypeRepo,
            ConsultationRequestAuditRepository consultationRequestAuditRepo,
            QuestionController questionController) {
        this.consultationRequestRepo = consultationRequestRepo;
        this.consultationRequestTypeRepo = consultationRequestTypeRepo;
        this.consultationRequestAuditRepo = consultationRequestAuditRepo;
        this.questionController = questionController;
    }

    /**
     * Get existing consultation request type.
     * @param msg        consultation request message
     * @param storedFrom when consultation request information is stored from
     * @return ConsultRequestType
     */
    private ConsultationType getOrCreateConsultationRequestType(ConsultRequest msg, Instant storedFrom) {
        return consultationRequestTypeRepo
                .findByStandardisedCode(msg.getConsultationType())
                .orElseGet(() -> createAndSaveNewType(msg, storedFrom));
    }

    /**
     * Create and save a ConsultationType from the information contained in the ConsultRequest message.
     * @param msg           Consultation request message
     * @param storedFrom    valid from in database
     * @return saved ConsultationType
     */
    private ConsultationType createAndSaveNewType(ConsultRequest msg, Instant storedFrom) {
        ConsultationType consultationType = new ConsultationType(msg.getConsultationType(),
                msg.getRequestedDateTime(), storedFrom);
        logger.debug("Created new {}", consultationType);
        return consultationRequestTypeRepo.save(consultationType);
    }

    /**
     * Get or create existing ConsultationRequest entity.
     * @param msg                       Consultation request message
     * @param mrn                       Patient identifier
     * @param visit                     Hospital visit of patient this consultation request refers to.
     * @param consultationType   Consultancy type as identified in message
     * @param storedFrom                time that emap-core started processing the message
     * @return ConsultationRequest entity wrapped in RowState
     */
    private RowState<ConsultationRequest, ConsultationRequestAudit> getOrCreateConsultationRequest(
            ConsultRequest msg, Mrn mrn, HospitalVisit visit, ConsultationType consultationType, Instant storedFrom) {
        return consultationRequestRepo
                .findByMrnIdAndHospitalVisitIdAndConsultationRequestTypeId(mrn, visit, consultationType)
                .map(obs -> new RowState<>(obs, msg.getRequestedDateTime(), storedFrom, false))
                .orElseGet(() -> createMinimalConsultationRequest(msg, mrn, visit, consultationType, storedFrom));
    }

    /**
     * Create minimal consultation request wrapped in RowState.
     * @param msg                       Consultation request message
     * @param mrn                       patient identifier
     * @param visit                     Hospital visit of the patient consultation request occurred at
     * @param consultationType   Consultation request type referred to in message
     * @param storedFrom                time that emap-core started processing the message
     * @return minimal consultation request wrapped in RowState
     */
    private RowState<ConsultationRequest, ConsultationRequestAudit> createMinimalConsultationRequest(
            ConsultRequest msg, Mrn mrn, HospitalVisit visit, ConsultationType consultationType,
            Instant storedFrom) {
        ConsultationRequest consultationRequest = new ConsultationRequest(consultationType, mrn, visit);
        logger.debug("Created new {}", consultationRequest);
        return new RowState<>(consultationRequest, msg.getStatusChangeTime(), storedFrom, true);
    }

    /**
     * Update message if observation has been created, or the message updated time is >= entity validFrom.
     * @param msg                   Consultation request message
     * @param consultationRequest   Consultation request
     * @return true if message should be updated
     */
    private boolean messageShouldBeUpdated(ConsultRequest msg, RowState<ConsultationRequest,
            ConsultationRequestAudit> consultationRequest) {
        return (consultationRequest.isEntityCreated() || !msg.getRequestedDateTime().isBefore(
                consultationRequest.getEntity().getValidFrom()));
    }

    /**
     * Update consultation request from consultation request message.
     * @param msg                   consultation request message
     * @param consultationRequest   consultation request referred to in message
     */
    private void updateConsultRequest(ConsultRequest msg, RowState<ConsultationRequest,
            ConsultationRequestAudit> consultationRequest) {
        ConsultationRequest cRequest = consultationRequest.getEntity();
        consultationRequest.assignIfDifferent(msg.isCancelled(), cRequest.getCancelled(), cRequest::setCancelled);
        consultationRequest.assignIfDifferent(msg.isClosedDueToDischarge(), cRequest.getClosedDueToDischarge(), cRequest::setClosedDueToDischarge);
    }

    /**
     * Process patient state message.
     * @param msg           message
     * @param mrn           patient id
     * @param visit         hospital visit this consultation request related to.
     * @param storedFrom    valid from in database
     * @throws EmapOperationMessageProcessingException if message can't be processed.
     */
    @Transactional
    public void processMessage(final ConsultRequest msg, Mrn mrn, HospitalVisit visit, final Instant storedFrom)
            throws EmapOperationMessageProcessingException {
        ConsultationType consultationType = getOrCreateConsultationRequestType(msg, storedFrom);
        RowState<ConsultationRequest, ConsultationRequestAudit> consultationRequest = getOrCreateConsultationRequest(
                msg, mrn, visit, consultationType, storedFrom);
        ConsultationRequest cRequest = consultationRequest.getEntity();

        consultationRequest.assignIfCurrentlyNullOrNewerAndDifferent(msg.getRequestedDateTime(),
                cRequest.getRequestedDateTime(), cRequest::setRequestedDateTime, msg.getRequestedDateTime(), storedFrom);
        consultationRequest.assignIfCurrentlyNullOrNewerAndDifferent(msg.getStatusChangeTime(),
                cRequest.getStatusChangeTime(), cRequest::setStatusChangeTime, msg.getRequestedDateTime(), storedFrom);
        consultationRequest.assignIfCurrentlyNullOrNewerAndDifferent(msg.getNotes(),
                cRequest.getComments(), cRequest::setComments, msg.getRequestedDateTime(), storedFrom);
        consultationRequest.assignIfCurrentlyNullOrNewerAndDifferent(msg.isClosedDueToDischarge(),
                cRequest.getClosedDueToDischarge(), cRequest::setClosedDueToDischarge, msg.getRequestedDateTime(), storedFrom);
        consultationRequest.assignIfCurrentlyNullOrNewerAndDifferent(msg.isCancelled(), cRequest.getCancelled(), cRequest::setCancelled,
                msg.getRequestedDateTime(), storedFrom);

        if (messageShouldBeUpdated(msg, consultationRequest)) {
            updateConsultRequest(msg, consultationRequest);
        }

        consultationRequest.saveEntityOrAuditLogIfRequired(consultationRequestRepo, consultationRequestAuditRepo);
        questionController.processConsultationRequestQuestions(msg.getQuestions(), cRequest, msg.getRequestedDateTime(), storedFrom);
    }
}
