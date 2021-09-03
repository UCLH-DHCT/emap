package uk.ac.ucl.rits.inform.datasinks.emapstar.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.datasinks.emapstar.RowState;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.decisions.AdvancedDecisionAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.decisions.AdvancedDecisionRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.decisions.AdvancedDecisionTypeAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.decisions.AdvancedDecisionTypeRepository;
import uk.ac.ucl.rits.inform.informdb.decisions.AdvancedDecision;
import uk.ac.ucl.rits.inform.informdb.decisions.AdvancedDecisionAudit;
import uk.ac.ucl.rits.inform.informdb.decisions.AdvancedDecisionType;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.interchange.AdvancedDecisionMessage;

import java.time.Instant;


/**
 *
 */
@Component
public class AdvancedDecisionController {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final AdvancedDecisionRepository advancedDecisionRepo;
    private final AdvancedDecisionAuditRepository advancedDecisionAuditRepo;
    private final AdvancedDecisionTypeRepository advancedDecisionTypeRepo;
    private final AdvancedDecisionTypeAuditRepository advancedDecisionTypeAuditRepo;
    private final QuestionController questionController;

    /**
     * Setting repositories holding information on advanced decisions.
     * @param advancedDecisionRepo           Holds current advanced decisions recorded for hospital patients.
     * @param advancedDecisionAuditRepo      Holds historic information on advanced decisions recorded for patients.
     * @param advancedDecisionTypeRepo       Holds type information of advanced decisions.
     * @param advancedDecisionTypeAuditRepo  Repository holding historic changes to advanced decisions types.
     * @param questionController             Question controller for questions in relation to consultation requests
     */
    public AdvancedDecisionController(
            AdvancedDecisionRepository advancedDecisionRepo,
            AdvancedDecisionTypeRepository advancedDecisionTypeRepo,
            AdvancedDecisionAuditRepository advancedDecisionAuditRepo,
            AdvancedDecisionTypeAuditRepository advancedDecisionTypeAuditRepo,
            QuestionController questionController) {
        this.advancedDecisionRepo = advancedDecisionRepo;
        this.advancedDecisionAuditRepo = advancedDecisionAuditRepo;
        this.advancedDecisionTypeRepo = advancedDecisionTypeRepo;
        this.advancedDecisionTypeAuditRepo = advancedDecisionTypeAuditRepo;
        this.questionController = questionController;
    }

    /**
     * Process advanced decision message.
     * @param msg         Message containing information for advanced decision of a patient.
     * @param visit       Hospital visit of the patient this advanced decision was recorded for.
     * @param mrn         Patient identifier to whom advanced decision corresponds.
     * @param storedFrom  Time point when advanced decision was recorded first.
     */
    @Transactional
    public void processMessage(final AdvancedDecisionMessage msg, HospitalVisit visit, Mrn mrn,
                               final Instant storedFrom) {
        AdvancedDecisionType advancedDecisionType = getOrCreateAdvancedDecisionType(msg, storedFrom);
        RowState<AdvancedDecision, AdvancedDecisionAudit> advancedDecisionState = getOrCreateAdvancedDecision(
                msg, visit, mrn, advancedDecisionType, storedFrom);
        AdvancedDecision advancedDecision = advancedDecisionState.getEntity();

        if (messageShouldBeUpdated(msg, advancedDecisionState)) {
            updateConsultRequest(msg, advancedDecisionState);
        }

        advancedDecisionState.saveEntityOrAuditLogIfRequired(advancedDecisionRepo, advancedDecisionAuditRepo);
        questionController.processAdvancedDecisionQuestions(msg.getQuestions(), advancedDecision,
                msg.getRequestedDateTime(), storedFrom);
    }

    /**
     * Check whether advanced decision type exists already. If it does, add the existing type to the advanced decision;
     * if not, create a new advanced decision type from the information provided in the message.
     * @param msg        Advanced decision message.
     * @param storedFrom When information in relation to advanced decision type has been stored from
     * @return AdvancedDecisionType
     */
    private AdvancedDecisionType getOrCreateAdvancedDecisionType(AdvancedDecisionMessage msg, Instant storedFrom) {
        return advancedDecisionTypeRepo
                .findByCareCode(msg.getAdvancedDecisionType())
                .orElseGet(() -> createAndSaveNewType(msg, storedFrom));
    }

