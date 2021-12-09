package uk.ac.ucl.rits.inform.datasinks.emapstar.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.datasinks.emapstar.RowState;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.RequiredDataMissingException;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.vist_observations.VisitObservationAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.vist_observations.VisitObservationRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.vist_observations.VisitObservationTypeAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.vist_observations.VisitObservationTypeRepository;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.visit_recordings.VisitObservation;
import uk.ac.ucl.rits.inform.informdb.visit_recordings.VisitObservationAudit;
import uk.ac.ucl.rits.inform.informdb.visit_recordings.VisitObservationType;
import uk.ac.ucl.rits.inform.informdb.visit_recordings.VisitObservationTypeAudit;
import uk.ac.ucl.rits.inform.interchange.visit_observations.Flowsheet;
import uk.ac.ucl.rits.inform.interchange.visit_observations.FlowsheetMetadata;

import javax.annotation.Resource;
import java.time.Instant;

/**
 * Interactions with observation visits.
 * @author Stef Piatek
 * @author Anika Cawthorn
 */
@Component
public class VisitObservationController {
    /**
     * Self-autowire so that @Caching annotation call will be intercepted.
     * Spring does not intercept internal calls, so using self here means that it will be intercepted for caching.
     */
    @Resource
    private VisitObservationController self;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final VisitObservationRepository visitObservationRepo;
    private final VisitObservationAuditRepository visitObservationAuditRepo;
    private final VisitObservationTypeRepository visitObservationTypeRepo;
    private final VisitObservationTypeAuditRepository visitObservationTypeAuditRepo;

    /**
     * @param visitObservationRepo          autowired
     * @param visitObservationAuditRepo     autowired
     * @param visitObservationTypeRepo      autowired
     * @param visitObservationTypeAuditRepo autowired
     */
    public VisitObservationController(
            VisitObservationRepository visitObservationRepo, VisitObservationAuditRepository visitObservationAuditRepo,
            VisitObservationTypeRepository visitObservationTypeRepo, VisitObservationTypeAuditRepository visitObservationTypeAuditRepo) {
        this.visitObservationRepo = visitObservationRepo;
        this.visitObservationAuditRepo = visitObservationAuditRepo;
        this.visitObservationTypeRepo = visitObservationTypeRepo;
        this.visitObservationTypeAuditRepo = visitObservationTypeAuditRepo;
    }

    /**
     * Process metadata, clearing existing cache for all visit observations.
     * @param msg        flowsheet metadata
     * @param storedFrom time that star started processing the message
     * @throws java.util.NoSuchElementException if the VisitObservationTypes that a mapping message is referring to cannot be found
     * @throws RequiredDataMissingException     if required data is missing
     */
    @Transactional
    @CacheEvict(value = "visitObservationType", allEntries = true)
    public void processMetadata(FlowsheetMetadata msg, Instant storedFrom) throws RequiredDataMissingException {
        if (msg.getInterfaceId() == null && msg.getFlowsheetRowEpicId() == null) {
            throw new RequiredDataMissingException("Both identifiers cannot be null");
        }
        // if both IDs are present, it's a mapping metadata message (as opposed to data for observation type)
        if (msg.getInterfaceId() != null && msg.getFlowsheetRowEpicId() != null) {
            processMappingMessage(msg, storedFrom);
        }
        RowState<VisitObservationType, VisitObservationTypeAudit> typeState = getOrCreateObservationTypeState(
                msg.getInterfaceId(), msg.getFlowsheetRowEpicId(), msg.getSourceObservationType(), msg.getLastUpdatedInstant(), storedFrom);
        VisitObservationType observationType = typeState.getEntity();
        // Update metadata with usable information
        Instant messageValidFrom = msg.getLastUpdatedInstant();
        Instant entityValidFrom = observationType.getValidFrom();
        typeState.assignIfCurrentlyNullOrNewerAndDifferent(
                msg.getName(), observationType.getName(), observationType::setName, messageValidFrom, entityValidFrom);
        typeState.assignIfCurrentlyNullOrNewerAndDifferent(
                msg.getDisplayName(), observationType.getDisplayName(), observationType::setDisplayName, messageValidFrom, entityValidFrom);
        typeState.assignIfCurrentlyNullOrNewerAndDifferent(
                msg.getDescription(), observationType.getDescription(), observationType::setDescription, messageValidFrom, entityValidFrom);
        typeState.assignIfCurrentlyNullOrNewerAndDifferent(
                msg.getValueType(), observationType.getPrimaryDataType(), observationType::setPrimaryDataType, messageValidFrom, entityValidFrom);
        typeState.assignIfCurrentlyNullOrNewerAndDifferent(
                msg.getCreationInstant(), observationType.getCreationTime(), observationType::setCreationTime, messageValidFrom, entityValidFrom);

        typeState.saveEntityOrAuditLogIfRequired(visitObservationTypeRepo, visitObservationTypeAuditRepo);
    }

