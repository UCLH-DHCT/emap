package uk.ac.ucl.rits.inform.datasinks.emapstar.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.datasinks.emapstar.RowState;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.ConsultationRequestRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.ConsultationRequestAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.ConsultationTypeRepository;
import uk.ac.ucl.rits.inform.informdb.consults.ConsultationRequestAudit;
import uk.ac.ucl.rits.inform.informdb.consults.ConsultationType;
import uk.ac.ucl.rits.inform.informdb.consults.ConsultationRequest;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.interchange.ConsultRequest;

import java.time.Instant;

/**
 * Functionality to create consultation requests for patients. A consultation request is initiated when specialist
 * advice on a patient's condition is requested through the treating physician.
 * @author Anika Cawthorn
 */
@Component
public class ConsultationRequestController {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ConsultationRequestRepository consultationRequestRepo;
    private final ConsultationTypeRepository consultationTypeRepo;
    private final ConsultationRequestAuditRepository consultationRequestAuditRepo;
    private final QuestionController questionController;

    /**
     * Setting repositories holding information on consultation requests.
     * @param consultationRequestRepo        Consultation request repo
     * @param consultationTypeRepo    Consultation request type repo
     * @param consultationRequestAuditRepo   Consultation request audit type repo
     * @param questionController             Question controller for questions in relation to consultation requests
     */
    public ConsultationRequestController(
            ConsultationRequestRepository consultationRequestRepo,
            ConsultationTypeRepository consultationTypeRepo,
            ConsultationRequestAuditRepository consultationRequestAuditRepo,
            QuestionController questionController) {
        this.consultationRequestRepo = consultationRequestRepo;
        this.consultationTypeRepo = consultationTypeRepo;
        this.consultationRequestAuditRepo = consultationRequestAuditRepo;
        this.questionController = questionController;
    }

    /**
     * Process consultation request message.
     * @param msg           Consultation request message
     * @param visit         Hospital visit this consultation request relates to.
     * @param storedFrom    valid from in database
     */
    @Transactional
    public void processMessage(final ConsultRequest msg, HospitalVisit visit, final Instant storedFrom) {
        ConsultationType consultationType = getOrCreateConsultationRequestType(msg, storedFrom);
        RowState<ConsultationRequest, ConsultationRequestAudit> consultationRequest = getOrCreateConsultationRequest(
                msg, visit, consultationType, storedFrom);
        ConsultationRequest request = consultationRequest.getEntity();

        if (messageShouldBeUpdated(msg, consultationRequest)) {
            updateConsultRequest(msg, consultationRequest);
        }

        consultationRequest.saveEntityOrAuditLogIfRequired(consultationRequestRepo, consultationRequestAuditRepo);
        questionController.processQuestions(msg.getQuestions(), consultationRequest.getEntity().getConsultId(),
                msg.getRequestedDateTime(), storedFrom);
    }

    /**
     * Check whether consultation type exists already. If it does, add it to consultation request; if not, create a new
     * consultation type from the information provided in the message.
     * @param msg        consultation request message
     * @param storedFrom when consultation request information is stored from
     * @return ConsultRequestType
     */
    @Cacheable(value = "ConsultationTypeCache", key = "ConsultationType")
    private ConsultationType getOrCreateConsultationRequestType(ConsultRequest msg, Instant storedFrom) {
        return consultationTypeRepo
                .findByCode(msg.getConsultationType())
                .orElseGet(() -> createAndSaveNewType(msg, storedFrom));
    }

    /**
     * Create and save a ConsultationType from the information contained in the ConsultRequest message.
     * @param msg           Consultation request message
     * @param storedFrom    Time that emap-core started processing the message
     * @return saved ConsultationType
     */
    private ConsultationType createAndSaveNewType(ConsultRequest msg, Instant storedFrom) {
        ConsultationType consultationType = new ConsultationType(msg.getConsultationType(),
                msg.getRequestedDateTime(), storedFrom);
        logger.debug("Created new {}", consultationType);
        return consultationTypeRepo.save(consultationType);
    }

    /**
     * Get or create existing ConsultationRequest entity.
     * @param msg                Consultation request message
     * @param visit              Hospital visit of patient this consultation request refers to.
     * @param consultationType   Consultancy type as identified in message
     * @param storedFrom         Time that emap-core started processing the message
     * @return ConsultationRequest entity wrapped in RowState
     */
    private RowState<ConsultationRequest, ConsultationRequestAudit> getOrCreateConsultationRequest(
            ConsultRequest msg, HospitalVisit visit, ConsultationType consultationType, Instant storedFrom) {
        return consultationRequestRepo
                .findByConsultId(msg.getEpicConsultId())
                .map(obs -> new RowState<>(obs, msg.getRequestedDateTime(), storedFrom, false))
                .orElseGet(() -> createMinimalConsultationRequest(msg, visit, consultationType, storedFrom));
    }

    /**
     * Create minimal consultation request wrapped in RowState.
     * @param msg                Consultation request message
     * @param visit              Hospital visit of the patient consultation request occurred at
     * @param consultationType   Consultation request type referred to in message
     * @param storedFrom         Time that emap-core started processing the message
     * @return minimal consultation request wrapped in RowState
     */
    private RowState<ConsultationRequest, ConsultationRequestAudit> createMinimalConsultationRequest(
            ConsultRequest msg, HospitalVisit visit, ConsultationType consultationType,
            Instant storedFrom) {
        ConsultationRequest consultationRequest = new ConsultationRequest(consultationType, visit,
                msg.getEpicConsultId());
        logger.debug("Created new {}", consultationRequest);
        return new RowState<>(consultationRequest, msg.getStatusChangeTime(), storedFrom, true);
    }

    /**
     * Decides whether or not message data held in the user data storage (accessed by researchers) needs to be updated
     * with the data held in the message that is processed.
     * @param msg                   Consultation request message
     * @param consultationRequest   Consultation request
     * @return true if message should be updated
     */
    private boolean messageShouldBeUpdated(ConsultRequest msg, RowState<ConsultationRequest,
            ConsultationRequestAudit> consultationRequest) {
        return (consultationRequest.isEntityCreated() || !msg.getStatusChangeTime().isBefore(
                consultationRequest.getEntity().getValidFrom()));
    }

    /**
     * Update consultation request from consultation request message.
     * @param msg                   consultation request message
     * @param requestState          consultation request referred to in message
     */
    private void updateConsultRequest(ConsultRequest msg, RowState<ConsultationRequest,
            ConsultationRequestAudit> requestState) {
        ConsultationRequest request = requestState.getEntity();

        requestState.assignIfDifferent(msg.getRequestedDateTime(), request.getRequestedDateTime(),
                request::setRequestedDateTime);
        requestState.assignIfDifferent(msg.getStatusChangeTime(), request.getStatusChangeTime(),
                request::setStatusChangeTime);
        requestState.assignInterchangeValue(msg.getNotes(), request.getComments(), request::setComments);
        requestState.assignIfDifferent(msg.isCancelled(), request.getCancelled(), request::setCancelled);
        requestState.assignIfDifferent(msg.isClosedDueToDischarge(), request.getClosedDueToDischarge(),
                request::setClosedDueToDischarge);
    }
}
