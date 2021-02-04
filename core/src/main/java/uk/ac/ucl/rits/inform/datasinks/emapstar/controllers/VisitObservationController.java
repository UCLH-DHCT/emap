package uk.ac.ucl.rits.inform.datasinks.emapstar.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.datasinks.emapstar.RowState;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.RequiredDataMissingException;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.VisitObservationAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.VisitObservationRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.VisitObservationTypeRepository;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.visit_recordings.VisitObservation;
import uk.ac.ucl.rits.inform.informdb.visit_recordings.VisitObservationAudit;
import uk.ac.ucl.rits.inform.informdb.visit_recordings.VisitObservationType;
import uk.ac.ucl.rits.inform.interchange.Flowsheet;

import java.time.Instant;

/**
 * Interactions with observation visits.
 * @author Stef Piatek
 */
@Component
public class VisitObservationController {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final VisitObservationRepository visitObservationRepo;
    private final VisitObservationAuditRepository visitObservationAuditRepo;
    private final VisitObservationTypeRepository visitObservationTypeRepo;

    /**
     * @param visitObservationRepo      autowired VisitObservationRepository
     * @param visitObservationAuditRepo autowired VisitObservationAuditRepository
     * @param visitObservationTypeRepo  autowired VisitObservationTypeRepository
     */
    public VisitObservationController(
            VisitObservationRepository visitObservationRepo, VisitObservationAuditRepository visitObservationAuditRepo,
            VisitObservationTypeRepository visitObservationTypeRepo) {
        this.visitObservationRepo = visitObservationRepo;
        this.visitObservationAuditRepo = visitObservationAuditRepo;
        this.visitObservationTypeRepo = visitObservationTypeRepo;
    }

    /**
     * Create, update or delete a flowsheet.
     * Will also create a new VisitObservationType if it doesn't already exist.
     * @param msg        flowsheet
     * @param visit      hospital visit
     * @param storedFrom time that emap-core started processing the message
     * @throws RequiredDataMissingException if isNumericType is not set
     */
    @Transactional
    public void processFlowsheet(Flowsheet msg, HospitalVisit visit, Instant storedFrom) throws RequiredDataMissingException {
        if (msg.getIsNumericType() == null) {
            throw new RequiredDataMissingException("Flowsheet isNumericType not set");
        }

        VisitObservationType observationType = getOrCreateObservationType(msg);
        RowState<VisitObservation, VisitObservationAudit> flowsheetState = getOrCreateFlowsheet(msg, visit, observationType, storedFrom);
        if (messageShouldBeUpdated(msg, flowsheetState)) {
            updateVisitObservation(msg, flowsheetState);
            flowsheetState.saveEntityOrAuditLogIfRequired(visitObservationRepo, visitObservationAuditRepo);
        }
    }

    /**
     * Get existing observation type or create and save minimal observation type.
     * @param msg flowsheet
     * @return VisitObservationType
     */
    private VisitObservationType getOrCreateObservationType(Flowsheet msg) {
        return visitObservationTypeRepo
                .findByIdInApplicationAndSourceSystemAndSourceApplication(msg.getFlowsheetId(), msg.getSourceSystem(), msg.getSourceApplication())
                .orElseGet(() -> createAndSaveNewType(msg));
    }

    /**
     * Create and save a minimal visit observation type.
     * @param msg flowsheet
     * @return saved minimal VisitObservationType
     */
    private VisitObservationType createAndSaveNewType(Flowsheet msg) {
        VisitObservationType type = new VisitObservationType(msg.getFlowsheetId(), msg.getSourceSystem(), msg.getSourceApplication());
        logger.debug(String.format("Created new %s", type));
        return visitObservationTypeRepo.save(type);
    }

    /**
     * Get or create existing observation entity.
     * @param msg             flowsheet
     * @param visit           hospital visit
     * @param observationType visit observation type
     * @param storedFrom      time that emap-core started processing the message
     * @return observation entity wrapped in RowState
     */
    private RowState<VisitObservation, VisitObservationAudit> getOrCreateFlowsheet(
            Flowsheet msg, HospitalVisit visit, VisitObservationType observationType, Instant storedFrom) {
        return visitObservationRepo
                .findByHospitalVisitIdAndVisitObservationTypeIdAndObservationDatetime(visit, observationType, msg.getObservationTime())
                .map(obs -> new RowState<>(obs, msg.getUpdatedTime(), storedFrom, false))
                .orElseGet(() -> createMinimalFlowsheetState(msg, visit, observationType, storedFrom));
    }

    /**
     * Create minimal visit observation wrapped in RowState.
     * @param msg             flowsheet
     * @param visit           hospital visit
     * @param observationType visit observation type
     * @param storedFrom      time that emap-core started processing the message
     * @return minimal observation entity wrapped in RowState
     */
    private RowState<VisitObservation, VisitObservationAudit> createMinimalFlowsheetState(
            Flowsheet msg, HospitalVisit visit, VisitObservationType observationType, Instant storedFrom) {
        VisitObservation obs = new VisitObservation(visit, observationType, msg.getObservationTime(), msg.getUpdatedTime(), storedFrom);
        return new RowState<>(obs, msg.getUpdatedTime(), storedFrom, true);
    }

    /**
     * Update message if observation has been created, or the message updated time is >= entity validFrom.
     * @param msg              flowsheet
     * @param observationState observation entity wrapped in RowState
     * @return true if message should be updated
     */
    private boolean messageShouldBeUpdated(Flowsheet msg, RowState<VisitObservation, VisitObservationAudit> observationState) {
        return observationState.isEntityCreated() || !msg.getUpdatedTime().isBefore(observationState.getEntity().getValidFrom());
    }

    /**
     * Update observation state from Flowsheet message.
     * @param msg              flowsheet
     * @param observationState observation entity wrapped in RowState
     */
    private void updateVisitObservation(Flowsheet msg, RowState<VisitObservation, VisitObservationAudit> observationState) {
        VisitObservation observation = observationState.getEntity();
        if (msg.getIsNumericType()) {
            observationState.assignInterchangeValue(msg.getNumericValue(), observation.getValueAsReal(), observation::setValueAsReal);
        } else {
            observationState.assignInterchangeValue(msg.getStringValue(), observation.getValueAsText(), observation::setValueAsText);
        }
        observationState.assignInterchangeValue(msg.getUnit(), observation.getUnit(), observation::setUnit);
        observationState.assignInterchangeValue(msg.getComment(), observation.getComment(), observation::setComment);
    }

}

