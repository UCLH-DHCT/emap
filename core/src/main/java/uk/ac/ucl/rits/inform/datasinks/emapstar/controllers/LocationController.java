package uk.ac.ucl.rits.inform.datasinks.emapstar.controllers;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
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
        List<LocationVisit> visitLocations = locationVisitRepo.findAllByHospitalVisitIdOrderByAdmissionTimeDesc(visit);
        if ((msg instanceof UpdatePatientInfo && !visitLocations.isEmpty())) {
            logger.debug("UpdatePatientInfo where previous visit location for this encounter already exists");
            return;
        }
        if (msg.getFullLocationString().equals(msg.getPreviousLocationString())) {
            logger.debug("Ignoring MoveMessage where the previous and current location strings are the same");
            return;
        }

        Pair<Long, RowState<LocationVisit, LocationVisitAudit>> indexAndNextLocation = getIndexOfCurrentAndNextLocationVisit(
                visitLocations, currentLocationId, validFrom, storedFrom);
        Long indexCurrentOrPrevious = indexAndNextLocation.getLeft();
        RowState<LocationVisit, LocationVisitAudit> nextLocation = indexAndNextLocation.getRight();

        RowState<LocationVisit, LocationVisitAudit> currentLocation = getOrCreateCurrentLocation(
                visit, storedFrom, currentLocationId, validFrom, visitLocations, indexCurrentOrPrevious);
        updateCurrentVisitIfRequired(nextLocation, validFrom, currentLocation);
        // If the current location is not created, then it was found - so increment counter for previous location
        if (!currentLocation.isEntityCreated()) {
            indexCurrentOrPrevious += 1;
        }
        List<RowState<LocationVisit, LocationVisitAudit>> previousVisits = updateOrCreatePreviousMoveLocations(
                visit, msg, storedFrom, validFrom, visitLocations, indexCurrentOrPrevious);

        saveAllLocationVisitsIfRequired(currentLocation, previousVisits);

    }

    /**
     * Get the index of the current or previous location (if exists) and the next location (if exists).
     * @param visitLocations visit locations in descending order of admission time
     * @param validFrom      message event date time
     * @param storedFrom     when the message has been read by emap core
     * @return Pair of nullable values: <index of the current or previous visit, next location visit>
     */
    private Pair<Long, RowState<LocationVisit, LocationVisitAudit>> getIndexOfCurrentAndNextLocationVisit(
            List<LocationVisit> visitLocations, Location currentLocation, Instant validFrom, Instant storedFrom) {

        RowState<LocationVisit, LocationVisitAudit> nextLocation = null;
        Long indexCurrentOrPrevious = null;
        for (LocationVisit location : visitLocations) {
            if (validFrom.isBefore(location.getAdmissionTime())) {
                nextLocation = new RowState<>(location, validFrom, storedFrom, false);
                indexCurrentOrPrevious = incrementNullable(indexCurrentOrPrevious);
            } else {
                indexCurrentOrPrevious = incrementNullable(indexCurrentOrPrevious);
                // exit early because we now know the index before the next location
                break;
            }
        }
        if (nextLocation != null && nextLocation.getEntity().getLocationId().equals(currentLocation)
                && nextLocation.getEntity().getInferredAdmission() && !nextLocation.getEntity().getInferredDischarge()
                && nextLocation.getEntity().getDischargeTime() != null) {
            logger.debug("Next location is inferred discharge for the same location, resetting it to be the current visit");
            nextLocation = null;
            indexCurrentOrPrevious -= 1;
        }

        return new ImmutablePair<>(indexCurrentOrPrevious, nextLocation);
    }

    /**
     * Get or create current location.
     * @param visit             hospital visit
     * @param storedFrom        time that emap star encountered the message
     * @param currentLocationId current location
     * @param validFrom         event time of the message
     * @param visitLocations    visit locations in descending order of admission time
     * @param indexOfCurrent    index of potential current location
     * @return current location visit
     */
    private RowState<LocationVisit, LocationVisitAudit> getOrCreateCurrentLocation(
            HospitalVisit visit, Instant storedFrom, Location currentLocationId, Instant validFrom,
            List<LocationVisit> visitLocations, Long indexOfCurrent) {

        RowState<LocationVisit, LocationVisitAudit> currentLocation;
        if (indexInRange(visitLocations, indexOfCurrent)) {
            LocationVisit location = visitLocations.get(indexOfCurrent.intValue());
            if (location.getLocationId().equals(currentLocationId)) {
                logger.debug("Current location found");
                currentLocation = new RowState<>(location, validFrom, storedFrom, false);
            } else {
                currentLocation = createOpenLocation(visit, currentLocationId, validFrom, storedFrom);
            }
        } else {
            currentLocation = createOpenLocation(visit, currentLocationId, validFrom, storedFrom);
        }
        return currentLocation;
    }

    /**
     * Discharge current visit if next visit exists and set admission time if it was inferred previously.
     * @param nextLocation    next location wrapped in row state
     * @param validFrom       event time from message
     * @param currentLocation current location wrapped in row state
     */
    private void updateCurrentVisitIfRequired(final RowState<LocationVisit, LocationVisitAudit> nextLocation,
                                              Instant validFrom, RowState<LocationVisit, LocationVisitAudit> currentLocation) {
        if (nextLocation != null) {
            logger.debug("Next location exists in star, discharging the current visit");
            Instant dischargeTime = nextLocation.getEntity().getAdmissionTime();
            setInferredDischargeAndTime(true, dischargeTime, currentLocation);
        }
        if (currentLocation.getEntity().getInferredAdmission()) {
            logger.debug("Current location was inferred, setting the admission time");
            setInferredAdmissionAndTime(false, validFrom, currentLocation);
        }
    }

    /**
     * Update or create previous message(s), ensuring that they are discharged.
     * If message has previous location, create or update the location.
     * If message previous location doesn't (exist or match existing previous location), infer discharge of existing previous location.
     * If no message previous location and no existing previous locations, return empty list
     * @param visit                  hospital visit
     * @param msg                    hl7 message
     * @param storedFrom             time that emap star started processing the message
     * @param validFrom              event time from the hl7 message
     * @param visitLocations         in descending order of admission time
     * @param indexOfPreviousMessage index of previous message
     * @return list of zero or more previous visits
     */
    private List<RowState<LocationVisit, LocationVisitAudit>> updateOrCreatePreviousMoveLocations(
            HospitalVisit visit, AdtMessage msg, Instant storedFrom, Instant validFrom,
            List<LocationVisit> visitLocations, Long indexOfPreviousMessage) {

        Optional<Location> previousLocationId = getPreviousLocationId(msg);
        RowState<LocationVisit, LocationVisitAudit> previousHl7Location = null;

        List<RowState<LocationVisit, LocationVisitAudit>> previousLocations = new ArrayList<>();
        if (indexInRange(visitLocations, indexOfPreviousMessage)) {
            LocationVisit existingLocation = visitLocations.get(indexOfPreviousMessage.intValue());
            if (previousLocationId.isPresent()) {
                if (previousLocationId.get().equals(existingLocation.getLocationId())) {
                    logger.debug("Previous location matches hl7 value: setting known discharge time");
                    previousHl7Location = new RowState<>(existingLocation, validFrom, storedFrom, false);
                    setInferredDischargeAndTime(false, validFrom, previousHl7Location);
                } else {
                    logger.debug("Previous location doesn't match hl7: inferring hl7 previous location");
                    previousHl7Location = createLocationWithInferredAdmit(visit, previousLocationId.get(), validFrom, validFrom, storedFrom);

                    if (openLocationOrInferredDischarge(existingLocation)) {
                        logger.debug("Inferring discharge of previous location");
                        RowState<LocationVisit, LocationVisitAudit> existingPrevious = new RowState<>(existingLocation, validFrom, storedFrom, false);
                        Instant inferredDischargeTime = validFrom.minus(1, ChronoUnit.SECONDS);
                        setInferredDischargeAndTime(true, inferredDischargeTime, existingPrevious);
                        previousLocations.add(existingPrevious);
                    }
                }
            } else {
                if (openLocationOrInferredDischarge(existingLocation)) {
                    logger.debug("No previous hl7 location, but found existing previous location. Inferring existing location discharge.");
                    RowState<LocationVisit, LocationVisitAudit> existingPrevious = new RowState<>(existingLocation, validFrom, storedFrom, false);
                    setInferredDischargeAndTime(true, validFrom, existingPrevious);
                    previousLocations.add(existingPrevious);
                }
            }
        } else if (previousLocationId.isPresent()) {
            logger.debug("No existing locations for visit, inferring admission time for hl7 message previous location");
            previousHl7Location = createLocationWithInferredAdmit(visit, previousLocationId.get(), validFrom, validFrom, storedFrom);
            setInferredDischargeAndTime(false, validFrom, previousHl7Location);
        } else {
            logger.debug("No existing locations for visit and no HL7 previous location");
        }

        // Always add previous hl7 location if it exists
        if (previousHl7Location != null) {
            previousLocations.add(previousHl7Location);
        }
        return previousLocations;
    }

    private boolean openLocationOrInferredDischarge(LocationVisit existingLocation) {
        return existingLocation.getDischargeTime() == null || existingLocation.getInferredDischarge();
    }

    /**
     * Save current and previous locations visits if required.
     * @param currentLocation current location visit
     * @param previousVisits  collection of zero or more previous location visits
     */
    private void saveAllLocationVisitsIfRequired(
            RowState<LocationVisit, LocationVisitAudit> currentLocation, Collection<RowState<LocationVisit, LocationVisitAudit>> previousVisits) {
        Collection<RowState<LocationVisit, LocationVisitAudit>> savingVisits = new ArrayList<>();
        savingVisits.add(currentLocation);
        savingVisits.addAll(previousVisits);
        savingVisits.forEach(rowState -> rowState.saveEntityOrAuditLogIfRequired(locationVisitRepo, locationVisitAuditRepo));
    }

    /**
     * Increment a possibly null Long.
     * If null, set to zero.
     * @param input input counter
     * @return incremented counter
     */
    private Long incrementNullable(@Nullable Long input) {
        Long incremented = input;
        if (incremented == null) {
            incremented = 0L;
        } else {
            incremented += 1;
        }
        return incremented;
    }

    /**
     * Is the index in the range of the collection.
     * @param collection collection of any objects
     * @param index      index
     * @return true if index is in range of the collection.
     */
    private boolean indexInRange(Collection<?> collection, Long index) {
        return index != null && (index < collection.size());
    }

    /**
     * Process discharging of a patient from current location.
     * If message previous location doesnt exist in star, create it with an inferred admission and discharge time.
     * If current location doesn't exist, create with inferred admission time. Then infer previous location's discharge time if that is null.
     * @param visit             hospital visit
     * @param msg               DischargePatient Message
     * @param storedFrom        when the message has been read by emap core
     * @param currentLocationId Location entity
     */
    private void processDischargeMessage(
            HospitalVisit visit, DischargePatient msg, Instant storedFrom, Location currentLocationId) {
        List<LocationVisit> visitLocations = locationVisitRepo.findAllByHospitalVisitIdOrderByAdmissionTimeDesc(visit);
        Instant dischargeTime = msg.getDischargeDateTime();

        Pair<Long, RowState<LocationVisit, LocationVisitAudit>> indexAndNextLocation = getIndexOfCurrentAndNextLocationVisit(
                visitLocations, currentLocationId, dischargeTime, storedFrom);
        Long indexCurrentOrPrevious = indexAndNextLocation.getLeft();

        RowState<LocationVisit, LocationVisitAudit> currentLocation = getOrCreateCurrentLocation(
                visit, storedFrom, currentLocationId, dischargeTime, visitLocations, indexCurrentOrPrevious);
        setInferredDischargeAndTime(false, dischargeTime, currentLocation);

        List<RowState<LocationVisit, LocationVisitAudit>> savingVisits = new ArrayList<>();
        savingVisits.add(currentLocation);

        if (currentLocation.isEntityCreated()) {
            logger.debug("Discharge message: star did not know about about original location, inferring the admission time");
            setInferredAdmissionAndTime(true, dischargeTime.minus(1, ChronoUnit.SECONDS), currentLocation);
        } else {
            indexCurrentOrPrevious += 1;
        }

        if (previousMessagesShouldBeUpdated(visitLocations, indexCurrentOrPrevious, currentLocation.isEntityCreated(), msg)) {
            LocationVisit existingPreviousLocation = null;
            if (indexInRange(visitLocations, indexCurrentOrPrevious)) {
                existingPreviousLocation = visitLocations.get(indexCurrentOrPrevious.intValue());
            }
            List<RowState<LocationVisit, LocationVisitAudit>> previousLocations = inferPreviousLocationData(
                    visit, existingPreviousLocation, msg, dischargeTime, storedFrom);
            savingVisits.addAll(previousLocations);
        }

        savingVisits.forEach(rowState -> rowState.saveEntityOrAuditLogIfRequired(locationVisitRepo, locationVisitAuditRepo));
    }

    /**
     * @param visit                    hospital visit
     * @param existingPreviousLocation Nullable existing previous location
     * @param msg                      DischargePatient Message
     * @param dischargeTime            time of discharge
     * @param storedFrom               when the message has been read by emap core
     * @return previous locations with inferred admit and discharge times.
     */
    private List<RowState<LocationVisit, LocationVisitAudit>> inferPreviousLocationData(
            HospitalVisit visit, @Nullable LocationVisit existingPreviousLocation, DischargePatient msg, Instant dischargeTime, Instant storedFrom) {
        Instant inferredDischargeTime = dischargeTime.minus(1, ChronoUnit.SECONDS);

        List<RowState<LocationVisit, LocationVisitAudit>> previousLocations = new ArrayList<>();
        int existingPreviousSecondsBack = 1;
        Optional<Location> optionalPreviousLocationId = getPreviousLocationId(msg);
        if (optionalPreviousLocationId.isPresent()) {
            Location previousLocationId = optionalPreviousLocationId.get();
            if (existingPreviousLocation == null || !previousLocationId.equals(existingPreviousLocation.getLocationId())) {
                logger.debug("Previous hl7 not location found in existing locations, inferring its admission and discharge time");
                Instant inferredAdmit = inferredDischargeTime.minus(existingPreviousSecondsBack + 1, ChronoUnit.SECONDS);
                Instant inferredDischarge = inferredDischargeTime.minus(existingPreviousSecondsBack, ChronoUnit.SECONDS);
                RowState<LocationVisit, LocationVisitAudit> inferredPrevious = createLocationWithInferredAdmit(
                        visit, previousLocationId, inferredAdmit, dischargeTime, storedFrom);
                setInferredDischargeAndTime(true, inferredDischarge, inferredPrevious);
                previousLocations.add(inferredPrevious);
                existingPreviousSecondsBack += 1;
            }
        }
        if (existingPreviousLocation != null && existingPreviousLocation.getDischargeTime() == null) {
            logger.debug("Current location wasn't found and previous location exists so inferring previous visits discharge time");
            RowState<LocationVisit, LocationVisitAudit> existingPreviousLocationState = new RowState<>(
                    existingPreviousLocation, dischargeTime, storedFrom, false);
            Instant inferredDischarge = inferredDischargeTime.minus(existingPreviousSecondsBack, ChronoUnit.SECONDS);
            setInferredDischargeAndTime(true, inferredDischarge, existingPreviousLocationState);
            previousLocations.add(existingPreviousLocationState);
        }
        return previousLocations;
    }

    /**
     * Should previous messages be updated from a discharge message.
     * Previous message should be updated if the current location was created (there may be previous messages that are discharged) and either:
     * - there are previous messages
     * - the hl7 message has a previous location string
     * @param visitLocations   visit locations in descending admission time order
     * @param indexOfPrevious  index of the previous location
     * @param isCurrentCreated was the current location created
     * @param msg              DischargePatietn message
     * @return true if previous locations should be updated
     */
    private boolean previousMessagesShouldBeUpdated(
            List<LocationVisit> visitLocations, Long indexOfPrevious, boolean isCurrentCreated, DischargePatient msg) {

        return isCurrentCreated && (indexInRange(visitLocations, indexOfPrevious) || msg.getPreviousLocationString().isSave());
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
     * Set the inferred state of discharge, and the discharge time of a location state.
     * @param isInferred    is the discharge time inferred
     * @param dischargeTime time to set the discharge
     * @param locationState to update
     */
    private void setInferredDischargeAndTime(Boolean isInferred, Instant dischargeTime, RowState<LocationVisit, LocationVisitAudit> locationState) {
        LocationVisit existingLocation = locationState.getEntity();
        locationState.assignIfDifferent(dischargeTime, existingLocation.getDischargeTime(), existingLocation::setDischargeTime);
        locationState.assignIfDifferent(isInferred, existingLocation.getInferredDischarge(), existingLocation::setInferredDischarge);
    }

    /**
     * @param isInferred
     * @param dischargeTime
     * @param locationState
     */
    private void setInferredAdmissionAndTime(Boolean isInferred, Instant dischargeTime, RowState<LocationVisit, LocationVisitAudit> locationState) {
        LocationVisit existingLocation = locationState.getEntity();
        locationState.assignIfDifferent(dischargeTime, existingLocation.getAdmissionTime(), existingLocation::setAdmissionTime);
        locationState.assignIfDifferent(isInferred, existingLocation.getInferredAdmission(), existingLocation::setInferredAdmission);
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
    private RowState<LocationVisit, LocationVisitAudit> createLocationWithInferredAdmit(
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
