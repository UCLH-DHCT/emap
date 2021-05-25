package uk.ac.ucl.rits.inform.datasinks.emapstar.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.datasinks.emapstar.RowState;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.PatientStateAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.PatientStateRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.PatientStateTypeRepository;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.informdb.state.PatientState;
import uk.ac.ucl.rits.inform.informdb.state.PatientStateAudit;
import uk.ac.ucl.rits.inform.informdb.state.PatientStateType;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.PatientInfection;

import java.time.Instant;


/**
 * Interactions with patient states.
 * @author Anika Cawthorn
 */
@Component
public class PatientStateController {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final PatientStateRepository patientStateRepo;
    private final PatientStateTypeRepository patientStateTypeRepo;
    private final PatientStateAuditRepository patientStateAuditRepo;

    private enum PatientStateDataType {
        PATIENT_INFECTION
    }

    /**
     * Setting repositories holding information on patient states.
     * @param patientStateRepo      autowired PatientStateRepository
     * @param patientStateAuditRepo autowired PatientStateAuditRepository
     * @param patientStateTypeRepo  autowired PatientStateTypeRepository
     */
    public PatientStateController(
            PatientStateRepository patientStateRepo, PatientStateAuditRepository patientStateAuditRepo,
            PatientStateTypeRepository patientStateTypeRepo) {
        this.patientStateRepo = patientStateRepo;
        this.patientStateAuditRepo = patientStateAuditRepo;
        this.patientStateTypeRepo = patientStateTypeRepo;
    }

    /**
     * Get existing patient state type or create and save minimal patient state type.
     * @param msg        patient infection
     * @param storedFrom when patient infection information is stored from
     * @return PatientStateType
     */
    private PatientStateType getOrCreatePatientStateType(PatientInfection msg, Instant storedFrom) {
        return patientStateTypeRepo
                .findByDataTypeAndName(PatientStateDataType.PATIENT_INFECTION.toString(), msg.getInfection())
                .orElseGet(() -> createAndSaveNewType(msg, storedFrom));
    }

    /**
     * Create and save a minimal patient state type from patient infection message.
     * @param msg           patient infection
     * @param storedFrom    valid from in database
     * @return saved minimal PatientStateType
     */
    private PatientStateType createAndSaveNewType(PatientInfection msg, Instant storedFrom) {
        PatientStateType patientStateType = new PatientStateType(msg.getInfection(),
                PatientStateDataType.PATIENT_INFECTION.toString(),
                msg.getUpdatedDateTime(), storedFrom);

        logger.debug("Created new {}", patientStateType);

        patientStateTypeRepo.save(patientStateType);
        return patientStateTypeRepo.save(patientStateType);
    }

    /**
     * Process patient state message.
     * @param msg           message
     * @param mrn           patient id
     * @param storedFrom    valid from in database
     * @throws EmapOperationMessageProcessingException if message can't be processed.
     */
    @Transactional
    public void processMessage(final PatientInfection msg, Mrn mrn, final Instant storedFrom)
            throws EmapOperationMessageProcessingException {
        PatientStateType patientStateType = getOrCreatePatientStateType(msg, storedFrom);

        RowState<PatientState, PatientStateAudit> patientState = getOrCreatePatientState(msg, mrn, patientStateType,
                storedFrom);

        patientState.assignIfCurrentlyNullOrNewerAndDifferent(msg.getComment(),
                patientState.getEntity().getComment(), patientState.getEntity()::setComment,
                msg.getUpdatedDateTime(), patientState.getEntity().getStoredFrom());
        patientState.assignIfCurrentlyNullOrNewerAndDifferent(msg.getInfectionResolved(),
                patientState.getEntity().getResolutionDateTime(), patientState.getEntity()::setResolutionDateTime,
                msg.getUpdatedDateTime(), storedFrom);
        patientState.assignIfCurrentlyNullOrNewerAndDifferent(msg.getInfectionOnset(),
                patientState.getEntity().getOnsetDate(), patientState.getEntity()::setOnsetDate,
                msg.getUpdatedDateTime(), patientState.getEntity().getStoredFrom());

        if (messageShouldBeUpdated(msg, patientState)) {
            updatePatientState(msg, patientState);
        }

        patientState.saveEntityOrAuditLogIfRequired(patientStateRepo, patientStateAuditRepo);
    }

    /**
     * Get or create existing patient state entity.
     * @param msg               patient infection message
     * @param mrn               patient identifier
     * @param patientStateType  patient state type referred to in message
     * @param storedFrom        time that emap-core started processing the message
     * @return observation entity wrapped in RowState
     */
    private RowState<PatientState, PatientStateAudit> getOrCreatePatientState(
            PatientInfection msg, Mrn mrn, PatientStateType patientStateType, Instant storedFrom) {
        return patientStateRepo
                .findByMrnIdAndPatientStateTypeIdNameAndAddedDateTime(mrn, msg.getInfection(),
                        msg.getInfectionAdded())
                .map(obs -> new RowState<>(obs, msg.getUpdatedDateTime(), storedFrom, false))
                .orElseGet(() -> createMinimalPatientState(msg, mrn, patientStateType, storedFrom));
    }

    /**
     * Create minimal visit observation wrapped in RowState.
     * @param msg               patient infection message
     * @param mrn               patient identifier
     * @param patientStateType  patient state type referred to in message
     * @param storedFrom        time that emap-core started processing the message
     * @return minimal patient state wrapped in RowState
     */
    private RowState<PatientState, PatientStateAudit> createMinimalPatientState(
            PatientInfection msg, Mrn mrn, PatientStateType patientStateType, Instant storedFrom) {

        PatientState patientState = new PatientState(patientStateType, mrn, msg.getInfectionAdded());
        return new RowState<>(patientState, msg.getUpdatedDateTime(), storedFrom, true);
    }

    /**
     * Update message if observation has been created, or the message updated time is >= entity validFrom.
     * @param msg               patient infection
     * @param patientState      patient state
     * @return true if message should be updated
     */
    private boolean messageShouldBeUpdated(PatientInfection msg, RowState<PatientState,
            PatientStateAudit> patientState) {
        return patientState.isEntityCreated() || !msg.getUpdatedDateTime().isBefore(patientState.getEntity().getPatientStateTypeId().getValidFrom());
    }

    /**
     * Checks whether some of the relevant attributes in patient state are empty.
     * @param patientState patient state for which attributes are to be checked
     * @return true if any of the important attributes are empty, otherwise false
     */
    private boolean holdsEmptyData(PatientState patientState) {
        return patientState.getComment() == null && patientState.getStatus() == null;
    }

    /**
     * Update patient state from patient infection message.
     * @param msg               patient infection message
     * @param patientState      patient state referred to in message
     */
    private void updatePatientState(PatientInfection msg, RowState<PatientState, PatientStateAudit> patientState) {
        PatientState pState = patientState.getEntity();
        patientState.assignInterchangeValue(msg.getComment(), pState.getComment(), pState::setComment);
        patientState.assignInterchangeValue(msg.getStatus(), pState.getStatus(), pState::setStatus);
        patientState.assignInterchangeValue(msg.getInfectionResolved(), pState.getResolutionDateTime(),
                pState::setResolutionDateTime);
        patientState.assignInterchangeValue(msg.getInfectionOnset(), pState.getOnsetDate(), pState::setOnsetDate);
    }
}
