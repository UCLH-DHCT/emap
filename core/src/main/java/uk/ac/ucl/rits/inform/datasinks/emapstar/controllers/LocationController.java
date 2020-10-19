package uk.ac.ucl.rits.inform.datasinks.emapstar.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.datasinks.emapstar.RowState;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.AuditLocationVisitRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LocationRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LocationVisitRepository;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.movement.AuditLocationVisit;
import uk.ac.ucl.rits.inform.informdb.movement.Location;
import uk.ac.ucl.rits.inform.informdb.movement.LocationVisit;
import uk.ac.ucl.rits.inform.interchange.adt.AdtMessage;

import java.time.Instant;

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
    public void updateOrCreateVisitLocation(HospitalVisit visit, AdtMessage msg, Instant storedFrom) {
        if (visit == null || msg.getFullLocationString().isUnknown()) {
            logger.debug("No visit or unknown location for AdtMessage: {}", msg);
            return;
        }
        Location locationEntity = getOrCreateLocation(msg.getFullLocationString().get());
        Instant validFrom = msg.bestGuessAtValidFrom();
        RowState<LocationVisit> existingLocationState = getOrCreateVisitLocation(visit, locationEntity, msg, validFrom, storedFrom);
        final LocationVisit originalLocationVisit = existingLocationState.getEntity().copy();

        if (locationVisitShouldBeUpdated(existingLocationState, msg)) {
            if (messageOutcomeIsNotSimpleMove(msg)) {
                // cancel messages etc.
                updateLocation(msg, existingLocationState);
            } else if (isNewLocationDifferent(locationEntity, originalLocationVisit)) {
                // Only move locations if original and message locations are different
                LocationVisit newLocationVisit = moveToNewLocation(msg, existingLocationState);
                locationVisitRepo.save(newLocationVisit);
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
                .orElseGet(() -> new Location(locationString));
    }


    private RowState<LocationVisit> getOrCreateVisitLocation(HospitalVisit visit, Location location, AdtMessage msg,
                                                             Instant validFrom, Instant storedFrom) {
        // get locations by the hospital visit with no parent visit_location
        // otherwise create visit_location
        LocationVisit locationVisit = new LocationVisit();
        return new RowState<>(locationVisit, Instant.now(), Instant.now(), true);
    }

    private boolean locationVisitShouldBeUpdated(RowState<LocationVisit> locationState, AdtMessage msg) {
        // location visit is not created
        // message valid from is the same or newer than the current locationState or current entity is not from a trusted source
        return false;
    }


    private boolean messageOutcomeIsNotSimpleMove(AdtMessage msg) {
        return false;
    }


    private LocationVisit moveToNewLocation(AdtMessage msg, RowState<LocationVisit> originalLocationState) {
        // discharge original location
        // create new location with current location and
        return new LocationVisit();
    }

    /**
     * Is the new location different from the original entity's location.
     * @param newLocation           new location
     * @param originalLocationVisit original visit location entity
     * @return true if locations are different
     */
    private boolean isNewLocationDifferent(Location newLocation, LocationVisit originalLocationVisit) {
        return !originalLocationVisit.getLocation().equals(newLocation);
    }

    private void updateLocation(AdtMessage msg, RowState<LocationVisit> locationState) {
        // otherwise update the location fk to the new location fk
        // update the source, admission and discharge datetime here too
    }

    private void manuallySaveLocationOrAuditIfRequired(LocationVisit originalLocation, RowState<LocationVisit> locationState,
                                                       Instant validFrom, Instant storedFrom) {
        AuditLocationVisit auditLocation = new AuditLocationVisit(originalLocation, validFrom, storedFrom);
        locationState.saveEntityOrAuditLogIfRequired(auditLocation, locationVisitRepo, auditLocationVisitRepo);
    }
}
