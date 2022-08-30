package uk.ac.ucl.rits.inform.datasinks.emapstar.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import uk.ac.ucl.rits.inform.datasinks.emapstar.RowState;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.RequiredDataMissingException;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.ConditionVisitLinkRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.conditions.PatientConditionRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.conditions.PatientConditionAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.conditions.ConditionTypeRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.conditions.ConditionTypeAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.conditions.AllergenReactionRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.conditions.AllergenReactionAuditRepository;
import uk.ac.ucl.rits.inform.informdb.conditions.ConditionType;
import uk.ac.ucl.rits.inform.informdb.conditions.AllergenReaction;
import uk.ac.ucl.rits.inform.informdb.conditions.ConditionTypeAudit;
import uk.ac.ucl.rits.inform.informdb.conditions.PatientCondition;
import uk.ac.ucl.rits.inform.informdb.conditions.PatientConditionAudit;
import uk.ac.ucl.rits.inform.informdb.conditions.ConditionVisits;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.PatientConditionMessage;
import uk.ac.ucl.rits.inform.interchange.PatientInfection;
import uk.ac.ucl.rits.inform.interchange.PatientProblem;
import uk.ac.ucl.rits.inform.interchange.PatientAllergy;
import uk.ac.ucl.rits.inform.interchange.ConditionAction;

import javax.annotation.Resource;
import java.time.Instant;
import java.util.List;
import java.util.Optional;


/**
 * Parses patient conditions from interchange messages.
 * <p>
 * Parses any patient condition that can have a start and end, e.g. problem lists, allergies and infections.
 * @author Anika Cawthorn
 * @author Stef Piatek
 * @author Tom Young
 */
@Component
public class PatientConditionController {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final PatientConditionRepository patientConditionRepo;
    private final PatientConditionAuditRepository patientConditionAuditRepo;
    private final ConditionVisitLinkRepository conditionVisitLinkRepository;
    private final AllergenReactionRepository allergenReactionRepo;
    private final AllergenReactionAuditRepository allergenReactionAuditRepo;

    @Resource
    private PatientConditionCache cache;

    /**
     * Types of patient conditions.
     *      PATIENT_INFECTION = Infection banner for infection control
     *      PROBLEM_LIST = Problem (not an infection)
     */
    enum PatientConditionType {
        PATIENT_INFECTION,
        PATIENT_ALLERGY,
        PROBLEM_LIST
    }

    /**
     * @param patientConditionRepo      autowired PatientConditionRepository
     * @param patientConditionAuditRepo autowired PatientConditionAuditRepository
     * @param conditionVisitLinkRepository autowired ConditionVisitLinkRepository
     * @param allergenReactionRepo autowired allergenReactionRepo
     * @param allergenReactionAuditRepo autowired allergenReactionAuditRepo
     */
    public PatientConditionController(PatientConditionRepository patientConditionRepo, PatientConditionAuditRepository
            patientConditionAuditRepo, ConditionVisitLinkRepository conditionVisitLinkRepository,
            AllergenReactionRepository allergenReactionRepo, AllergenReactionAuditRepository allergenReactionAuditRepo) {
        this.patientConditionRepo = patientConditionRepo;
        this.patientConditionAuditRepo = patientConditionAuditRepo;
        this.conditionVisitLinkRepository = conditionVisitLinkRepository;
        this.allergenReactionRepo = allergenReactionRepo;
        this.allergenReactionAuditRepo = allergenReactionAuditRepo;
    }

    /**
     * Process any type of patient condition message.
     * @param msg        message
     * @param mrn        patient id
     * @param visit      hospital visit can be null
     * @param storedFrom valid from in database
     * @throws EmapOperationMessageProcessingException if message can't be processed.
     * @throws RequiredDataMissingException if the PatientConditionMessage is not of a supported type
     */
    public void processMessage(final PatientConditionMessage msg, Mrn mrn, HospitalVisit visit, final Instant storedFrom)
            throws EmapOperationMessageProcessingException {

        if (msg instanceof PatientProblem) {
            processMessage((PatientProblem) msg, mrn, visit, storedFrom);
        } else if (msg instanceof PatientAllergy) {
            processMessage((PatientAllergy) msg, mrn, visit, storedFrom);
        } else if (msg instanceof PatientInfection) {
            processMessage((PatientInfection) msg, mrn, visit, storedFrom);
        } else {
            logger.debug("Failed to process a {} message. Unsupported derived type", msg.getClass());
            throw new RequiredDataMissingException(String.format("Type of the message %s could not be processed", msg.getClass()));
        }
    }