    /**
     * Create and save a new AdvancedDecisionType from the information contained in the AdvancedDecisionMessage.
     * @param msg           Advanced decision message.
     * @param storedFrom    Time that emap-core started processing this type of message.
     * @return saved AdvancedDecisionType
     */
    private AdvancedDecisionType createAndSaveNewType(AdvancedDecisionMessage msg, Instant storedFrom) {
        AdvancedDecisionType advancedDecisionType = new AdvancedDecisionType(msg.getAdvancedDecisionType(),
                msg.getAdvancedDecisionTypeName(), msg.getRequestedDateTime(), storedFrom);
        logger.debug("Created new {}", advancedDecisionType);
        return advancedDecisionTypeRepo.save(advancedDecisionType);
    }

    /**
     * Get existing or create new advanced decision.
     * @param msg                   Advanced decision message.
     * @param visit                 Hospital visit of patient this advanced decision message refers to.
     * @param mrn                   Patient this advanced decision is recorded for.
     * @param advancedDecisionType  Type of advanced decision recorded for patient.
     * @param storedFrom            Time that emap-core started processing this advanced decision message.
     * @return AdvancedDecision entity wrapped in RowState
     */
    private RowState<AdvancedDecision, AdvancedDecisionAudit> getOrCreateAdvancedDecision(
            AdvancedDecisionMessage msg, HospitalVisit visit, Mrn mrn, AdvancedDecisionType advancedDecisionType,
            Instant storedFrom) {
        return advancedDecisionRepo
                .findByAdvancedDecisionNumber(msg.getAdvancedDecisionId())
                .map(obs -> new RowState<>(obs, msg.getRequestedDateTime(), storedFrom, false))
                .orElseGet(() -> createMinimalAdvancedDecision(msg, visit, mrn, advancedDecisionType, storedFrom));
    }

    /**
     * Create minimal advanced decision wrapped in RowState.
     * @param msg                   Advanced decision message
     * @param visit                 Hospital visit of the patient advanced decision was recorded for.
     * @param mrn                   Identifier of patient the advanced decision has been recorded for.
     * @param advancedDecisionType  Type of advanced decision recorded for patient.
     * @param storedFrom         Time that emap-core started processing the advanced decision of that patient.
     * @return minimal advanced decision wrapped in RowState
     */
    private RowState<AdvancedDecision, AdvancedDecisionAudit> createMinimalAdvancedDecision(
            AdvancedDecisionMessage msg, HospitalVisit visit, Mrn mrn, AdvancedDecisionType advancedDecisionType,
            Instant storedFrom) {
        AdvancedDecision advancedDecision = new AdvancedDecision(advancedDecisionType, visit, mrn,
                msg.getAdvancedDecisionId());
        logger.debug("Created new {}", advancedDecision);
        return new RowState<>(advancedDecision, msg.getStatusChangeTime(), storedFrom, true);
    }

    /**
     * Decides whether or not the data held for a specific advanced decision needs to be updated or not.
     * @param msg                     Advanced decision message.
     * @param advancedDecisionState   State of advanced decision created from message.
     * @return true if message should be updated
     */
    private boolean messageShouldBeUpdated(AdvancedDecisionMessage msg, RowState<AdvancedDecision,
            AdvancedDecisionAudit> advancedDecisionState) {
        return (advancedDecisionState.isEntityCreated() || !msg.getStatusChangeTime().isBefore(
                advancedDecisionState.getEntity().getValidFrom()));
    }

    /**
     * Update advanced decision data with information from advanced decision message.
     * @param msg                       Advanced decision message.
     * @param advancedDecisionState     Advanced decision referred to in message
     */
    private void updateConsultRequest(AdvancedDecisionMessage msg, RowState<AdvancedDecision,
            AdvancedDecisionAudit> advancedDecisionState) {
        AdvancedDecision advancedDecision = advancedDecisionState.getEntity();

        advancedDecisionState.assignIfDifferent(msg.getRequestedDateTime(), advancedDecision.getRequestedDateTime(),
                advancedDecision::setRequestedDateTime);
        advancedDecisionState.assignIfDifferent(msg.getStatusChangeTime(), advancedDecision.getStatusChangeTime(),
                advancedDecision::setStatusChangeTime);
        advancedDecisionState.assignIfDifferent(msg.isCancelled(), advancedDecision.getCancelled(),
                advancedDecision::setCancelled);
        advancedDecisionState.assignIfDifferent(msg.isClosedDueToDischarge(),
                advancedDecision.getClosedDueToDischarge(),
                advancedDecision::setClosedDueToDischarge);
    }
}
