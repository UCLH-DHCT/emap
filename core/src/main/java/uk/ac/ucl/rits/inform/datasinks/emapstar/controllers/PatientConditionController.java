package uk.ac.ucl.rits.inform.datasinks.emapstar.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.datasinks.emapstar.RowState;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.RequiredDataMissingException;
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
import java.util.List;
import java.util.Optional;


/**
 * Parses patient conditions from interchange messages.
 * <p>
 * Currently planned to deal with patient infections and problem lists but should parse any condition that can have a start and end.
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
     * @param patientConditionRepo      autowired PatientConditionRepository
     * @param patientConditionAuditRepo autowired PatientConditionAuditRepository
     * @param conditionTypeRepo         autowired ConditionTypeRepository
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
     * @param conditionCode   EPIC code for the condition within the type
     * @param updatedDateTime when the condition information is valid from
     * @param storedFrom      when patient infection information is stored from
     * @return ConditionType
     */
    @Cacheable(value = "conditionType", key = "{#type, #conditionCode}")
    public ConditionType getOrCreateConditionType(
            PatientConditionType type, String conditionCode, Instant updatedDateTime, Instant storedFrom) {
        return conditionTypeRepo
                .findByDataTypeAndInternalCode(type.toString(), conditionCode)
                .orElseGet(() -> {
                    ConditionType conditionType = new ConditionType(type.toString(), conditionCode, updatedDateTime, storedFrom);
                    logger.debug("Created new {}", conditionType);
                    return conditionTypeRepo.save(conditionType);
                });
    }

    /**
     * Persist condition type with new name and remove this entry from the cache.
     * @param typeToUpdate  condition type entity to updated
     * @param type          type of condition, used as the key for the cache
     * @param conditionCode used as the key for the cache
     * @param name          updated name to add
     */
    @CacheEvict(value = "conditionType", key = "{#type, #conditionCode}")
    public void updateNameAndClearFromCache(ConditionType typeToUpdate, String name, PatientConditionType type, String conditionCode) {
        logger.trace("Adding name '{}' to {}", name, typeToUpdate);
        typeToUpdate.setName(name);
        conditionTypeRepo.save(typeToUpdate);
    }

    /**
     * Process patient condition message.
     * @param msg        message
     * @param mrn        patient id
     * @param storedFrom valid from in database
     * @throws EmapOperationMessageProcessingException if message can't be processed.
     */
    @Transactional
    public void processMessage(final PatientInfection msg, Mrn mrn, final Instant storedFrom)
            throws EmapOperationMessageProcessingException {
        ConditionType conditionType = getOrCreateConditionType(
                PatientConditionType.PATIENT_INFECTION, msg.getInfectionCode(), msg.getUpdatedDateTime(), storedFrom);

        updateConditionTypeNameIfDifferent(msg, conditionType);
        deletePreviousInfectionTypesOrClearCache(msg, storedFrom);

        RowState<PatientCondition, PatientConditionAudit> patientCondition = getOrCreatePatientCondition(msg, mrn, conditionType, storedFrom);

        if (messageShouldBeUpdated(msg, patientCondition)) {
            updatePatientCondition(msg, patientCondition);
        }

        patientCondition.saveEntityOrAuditLogIfRequired(patientConditionRepo, patientConditionAuditRepo);
    }

    /**
     * HL7 only has infection code, so update these if there is a known name and it's different.
     * Don't need to check for messages being newer because messages from hoover are ordered.
     * @param msg           patient infection message
     * @param conditionType condition type to update
     */
    private void updateConditionTypeNameIfDifferent(PatientInfection msg, ConditionType conditionType) {
        if (msg.getInfectionName().isUnknown()) {
            return;
        }
        String infectionName = msg.getInfectionName().get();
        if (!infectionName.equals(conditionType.getName())) {
            updateNameAndClearFromCache(conditionType, infectionName, PatientConditionType.PATIENT_INFECTION, msg.getInfectionCode());
        }
    }

    /**
     * We can't trust patient infections from HL7 as no ID, so delete these if the infection ID is known.
     * If processing HL7 then clear the cache of infection types so that new types will be updated if added from hl7.
     * @param msg         patient infection message
     * @param deleteUntil time to delete messages up until (inclusive)
     */
    private void deletePreviousInfectionTypesOrClearCache(PatientInfection msg, Instant deleteUntil) {
        if (msg.getEpicInfectionId().isSave()) {
            logger.debug("Deleting all infections up to {}", msg.getUpdatedDateTime());
            List<ConditionType> hl7InfectionTypes = getAllInfectionTypesAndCacheResults();
            auditAndDeletePatientConditionsUntil(hl7InfectionTypes, msg.getUpdatedDateTime(), deleteUntil);
        } else {
            clearCacheOfInfectionTypes();
        }
    }

    /**
     * Get all patient infection condition types or get form cache if it hasn't been cleared.
     * @return all patient infection condition types
     */
    @Cacheable(value = "infectionTypes")
    public List<ConditionType> getAllInfectionTypesAndCacheResults() {
        return conditionTypeRepo.findAllByDataType(PatientConditionType.PATIENT_INFECTION.toString());
    }

    /**
     * Audit and delete all patient conditions that are of the condition types and are valid until the delete until date.
     * @param hl7InfectionTypes patient infection condition types to delete
     * @param deleteUntil       instant to delete until
     * @param storedFrom        time that star started processing the message
     */
    private void auditAndDeletePatientConditionsUntil(List<ConditionType> hl7InfectionTypes, Instant deleteUntil, Instant storedFrom) {
        List<PatientCondition> hl7Infections = patientConditionRepo
                .findAllByValidFromLessThanEqualAndInternalIdIsNullAndConditionTypeIdIn(deleteUntil, hl7InfectionTypes);
        for (PatientCondition hl7Infection : hl7Infections) {
            logger.debug("Deleting {}", hl7Infection);
            PatientConditionAudit hl7InfectionAudit = hl7Infection.createAuditEntity(deleteUntil, storedFrom);
            patientConditionAuditRepo.save(hl7InfectionAudit);
            patientConditionRepo.delete(hl7Infection);
        }
    }

    /**
     * Clear cache of patient infection condition types.
     */
    @CacheEvict(value = "infectionTypes", allEntries = true)
    public void clearCacheOfInfectionTypes() {
        return;
    }

    /**
     * Get or create existing patient condition entity.
     * @param msg           patient infection message
     * @param mrn           patient identifier
     * @param conditionType condition type referred to in message
     * @param storedFrom    time that emap-core started processing the message
     * @return observation entity wrapped in RowState
     * @throws RequiredDataMissingException if no patient infection Id in hoover data or unrecognised source system
     */
    private RowState<PatientCondition, PatientConditionAudit> getOrCreatePatientCondition(
            PatientInfection msg, Mrn mrn, ConditionType conditionType, Instant storedFrom) throws RequiredDataMissingException {
        Optional<PatientCondition> patientCondition;
        final Long epicInfectionId;
        switch (msg.getSourceSystem()) {
            case "EPIC":
                epicInfectionId = null;
                patientCondition = patientConditionRepo
                        .findByMrnIdAndConditionTypeIdAndAddedDateTime(mrn, conditionType, msg.getInfectionAdded());
                break;
            case "hoover":
                if (msg.getEpicInfectionId().isUnknown()) {
                    throw new RequiredDataMissingException("No patientInfectionId from hoover");
                }
                epicInfectionId = msg.getEpicInfectionId().get();
                patientCondition = patientConditionRepo.findByConditionTypeIdAndInternalId(conditionType, epicInfectionId);
                break;
            default:
                throw new RequiredDataMissingException(String.format("'%s' is not a recognised source system", msg.getSourceSystem()));
        }

        return patientCondition
                .map(obs -> new RowState<>(obs, msg.getUpdatedDateTime(), storedFrom, false))
                .orElseGet(() -> createMinimalPatientCondition(
                        epicInfectionId, mrn, conditionType, msg.getInfectionAdded(), msg.getUpdatedDateTime(), storedFrom));
    }

    /**
     * Create minimal patient condition wrapped in RowState.
     * @param epicConditionId internal epic Id for condition
     * @param mrn             patient identifier
     * @param conditionType   condition type
     * @param conditionAdded  condition added at
     * @param validFrom       hospital time that the data is true from
     * @param storedFrom      time that emap-core started processing the message
     * @return minimal patient condition wrapped in RowState
     */
    private RowState<PatientCondition, PatientConditionAudit> createMinimalPatientCondition(
            Long epicConditionId, Mrn mrn, ConditionType conditionType, Instant conditionAdded, Instant validFrom, Instant storedFrom) {

        PatientCondition patientCondition = new PatientCondition(epicConditionId, conditionType, mrn, conditionAdded);
        return new RowState<>(patientCondition, validFrom, storedFrom, true);
    }

    /**
     * Update message if observation has been created, or the message updated time is >= entity validFrom.
     * @param msg           patient infection
     * @param conditionDate row state of condition
     * @return true if message should be updated
     */
    private boolean messageShouldBeUpdated(PatientInfection msg, RowState<PatientCondition, PatientConditionAudit> conditionDate) {
        return conditionDate.isEntityCreated() || !msg.getUpdatedDateTime().isBefore(conditionDate.getEntity().getConditionTypeId().getValidFrom());
    }

    /**
     * Update patient condition from patient infection message.
     * @param msg            patient infection message
     * @param conditionState patient condition entity to update
     */
    private void updatePatientCondition(PatientInfection msg, RowState<PatientCondition, PatientConditionAudit> conditionState) {
        PatientCondition condition = conditionState.getEntity();
        conditionState.assignInterchangeValue(msg.getComment(), condition.getComment(), condition::setComment);
        conditionState.assignInterchangeValue(msg.getStatus(), condition.getStatus(), condition::setStatus);
        conditionState.assignInterchangeValue(msg.getInfectionResolved(), condition.getResolutionDateTime(), condition::setResolutionDateTime);
        conditionState.assignInterchangeValue(msg.getInfectionOnset(), condition.getOnsetDate(), condition::setOnsetDate);
    }
}