    /**
     * Update patient condition from patient condition message.
     * @param msg            patient condition message
     * @param visit          hospital visit
     * @param conditionState patient condition entity to update
     */
    private void updatePatientCondition(PatientConditionMessage msg, HospitalVisit visit, RowState<PatientCondition,
            PatientConditionAudit> conditionState) {

        PatientCondition condition = conditionState.getEntity();
        conditionState.assignInterchangeValue(msg.getEpicConditionId(), condition.getInternalId(), condition::setInternalId);
        conditionState.assignIfDifferent(msg.getUpdatedDateTime(), condition.getValidFrom(), condition::setValidFrom);
        conditionState.assignIfDifferent(visit, condition.getHospitalVisitId(), condition::setHospitalVisitId);
        conditionState.assignInterchangeValue(msg.getStatus(), condition.getStatus(), condition::setStatus);
        conditionState.assignInterchangeValue(msg.getComment(), condition.getComment(), condition::setComment);
        conditionState.assignInterchangeValue(msg.getOnsetDate(), condition.getOnsetDate(), condition::setOnsetDate);
        conditionState.assignInterchangeValue(msg.getSeverity(), condition.getSeverity(), condition::setSeverity);
    }

    /**
     * Create minimal patient condition wrapped in RowState.
     * @param mrn            patient identifier
     * @param conditionType  condition type
     * @param conditionId    identifier for condition as used in EPIC
     * @param validFrom      hospital time that the data is true from
     * @param storedFrom     time that emap-core started processing the message
     * @return minimal patient condition wrapped in RowState
     */
    private RowState<PatientCondition, PatientConditionAudit> createMinimalPatientCondition(
            Mrn mrn, ConditionType conditionType, Long conditionId, Instant validFrom, Instant storedFrom) {

        PatientCondition patientCondition = new PatientCondition(conditionType, mrn, conditionId);
        return new RowState<>(patientCondition, validFrom, storedFrom, true);
    }

    /**
     * Process allergy message, which includes an allergy with an associated status and optional severity.
     * @param msg        message
     * @param mrn        patient id
     * @param visit      hospital visit can be null
     * @param storedFrom valid from in database
     * @throws EmapOperationMessageProcessingException if message can't be processed.
     */
    private void processMessage(final PatientAllergy msg, Mrn mrn, HospitalVisit visit, final Instant storedFrom)
            throws EmapOperationMessageProcessingException {

        var conditionType = getOrCreateConditionType(
                PatientConditionType.PATIENT_ALLERGY, msg.getConditionCode(), msg.getUpdatedDateTime(), storedFrom);

        cache.updateAndClearFromCache(conditionType, msg, PatientConditionType.PROBLEM_LIST, msg.getConditionCode(),
                msg.getUpdatedDateTime());

        var patientCondition = getOrCreatePatientCondition(msg, mrn, conditionType.getEntity(), storedFrom);

        if (conditionShouldBeUpdated(msg, patientCondition)) {
            updatePatientAllergy(msg, visit, patientCondition);
        }

        patientCondition.saveEntityOrAuditLogIfRequired(patientConditionRepo, patientConditionAuditRepo);
        savePatientConditionHospitalVisitLink(patientCondition.getEntity(), visit);
        saveAllergyReactions(msg, patientCondition.getEntity(), storedFrom);
    }

