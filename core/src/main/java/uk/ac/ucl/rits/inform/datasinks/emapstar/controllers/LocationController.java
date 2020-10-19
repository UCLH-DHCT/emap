package uk.ac.ucl.rits.inform.datasinks.emapstar.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.Optional;

/**
 * Controls interaction with Locations.
 */
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
        String locationString = msg.getFullLocationString().get();
        Optional<Location> potentialLocation = locationRepo.findByLocationStringEquals(locationString);
        if (potentialLocation.isEmpty()) {
            throw new IllegalArgumentException(
                    String.format("Location string '%s' was not found in location table for message: %s", locationString, msg));
        }

        Instant validFrom = getValidFrom(msg);
        RowState<LocationVisit> locationState = getOrCreateParentLocationVisit(visit, potentialLocation.get(), msg, validFrom, storedFrom);
        if (locationVisitHasParent(msg)) {
            locationState = getOrCreateChildLocationVisit(locationState, potentialLocation.get(), msg, validFrom, storedFrom);
        }
        final LocationVisit originalLocation = locationState.getEntity().copy();

        if (locationVisitShouldBeUpdated(locationState, msg)) {
            updateLocation(msg, locationState);
            manuallySaveLocationOrAuditIfRequired(originalLocation, locationState, validFrom, storedFrom);
        }
    }


    /**
     * If the event occurred exists, use it. Otherwise use the event recorded date time.
     * @param msg Adt message
     * @return the correct Instant for valid from.
     */
    private Instant getValidFrom(AdtMessage msg) {
        // should this just be a method in AdtMessage and used for all valid from?
        return (msg.getEventOccurredDateTime() == null) ? msg.getRecordedDateTime() : msg.getEventOccurredDateTime();
    }

    private RowState<LocationVisit> getOrCreateParentLocationVisit(HospitalVisit visit, Location location, AdtMessage msg,
                                                                   Instant validFrom, Instant storedFrom) {
        // get locations by the hospital visit with no parent visit_location
        // otherwise create visit_location
        LocationVisit locationVisit = new LocationVisit();
        return new RowState<>(locationVisit, Instant.now(), Instant.now(), true);
    }


    private boolean locationVisitHasParent(AdtMessage msg) {
        // message is for a transient visit, e.g. where a bed visit is not given up while they go to the MRI scanner.
        return false;
    }

    private RowState<LocationVisit> getOrCreateChildLocationVisit(RowState<LocationVisit> parentLocationState, Location location, AdtMessage msg,
                                                                  Instant validFrom, Instant storedFrom) {
        // get location by parent location visit id
        // otherwise create the child visit
        // if created location_visit and it's meant to have a parent, throw an invalid state error because the parent doesn't exist
        LocationVisit locationVisit = new LocationVisit();
        return new RowState<>(locationVisit, Instant.now(), Instant.now(), true);
    }

    private boolean locationVisitShouldBeUpdated(RowState<LocationVisit> locationState, AdtMessage msg) {
        // message valid from is the same or newer than the current locationState
        return false;
    }

    private void updateLocation(AdtMessage msg, RowState<LocationVisit> locationState) {
        // if the message has a parent and the message moves back to parent location, then remove the child location
        // otherwise update the location fk to the new location fk
        // on discharge, do we just delete the row?
        // if using, update the admission and discharge datetime here too
    }

    private void manuallySaveLocationOrAuditIfRequired(LocationVisit originalLocation, RowState<LocationVisit> locationState,
                                                       Instant validFrom, Instant storedFrom) {
        // would be nice to create the audit entity within the generic function but I guess I'd need to make the AuditCore be an abstract class?
        AuditLocationVisit auditLocation = new AuditLocationVisit(originalLocation, validFrom, storedFrom);
        locationState.saveEntityOrAuditLogIfRequired(auditLocation, locationVisitRepo, auditLocationVisitRepo);
    }


}