    /**
     * There are two different types of metadata: i) containing the mapping between an interfaceId and idInApplication and
     * ii) containing lots of naming data for the particular VisitObservationType.
     * @param msg        Flowsheet metadata message containing mapping information
     * @param storedFrom When this information was first processed in Star.
     * @throws RequiredDataMissingException if relevant VisitObservationType(s) cannot be found
     */
    public void processMappingMessage(FlowsheetMetadata msg, Instant storedFrom) throws RequiredDataMissingException {
        if (mappingExists(msg.getInterfaceId(), msg.getFlowsheetRowEpicId())) {
            return;
        }
        RowState<VisitObservationType, VisitObservationTypeAudit> votCaboodleState = visitObservationTypeRepo
                .find(null, msg.getFlowsheetRowEpicId(), msg.getSourceObservationType())
                .map(vot -> new RowState<>(vot, msg.getLastUpdatedInstant(), storedFrom, false))
                .orElse(null);
        RowState<VisitObservationType, VisitObservationTypeAudit> votEpicState = visitObservationTypeRepo
                .find(msg.getInterfaceId(), null, msg.getSourceObservationType())
                .map(vot -> new RowState<>(vot, msg.getLastUpdatedInstant(), storedFrom, false))
                .orElse(null);
        if (votCaboodleState == null && votEpicState == null) {
            RowState<VisitObservationType, VisitObservationTypeAudit> vot = getOrCreateObservationTypeState(msg.getInterfaceId(),
                    msg.getFlowsheetRowEpicId(), msg.getSourceObservationType(), msg.getLastUpdatedInstant(), storedFrom);
            vot.saveEntityOrAuditLogIfRequired(visitObservationTypeRepo, visitObservationTypeAuditRepo);
        } else if (votCaboodleState != null) {
            VisitObservationType votCaboodle = votCaboodleState.getEntity();
            votCaboodleState.assignIfDifferent(msg.getInterfaceId(), votCaboodle.getInterfaceId(), votCaboodle::setInterfaceId);
            if (votEpicState != null) {
                VisitObservationType votEpic = votEpicState.getEntity();
                for (VisitObservation visit : visitObservationRepo.findAllByVisitObservationTypeId(votEpic)) {
                    RowState<VisitObservation, VisitObservationAudit> vState = new RowState<>(visit, msg.getLastUpdatedInstant(),
                            storedFrom, false);
                    vState.assignIfDifferent(votCaboodle, votEpic, vState.getEntity()::setVisitObservationTypeId);
                    vState.saveEntityOrAuditLogIfRequired(visitObservationRepo, visitObservationAuditRepo);
                }
                deleteVisitObservationType(votEpic);
            }
            votCaboodleState.saveEntityOrAuditLogIfRequired(visitObservationTypeRepo, visitObservationTypeAuditRepo);
        } else {
            VisitObservationType votEpic = votEpicState.getEntity();
            votEpicState.assignIfDifferent(msg.getFlowsheetRowEpicId(), votEpic.getIdInApplication(), votEpic::setIdInApplication);
            votEpicState.saveEntityOrAuditLogIfRequired(visitObservationTypeRepo, visitObservationTypeAuditRepo);
        }
    }

    /**
     * Create, update or delete a flowsheet, saving the visit observation to the cache.
     * Will also create a new VisitObservationType if it doesn't already exist.
     * @param msg        flowsheet
     * @param visit      hospital visit
     * @param storedFrom time that emap-core started processing the message
     * @throws RequiredDataMissingException if isNumericType is not set
     */
    @Transactional
    public void processFlowsheet(Flowsheet msg, HospitalVisit visit, Instant storedFrom) throws RequiredDataMissingException {
        if (msg.getValueType() == null) {
            throw new RequiredDataMissingException("Flowsheet DataType not set");
        }
        VisitObservationType observationType = self.getOrCreatePersistedObservationType(
                msg.getInterfaceId(), msg.getFlowsheetRowEpicId(), msg.getSourceObservationType(), msg.getLastUpdatedInstant(), storedFrom);

        RowState<VisitObservation, VisitObservationAudit> flowsheetState = getOrCreateFlowsheet(msg, visit, observationType, storedFrom);
        if (flowsheetState.messageShouldBeUpdated(msg.getLastUpdatedInstant())) {
            updateVisitObservation(msg, flowsheetState);
            flowsheetState.saveEntityOrAuditLogIfRequired(visitObservationRepo, visitObservationAuditRepo);
        }
    }

    /**
     * If mapping between flowsheet row EPIC id and interface id already exists, nothing (?) needs to change.
     * @param interfaceId     Identifier of observation type.
     * @param idInApplication IdInApplication of observation type.
     * @return True if mapping between interfaceId and idInApplication already exists, otherwise false.
     */
    public boolean mappingExists(String interfaceId, String idInApplication) {
        return visitObservationTypeRepo.findByInterfaceIdAndIdInApplication(interfaceId, idInApplication).isPresent();
    }