    /**
     * Get or create existing patient condition entity. Can create on the internal ID as it exists in both the hl7
     * messages and in clarity.
     * @param msg           patient infection message
     * @param mrn           patient identifier
     * @param conditionType condition type referred to in message
     * @param storedFrom    time that emap-core started processing the message
     * @return observation entity wrapped in RowState
     */
    private RowState<PatientCondition, PatientConditionAudit> getOrCreatePatientCondition(
            PatientConditionMessage msg, Mrn mrn, ConditionType conditionType, Instant storedFrom) {
        Instant updatedTime = msg.getUpdatedDateTime();

        Optional<PatientCondition> patientCondition = patientConditionRepo.findByMrnIdAndConditionTypeIdAndInternalId(
                mrn, conditionType, msg.getEpicConditionId().get());

        return patientCondition
                .map(obs -> new RowState<>(obs, updatedTime, storedFrom, false))
                .orElseGet(() -> createMinimalPatientCondition(mrn, conditionType, msg.getEpicConditionId().get(), updatedTime, storedFrom));
    }

    /**
     * Update specific patient allergy attributes from patient allergy message.
     * @param msg            patient allergy message
     * @param visit          hospital visit
     * @param conditionState patient condition entity to update
     */
    private void updatePatientAllergy(PatientAllergy msg, HospitalVisit visit, RowState<PatientCondition,
            PatientConditionAudit> conditionState) {
        PatientCondition condition = conditionState.getEntity();
        conditionState.assignIfDifferent(msg.getAddedDatetime(), condition.getAddedDatetime(), condition::setAddedDatetime);

        if (msg.getAction() == ConditionAction.DELETE) {
            conditionState.assignIfDifferent(true, condition.getIsDeleted(), condition::setIsDeleted);
        }

        updatePatientCondition(msg, visit, conditionState);
    }

    /**
     * Save all the reactions associated with an allergy for a particular condition.
     * @param msg Patient allergy message
     * @param condition Patient condition entity
     * @param storedFrom Time at which emap core started processing this message
     */
    private void saveAllergyReactions(PatientAllergy msg, PatientCondition condition, final Instant storedFrom) {

        var updatedTime = msg.getUpdatedDateTime();

        // Remove all the reactions that were present before this message
        var reactions = allergenReactionRepo.findAllByPatientConditionIdAndValidFromLessThanEqual(condition, updatedTime);

        for (var reaction : reactions) {
            logger.debug("Deleting allergen reaction: {}", reaction);
            var auditEntity = reaction.createAuditEntity(updatedTime, storedFrom);
            allergenReactionAuditRepo.save(auditEntity);
            allergenReactionRepo.delete(reaction);
        }

        for (String reactionName : msg.getReactions()) {
            allergenReactionRepo.save(new AllergenReaction(reactionName, condition, updatedTime, storedFrom));
        }
    }

    /**
     * Process patient problem, which includes a single problem (subtype of condition) and an associated status.
     * @param msg        message
     * @param mrn        patient id
     * @param visit      hospital visit can be null
     * @param storedFrom valid from in database
     * @throws EmapOperationMessageProcessingException if message can't be processed.
     */
    private void processMessage(final PatientProblem msg, Mrn mrn, HospitalVisit visit, final Instant storedFrom)
            throws EmapOperationMessageProcessingException {

        var conditionType = getOrCreateConditionType(
                PatientConditionType.PROBLEM_LIST, msg.getConditionCode(), msg.getUpdatedDateTime(), storedFrom);
        cache.updateAndClearFromCache(conditionType, msg, PatientConditionType.PROBLEM_LIST, msg.getConditionCode(),
                msg.getUpdatedDateTime());

        var patientCondition = getOrCreatePatientCondition(msg, mrn, conditionType.getEntity(), storedFrom);

        if (problemMessageShouldBeUpdated(msg, patientCondition)) {
            updatePatientProblem(msg, visit, patientCondition);
        }
        patientCondition.saveEntityOrAuditLogIfRequired(patientConditionRepo, patientConditionAuditRepo);
        savePatientConditionHospitalVisitLink(patientCondition.getEntity(), visit);
    }

