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
     * Process metadata, clearing existing cache for the visit observation.
     * @param msg        flowsheet metadata
     * @param storedFrom time that star started processing the message
     * @throws RequiredDataMissingException
     */
    @Transactional
    public void processMetadata(FlowsheetMetadata msg, Instant storedFrom) throws RequiredDataMissingException {
        if (msg.getId() == null) {
            throw new RequiredDataMissingException("Flowsheet id not set");
        }
        RowState<VisitObservationType, VisitObservationTypeAudit> typeState = getOrCreateObservationTypeClearingCache(
                msg.getId(), msg.getSourceSystem(), msg.getSourceObservationType(), msg.getLastUpdatedInstant(), storedFrom);
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

        typeState.saveEntityOrAuditLogIfRequired(visitObservationTypeRepo, visitObservationTypeAuditRepo);
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

        RowState<VisitObservationType, VisitObservationTypeAudit> typeState = getOrCreateObservationTypeFromCache(
                msg.getId(), msg.getSourceSystem(), msg.getSourceObservationType(), msg.getLastUpdatedInstant(), storedFrom);
        typeState.saveEntityOrAuditLogIfRequired(visitObservationTypeRepo, visitObservationTypeAuditRepo);

        RowState<VisitObservation, VisitObservationAudit> flowsheetState = getOrCreateFlowsheet(msg, visit, typeState.getEntity(), storedFrom);
        if (flowsheetState.messageShouldBeUpdated(msg.getLastUpdatedInstant())) {
            updateVisitObservation(msg, flowsheetState);
            flowsheetState.saveEntityOrAuditLogIfRequired(visitObservationRepo, visitObservationAuditRepo);
        }
    }

    /**
     * Get existing observation type or create, adding to cache.
     * @param idInApplication Id of the observation in the application (e.g. flowsheet row epic ID)
     * @param sourceSystem    source system
     * @param observationType type of observation (e.g. flowsheet)
     * @param validFrom       Timestamp from which information valid from
     * @param storedFrom      time that emap-core started processing the message
     * @return VisitObservationType
     */
    @Cacheable(value = "visitObservationType", key = "{ #idInApplication, #sourceSystem, #observationType }")
    public RowState<VisitObservationType, VisitObservationTypeAudit> getOrCreateObservationTypeFromCache(
            String idInApplication, String sourceSystem, String observationType, Instant validFrom, Instant storedFrom) {
        return getOrCreateObservationType(idInApplication, sourceSystem, observationType, validFrom, storedFrom);
    }

    /**
     * Get existing observation type or create, evicting cache as we expect new information to be added to the observation type.
     * @param idInApplication Id of the observation in the application (e.g. flowsheet row epic ID)
     * @param sourceSystem    source system
     * @param observationType type of observation (e.g. flowsheet)
     * @param validFrom       Timestamp from which information valid from
     * @param storedFrom      time that emap-core started processing the message
     * @return VisitObservationType
     */
    @CacheEvict(value = "visitObservationType", key = "{ #idInApplication, #sourceSystem, #observationType }")
    public RowState<VisitObservationType, VisitObservationTypeAudit> getOrCreateObservationTypeClearingCache(
            String idInApplication, String sourceSystem, String observationType, Instant validFrom, Instant storedFrom) {
        return getOrCreateObservationType(idInApplication, sourceSystem, observationType, validFrom, storedFrom);
    }

    private RowState<VisitObservationType, VisitObservationTypeAudit> getOrCreateObservationType(
            String flowsheetId, String sourceSystem, String observationType, Instant validFrom, Instant storedFrom) {
        return visitObservationTypeRepo
                .findByIdInApplicationAndSourceSystemAndSourceObservationType(flowsheetId, sourceSystem, observationType)
                .map(vot -> new RowState<>(vot, validFrom, storedFrom, false))
                .orElseGet(() -> createNewType(flowsheetId, sourceSystem, observationType, validFrom, storedFrom));
    }

    /**
     * Create a minimal visit observation type.
     * @param idInApplication Id of the observation in the application (e.g. flowsheet row epic ID)
     * @param sourceSystem    source system
     * @param observationType type of observation (e.g. flowsheet)
     * @param validFrom       Timestamp from which information valid from
     * @param storedFrom      time that emap-core started processing the message
     * @return minimal VisitObservationType wrapped in row state
     */
    private RowState<VisitObservationType, VisitObservationTypeAudit> createNewType(
            String idInApplication, String sourceSystem, String observationType, Instant validFrom, Instant storedFrom) {
        VisitObservationType type = new VisitObservationType(idInApplication, sourceSystem, observationType);
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
        VisitObservation obs = new VisitObservation(visit, observationType, msg.getObservationTime(), msg.getLastUpdatedInstant(), storedFrom);
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

