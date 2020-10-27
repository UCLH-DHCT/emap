package uk.ac.ucl.rits.inform.datasinks.emapstar.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.datasinks.emapstar.DataSources;
import uk.ac.ucl.rits.inform.datasinks.emapstar.RowState;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.AuditLocationVisitRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LocationRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LocationVisitRepository;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.movement.AuditLocationVisit;
import uk.ac.ucl.rits.inform.informdb.movement.Location;
import uk.ac.ucl.rits.inform.informdb.movement.LocationVisit;
import uk.ac.ucl.rits.inform.interchange.adt.AdmitPatient;
import uk.ac.ucl.rits.inform.interchange.adt.AdtCancellation;
import uk.ac.ucl.rits.inform.interchange.adt.AdtMessage;
import uk.ac.ucl.rits.inform.interchange.adt.CancelDischargePatient;
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
    private final AuditLocationVisitRepository auditLocationVisitRepo;

    /**
     * Constructor implicitly autowiring beans.
     * @param locationVisitRepo      location visit repo
     * @param locationRepo           location repo
     * @param auditLocationVisitRepo audit location repo
     */
    public LocationController(LocationVisitRepository locationVisitRepo, LocationRepository locationRepo,
                              AuditLocationVisitRepository auditLocationVisitRepo) {
        this.locationVisitRepo = locationVisitRepo;
        this.locationRepo = locationRepo;
        this.auditLocationVisitRepo = auditLocationVisitRepo;
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
        RowState<LocationVisit> existingLocationState = getOrCreateVisitLocation(visit, locationEntity, msg.getSourceSystem(), validFrom, storedFrom);
        final LocationVisit originalLocationVisit = existingLocationState.getEntity().copy();

        if (locationVisitShouldBeUpdated(existingLocationState, msg)) {
            if (messageOutcomeIsSimpleMove(msg)) {
                if (isNewLocationDifferent(locationEntity, originalLocationVisit)) {
                    moveToNewLocation(msg.getSourceSystem(), locationEntity, visit, validFrom, storedFrom, existingLocationState);
                } else if (!existingLocationState.isEntityCreated()) {
                    logger.debug("Move message doesn't change the location: {}", msg);
                }
            } else {
                if (msg instanceof DischargePatient) {
                    dischargeLocation(msg, validFrom, storedFrom, existingLocationState);
                } else if (msg instanceof CancelDischargePatient) {
                    removeDischargeLocation(msg, validFrom, storedFrom, existingLocationState);
                } else if (msg instanceof AdtCancellation) {
                    removeLocation(msg, validFrom, storedFrom, existingLocationState);
                } else if (msg instanceof SwapLocations) {
                    swapLocations(msg, validFrom, storedFrom, existingLocationState);
                }
                // cancel messages etc.
                updateLocation(msg, existingLocationState);
            }
            manuallySaveLocationOrAuditIfRequired(originalLocationVisit, existingLocationState, validFrom, storedFrom);
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
     * Get existing visit location or create a new one.
     * @param visit        Hospital Visit
     * @param location     Location
     * @param sourceSystem source system
     * @param validFrom    message event date time
     * @param storedFrom   time that emap-core encountered the message
     * @return LocationVisit wrapped in Row state
     */
    private RowState<LocationVisit> getOrCreateVisitLocation(HospitalVisit visit, Location location, String sourceSystem,
                                                             Instant validFrom, Instant storedFrom) {
        return locationVisitRepo.findByHospitalVisitIdAndDischargeTimeIsNull(visit)
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

    private void dischargeLocation(AdtMessage msg, Instant validFrom, Instant storedFrom, RowState<LocationVisit> existingLocationState) {
    }

    private void removeDischargeLocation(AdtMessage msg, Instant validFrom, Instant storedFrom, RowState<LocationVisit> existingLocationState) {
    }

    private void removeLocation(AdtMessage msg, Instant validFrom, Instant storedFrom, RowState<LocationVisit> existingLocationState) {
    }

    private void swapLocations(AdtMessage msg, Instant validFrom, Instant storedFrom, RowState<LocationVisit> existingLocationState) {
    }

    private void updateLocation(AdtMessage msg, RowState<LocationVisit> locationState) {
        // otherwise update the location fk to the new location fk
        // update the source, admission and discharge datetime here too
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
        AuditLocationVisit auditLocation = new AuditLocationVisit(originalLocation, validFrom, storedFrom);
        locationState.saveEntityOrAuditLogIfRequired(auditLocation, locationVisitRepo, auditLocationVisitRepo);
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
                .forEach(locationVisit -> {
                    auditLocationVisitRepo.save(new AuditLocationVisit(locationVisit, validFrom, storedFrom));
                    logger.debug("Deleting LocationVisit: {}", locationVisit);
                    locationVisitRepo.delete(locationVisit);
                });
    }
}