    /**
     * Update specific patient problem attributes from patient problem message.
     * @param msg            patient problem message
     * @param visit          hospital visit
     * @param conditionState patient condition entity to update
     * @throws RequiredDataMissingException if the status of the message is not defined
     */
    private void updatePatientProblem(PatientProblem msg, HospitalVisit visit, RowState<PatientCondition,
            PatientConditionAudit> conditionState) throws RequiredDataMissingException {
        PatientCondition condition = conditionState.getEntity();
        conditionState.assignIfDifferent(msg.getResolvedDate(), condition.getResolutionDate(), condition::setResolutionDate);
        conditionState.assignIfDifferent(msg.getAddedDate(), condition.getAddedDate(), condition::setAddedDate);
        updatePatientCondition(msg, visit, conditionState);

        if (msg.getAction().equals(ConditionAction.DELETE)) {

            if (msg.getStatus().isUnknown()) {
                throw new RequiredDataMissingException("Failed to determine if the message is a delete without a defined status");
            } else if (msg.getStatus().get().equalsIgnoreCase("active")) {
                conditionState.assignIfDifferent(true, condition.getIsDeleted(), condition::setIsDeleted);
            }

        } else {
            conditionState.assignIfDifferent(false, condition.getIsDeleted(), condition::setIsDeleted);
        }
    }

    /**
     * Process patient condition message.
     * @param msg        message
     * @param mrn        patient id
     * @param visit      hospital visit
     * @param storedFrom valid from in database
     * @throws EmapOperationMessageProcessingException if message can't be processed.
     */
    private void processMessage(final PatientInfection msg, Mrn mrn, HospitalVisit visit, final Instant storedFrom)
            throws EmapOperationMessageProcessingException {

        var conditionType = getOrCreateConditionType(
                PatientConditionType.PATIENT_INFECTION, msg.getConditionCode(), msg.getUpdatedDateTime(), storedFrom);

        cache.updateAndClearFromCache(conditionType, msg, PatientConditionType.PATIENT_ALLERGY, msg.getConditionCode(),
                msg.getUpdatedDateTime());
        deletePreviousInfectionOrClearInfectionTypesCache(msg, storedFrom);

        var patientCondition = getOrCreatePatientInfection(msg, mrn, conditionType.getEntity(), storedFrom);

        if (this.conditionShouldBeUpdated(msg, patientCondition)) {
            updatePatientInfection(msg, visit, patientCondition);
        }

        patientCondition.saveEntityOrAuditLogIfRequired(patientConditionRepo, patientConditionAuditRepo);
    }

    /**
     * Update specific patient infection attributes from patient infection message.
     * @param msg            patient infection message
     * @param visit          hospital visit
     * @param conditionState patient condition entity to update
     */
    private void updatePatientInfection(PatientInfection msg, HospitalVisit visit, RowState<PatientCondition,
            PatientConditionAudit> conditionState) {
        PatientCondition condition = conditionState.getEntity();
        conditionState.assignInterchangeValue(
                msg.getResolvedDatetime(), condition.getResolutionDatetime(), condition::setResolutionDatetime);
        conditionState.assignIfDifferent(msg.getAddedDatetime(), condition.getAddedDatetime(), condition::setAddedDatetime);
        updatePatientCondition(msg, visit, conditionState);
    }

    /**
     * Saves a link between the patient condition record and the hospital visit record.
     * @param condition Patient condition record
     * @param visit Hospital visit record
     * @throws RequiredDataMissingException if the condition is null
     */
    private void savePatientConditionHospitalVisitLink(PatientCondition condition, HospitalVisit visit)
            throws RequiredDataMissingException {

        if (condition == null) {
            throw new RequiredDataMissingException("Failed to link null condition to visit");
        }

        if (visit != null
                && conditionVisitLinkRepository.findByPatientConditionIdAndHospitalVisitId(condition, visit).isEmpty()) {
            conditionVisitLinkRepository.save(new ConditionVisits(condition, visit));
        }
    }

    /**
     * Get existing patient condition type (from database or cache) or create a new one.
     * @param type            Patient condition type
     * @param conditionCode   Code for the condition within the type
     * @param updatedDateTime when the condition information is valid from
     * @param storedFrom      when the condition information had been started to be processed by emap
     * @return ConditionType wrapped in a row state
     */
    private RowState<ConditionType, ConditionTypeAudit> getOrCreateConditionType(
            PatientConditionType type, String conditionCode, Instant updatedDateTime, Instant storedFrom) {
        return cache.getConditionType(type, conditionCode)
                .map(typeEntity -> new RowState<>(typeEntity, updatedDateTime, storedFrom, false))
                .orElseGet(() -> {
                    ConditionType typeEntity = cache.createNewType(type, conditionCode, updatedDateTime, storedFrom);
                    return new RowState<>(typeEntity, updatedDateTime, storedFrom, true);
                });
    }

