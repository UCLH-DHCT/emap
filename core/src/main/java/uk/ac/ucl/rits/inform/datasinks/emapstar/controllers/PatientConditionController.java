package uk.ac.ucl.rits.inform.datasinks.emapstar.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import uk.ac.ucl.rits.inform.datasinks.emapstar.RowState;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.RequiredDataMissingException;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.ConditionTypeAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.ConditionTypeRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.PatientConditionAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.PatientConditionRepository;
import uk.ac.ucl.rits.inform.informdb.conditions.ConditionType;
import uk.ac.ucl.rits.inform.informdb.conditions.ConditionTypeAudit;
import uk.ac.ucl.rits.inform.informdb.conditions.PatientCondition;
import uk.ac.ucl.rits.inform.informdb.conditions.PatientConditionAudit;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.PatientConditionMessage;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.PatientInfection;
import uk.ac.ucl.rits.inform.interchange.PatientProblem;

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

    @Resource
    private PatientConditionCache cache;

    /**
     * Types of patient conditions.
     *      PATIENT_INFECTION = Infection banner for infection control
     *      PROBLEM_LIST = Problem (not an infection)
     */
    enum PatientConditionType {
        PATIENT_INFECTION,
        PROBLEM_LIST
    }

    /**
     * @param patientConditionRepo      autowired PatientConditionRepository
     * @param patientConditionAuditRepo autowired PatientConditionAuditRepository
     */
    public PatientConditionController(
            PatientConditionRepository patientConditionRepo, PatientConditionAuditRepository patientConditionAuditRepo) {
        this.patientConditionRepo = patientConditionRepo;
        this.patientConditionAuditRepo = patientConditionAuditRepo;
    }


    /**
     * Process patient problem message, which includes a single problem (subtype of condition) and an associated.
     * status and optional severity
     * @param msg        message
     * @param mrn        patient id
     * @param visit      hospital visit can be null
     * @param storedFrom valid from in database
     * @throws EmapOperationMessageProcessingException if message can't be processed.
     */
    public void processMessage(final PatientProblem msg, Mrn mrn, HospitalVisit visit, final Instant storedFrom)
            throws EmapOperationMessageProcessingException {

        RowState<ConditionType, ConditionTypeAudit> conditionType = getOrCreateConditionType(
                PatientConditionType.PROBLEM_LIST, msg.getConditionCode(), msg.getUpdatedDateTime(), storedFrom
        );
        cache.updateNameAndClearFromCache(conditionType, msg.getConditionName(), PatientConditionType.PROBLEM_LIST,
                msg.getConditionCode(), msg.getUpdatedDateTime(), storedFrom);

        if (msg.getUpdatedDateTime().isAfter(conditionType.getEntity().getValidFrom())) {
            updateConditionName(conditionType.getEntity(), msg.getConditionName());
        }

        RowState<PatientCondition, PatientConditionAudit> patientCondition = getOrCreatePatientProblem(msg, mrn,
                conditionType.getEntity(), storedFrom);

        if (messageShouldBeUpdated(msg, patientCondition)) {
            updatePatientCondition(msg, visit, patientCondition);
        }

        patientCondition.saveEntityOrAuditLogIfRequired(patientConditionRepo, patientConditionAuditRepo);

        if (msg.getAction().equals("DE") && msg.statusIsActive()) {
            deleteConditionAndAudit(patientCondition, msg, storedFrom);
        }
    }

    private void deleteConditionAndAudit(RowState<PatientCondition, PatientConditionAudit> patientCondition,
                                         PatientProblem msg, final Instant storedFrom){
        patientConditionAuditRepo.save(patientCondition.getEntity().createAuditEntity(msg.getUpdatedDateTime(),
                storedFrom));
        logger.debug("Deleting PatientCondition: {}", patientCondition);
        patientConditionRepo.delete(patientCondition.getEntity());
    }

    /**
     * Update the name of a condition if it is defined.
     * @param conditionType Specific type of condition with an internal code
     * @param conditionName Human-readable name of the condition
     */
    private void updateConditionName(ConditionType conditionType, InterchangeValue<String> conditionName) {

        if (conditionName.isSave()) {
            conditionType.setName(conditionName.get());
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
     * Process patient condition message.
     * @param msg        message
     * @param mrn        patient id
     * @param visit      hospital visit
     * @param storedFrom valid from in database
     * @throws EmapOperationMessageProcessingException if message can't be processed.
     */
    public void processMessage(final PatientInfection msg, Mrn mrn, HospitalVisit visit, final Instant storedFrom)
            throws EmapOperationMessageProcessingException {
        RowState<ConditionType, ConditionTypeAudit> conditionType = getOrCreateConditionType(
                PatientConditionType.PATIENT_INFECTION, msg.getConditionCode(), msg.getUpdatedDateTime(), storedFrom
        );

        cache.updateNameAndClearFromCache(conditionType, msg.getConditionName(), PatientConditionType.PATIENT_INFECTION,
                msg.getConditionCode(), msg.getUpdatedDateTime(), storedFrom);

        deletePreviousInfectionOrClearInfectionTypesCache(msg, storedFrom);

        RowState<PatientCondition, PatientConditionAudit> patientCondition = getOrCreatePatientInfection(
                msg, mrn, conditionType.getEntity(), storedFrom
        );

        if (messageShouldBeUpdated(msg, patientCondition)) {
            updatePatientCondition(msg, visit, patientCondition);
        }

        patientCondition.saveEntityOrAuditLogIfRequired(patientConditionRepo, patientConditionAuditRepo);
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
     * Get or create existing patient condition entity.
     * @param msg           patient infection message
     * @param mrn           patient identifier
     * @param conditionType condition type referred to in message
     * @param storedFrom    time that emap-core started processing the message
     * @return observation entity wrapped in RowState
     * @throws RequiredDataMissingException if no patient infection Id in hoover data or unrecognised source system
     */
    private RowState<PatientCondition, PatientConditionAudit> getOrCreatePatientInfection(
            PatientConditionMessage msg, Mrn mrn, ConditionType conditionType, Instant storedFrom)
            throws RequiredDataMissingException {
        Optional<PatientCondition> patientCondition;
        final Long epicInfectionId;
        switch (msg.getSourceSystem()) {
            case "EPIC":
                epicInfectionId = null;
                patientCondition = patientConditionRepo
                        .findByMrnIdAndConditionTypeIdAndAddedDateTime(mrn, conditionType, msg.getAddedTime());
                break;
            case "clarity":
                if (msg.getEpicConditionId().isUnknown()) {
                    throw new RequiredDataMissingException("No patientInfectionId from clarity");
                }
                epicInfectionId = msg.getEpicConditionId().get();
                patientCondition = patientConditionRepo.findByConditionTypeIdAndInternalId(conditionType, epicInfectionId);
                break;
            default:
                throw new RequiredDataMissingException(String.format("'%s' is not a recognised source system", msg.getSourceSystem()));
        }

        return patientCondition
                .map(obs -> new RowState<>(obs, msg.getUpdatedDateTime(), storedFrom, false))
                .orElseGet(() -> createMinimalPatientCondition(epicInfectionId, mrn, conditionType,
                        msg.getAddedTime(), msg.getUpdatedDateTime(), storedFrom));
    }

    /**
     * Get or create existing patient condition entity.
     * @param msg           patient infection message
     * @param mrn           patient identifier
     * @param conditionType condition type referred to in message
     * @param storedFrom    time that emap-core started processing the message
     * @return observation entity wrapped in RowState
     */
    private RowState<PatientCondition, PatientConditionAudit> getOrCreatePatientProblem(
            PatientConditionMessage msg, Mrn mrn, ConditionType conditionType, Instant storedFrom) {

        Instant addedTime = msg.getAddedTime();
        Instant updatedTime = msg.getUpdatedDateTime();

        Optional<PatientCondition> patientCondition = patientConditionRepo.findByMrnIdAndConditionTypeIdAndAddedDateTime(
                mrn, conditionType, addedTime);

        Long epicId = null;

        if (msg.getEpicConditionId().isSave()) {
            epicId = msg.getEpicConditionId().get();
        }

        final Long finalEpicId = epicId;
        return patientCondition
                .map(obs -> new RowState<>(obs, updatedTime, storedFrom, false))
                .orElseGet(() -> createMinimalPatientCondition(finalEpicId, mrn, conditionType, addedTime,
                        updatedTime, storedFrom));
    }

    /**
     * Create minimal patient condition wrapped in RowState.
     * @param epicId         ID in EPIC for this condition
     * @param mrn            patient identifier
     * @param conditionType  condition type
     * @param conditionAdded condition added at
     * @param validFrom      hospital time that the data is true from
     * @param storedFrom     time that emap-core started processing the message
     * @return minimal patient condition wrapped in RowState
     */
    private RowState<PatientCondition, PatientConditionAudit> createMinimalPatientCondition(
            Long epicId, Mrn mrn, ConditionType conditionType, Instant conditionAdded, Instant validFrom, Instant storedFrom) {

        PatientCondition patientCondition = new PatientCondition(epicId, conditionType, mrn, conditionAdded);
        return new RowState<>(patientCondition, validFrom, storedFrom, true);
    }

    /**
     * Update message if observation has been created, or the message updated time is >= entity validFrom.
     * @param msg           patient condition message
     * @param conditionDate row state of condition
     * @return true if message should be updated
     */
    private boolean messageShouldBeUpdated(PatientConditionMessage msg, RowState<PatientCondition, PatientConditionAudit> conditionDate) {
        return conditionDate.isEntityCreated() || !msg.getUpdatedDateTime().isBefore(conditionDate.getEntity().getConditionTypeId().getValidFrom());
    }

    /**
     * Update patient condition from patient infection message.
     * @param msg            patient infection message
     * @param visit          hospital visit
     * @param conditionState patient condition entity to update
     */
    private void updatePatientCondition(PatientConditionMessage msg, HospitalVisit visit, RowState<PatientCondition,
            PatientConditionAudit> conditionState) {

        PatientCondition condition = conditionState.getEntity();
        conditionState.assignIfDifferent(msg.getUpdatedDateTime(), condition.getValidFrom(), condition::setValidFrom);
        conditionState.assignIfDifferent(visit, condition.getHospitalVisitId(), condition::setHospitalVisitId);
        conditionState.assignIfDifferent(msg.getStatus(), condition.getStatus(), condition::setStatus);
        conditionState.assignInterchangeValue(msg.getComment(), condition.getComment(), condition::setComment);
        conditionState.assignInterchangeValue(
                msg.getResolvedTime(), condition.getResolutionDateTime(), condition::setResolutionDateTime);
        conditionState.assignInterchangeValue(msg.getOnsetTime(), condition.getOnsetDate(), condition::setOnsetDate);
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
     * Create a new condition type and clear the cache for this key.
     * @param type            Condition Type
     * @param conditionCode   EPIC code for the condition within the type
     * @param updatedDateTime when the condition information is valid from
     * @param storedFrom      when the condition information had been started to be processed by emap
     * @return persisted ConditionType
     */
    @CacheEvict(value = "conditionType", key = "{#type, #conditionCode}")
    public ConditionType createNewType(
            PatientConditionController.PatientConditionType type, String conditionCode, Instant updatedDateTime, Instant storedFrom) {
        ConditionType conditionType = new ConditionType(type.toString(), conditionCode, updatedDateTime, storedFrom);
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
     * @param name          updated name to add
     * @param type          used as the key for the cache
     * @param conditionCode used as the key for the cache
     * @param validFrom     when the condition information is valid from
     * @param storedFrom    when the condition information had been started to be processed by emap
     */
    @CacheEvict(value = "conditionType", key = "{#type, #conditionCode}")
    public void updateNameAndClearFromCache(
            RowState<ConditionType, ConditionTypeAudit> typeState,
            InterchangeValue<String> name, PatientConditionController.PatientConditionType type,
            String conditionCode, Instant validFrom, Instant storedFrom) {

        ConditionType typeEntity = typeState.getEntity();
        typeState.assignIfCurrentlyNullOrNewerAndDifferent(name, typeEntity.getName(), typeEntity::setName, validFrom, storedFrom);
        typeState.saveEntityOrAuditLogIfRequired(conditionTypeRepo, conditionTypeAuditRepo);

    }

}
