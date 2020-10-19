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
import uk.ac.ucl.rits.inform.informdb.movement.LocationVisit;
import uk.ac.ucl.rits.inform.interchange.adt.AdtMessage;

import java.time.Instant;

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
    public LocationController(LocationVisitRepository locationVisitRepo, LocationRepository locationRepo, AuditLocationVisitRepository auditLocationVisitRepo) {
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
        Instant validFrom = getValidFrom(msg);
        RowState<LocationVisit> locationState = getOrCreateLocationVisit(visit, msg, validFrom, storedFrom);
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
        return (msg.getEventOccurredDateTime() == null) ? msg.getRecordedDateTime() : msg.getEventOccurredDateTime();
    }

    private RowState<LocationVisit> getOrCreateLocationVisit(HospitalVisit visit, AdtMessage msg, Instant validFrom, Instant storedFrom) {
        // TODO
        LocationVisit location = new LocationVisit();
        return new RowState<>(location, Instant.now(), Instant.now(), true);
    }

    private boolean locationVisitShouldBeUpdated(RowState<LocationVisit> locationState, AdtMessage msg) {
        // TODO
        return false;
    }

    private void updateLocation(AdtMessage msg, RowState<LocationVisit> locationState) {
        //TODO
    }

    private void manuallySaveLocationOrAuditIfRequired(LocationVisit originalLocation, RowState<LocationVisit> locationState,
                                                       Instant validFrom, Instant storedFrom) {
        // would be nice to create the audit entity within the generic function but I guess I'd need to make the AuditCore be an abstract class?
        AuditLocationVisit auditLocation = new AuditLocationVisit(originalLocation, validFrom, storedFrom);
        locationState.saveEntityOrAuditLogIfRequired(auditLocation, locationVisitRepo, auditLocationVisitRepo);
    }


}