    /**
     * Get or create visit observation type, persisting and caching the output of this method.
     * @param idInApplication Id of the observation in the application (e.g. flowsheet row epic ID)
     * @param interfaceId     Id of observation type in HL messages
     * @param observationType type of observation (e.g. flowsheet)
     * @param validFrom       Timestamp from which information valid from
     * @param storedFrom      time that emap-core started processing the message
     * @return persisted VisitObservationType
     * @throws RequiredDataMissingException if both identifiers are missing
     */
    @Cacheable(value = "visitObservationType", key = "{ #interfaceId, #idInApplication, #observationType }")
    public VisitObservationType getOrCreatePersistedObservationType(
            String interfaceId, String idInApplication, String observationType, Instant validFrom, Instant storedFrom)
            throws RequiredDataMissingException {
        return visitObservationTypeRepo
                .find(interfaceId, idInApplication, observationType)
                .orElseGet(() -> {
                    VisitObservationType type = new VisitObservationType(idInApplication, interfaceId, observationType, validFrom, storedFrom);
                    return visitObservationTypeRepo.save(type);
                });
    }

    /**
     * Deletes VisitObservationType that was created in the absence of mapping information. Once mapping information is
     * present, the metadata VisitObservationType will be updated instead and the key information replaced respectively.
     * @param vVisitObservationType VisitObservationType to be deleted as object for repository deletion
     */
    public void deleteVisitObservationType(VisitObservationType vVisitObservationType) {
        visitObservationTypeRepo.delete(vVisitObservationType);
    }

    /**
     * Retrieves the existing information if visit observation type already exists, otherwise creates a new visit
     * observation type.
     * @param idInApplication Flowsheet row EPIC identifier
     * @param interfaceId     Interface id
     * @param observationType Type of visit observation
     * @param validFrom       When last updated
     * @param storedFrom      When this type of information was first processed from
     * @return RowState<VisitObservationType, VisitObservationTypeAudit> containing either existing or newly create repo information
     * @throws RequiredDataMissingException if both identifiers are null
     */
    private RowState<VisitObservationType, VisitObservationTypeAudit> getOrCreateObservationTypeState(
            String interfaceId, String idInApplication, String observationType, Instant validFrom, Instant storedFrom)
            throws RequiredDataMissingException {
        return visitObservationTypeRepo
                .find(interfaceId, idInApplication, observationType)
                .map(vot -> new RowState<>(vot, validFrom, storedFrom, false))
                .orElseGet(() -> createNewTypeInState(interfaceId, idInApplication, observationType, validFrom, storedFrom));
    }

    /**
     * Create a minimal visit observation type.
     * @param idInApplication       Id of the observation in the application (e.g. flowsheet row epic ID)
     * @param interfaceId           hl7 interface id
     * @param sourceObservationType type of visit observation
     * @param validFrom             Timestamp from which information valid from
     * @param storedFrom            time that emap-core started processing the message
     * @return minimal VisitObservationType wrapped in row state
     */
    private RowState<VisitObservationType, VisitObservationTypeAudit> createNewTypeInState(
            String idInApplication, String interfaceId, String sourceObservationType, Instant validFrom, Instant storedFrom) {
        VisitObservationType type = new VisitObservationType(idInApplication, interfaceId, sourceObservationType, validFrom, storedFrom);
        return new RowState<>(type, validFrom, storedFrom, true);
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
                .map(obs -> new RowState<>(obs, msg.getLastUpdatedInstant(), storedFrom, false))
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
        VisitObservation obs = new VisitObservation(visit, observationType, msg.getObservationTime(),
                msg.getSourceSystem(), msg.getLastUpdatedInstant(), storedFrom);
        return new RowState<>(obs, msg.getLastUpdatedInstant(), storedFrom, true);
    }

    /**
     * Update observation state from Flowsheet message.
     * @param msg              flowsheet
     * @param observationState observation entity wrapped in RowState
     * @throws RequiredDataMissingException if data type is not recognised for flowsheets
     */
    private void updateVisitObservation(Flowsheet msg, RowState<VisitObservation, VisitObservationAudit> observationState)
            throws RequiredDataMissingException {
        VisitObservation observation = observationState.getEntity();
        switch (msg.getValueType()) {
            case NUMERIC:
                observationState.assignInterchangeValue(msg.getNumericValue(), observation.getValueAsReal(), observation::setValueAsReal);
                break;
            case TEXT:
                observationState.assignInterchangeValue(msg.getStringValue(), observation.getValueAsText(), observation::setValueAsText);
                break;
            case DATE:
                observationState.assignInterchangeValue(msg.getDateValue(), observation.getValueAsDate(), observation::setValueAsDate);
                break;
            default:
                throw new RequiredDataMissingException(String.format("Flowsheet DataType '%s' not recognised", msg.getValueType()));
        }
        observationState.assignInterchangeValue(msg.getUnit(), observation.getUnit(), observation::setUnit);
        observationState.assignInterchangeValue(msg.getComment(), observation.getComment(), observation::setComment);
    }

}

