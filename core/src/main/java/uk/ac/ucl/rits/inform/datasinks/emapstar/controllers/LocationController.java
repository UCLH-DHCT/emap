package uk.ac.ucl.rits.inform.datasinks.emapstar.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.datasinks.emapstar.DataSources;
import uk.ac.ucl.rits.inform.datasinks.emapstar.RowState;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LocationRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LocationVisitAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LocationVisitRepository;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.movement.Location;
import uk.ac.ucl.rits.inform.informdb.movement.LocationVisit;
import uk.ac.ucl.rits.inform.informdb.movement.LocationVisitAudit;
import uk.ac.ucl.rits.inform.interchange.Hl7Value;
import uk.ac.ucl.rits.inform.interchange.adt.AdmitPatient;
import uk.ac.ucl.rits.inform.interchange.adt.AdtCancellation;
import uk.ac.ucl.rits.inform.interchange.adt.AdtMessage;
import uk.ac.ucl.rits.inform.interchange.adt.CancelAdmitPatient;
import uk.ac.ucl.rits.inform.interchange.adt.CancelDischargePatient;
import uk.ac.ucl.rits.inform.interchange.adt.CancelTransferPatient;
import uk.ac.ucl.rits.inform.interchange.adt.DischargePatient;
import uk.ac.ucl.rits.inform.interchange.adt.RegisterPatient;
import uk.ac.ucl.rits.inform.interchange.adt.SwapLocations;
import uk.ac.ucl.rits.inform.interchange.adt.TransferPatient;
import uk.ac.ucl.rits.inform.interchange.adt.UpdatePatientInfo;

import java.time.Instant;
import java.util.Collection;

/**
 * Controls interaction with Locations.
 * @author Stef Piatek
 */