    /**
     * We can't trust patient infections from HL7 as no ID, so delete these if the infection ID is known.
     * If processing HL7 then clear the cache of infection types so that new types will be updated if added from hl7.
     * @param msg         patient infection message
     * @param deleteUntil time to delete messages up until (inclusive)
     */
    private void deletePreviousInfectionOrClearInfectionTypesCache(PatientConditionMessage msg, Instant deleteUntil) {
        if (msg.getEpicConditionId().isSave()) {
            logger.debug("Deleting all infections up to {}", msg.getUpdatedDateTime());
            List<ConditionType> hl7InfectionTypes = cache.getAllInfectionTypesAndCacheResults();
            auditAndDeletePatientConditionsUntil(hl7InfectionTypes, msg.getUpdatedDateTime(), deleteUntil);
        } else {
            cache.clearCacheOfInfectionTypes();
        }
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
     * Get or create existing patient condition entity from a patient infection or allergy message.
     * @param msg           patient infection or allergy message
     * @param mrn           patient identifier
     * @param conditionType condition type referred to in message
     * @param storedFrom    time that emap-core started processing the message
     * @return observation entity wrapped in RowState
     * @throws RequiredDataMissingException if no patient infection Id in hoover data or unrecognised source system
     */
    private RowState<PatientCondition, PatientConditionAudit> getOrCreatePatientInfection(
            PatientInfection msg, Mrn mrn, ConditionType conditionType, Instant storedFrom)
            throws RequiredDataMissingException {
        Optional<PatientCondition> patientCondition;
        final Long epicConditionId;
        switch (msg.getSourceSystem()) {
            case "EPIC":
                epicConditionId = null;
                patientCondition = patientConditionRepo
                        .findByMrnIdAndConditionTypeIdAndAddedDatetime(mrn, conditionType, msg.getAddedDatetime());
                break;
            case "clarity":
                if (msg.getEpicConditionId().isUnknown()) {
                    throw new RequiredDataMissingException("No epicConditionId from clarity");
                }
                epicConditionId = msg.getEpicConditionId().get();
                patientCondition = patientConditionRepo.findByConditionTypeIdAndInternalId(conditionType, epicConditionId);
                break;
            default:
                throw new RequiredDataMissingException(String.format("'%s' is not a recognised source system", msg.getSourceSystem()));
        }

        return patientCondition
                .map(obs -> new RowState<>(obs, msg.getUpdatedDateTime(), storedFrom, false))
                .orElseGet(() -> createMinimalPatientCondition(mrn, conditionType, epicConditionId,
                        msg.getUpdatedDateTime(), storedFrom));
    }

    /**
     * Update message if observation has been created, or the message updated time is >= entity validFrom.
     * @param msg       patient condition message
     * @param condition row state of condition
     * @return true if message should be updated
     */
    private boolean conditionShouldBeUpdated(PatientConditionMessage msg, RowState<PatientCondition, PatientConditionAudit> condition) {
        return condition.isEntityCreated() || !msg.getUpdatedDateTime().isBefore(condition.getEntity().getConditionTypeId().getValidFrom());
    }

    /**
     * Update the problem message. Requires special treatment for EPIC hl7 messages which are identical (including
     * the updated datetime) apart from the action. The AD messages seem to take precedence when comparing to the source
     * of truth. Therefore, the problem should not be updated with a DE action if there was an AD action message
     * that was processed before but has the same updated datetime.
     * @param msg patient problem message
     * @param condition patient condition row state
     * @return true if the entity should be updated
     */
    private boolean problemMessageShouldBeUpdated(PatientProblem msg, RowState<PatientCondition, PatientConditionAudit> condition) {

        Instant validFrom = condition.getEntity().getConditionTypeId().getValidFrom();

        // hl7 messages with an identical message updated datetime should favour the AD action over DE
        if (!condition.isEntityCreated()
                && msg.getSourceSystem().equals("EPIC")
                && msg.getUpdatedDateTime().equals(validFrom)
                && msg.getAction().equals(ConditionAction.DELETE)) {
            return false;
        }

        return this.conditionShouldBeUpdated(msg, condition);
    }
}


/**
 * Helper component, used because Spring cache doesn't intercept self-invoked method calls.
 */
@Component
class PatientConditionCache {
    private static final Logger logger = LoggerFactory.getLogger(PatientConditionCache.class);
    private final ConditionTypeRepository conditionTypeRepo;
    private final ConditionTypeAuditRepository conditionTypeAuditRepo;

