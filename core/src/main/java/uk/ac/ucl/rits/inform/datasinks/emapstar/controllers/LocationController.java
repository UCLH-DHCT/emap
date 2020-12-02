package uk.ac.ucl.rits.inform.datasinks.emapstar.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.datasinks.emapstar.RowState;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.IncompatibleDatabaseStateException;
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
import uk.ac.ucl.rits.inform.interchange.adt.ImpliedAdtMessage;
import uk.ac.ucl.rits.inform.interchange.adt.RegisterPatient;
import uk.ac.ucl.rits.inform.interchange.adt.SwapLocations;
import uk.ac.ucl.rits.inform.interchange.adt.TransferPatient;
import uk.ac.ucl.rits.inform.interchange.adt.UpdatePatientInfo;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

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
        if (untrustedMessageType(msg)) {
            logger.debug("Message source or message type is untrusted");
            return;
        }

        Location locationEntity = getOrCreateLocation(msg.getFullLocationString().get());
        Instant validFrom = msg.bestGuessAtValidFrom();


        if (messageOutcomeIsSimpleMove(msg)) {
            processMoveMessage(visit, msg, storedFrom, locationEntity, validFrom);
        } else if (msg instanceof DischargePatient) {
            processDischargeMessage(visit, (DischargePatient) msg, storedFrom, locationEntity);
        } else if ((msg instanceof AdtCancellation)) {
            processCancellationMessage(visit, msg, storedFrom, locationEntity, validFrom);
        }
    }

    /**
     * SwapLocations for two visits based on the SwapLocations message information.
     * @param visitA     first of two visits to be swapped
     * @param visitB     second visit to be swapped
     * @param msg        SwapLocations message
     * @param storedFrom when the message has been read by emap core
     * @throws IncompatibleDatabaseStateException if the location visit was created and another open visit location already exists
     */
    @Transactional
    public void swapLocations(HospitalVisit visitA, HospitalVisit visitB, SwapLocations msg, Instant storedFrom)
            throws IncompatibleDatabaseStateException {
        if (msg.getFullLocationString().isUnknown() || msg.getOtherFullLocationString().isUnknown()) {
            logger.debug("SwapLocations message is missing location: {}", msg);
            return;
        }
        Instant validFrom = msg.bestGuessAtValidFrom();
        // get or create first visit location before the swap
        Location locationB = getOrCreateLocation(msg.getOtherFullLocationString().get());
        RowState<LocationVisit, LocationVisitAudit> visitStateA = getOrCreateOpenLocationByLocation(
                visitA, locationB, validFrom, storedFrom);
        // get or create second visit location before the swap
        Location locationA = getOrCreateLocation(msg.getFullLocationString().get());
        RowState<LocationVisit, LocationVisitAudit> visitStateB = getOrCreateOpenLocationByLocation(
                visitB, locationA, validFrom, storedFrom);
        // swap to the correct locations
        visitStateA.assignHl7ValueIfDifferent(
                Hl7Value.buildFromHl7(locationA), visitStateA.getEntity().getLocationId(), visitStateA.getEntity()::setLocationId);
        visitStateB.assignHl7ValueIfDifferent(
                Hl7Value.buildFromHl7(locationB), visitStateB.getEntity().getLocationId(), visitStateB.getEntity()::setLocationId);
        // save newly created or audit
        visitStateA.saveEntityOrAuditLogIfRequired(locationVisitRepo, locationVisitAuditRepo);
        visitStateB.saveEntityOrAuditLogIfRequired(locationVisitRepo, locationVisitAuditRepo);
    }

    /**
     * Ensures that the swap location won't create two open location visits for a hospital visit.
     * @param visit              HospitalVisit
     * @param locationVisitState RowState of the Location Visit
     * @return the LocationVisit entity
     * @throws IncompatibleDatabaseStateException if the location visit was created and another open visit location already exists
     */
    private LocationVisit validateLocationStateAndGetEntity(HospitalVisit visit, RowState<LocationVisit, LocationVisitAudit> locationVisitState)
            throws IncompatibleDatabaseStateException {
        if (locationVisitState.isEntityCreated() && locationVisitRepo.findByHospitalVisitIdAndDischargeTimeIsNull(visit).isPresent()) {
            throw new IncompatibleDatabaseStateException("Open Location to be swapped was not found, but another open location already exists");
        }
        return locationVisitState.getEntity();
    }

    /**
     * @param visit             hospital visit
     * @param msg               Adt Message
     * @param storedFrom        when the message has been read by emap core
     * @param currentLocationId Location entity
     * @param validFrom         message event date time
     */
    private void processMoveMessage(HospitalVisit visit, AdtMessage msg, Instant storedFrom, Location currentLocationId, Instant validFrom) {
        List<LocationVisit> visitLocations = locationVisitRepo.findAllByHospitalVisitId(visit);
        if ((msg instanceof UpdatePatientInfo && !visitLocations.isEmpty())) {
            logger.debug("UpdatePatientInfo where previous visit location for this encounter already exists");
            return;
        }
        if (msg.getFullLocationString().equals(msg.getPreviousLocationString())) {
            logger.debug("Ignoring MoveMessage where the previous and current location strings are the same");
            return;
        }

        List<RowState<LocationVisit, LocationVisitAudit>> savingVisits = new ArrayList<>();
        // Previous location may not exist (ADT^A01 presentation message)
        List<RowState<LocationVisit, LocationVisitAudit>> previousMessages = updateOrCreatePreviousVisitsFromMove(
                visit, msg, visitLocations, validFrom, storedFrom);
        savingVisits.addAll(previousMessages);

        // current location
        RowState<LocationVisit, LocationVisitAudit> currentLocationState = updateOrCreateCurrentLocationFromMove(
                visit, currentLocationId, visitLocations, validFrom, storedFrom);
        if (msg instanceof UpdatePatientInfo) {
            LocationVisit currentLocation = currentLocationState.getEntity();
            currentLocationState.assignIfDifferent(true, currentLocation.getInferredAdmission(), currentLocation::setInferredAdmission);
        }
        savingVisits.add(currentLocationState);

        savingVisits.forEach(rowState -> rowState.saveEntityOrAuditLogIfRequired(locationVisitRepo, locationVisitAuditRepo));
    }

    /**
     * Process discharging of a patient from current location.
     * If current location doesn't exist, create with inferred admission time. Then infer previous location discharge time.
     * If previous location doesnt exist in the database, creates it with an inferred admission and discharge time.
     * @param visit      hospital visit
     * @param msg        DischargePatient Message
     * @param storedFrom when the message has been read by emap core
     * @param location   Location entity
     */
    private void processDischargeMessage(
            HospitalVisit visit, DischargePatient msg, Instant storedFrom, Location location) {


        List<LocationVisit> visitLocations = locationVisitRepo.findAllByHospitalVisitId(visit);
        List<RowState<LocationVisit, LocationVisitAudit>> savingVisits = new ArrayList<>();

        Instant dischargeTime = msg.getDischargeDateTime();
        // Always added after if block so if not overridden with existing current visit, inferred location will be saved.
        RowState<LocationVisit, LocationVisitAudit> currentVisit = inferLocation(visit, location, dischargeTime, dischargeTime, storedFrom);
        Instant inferredDischargeTime = dischargeTime.minus(1, ChronoUnit.SECONDS);

        Optional<LocationVisit> mostRecentOptional = getPreviousLocationVisit(visitLocations, dischargeTime);
        if (mostRecentOptional.isPresent()) {
            LocationVisit mostRecentLocation = mostRecentOptional.get();
            RowState<LocationVisit, LocationVisitAudit> mostRecentExistingState = new RowState<>(
                    mostRecentLocation, dischargeTime, storedFrom, false);
            if (mostRecentLocation.getLocationId().equals(location)) {
                // most recent matches the current location - set this to be the current and discharge
                currentVisit = mostRecentExistingState;
                dischargeLocation(dischargeTime, currentVisit);
            } else {
                // most recent doesn't match current - infer discharge time for most recent and don't override inferred current visit
                setInferredDischargeAndTime(true, inferredDischargeTime, mostRecentExistingState);
                savingVisits.add(mostRecentExistingState);
            }
            // no previous location so infer previous location and don't override inferred current visit
            Optional<Location> previousLocation = getPreviousLocationId(msg);
            if (previousLocation.isPresent()) {
                RowState<LocationVisit, LocationVisitAudit> previousState = inferLocation(
                        visit, previousLocation.get(), inferredDischargeTime, dischargeTime, storedFrom);
                setInferredDischargeAndTime(true, inferredDischargeTime, previousState);
                savingVisits.add(previousState);
            }
        }

        savingVisits.add(currentVisit);
        savingVisits.forEach(rowState -> rowState.saveEntityOrAuditLogIfRequired(locationVisitRepo, locationVisitAuditRepo));
    }

    /**
     * Create or update previous location from message.
     * If message's previous location visit was created and a location existed before that, infer the discharge date for that location.
     * @param visit        hospital visit
     * @param msg          Adt message
     * @param allLocations all locations for this visit, sorted by admission time
     * @param messageTime  time of the message
     * @param storedFrom   when the message has been read by emap core
     * @return updated/created message previous messages
     */
    private List<RowState<LocationVisit, LocationVisitAudit>> updateOrCreatePreviousVisitsFromMove(
            HospitalVisit visit, AdtMessage msg, List<LocationVisit> allLocations, Instant messageTime, Instant storedFrom) {
        Optional<Location> previousLocationId = getPreviousLocationId(msg);
        List<RowState<LocationVisit, LocationVisitAudit>> previousVisits = new ArrayList<>();

        Optional<LocationVisit> optionalPreviousLocation = getPreviousLocationVisit(allLocations, messageTime);
        if (optionalPreviousLocation.isEmpty()) {
            previousLocationId.ifPresent(loc -> previousVisits.add(inferLocation(visit, loc, messageTime, messageTime, storedFrom)));
            return previousVisits;
        }

        LocationVisit previousLocation = optionalPreviousLocation.get();
        RowState<LocationVisit, LocationVisitAudit> previousLocState = new RowState<>(previousLocation, messageTime, storedFrom, false);
        if (previousLocationId.isPresent()) {
            if (previousLocation.getLocationId().equals(previousLocationId.get())) {
                // previous location matches message - set the known discharge time
                setInferredDischargeAndTime(false, messageTime, previousLocState);
            } else {
                // infer location from message location
                previousVisits.add(inferLocation(visit, previousLocationId.get(), messageTime, messageTime, storedFrom));
                // then infer discharge time of existing location
                Instant inferredDischargeTime = messageTime.minus(1, ChronoUnit.SECONDS);
                setInferredDischargeAndTime(true, inferredDischargeTime, previousLocState);
            }
        } else {
            // no previous visit in message, infer discharge for previous message (because we don't know explicitly that it is the previous location)
            setInferredDischargeAndTime(true, messageTime, previousLocState);
        }
        previousVisits.add(previousLocState);
        return previousVisits;
    }

    /**
     * Update existing current location from inferred location in database, or create a new current location.
     * @param visit             hospital visit
     * @param currentLocationId current location
     * @param allLocations      all locations for this visit, sorted by admission time
     * @param validFrom         event datetime of the message
     * @param storedFrom        when the message has been read by emap core
     * @return RowState of the location visit
     */
    private RowState<LocationVisit, LocationVisitAudit> updateOrCreateCurrentLocationFromMove(
            HospitalVisit visit, Location currentLocationId, Collection<LocationVisit> allLocations,
            Instant validFrom, Instant storedFrom) {
        // by default, create a new, open location visit
        RowState<LocationVisit, LocationVisitAudit> currentLocationState = createOpenLocation(visit, currentLocationId, validFrom, storedFrom);

        Optional<LocationVisit> optionalNextLocation = getNextLocationVisit(allLocations, validFrom);
        if (optionalNextLocation.isPresent()) {
            if (optionalNextLocation.get().getInferredAdmission() && optionalNextLocation.get().getLocationId().equals(currentLocationId)) {
                // current location was inferred - update the inferred admission
                LocationVisit currentLocation = optionalNextLocation.get();
                currentLocationState = new RowState<>(currentLocation, validFrom, storedFrom, false);
                currentLocationState.assignIfDifferent(validFrom, currentLocation.getAdmissionTime(), currentLocation::setAdmissionTime);
                currentLocationState.assignIfDifferent(false, currentLocation.getInferredAdmission(), currentLocation::setInferredAdmission);
            } else {
                // next location is real - create new current location with inferred discharge time
                Instant dischargeTime = optionalNextLocation.get().getAdmissionTime();
                setInferredDischargeAndTime(true, dischargeTime, currentLocationState);
            }
        }
        return currentLocationState;
    }

    /**
     * Get previous location.
     * @param msg Adt message
     * @return optional Location
     */
    private Optional<Location> getPreviousLocationId(AdtMessage msg) {
        Location previousLocation = null;
        if (msg.getPreviousLocationString().isSave()) {
            previousLocation = getOrCreateLocation(msg.getPreviousLocationString().get());
        }
        return Optional.ofNullable(previousLocation);
    }


    /**
     * Get most recent message before the current message time.
     * @param visitLocations all location visits for the hospital visit
     * @param messageTime    to filter until
     * @return optional location visit
     */
    private Optional<LocationVisit> getPreviousLocationVisit(Collection<LocationVisit> visitLocations, Instant messageTime) {
        return visitLocations.stream()
                .filter(loc -> !loc.getAdmissionTime().isAfter(messageTime))
                .max(Comparator.comparing(LocationVisit::getAdmissionTime));
    }

    /**
     * Find the next location after the admission time.
     * @param visitLocations all locations for the visit
     * @param messageTime    to filter after
     * @return Optional location visit
     */
    private Optional<LocationVisit> getNextLocationVisit(Collection<LocationVisit> visitLocations, Instant messageTime) {
        return visitLocations.stream()
                .filter(loc -> loc.getAdmissionTime().isAfter(messageTime))
                .min(Comparator.comparing(LocationVisit::getAdmissionTime));
    }

    /**
     * Set the inferred state of discharge, and the discharge time of a location state.
     * @param isInferred    is the discharge time inferred
     * @param dischargeTime time to set the discharge
     * @param locationState to update
     */
    private void setInferredDischargeAndTime(Boolean isInferred, Instant
            dischargeTime, RowState<LocationVisit, LocationVisitAudit> locationState) {
        LocationVisit existingLocation = locationState.getEntity();
        locationState.assignIfDifferent(dischargeTime, existingLocation.getDischargeTime(), existingLocation::setDischargeTime);
        locationState.assignIfDifferent(isInferred, existingLocation.getInferredDischarge(), existingLocation::setInferredDischarge);
    }

    /**
     * @param msg Adt Message
     * @return true if message should not be processed.
     */
    private boolean untrustedMessageType(AdtMessage msg) {
        return msg instanceof ImpliedAdtMessage;
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
            deleteOpenVisitLocation(visit, locationEntity, validFrom, storedFrom);
        } else if (msg instanceof CancelDischargePatient) {
            // Can have an update between a discharge and a cancel discharge, which would create a new open visit, so remove this if this has happened
            deleteOpenVisitLocation(visit, locationEntity, validFrom, storedFrom);
            removeDischargeDateTime(visit, msg, locationEntity, validFrom, storedFrom);
        } else if (msg instanceof CancelTransferPatient) {
            CancelTransferPatient cancelTransferPatient = (CancelTransferPatient) msg;
            Location cancelledLocation = getOrCreateLocation(cancelTransferPatient.getCancelledLocation());
            deleteOpenVisitLocation(visit, cancelledLocation, validFrom, storedFrom);
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
     * @param visit      Hospital Visit
     * @param location   Location
     * @param validFrom  message event date time
     * @param storedFrom time that emap-core encountered the message
     * @return LocationVisit wrapped in Row state
     */
    private RowState<LocationVisit, LocationVisitAudit> getOrCreateOpenLocation(
            HospitalVisit visit, Location location, Instant validFrom, Instant storedFrom) {
        logger.debug("Get or create open location for visit {}", visit);
        return locationVisitRepo.findByHospitalVisitIdAndDischargeTimeIsNull(visit)
                .map(loc -> new RowState<>(loc, validFrom, storedFrom, false))
                .orElseGet(() -> createOpenLocation(visit, location, validFrom, storedFrom));
    }

    private RowState<LocationVisit, LocationVisitAudit> createOpenLocation(
            HospitalVisit visit, Location location, Instant validFrom, Instant storedFrom) {
        LocationVisit locationVisit = new LocationVisit(validFrom, storedFrom, location, visit);
        logger.debug("Created new LocationVisit: {}", locationVisit);
        return new RowState<>(locationVisit, validFrom, storedFrom, true);
    }

    /**
     * Create a location visit with inferred admission time, from a known discharge time.
     * @param visit         Hospital Visit
     * @param location      Location
     * @param dischargeTime time of discharge
     * @param validFrom     message event date time
     * @param storedFrom    time that emap-core encountered the message
     * @return LocationVisit wrapped in Row state
     */
    private RowState<LocationVisit, LocationVisitAudit> inferLocation(
            HospitalVisit visit, Location location, Instant dischargeTime, Instant validFrom, Instant storedFrom) {
        Instant inferredAdmitTime = dischargeTime.minus(1, ChronoUnit.SECONDS);
        LocationVisit inferredLocation = new LocationVisit(inferredAdmitTime, validFrom, storedFrom, location, visit);
        inferredLocation.setInferredAdmission(true);
        inferredLocation.setDischargeTime(dischargeTime);
        logger.debug("Inferred previous LocationVisit: {}", inferredLocation);
        return new RowState<>(inferredLocation, validFrom, storedFrom, true);
    }

    private RowState<LocationVisit, LocationVisitAudit> getOrCreateOpenLocationByLocation(
            HospitalVisit visit, Location location, Instant validFrom, Instant storedFrom) {
        logger.debug("Ger or create open location ({}) for visit {}", location, visit);
        return locationVisitRepo.findByHospitalVisitIdAndLocationIdAndDischargeTimeIsNull(visit, location)
                .map(loc -> new RowState<>(loc, validFrom, storedFrom, false))
                .orElseGet(() -> {
                    LocationVisit locationVisit = new LocationVisit(validFrom, storedFrom, location, visit);
                    logger.debug("Created new LocationVisit: {}", locationVisit);
                    return new RowState<>(locationVisit, validFrom, storedFrom, true);
                });
    }

    /**
     * Update location visit if newly created or if newer.
     * @param locationState Location Visit wrapped in RowState
     * @param msg           Adt Message
     * @return true if the visit should be updated
     */
    private boolean locationVisitShouldBeUpdated(RowState<LocationVisit, LocationVisitAudit> locationState, AdtMessage msg) {
        return locationState.isEntityCreated() || !locationState.getEntity().getValidFrom().isAfter(msg.bestGuessAtValidFrom());
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
     * @param validFrom     Time of the message event
     * @param retiringState RowState of the retiring location visit
     */
    private void dischargeLocation(final Instant validFrom, RowState<LocationVisit, LocationVisitAudit> retiringState) {
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
        RowState<LocationVisit, LocationVisitAudit> visitState = getLatestDischargedOrCreateOpenLocationVisit(
                visit, location, validFrom, storedFrom);
        LocationVisit locationVisit = visitState.getEntity();
        visitState.removeIfExists(locationVisit.getDischargeTime(), locationVisit::setDischargeTime, validFrom);
        logger.debug("removed location visit: {}", locationVisit);
        visitState.saveEntityOrAuditLogIfRequired(locationVisitRepo, locationVisitAuditRepo);
    }

    /**
     * Get the most recent discharged location visit for the location, or create an open visit at this location.
     * @param visit      Hospital Visit
     * @param location   Location entity
     * @param validFrom  message event date time
     * @param storedFrom time that emap-core encountered the message
     * @return LocationVisit wrapped in Row state
     */
    private RowState<LocationVisit, LocationVisitAudit> getLatestDischargedOrCreateOpenLocationVisit(
            HospitalVisit visit, Location location, Instant validFrom, Instant storedFrom) {
        return locationVisitRepo.findFirstByHospitalVisitIdAndLocationIdAndDischargeTimeLessThanEqualOrderByDischargeTimeDesc(
                visit, location, validFrom)
                .map(loc -> new RowState<>(loc, validFrom, storedFrom, false))
                .orElseGet(() -> {
                    // create non-discharged location
                    LocationVisit locationVisit = new LocationVisit(validFrom, storedFrom, location, visit);
                    logger.debug("Created new LocationVisit instead of discharging an existing one: {}", locationVisit);
                    return new RowState<>(locationVisit, validFrom, storedFrom, true);
                });
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
     * @param locationEntity Location entity
     * @param validFrom      Time of the message event
     * @param storedFrom     Time that emap-core encountered the message
     */
    private void deleteOpenVisitLocation(HospitalVisit visit, Location locationEntity, Instant validFrom, Instant storedFrom) {
        RowState<LocationVisit, LocationVisitAudit> retiring = getOrCreateOpenLocation(
                visit, locationEntity, validFrom, storedFrom);
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