@Component
public class LocationController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final LocationVisitRepository locationVisitRepo;
    private final LocationRepository locationRepo;
    private final LocationVisitAuditRepository locationVisitAuditRepo;

    /**
     * Constructor implicitly autowiring beans.
     * @param locationVisitRepo      location visit repo
     * @param locationRepo           location repo
     * @param locationVisitAuditRepo audit location repo
     */
    public LocationController(LocationVisitRepository locationVisitRepo, LocationRepository locationRepo,
                              LocationVisitAuditRepository locationVisitAuditRepo) {
        this.locationVisitRepo = locationVisitRepo;
        this.locationRepo = locationRepo;
        this.locationVisitAuditRepo = locationVisitAuditRepo;
    }

    /**
     * Update existing location visit or create it, from the Adt Message.
     * @param visit      hospital visit
     * @param msg        Adt Message
     * @param storedFrom when the message has been read by emap core
     */
    @Transactional
    public void processVisitLocation(HospitalVisit visit, AdtMessage msg, Instant storedFrom) {
        if (visit == null || msg.getFullLocationString().isUnknown()) {
            logger.debug("No visit or unknown location for AdtMessage: {}", msg);
            return;
        }
        Location locationEntity = getOrCreateLocation(msg.getFullLocationString().get());
        Instant validFrom = msg.bestGuessAtValidFrom();
        if (messageOutcomeIsSimpleMove(msg) || msg instanceof DischargePatient) {
            processMoveOrDischarge(visit, msg, storedFrom, locationEntity, validFrom);
        } else if ((msg instanceof AdtCancellation)) {
            processCancellationMessage(visit, msg, storedFrom, locationEntity, validFrom);
        }
    }

    @Transactional
    public void swapLocations(HospitalVisit visitA, HospitalVisit visitB, SwapLocations msg, Instant storedFrom) {
        if (msg.getFullLocationString().isUnknown() || msg.getOtherFullLocationString().isUnknown()) {
            logger.debug("SwapLocations message is missing location: {}", msg);
            return;
        }
        Instant validFrom = msg.bestGuessAtValidFrom();
        // get or create first visit location before the swap
        Location locationB = getOrCreateLocation(msg.getOtherFullLocationString().get());
        RowState<LocationVisit> visitStateA = getOrCreateOpenLocationByLocation(
                visitA, locationB, msg.getSourceSystem(), validFrom, storedFrom);
        final LocationVisit originalVisitA = validateLocationStateAndGetEntity(visitA, visitStateA);
        // get or create second visit location before the swap
        Location locationA = getOrCreateLocation(msg.getFullLocationString().get());
        RowState<LocationVisit> visitStateB = getOrCreateOpenLocationByLocation(
                visitB, locationA, msg.getSourceSystem(), validFrom, storedFrom);
        final LocationVisit originalVisitB = validateLocationStateAndGetEntity(visitB, visitStateB);
        // swap to the correct locations
        visitStateA.assignHl7ValueIfDifferent(
                Hl7Value.buildFromHl7(locationA), visitStateA.getEntity().getLocation(), visitStateA.getEntity()::setLocation);
        visitStateB.assignHl7ValueIfDifferent(
                Hl7Value.buildFromHl7(locationB), visitStateB.getEntity().getLocation(), visitStateB.getEntity()::setLocation);
        // save newly created or audit
        manuallySaveLocationOrAuditIfRequired(originalVisitA, visitStateA, validFrom, storedFrom);
        manuallySaveLocationOrAuditIfRequired(originalVisitB, visitStateB, validFrom, storedFrom);
    }

    /**
     * Ensures that the swap location won't create two open location visits for a hospital visit.
     * @param visit              HospitalVisit
     * @param locationVisitState RowState of the Location Visit
     * @return the LocationVisit entity
     * @throws IllegalStateException if the location visit was created and another open visit location already exists
     */
    private LocationVisit validateLocationStateAndGetEntity(HospitalVisit visit, RowState<LocationVisit> locationVisitState)
            throws IllegalStateException {
        if (locationVisitState.isEntityCreated() && locationVisitRepo.findByHospitalVisitIdAndDischargeTimeIsNull(visit).isPresent()) {
            throw new IllegalStateException("Open Location to be swapped was not found, but another open location already exists");
        }
        return locationVisitState.getEntity();
    }


    /**
     * @param visit          hospital visit
     * @param msg            Adt Message
     * @param storedFrom     when the message has been read by emap core
     * @param locationEntity Location entity
     * @param validFrom      message event date time
     */
    private void processMoveOrDischarge(HospitalVisit visit, AdtMessage msg, Instant storedFrom, Location locationEntity, Instant validFrom) {
        RowState<LocationVisit> existingLocationState = getOrCreateOpenLocation(visit, locationEntity, msg.getSourceSystem(), validFrom, storedFrom);
        final LocationVisit originalLocationVisit = existingLocationState.getEntity().copy();

        if (locationVisitShouldBeUpdated(existingLocationState, msg)) {
            if (msg instanceof DischargePatient) {
                dischargeLocation(validFrom, existingLocationState);
            } else {
                if (isNewLocationDifferent(locationEntity, originalLocationVisit)) {
                    moveToNewLocation(msg.getSourceSystem(), locationEntity, visit, validFrom, storedFrom, existingLocationState);
                } else if (!existingLocationState.isEntityCreated()) {
                    logger.debug("Move message doesn't change the location: {}", msg);
                }
            }
            manuallySaveLocationOrAuditIfRequired(originalLocationVisit, existingLocationState, validFrom, storedFrom);
        }
    }

    /**
     * Update existing location visit or create it, from the Adt Message.
     * @param visit          hospital visit
     * @param msg            Adt Message
     * @param storedFrom     when the message has been read by emap core
     * @param locationEntity Location entity
     * @param validFrom      message event date time
     */
    @Transactional
    public void processCancellationMessage(HospitalVisit visit, AdtMessage msg, Instant storedFrom, Location locationEntity, Instant validFrom) {
        if (msg instanceof CancelAdmitPatient) {
            deleteOpenVisitLocation(visit, msg, locationEntity, validFrom, storedFrom);
        } else if (msg instanceof CancelDischargePatient) {
            // Can have an update between a discharge and a cancel discharge, which would create a new open visit, so remove this if this has happened
            deleteOpenVisitLocation(visit, msg, locationEntity, validFrom, storedFrom);
            removeDischargeDateTime(visit, msg, locationEntity, validFrom, storedFrom);
        } else if (msg instanceof CancelTransferPatient) {
            CancelTransferPatient cancelTransferPatient = (CancelTransferPatient) msg;
            Location cancelledLocation = getOrCreateLocation(cancelTransferPatient.getCancelledLocation());
            deleteOpenVisitLocation(visit, msg, cancelledLocation, validFrom, storedFrom);
            removeDischargeDateTime(visit, msg, locationEntity, validFrom, storedFrom);
        }
    }

    /**
     * Gets location entity by string if it exists, otherwise creates it.
     * @param locationString full location string.
     * @return Location entity
     */
    private Location getOrCreateLocation(String locationString) {
        return locationRepo.findByLocationStringEquals(locationString)
                .orElseGet(() -> {
                    Location location = new Location(locationString);
                    return locationRepo.save(location);
                });
    }

    /**
     * Get open visit location or create a new one.
     * @param visit        Hospital Visit
     * @param location     Location
     * @param sourceSystem source system
     * @param validFrom    message event date time
     * @param storedFrom   time that emap-core encountered the message
     * @return LocationVisit wrapped in Row state
     */
    private RowState<LocationVisit> getOrCreateOpenLocation(HospitalVisit visit, Location location, String sourceSystem,
                                                            Instant validFrom, Instant storedFrom) {
        logger.debug("Get or create open location for visit {}", visit);
        return locationVisitRepo.findByHospitalVisitIdAndDischargeTimeIsNull(visit)
                .map(loc -> new RowState<>(loc, validFrom, storedFrom, false))
                .orElseGet(() -> {
                    LocationVisit locationVisit = new LocationVisit(validFrom, storedFrom, location, visit, sourceSystem);
                    logger.debug("Created new LocationVisit: {}", locationVisit);
                    return new RowState<>(locationVisit, validFrom, storedFrom, true);
                });
    }

    private RowState<LocationVisit> getOrCreateOpenLocationByLocation(HospitalVisit visit, Location location, String sourceSystem,
                                                                      Instant validFrom, Instant storedFrom) {
        logger.debug("Ger or create open location ({}) for visit {}", location, visit);
        return locationVisitRepo.findByHospitalVisitIdAndLocationIdAndDischargeTimeIsNull(visit, location)
                .map(loc -> new RowState<>(loc, validFrom, storedFrom, false))
                .orElseGet(() -> {
                    LocationVisit locationVisit = new LocationVisit(validFrom, storedFrom, location, visit, sourceSystem);
                    logger.debug("Created new LocationVisit: {}", locationVisit);
                    return new RowState<>(locationVisit, validFrom, storedFrom, true);
                });
    }

    /**
     * Update location visit if message is from a trusted source update if newer or if database source isn't trusted.
     * Otherwise only update if if is newly created.
     * @param locationState Location Visit wrapped in RowState
     * @param msg           Adt Message
     * @return true if the visit should be updated
     */
    private boolean locationVisitShouldBeUpdated(RowState<LocationVisit> locationState, AdtMessage msg) {
        // always update if a message is created
        if (locationState.isEntityCreated()) {
            return true;
        }
        LocationVisit visit = locationState.getEntity();
        // if message source is trusted and (entity source system is untrusted or message is newer)
        return DataSources.isTrusted(msg.getSourceSystem())
                && (!DataSources.isTrusted(visit.getSourceSystem()) || !visit.getValidFrom().isAfter(msg.bestGuessAtValidFrom()));
    }


    /**
     * Is message outcome a simple move?
     * @param msg AdtMessage
     * @return true if a message outcome is moving from one location to another.
     */
    private boolean messageOutcomeIsSimpleMove(AdtMessage msg) {
        return msg instanceof TransferPatient || msg instanceof UpdatePatientInfo || msg instanceof AdmitPatient || msg instanceof RegisterPatient;
    }


    /**
     * Discharge from old location, and admit to new location (saving the new entity).
     * @param sourceSystem   Source system of the message
     * @param locationEntity Location entity
     * @param visit          Hospital visit entity
     * @param validFrom      Time of the message event
     * @param storedFrom     Time that emap-core encountered the message
     * @param retiringState  RowState of the retiring location visit
     */
    private void moveToNewLocation(String sourceSystem, Location locationEntity, HospitalVisit visit,
                                   Instant validFrom, Instant storedFrom, RowState<LocationVisit> retiringState) {
        LocationVisit retiring = retiringState.getEntity();
        logger.debug("Discharging visit: {}", retiring);
        retiringState.assignIfDifferent(validFrom, retiring.getDischargeTime(), retiring::setDischargeTime);

        LocationVisit newLocation = new LocationVisit(validFrom, storedFrom, locationEntity, visit, sourceSystem);
        logger.debug("New visit: {}", newLocation);
        locationVisitRepo.save(newLocation);
    }

    /**
     * Is the new location different from the original entity's location.
     * @param newLocation           new location
     * @param originalLocationVisit original visit location entity
     * @return true if locations are different
     */
    private boolean isNewLocationDifferent(Location newLocation, LocationVisit originalLocationVisit) {
        return !newLocation.equals(originalLocationVisit.getLocation());
    }

    /**
     * @param validFrom     Time of the message event
     * @param retiringState RowState of the retiring location visit
     */
    private void dischargeLocation(final Instant validFrom, RowState<LocationVisit> retiringState) {
        LocationVisit location = retiringState.getEntity();
        retiringState.assignIfDifferent(validFrom, location.getDischargeTime(), location::setDischargeTime);
        logger.debug("Discharged location visit: {}", location);
    }

    /**
     * remove discharge date time from most location visit at the location.
     * @param visit      Hospital Visit
     * @param msg        Adt message
     * @param location   Location entity
     * @param validFrom  message event date time
     * @param storedFrom time that emap-core encountered the message
     */
    private void removeDischargeDateTime(HospitalVisit visit, AdtMessage msg, Location location, Instant validFrom, Instant storedFrom) {
        RowState<LocationVisit> visitState = getLatestDischargedOrCreateOpenLocationVisit(visit, location, msg, validFrom, storedFrom);
        LocationVisit locationVisit = visitState.getEntity();
        LocationVisit originalLocationVisit = locationVisit.copy();
        visitState.removeIfExists(locationVisit.getDischargeTime(), locationVisit::setDischargeTime, validFrom);
        logger.debug("removed location visit: {}", locationVisit);
        manuallySaveLocationOrAuditIfRequired(originalLocationVisit, visitState, validFrom, storedFrom);

    }

    /**
     * Get the most recent discharged location visit for the location, or create an open visit at this location.
     * @param visit      Hospital Visit
     * @param msg        Adt message
     * @param location   Location entity
     * @param validFrom  message event date time
     * @param storedFrom time that emap-core encountered the message
     * @return LocationVisit wrapped in Row state
     */
    private RowState<LocationVisit> getLatestDischargedOrCreateOpenLocationVisit(HospitalVisit visit, Location location, AdtMessage msg,
                                                                                 Instant validFrom, Instant storedFrom) {
        return locationVisitRepo.findFirstByHospitalVisitIdAndLocationIdAndDischargeTimeLessThanEqualOrderByDischargeTimeDesc(
                visit, location, validFrom)
                .map(loc -> new RowState<>(loc, validFrom, storedFrom, false))
                .orElseGet(() -> {
                    // create non-discharged location
                    LocationVisit locationVisit = new LocationVisit(validFrom, storedFrom, location, visit, msg.getSourceSystem());
                    logger.debug("Created new LocationVisit instead of discharging an existing one: {}", locationVisit);
                    return new RowState<>(locationVisit, validFrom, storedFrom, true);
                });
    }

    /**
     * Save location or audit of location.
     * @param originalLocation original location entity
     * @param locationState    Location Visit wrapped in RowState
     * @param validFrom        Time of the message event
     * @param storedFrom       Time that emap-core encountered the message
     */
    private void manuallySaveLocationOrAuditIfRequired(LocationVisit originalLocation, RowState<LocationVisit> locationState,
                                                       Instant validFrom, Instant storedFrom) {
        LocationVisitAudit auditLocation = new LocationVisitAudit(originalLocation, validFrom, storedFrom);
        locationState.saveEntityOrAuditLogIfRequired(auditLocation, locationVisitRepo, locationVisitAuditRepo);
    }

    /**
     * Log current state and delete location visit.
     * @param validFrom     Time of the message event
     * @param storedFrom    Time that emap-core encountered the message
     * @param locationVisit Location visit to be deleted
     */
    private void deleteLocationVisit(Instant validFrom, Instant storedFrom, LocationVisit locationVisit) {
        locationVisitAuditRepo.save(new LocationVisitAudit(locationVisit, validFrom, storedFrom));
        logger.info("Deleting LocationVisit: {}", locationVisit);
        locationVisitRepo.delete(locationVisit);
    }

    /**
     * Delete the open visit location for the hospital visit.
     * @param visit          Hospital visit entity
     * @param msg            AdtMessage
     * @param locationEntity Location entity
     * @param validFrom      Time of the message event
     * @param storedFrom     Time that emap-core encountered the message
     */
    private void deleteOpenVisitLocation(HospitalVisit visit, AdtMessage msg, Location locationEntity, Instant validFrom, Instant storedFrom) {
        RowState<LocationVisit> retiring = getOrCreateOpenLocation(visit, locationEntity, msg.getSourceSystem(), validFrom, storedFrom);
        if (!retiring.isEntityCreated()) {
            // if the location visit has just been created, we just don't save it. Otherwise delete it.
            deleteLocationVisit(validFrom, storedFrom, retiring.getEntity());
        }
    }

    /**
     * Delete all location visits for list of hospital visits.
     * @param visits     Hospital visits
     * @param validFrom  Time of the message event
     * @param storedFrom Time that emap-core encountered the message
     */
    public void deleteLocationVisits(Collection<HospitalVisit> visits, Instant validFrom, Instant storedFrom) {
        visits.stream()
                .flatMap(visit -> locationVisitRepo.findAllByHospitalVisitId(visit).stream())
                .forEach(locationVisit -> deleteLocationVisit(validFrom, storedFrom, locationVisit));
    }
}