    /**
     * @param conditionTypeRepo      autowired ConditionTypeRepository
     * @param conditionTypeAuditRepo interacting with the condition type audit tables
     */
    PatientConditionCache(ConditionTypeRepository conditionTypeRepo, ConditionTypeAuditRepository conditionTypeAuditRepo) {
        this.conditionTypeRepo = conditionTypeRepo;
        this.conditionTypeAuditRepo = conditionTypeAuditRepo;
    }


    /**
     * Get existing condition type and update cache.
     * @param type          Condition Type
     * @param conditionCode EPIC code for the condition within the type
     * @return Optional ConditionType
     */
    @Cacheable(value = "conditionType", key = "{#type, #conditionCode}")
    public Optional<ConditionType> getConditionType(
            PatientConditionController.PatientConditionType type, String conditionCode) {
        logger.trace("** Querying condition: {}, code {}", type.toString(), conditionCode);
        return conditionTypeRepo.findByDataTypeAndInternalCode(type.toString(), conditionCode);
    }

    /**
     * Create and save a new condition type clear the cache for this key.
     * @param type            Condition Type
     * @param conditionCode   EPIC code for the condition within the type
     * @param updatedDateTime when the condition information is valid from
     * @param storedFrom      when the condition information had been started to be processed by emap
     * @return new ConditionType
     */
    @CacheEvict(value = "conditionType", key = "{#type, #conditionCode}")
    public ConditionType createNewType(
            PatientConditionController.PatientConditionType type, String conditionCode, Instant updatedDateTime, Instant storedFrom) {
        ConditionType conditionType = new ConditionType(type.toString(), conditionCode, updatedDateTime, storedFrom);
        conditionTypeRepo.save(conditionType);
        logger.debug("Created new {}", conditionType);
        return conditionType;
    }

    /**
     * Get all patient infection condition types or get form cache if it hasn't been cleared.
     * @return all patient infection condition types
     */
    @Cacheable(value = "infectionTypes")
    public List<ConditionType> getAllInfectionTypesAndCacheResults() {
        return conditionTypeRepo.findAllByDataType(PatientConditionController.PatientConditionType.PATIENT_INFECTION.toString());
    }

    /**
     * Clear cache of patient infection condition types.
     */
    @CacheEvict(value = "infectionTypes", allEntries = true)
    public void clearCacheOfInfectionTypes() {
        logger.trace("** Clearing cache of all infection types");
    }

    /**
     * Persist condition type with new name and remove this entry from the cache.
     * @param typeState     condition type entity to updated
     * @param msg           message from which to update the entity from
     * @param type          used as the key for the cache
     * @param conditionCode used as the key for the cache
     * @param validFrom     when the condition information is valid from
     */
    @CacheEvict(value = "conditionType", key = "{#type, #conditionCode}")
    public void updateAndClearFromCache(
            RowState<ConditionType, ConditionTypeAudit> typeState,
            PatientConditionMessage msg, PatientConditionController.PatientConditionType type,
            String conditionCode, Instant validFrom) {

        ConditionType typeEntity = typeState.getEntity();

        typeState.assignIfCurrentlyNullOrNewerAndDifferent(msg.getConditionName(), typeEntity.getName(),
                typeEntity::setName, validFrom, typeEntity.getValidFrom());
        typeState.assignIfCurrentlyNullOrNewerAndDifferent(msg.getSubType(), typeEntity.getSubType(),
                typeEntity::setSubType, validFrom, typeEntity.getValidFrom());

        typeState.saveEntityOrAuditLogIfRequired(conditionTypeRepo, conditionTypeAuditRepo);
    }

}
