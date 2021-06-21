package uk.ac.ucl.rits.inform.datasinks.emapstar.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.datasinks.emapstar.RowState;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.ConditionTypeRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.PatientConditionAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.PatientConditionRepository;
import uk.ac.ucl.rits.inform.informdb.conditions.ConditionType;
import uk.ac.ucl.rits.inform.informdb.conditions.PatientCondition;
import uk.ac.ucl.rits.inform.informdb.conditions.PatientConditionAudit;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.PatientInfection;

import java.time.Instant;


/**
 * Interactions with patient conditions.
 * @author Anika Cawthorn
 * @author Stef Piatek
 */
@Component
public class PatientConditionController {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final PatientConditionRepository patientConditionRepo;
    private final ConditionTypeRepository conditionTypeRepo;
    private final PatientConditionAuditRepository patientConditionAuditRepo;

    private enum PatientConditionType {
        PATIENT_INFECTION
    }

    /**
     * Setting repositories holding information on patient conditions.
     * @param patientConditionRepo      autowired PatientStateRepository
     * @param patientConditionAuditRepo autowired PatientStateAuditRepository
     * @param conditionTypeRepo         autowired PatientStateTypeRepository
     */
    public PatientConditionController(
            PatientConditionRepository patientConditionRepo, PatientConditionAuditRepository patientConditionAuditRepo,
            ConditionTypeRepository conditionTypeRepo) {
        this.patientConditionRepo = patientConditionRepo;
        this.patientConditionAuditRepo = patientConditionAuditRepo;
        this.conditionTypeRepo = conditionTypeRepo;
    }

    /**
     * Get existing condition type or create and save minimal condition type.
     * @param type            Condition Type
     * @param typeName        name of the individual condition within the type
     * @param updatedDateTime when the condition information is valid from
     * @param storedFrom      when patient infection information is stored from
     * @return PatientStateType
     */
    @Cacheable(value = "conditionType", key = "{#dataType, #typeName}")
    public ConditionType getOrCreatePatientStateType(PatientConditionType type, String typeName, Instant updatedDateTime, Instant storedFrom) {
        return conditionTypeRepo
                .findByDataTypeAndName(type.toString(), typeName)
                .orElseGet(() -> {
                    ConditionType conditionType = new ConditionType(type.toString(), typeName, updatedDateTime, storedFrom);
                    logger.debug("Created new {}", conditionType);
                    return conditionTypeRepo.save(conditionType);
                });
    }

    /**
     * Process patient state message.
     * @param msg        message
     * @param mrn        patient id
     * @param storedFrom valid from in database
     * @throws EmapOperationMessageProcessingException if message can't be processed.
     */
    @Transactional
    public void processMessage(final PatientInfection msg, Mrn mrn, final Instant storedFrom)
            throws EmapOperationMessageProcessingException {
        ConditionType conditionType = getOrCreatePatientStateType(
                PatientConditionType.PATIENT_INFECTION, msg.getInfection(), msg.getUpdatedDateTime(), storedFrom);
        // should delete all messages which are valid from previous message if the source is from hoover
        RowState<PatientCondition, PatientConditionAudit> patientState = getOrCreatePatientState(msg, mrn, conditionType,
                storedFrom);
        patientState.assignIfCurrentlyNullOrNewerAndDifferent(msg.getComment(),
                patientState.getEntity().getComment(), patientState.getEntity()::setComment,
                msg.getUpdatedDateTime(), storedFrom);
        patientState.assignIfCurrentlyNullOrNewerAndDifferent(msg.getInfectionResolved(),
                patientState.getEntity().getResolutionDateTime(), patientState.getEntity()::setResolutionDateTime,
                msg.getUpdatedDateTime(), storedFrom);
        patientState.assignIfCurrentlyNullOrNewerAndDifferent(msg.getInfectionOnset(),
                patientState.getEntity().getOnsetDate(), patientState.getEntity()::setOnsetDate,
                msg.getUpdatedDateTime(), storedFrom);

        if (messageShouldBeUpdated(msg, patientState)) {
            updatePatientCondition(msg, patientState);
        }

        patientState.saveEntityOrAuditLogIfRequired(patientConditionRepo, patientConditionAuditRepo);
    }

    /**
     * Get or create existing patient state entity.
     * @param msg           patient infection message
     * @param mrn           patient identifier
     * @param conditionType patient state type referred to in message
     * @param storedFrom    time that emap-core started processing the message
     * @return observation entity wrapped in RowState
     */
    private RowState<PatientCondition, PatientConditionAudit> getOrCreatePatientState(
            PatientInfection msg, Mrn mrn, ConditionType conditionType, Instant storedFrom) {
        return patientConditionRepo
                .findByMrnIdAndConditionTypeIdAndAddedDateTime(mrn, conditionType,
                        msg.getInfectionAdded())
                .map(obs -> new RowState<>(obs, msg.getUpdatedDateTime(), storedFrom, false))
                .orElseGet(() -> createMinimalPatientState(mrn, conditionType, msg.getInfectionAdded(), msg.getUpdatedDateTime(), storedFrom));
    }

    /**
     * Create minimal visit observation wrapped in RowState.
     * @param mrn            patient identifier
     * @param conditionType  condition type
     * @param conditionAdded condition added at
     * @param validFrom      hospital time that the data is true from
     * @param storedFrom     time that emap-core started processing the message
     * @return minimal patient state wrapped in RowState
     */
    private RowState<PatientCondition, PatientConditionAudit> createMinimalPatientState(
            Mrn mrn, ConditionType conditionType, Instant conditionAdded, Instant validFrom, Instant storedFrom) {

        PatientCondition patientCondition = new PatientCondition(conditionType, mrn, conditionAdded);
        return new RowState<>(patientCondition, validFrom, storedFrom, true);
    }

    /**
     * Update message if observation has been created, or the message updated time is >= entity validFrom.
     * @param msg           patient infection
     * @param conditionDate row state of condition
     * @return true if message should be updated
     */
    private boolean messageShouldBeUpdated(PatientInfection msg, RowState<PatientCondition,
            PatientConditionAudit> conditionDate) {
        return conditionDate.isEntityCreated() || !msg.getUpdatedDateTime().isBefore(conditionDate.getEntity().getConditionTypeId().getValidFrom());
    }

    /**
     * Update patient state from patient infection message.
     * @param msg          patient infection message
     * @param patientState patient condition entity to update
     */
    private void updatePatientCondition(PatientInfection msg, RowState<PatientCondition, PatientConditionAudit> patientState) {
        PatientCondition state = patientState.getEntity();
        patientState.assignInterchangeValue(msg.getComment(), state.getComment(), state::setComment);
        patientState.assignInterchangeValue(msg.getStatus(), state.getStatus(), state::setStatus);
        patientState.assignInterchangeValue(msg.getInfectionResolved(), state.getResolutionDateTime(), state::setResolutionDateTime);
        patientState.assignInterchangeValue(msg.getInfectionOnset(), state.getOnsetDate(), state::setOnsetDate);
    }
}
