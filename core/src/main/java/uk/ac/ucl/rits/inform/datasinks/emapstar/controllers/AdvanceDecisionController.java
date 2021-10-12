package uk.ac.ucl.rits.inform.datasinks.emapstar.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.datasinks.emapstar.RowState;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.decisions.AdvanceDecisionAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.decisions.AdvanceDecisionRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.decisions.AdvanceDecisionTypeRepository;
import uk.ac.ucl.rits.inform.informdb.decisions.AdvanceDecision;
import uk.ac.ucl.rits.inform.informdb.decisions.AdvanceDecisionType;
import uk.ac.ucl.rits.inform.informdb.decisions.AdvanceDecisionAudit;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.interchange.AdvanceDecisionMessage;

import java.time.Instant;


/**
 * Controller required to process internal advance decision messages and write data into UDS.
 * @author Anika Cawthorn
 */
@Component
public class AdvanceDecisionController {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final AdvanceDecisionRepository advanceDecisionRepo;
    private final AdvanceDecisionAuditRepository advanceDecisionAuditRepo;
    private final AdvanceDecisionTypeRepository advanceDecisionTypeRepo;
    private final QuestionController questionController;

    /**
     * Setting repositories that enable searching for components of advanced decisions.
     * @param advanceDecisionRepo      Repository with search functionality for advanced decisions.
     * @param advanceDecisionTypeRepo  Repository with search functionality for advanced decisions types.
     * @param advanceDecisionAuditRepo Repository with search functionality for advanced decision audit data.
     * @param questionController        Controller for handling questions attached to advanced decisions.
     */
    public AdvanceDecisionController(
            AdvanceDecisionRepository advanceDecisionRepo,
            AdvanceDecisionTypeRepository advanceDecisionTypeRepo,
            AdvanceDecisionAuditRepository advanceDecisionAuditRepo,
            QuestionController questionController) {
        this.advanceDecisionRepo = advanceDecisionRepo;
        this.advanceDecisionAuditRepo = advanceDecisionAuditRepo;
        this.advanceDecisionTypeRepo = advanceDecisionTypeRepo;
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
    public void processMessage(final AdvanceDecisionMessage msg, HospitalVisit visit, Mrn mrn,
                               final Instant storedFrom) {
        AdvanceDecisionType advanceDecisionType = getOrCreateAdvancedDecisionType(msg, storedFrom);
        RowState<AdvanceDecision, AdvanceDecisionAudit> advanceDecisionState = getOrCreateAdvancedDecision(
                msg, visit, mrn, advanceDecisionType, storedFrom);

        if (messageShouldBeUpdated(msg.getStatusChangeDatetime(), advanceDecisionState)) {
            updateConsultRequest(msg, advanceDecisionState);
        }

        advanceDecisionState.saveEntityOrAuditLogIfRequired(advanceDecisionRepo, advanceDecisionAuditRepo);
        questionController.processQuestions(msg.getQuestions(), ParentTableType.ADVANCE_DECISION.toString(),
                advanceDecisionState.getEntity().getAdvanceDecisionId(), msg.getRequestedDatetime(), storedFrom);
    }

    /**
     * Check whether advanced decision type exists already. If it does, add the existing type to the advanced decision;
     * if not, create a new advance decision type from the information provided in the message.
     * @param msg        Advance decision message.
     * @param storedFrom When information in relation to advanced decision type has been stored from
     * @return AdvancedDecisionType
     */
    @Cacheable(value = "advanceDecisionType", key = "#msg.advanceCareCode")
    public AdvanceDecisionType getOrCreateAdvancedDecisionType(AdvanceDecisionMessage msg, Instant storedFrom) {
        return advanceDecisionTypeRepo
                .findByCareCode(msg.getAdvanceCareCode())
                .orElseGet(() -> createAndSaveNewType(msg, storedFrom));
    }

    /**
     * Create and save a new AdvanceDecisionType from the information contained in the AdvancedDecisionMessage.
     * @param msg           Advance decision message.
     * @param storedFrom    Time that emap-core started processing this type of message.
     * @return saved AdvancedDecisionType
     */
    private AdvanceDecisionType createAndSaveNewType(AdvanceDecisionMessage msg, Instant storedFrom) {
        AdvanceDecisionType advanceDecisionType = new AdvanceDecisionType(msg.getAdvanceCareCode(), msg.getAdvanceDecisionTypeName());
        logger.debug("Created new {}", advanceDecisionType);
        return advanceDecisionTypeRepo.save(advanceDecisionType);
    }

    /**
     * Get existing or create new advance decision.
     * @param msg                   Advance decision message.
     * @param visit                 Hospital visit of patient this advanced decision message refers to.
     * @param mrn                   Patient this advanced decision is recorded for.
     * @param advanceDecisionType  Type of advanced decision recorded for patient.
     * @param storedFrom            Time that emap-core started processing this advanced decision message.
     * @return AdvancedDecision entity wrapped in RowState
     */
    private RowState<AdvanceDecision, AdvanceDecisionAudit> getOrCreateAdvancedDecision(
            AdvanceDecisionMessage msg, HospitalVisit visit, Mrn mrn, AdvanceDecisionType advanceDecisionType,
            Instant storedFrom) {
        return advanceDecisionRepo
                .findByInternalId(msg.getAdvanceDecisionNumber())
                .map(obs -> new RowState<>(obs, msg.getStatusChangeDatetime(), storedFrom, false))
                .orElseGet(() -> createMinimalAdvanceDecision(msg, visit, mrn, advanceDecisionType, storedFrom));
    }

    /**
     * Create minimal advance decision wrapped in RowState.
     * @param msg                   Advance decision message
     * @param visit                 Hospital visit of the patient advanced decision was recorded for.
     * @param mrn                   Identifier of patient the advanced decision has been recorded for.
     * @param advanceDecisionType  Type of advanced decision recorded for patient.
     * @param storedFrom         Time that emap-core started processing the advanced decision of that patient.
     * @return minimal advanced decision wrapped in RowState
     */
    private RowState<AdvanceDecision, AdvanceDecisionAudit> createMinimalAdvanceDecision(
            AdvanceDecisionMessage msg, HospitalVisit visit, Mrn mrn, AdvanceDecisionType advanceDecisionType,
            Instant storedFrom) {
        AdvanceDecision advanceDecision = new AdvanceDecision(advanceDecisionType, visit, mrn,
                msg.getAdvanceDecisionNumber());
        logger.debug("Created new {}", advanceDecision);
        return new RowState<>(advanceDecision, msg.getStatusChangeDatetime(), storedFrom, true);
    }

    /**
     * Decides whether or not the data held for a specific advance decision needs to be updated or not.
     * @param statusChangeDatetime    Datetime of AdvanceDecisionMessage that's currently processed.
     * @param advancedDecisionState   State of advance decision created from message.
     * @return true if message should be updated
     */
    private boolean messageShouldBeUpdated(Instant statusChangeDatetime, RowState<AdvanceDecision,
            AdvanceDecisionAudit> advancedDecisionState) {
        return (advancedDecisionState.isEntityCreated() || !statusChangeDatetime.isBefore(
                advancedDecisionState.getEntity().getValidFrom()));
    }

    /**
     * Update advance decision data with information from AdvanceDecisionMessage.
     * @param msg                   Advance decision message.
     * @param advanceDecisionState  Advance decision referred to in message
     */
    private void updateConsultRequest(AdvanceDecisionMessage msg, RowState<AdvanceDecision,
            AdvanceDecisionAudit> advanceDecisionState) {
        AdvanceDecision advanceDecision = advanceDecisionState.getEntity();

        advanceDecisionState.assignIfDifferent(msg.getRequestedDatetime(), advanceDecision.getRequestedDatetime(),
                advanceDecision::setRequestedDatetime);
        advanceDecisionState.assignIfDifferent(msg.getStatusChangeDatetime(), advanceDecision.getStatusChangeDatetime(),
                advanceDecision::setStatusChangeDatetime);
        advanceDecisionState.assignIfDifferent(msg.isCancelled(), advanceDecision.getCancelled(),
                advanceDecision::setCancelled);
        advanceDecisionState.assignIfDifferent(msg.isClosedDueToDischarge(),
                advanceDecision.getClosedDueToDischarge(),
                advanceDecision::setClosedDueToDischarge);
    }
}
