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
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.MessageLocationCancelledException;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.RequiredDataMissingException;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LocationRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LocationVisitAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LocationVisitRepository;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.movement.Location;
import uk.ac.ucl.rits.inform.informdb.movement.LocationVisit;
import uk.ac.ucl.rits.inform.informdb.movement.LocationVisitAudit;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
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
     * @throws RequiredDataMissingException      If cancellation information is missing
     * @throws MessageLocationCancelledException If the message location has been cancelled
     */
    @Transactional
    public void processVisitLocation(HospitalVisit visit, AdtMessage msg, Instant storedFrom)
            throws RequiredDataMissingException, MessageLocationCancelledException {
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
        visitStateA.assignInterchangeValue(
                InterchangeValue.buildFromHl7(locationA), visitStateA.getEntity().getLocationId(), visitStateA.getEntity()::setLocationId);
        visitStateB.assignInterchangeValue(
                InterchangeValue.buildFromHl7(locationB), visitStateB.getEntity().getLocationId(), visitStateB.getEntity()::setLocationId);
        // save newly created or audit
        visitStateA.saveEntityOrAuditLogIfRequired(locationVisitRepo, locationVisitAuditRepo);
        visitStateB.saveEntityOrAuditLogIfRequired(locationVisitRepo, locationVisitAuditRepo);
    }

    /**
     * @param visit             hospital visit
     * @param msg               Adt Message
     * @param storedFrom        when the message has been read by emap core
     * @param currentLocationId Location entity
     * @param validFrom         message event date time
     * @throws MessageLocationCancelledException if the message location has already been cancelled for the time
     */
    private void processMoveMessage(HospitalVisit visit, AdtMessage msg, Instant storedFrom, Location currentLocationId, Instant validFrom)
            throws MessageLocationCancelledException {
        if (!(msg instanceof UpdatePatientInfo) && locationVisitAuditRepo.messageLocationIsCancelled(visit, currentLocationId, validFrom, false)) {
            throw new MessageLocationCancelledException("Admission or Transfer was cancelled");
        }

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
                visit, storedFrom, currentLocationId, validFrom, visitLocations, indexCurrentOrPrevious, false);
        updateCurrentVisitIfRequired(nextLocation, validFrom, currentLocation);
        // If the current location is not created, then it was found - so increment counter for previous location
        if (!currentLocation.isEntityCreated()) {
            indexCurrentOrPrevious += 1;
        } else if (msg instanceof UpdatePatientInfo) {
            // UpdatePatientInfo message processing should always be inferred
            currentLocation.getEntity().setInferredAdmission(true);
        }

        updateOrCreatePreviousMoveLocations(visit, msg, storedFrom, validFrom, visitLocations, indexCurrentOrPrevious);
        currentLocation.saveEntityOrAuditLogIfRequired(locationVisitRepo, locationVisitAuditRepo);
    }

    /**
     * Get the index of the current or previous location (if exists) and the next location (if exists).
     * @param visitLocations  visit locations in descending order of admission time
     * @param currentLocation current location
     * @param admissionTime   admission time for the current location
     * @param storedFrom      when the message has been read by emap core
     * @return Pair of nullable values: <index of the current or previous visit, next location visit>
     */
    private Pair<Long, RowState<LocationVisit, LocationVisitAudit>> getIndexOfCurrentAndNextLocationVisit(
            List<LocationVisit> visitLocations, Location currentLocation, Instant admissionTime, Instant storedFrom) {

        RowState<LocationVisit, LocationVisitAudit> nextLocation = null;
        Long indexCurrentOrPrevious = null;
        for (LocationVisit location : visitLocations) {
            indexCurrentOrPrevious = incrementNullable(indexCurrentOrPrevious);
            if (!admissionTime.isBefore(location.getAdmissionTime())) {
                logger.trace("Reached a visit which is before the current admission time");
                break;
            }
            if (isCurrentVisit(location, currentLocation, indexCurrentOrPrevious, visitLocations, admissionTime)) {
                // Early exit because we think we are on the current visit
                break;
            }
            nextLocation = new RowState<>(location, admissionTime, storedFrom, false);
            // if we get to the end and we haven't skipped over the current, increment it again because there is no current or past visit
            if (indexCurrentOrPrevious == visitLocations.size() - 1) {
                indexCurrentOrPrevious = incrementNullable(indexCurrentOrPrevious);
            }
        }
        logger.trace("Next location: {}", nextLocation);

        return new ImmutablePair<>(indexCurrentOrPrevious, nextLocation);
    }

    /**
     * Is the "next visit" likely to be the current visit.
     * @param location          potential current location visit
     * @param currentLocationId location Id from hl7 message
     * @param currentIndex      index of potential current location visit
     * @param visitLocations    list of all location visits, in descending order of admission time
     * @param admissionTime     admission time for the current location
     * @return true if the message appears to be current.
     */
    private boolean isCurrentVisit(
            LocationVisit location, Location currentLocationId, Long currentIndex, List<LocationVisit> visitLocations, Instant admissionTime) {
        boolean isCurrentVisit = false;
        // same location, inferred admission
        if (location.getLocationId().equals(currentLocationId) && location.getInferredAdmission()) {
            Long precedingLocationIndex = currentIndex + 1;
            if (indexInRange(visitLocations, precedingLocationIndex)) {
                Instant precedingVisitAdmission = visitLocations.get(precedingLocationIndex.intValue()).getAdmissionTime();
                logger.debug("Current message is after the preceding location's admission time, will use it as the current visit");
                if (admissionTime.isAfter(precedingVisitAdmission)) {
                    isCurrentVisit = true;
                }
            } else {
                logger.debug("No visits before this and it seems like it's the current visit");
                isCurrentVisit = true;
            }
        }
        return isCurrentVisit;
    }

    /**
     * Get or create current location.
     * @param visit               hospital visit
     * @param storedFrom          time that emap star encountered the message
     * @param currentLocationId   current location
     * @param validFrom           event time of the message
     * @param visitLocations      visit locations in descending order of admission time
     * @param indexOfCurrent      index of potential current location
     * @param forDischargeMessage if true, only matches on location
     * @return current location visit
     */
    private RowState<LocationVisit, LocationVisitAudit> getOrCreateCurrentLocation(
            HospitalVisit visit, Instant storedFrom, Location currentLocationId, Instant validFrom,
            List<LocationVisit> visitLocations, Long indexOfCurrent, boolean forDischargeMessage) {

        RowState<LocationVisit, LocationVisitAudit> currentLocation;
        if (indexInRange(visitLocations, indexOfCurrent)) {
            LocationVisit location = visitLocations.get(indexOfCurrent.intValue());
            if (locationLooksLikeCurrentLocation(location, currentLocationId, visitLocations, forDischargeMessage)) {
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
     * If existing location is current if the location matches plus another condition.
     * <ul>
     *     <li> is from a discharge message </li>
     *     <li> existing location has inferred admission </li>
     *     <li> only one open location exists (happens when there is a duplicate admit message) </li>
     * </ul>
     * @param currentOrPrevious   existing location
     * @param currentLocationId   location Id from hl7 message
     * @param visitLocations      all visit locations
     * @param forDischargeMessage if true, only needs to match on location
     * @return true if the location is current
     */
    private boolean locationLooksLikeCurrentLocation(
            LocationVisit currentOrPrevious, Location currentLocationId, Collection<LocationVisit> visitLocations, boolean forDischargeMessage) {
        return currentOrPrevious.getLocationId().equals(currentLocationId)
                && (forDischargeMessage || currentOrPrevious.getInferredAdmission() || isDuplicateAdmit(visitLocations, currentOrPrevious));
    }

    /**
     * @param visitLocations    all visit locations
     * @param currentOrPrevious existing location
     * @return true if only one visit exists and it hasn't been discharged
     */
    private boolean isDuplicateAdmit(Collection<LocationVisit> visitLocations, LocationVisit currentOrPrevious) {
        return visitLocations.size() == 1 && currentOrPrevious.getDischargeTime() == null;
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
     * Update or create previous message(s), ensuring that they are discharged and saving any changes.
     * If message has previous location, create or update the location.
     * If message previous location doesn't (exist or match existing previous location), infer discharge of existing previous location.
     * If no message previous location and no existing previous locations, return empty list
     * @param visit                  hospital visit
     * @param msg                    hl7 message
     * @param storedFrom             time that emap star started processing the message
     * @param validFrom              event time from the hl7 message
     * @param visitLocations         in descending order of admission time
     * @param indexOfPreviousMessage index of previous message
     */
    private void updateOrCreatePreviousMoveLocations(
            HospitalVisit visit, AdtMessage msg, Instant storedFrom, Instant validFrom,
            List<LocationVisit> visitLocations, Long indexOfPreviousMessage) {

        Optional<Location> previousLocationId = getPreviousLocationId(msg);
        RowState<LocationVisit, LocationVisitAudit> previousHl7Location = null;

        if (indexInRange(visitLocations, indexOfPreviousMessage)) {
            LocationVisit existingLocation = visitLocations.get(indexOfPreviousMessage.intValue());
            if (previousLocationId.isPresent()) {
                if (previousLocationId.get().equals(existingLocation.getLocationId())) {
                    logger.debug("Previous location matches hl7 value: inferring discharge time");
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
                        existingPrevious.saveEntityOrAuditLogIfRequired(locationVisitRepo, locationVisitAuditRepo);
                    }
                }
            } else {
                if (zeroLengthVisitWouldBeCreated(msg, existingLocation, validFrom)) {
                    logger.debug("Removing previous location because it would create a zero-length current location");
                    deleteLocationVisit(validFrom, storedFrom, existingLocation);
                } else if (openLocationOrInferredDischarge(existingLocation)) {
                    logger.debug("No previous hl7 location, but found existing previous location. Inferring existing location discharge.");
                    RowState<LocationVisit, LocationVisitAudit> existingPrevious = new RowState<>(existingLocation, validFrom, storedFrom, false);
                    setInferredDischargeAndTime(true, validFrom, existingPrevious);
                    existingPrevious.saveEntityOrAuditLogIfRequired(locationVisitRepo, locationVisitAuditRepo);
                }
            }
        } else if (previousLocationId.isPresent()) {
            logger.debug("No existing locations for visit, inferring admission time for hl7 message previous location");
            previousHl7Location = createLocationWithInferredAdmit(visit, previousLocationId.get(), validFrom, validFrom, storedFrom);
            setInferredDischargeAndTime(false, validFrom, previousHl7Location);
        } else {
            logger.debug("No existing locations for visit and no HL7 previous location");
        }

        // Always save previous hl7 location if it exists
        if (previousHl7Location != null) {
            previousHl7Location.saveEntityOrAuditLogIfRequired(locationVisitRepo, locationVisitAuditRepo);
        }
    }

    private boolean zeroLengthVisitWouldBeCreated(AdtMessage msg, LocationVisit existingLocation, Instant validFrom) {
        return validFrom.equals(existingLocation.getAdmissionTime())
                && existingLocation.getLocationId().getLocationString().equals(msg.getFullLocationString().get());
    }

    private boolean openLocationOrInferredDischarge(LocationVisit existingLocation) {
        return existingLocation.getDischargeTime() == null || existingLocation.getInferredDischarge();
    }

    /**
     * Increment a possibly null Long.
     * If null, set to zero.
     * @param input input counter
     * @return incremented counter
     */
    private Long incrementNullable(@Nullable Long input) {
        return input == null ? 0 : input + 1;
    }

    /**
     * Is the index in the range of the collection.
     * @param collection collection of any objects
     * @param index      index, may be null collection is empty
     * @return true if index is in range of the collection.
     */
    private boolean indexInRange(Collection<?> collection, @Nullable Long index) {
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
     * @throws MessageLocationCancelledException if discharge message was cancelled
     */
    private void processDischargeMessage(
            HospitalVisit visit, DischargePatient msg, Instant storedFrom, Location currentLocationId) throws MessageLocationCancelledException {
        if (locationVisitAuditRepo.messageLocationIsCancelled(visit, currentLocationId, msg.getDischargeDateTime(), true)) {
            throw new MessageLocationCancelledException("Discharge has previously been cancelled");
        }

        List<LocationVisit> visitLocations = locationVisitRepo.findAllByHospitalVisitIdOrderByAdmissionTimeDesc(visit);
        Instant dischargeTime = msg.getDischargeDateTime();

        Pair<Long, RowState<LocationVisit, LocationVisitAudit>> indexAndNextLocation = getIndexOfCurrentAndNextLocationVisit(
                visitLocations, currentLocationId, dischargeTime, storedFrom);
        Long indexCurrentOrPrevious = indexAndNextLocation.getLeft();

        RowState<LocationVisit, LocationVisitAudit> currentLocation = getOrCreateCurrentLocation(
                visit, storedFrom, currentLocationId, dischargeTime, visitLocations, indexCurrentOrPrevious, true);
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
    private void setInferredDischargeAndTime(Boolean isInferred, Instant
            dischargeTime, RowState<LocationVisit, LocationVisitAudit> locationState) {
        LocationVisit existingLocation = locationState.getEntity();
        locationState.assignIfDifferent(dischargeTime, existingLocation.getDischargeTime(), existingLocation::setDischargeTime);
        locationState.assignIfDifferent(isInferred, existingLocation.getInferredDischarge(), existingLocation::setInferredDischarge);
    }

    /**
     * @param isInferred
     * @param dischargeTime
     * @param locationState
     */
    private void setInferredAdmissionAndTime(Boolean isInferred, Instant
            dischargeTime, RowState<LocationVisit, LocationVisitAudit> locationState) {
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
     * @param visit      hospital visit
     * @param msg        Adt Message
     * @param storedFrom when the message has been read by emap core
     * @param locationId Location entity
     * @param validFrom  message event date time
     * @throws RequiredDataMissingException if cancellation information missing
     */
    @Transactional
    public void processCancellationMessage(
            HospitalVisit visit, AdtMessage msg, Instant storedFrom, Location locationId, Instant validFrom) throws RequiredDataMissingException {
        if (msg instanceof CancelAdmitPatient) {
            Instant cancellationTime;
            try {
                cancellationTime = getCancellationTime((AdtCancellation) msg);
            } catch (RequiredDataMissingException e) {
                List<LocationVisit> existingLocations = locationVisitRepo.findAllByHospitalVisitId(visit);
                if (existingLocations.size() == 1 && locationId.equals(existingLocations.get(0).getLocationId())) {
                    logger.info("Cancellation time missing from message, but only one matching location for cancellation so cancelling that");
                    cancellationTime = existingLocations.get(0).getAdmissionTime();
                } else {
                    throw e;
                }
            }

            Optional<LocationVisit> retiringVisit = locationVisitRepo
                    .findByHospitalVisitIdAndLocationIdAndAdmissionTime(visit, locationId, cancellationTime);
            if (retiringVisit.isPresent()) {
                deleteLocationVisit(cancellationTime, storedFrom, retiringVisit.get());
            } else {
                recordLocationAsDeleted(visit, locationId, false, true, cancellationTime, storedFrom);
            }
        } else if (msg instanceof CancelDischargePatient) {
            Instant cancellationTime = getCancellationTime((AdtCancellation) msg);
            Optional<LocationVisit> retiringVisit = locationVisitRepo
                    .findByHospitalVisitIdAndLocationIdAndDischargeTime(visit, locationId, cancellationTime);
            if (retiringVisit.isPresent()) {
                rollbackDischargeToPreviousValue(visit, storedFrom, locationId, validFrom, cancellationTime, retiringVisit.get());
            } else {
                recordLocationAsDeleted(visit, locationId, true, false, cancellationTime, storedFrom);
            }
        } else if (msg instanceof CancelTransferPatient) {
            CancelTransferPatient cancelTransferPatient = (CancelTransferPatient) msg;
            if (cancelTransferPatient.getCancelledLocation() == null) {
                throw new RequiredDataMissingException("CancelTransfer message: doesn't have location to cancel");
            }
            processCancelTransfer(visit, storedFrom, cancelTransferPatient);
        }
    }

    private void recordLocationAsDeleted(
            HospitalVisit visit, Location locationId,
            boolean inferredAdmit, boolean inferredDischarge, Instant cancellationTime, Instant storedFrom) {
        LocationVisitAudit cancelled = locationVisitAuditRepo
                .findByHospitalVisitIdAndLocationIdAndAdmissionTimeAndDischargeTime(
                        visit.getHospitalVisitId(), locationId, cancellationTime, cancellationTime)
                .orElseGet(() -> buildCancelledAudit(visit, locationId, cancellationTime, storedFrom));
        cancelled.setStoredUntil(storedFrom);
        cancelled.setInferredAdmission(inferredAdmit || cancelled.getInferredAdmission());
        cancelled.setInferredDischarge(inferredDischarge || cancelled.getInferredDischarge());
        logger.info("Cancelling location {} at {} for later admission ({}) or discharge ({})",
                locationId.getLocationString(), cancellationTime, !cancelled.getInferredAdmission(), !cancelled.getInferredDischarge());
        locationVisitAuditRepo.save(cancelled);
        logger.debug("LocationVisitAudit saved: {}", cancelled);
    }

    private LocationVisitAudit buildCancelledAudit(HospitalVisit visit, Location locationId, Instant cancellationTime, Instant storedFrom) {
        LocationVisitAudit cancelled = new LocationVisitAudit();
        cancelled.setAdmissionTime(cancellationTime);
        cancelled.setDischargeTime(cancellationTime);
        cancelled.setValidFrom(cancellationTime);
        cancelled.setValidUntil(cancellationTime);
        cancelled.setStoredFrom(storedFrom);
        cancelled.setLocationId(locationId);
        cancelled.setHospitalVisitId(visit.getHospitalVisitId());
        cancelled.setInferredDischarge(false);
        cancelled.setInferredAdmission(false);
        return cancelled;
    }

    private void processCancelTransfer(
            HospitalVisit visit, Instant storedFrom, CancelTransferPatient cancelTransferPatient) throws RequiredDataMissingException {
        List<LocationVisit> visitLocations = locationVisitRepo.findAllByHospitalVisitIdOrderByAdmissionTimeDesc(visit);
        Instant cancellationTime = getCancellationTime(cancelTransferPatient);
        Location cancelledLocationId = getOrCreateLocation(cancelTransferPatient.getCancelledLocation());

        Pair<Long, RowState<LocationVisit, LocationVisitAudit>> indexAndNextLocation = getIndexOfCurrentAndNextLocationVisit(
                visitLocations, cancelledLocationId, cancellationTime, storedFrom);
        Long indexCurrentOrPrevious = indexAndNextLocation.getLeft();

        if (!indexInRange(visitLocations, indexCurrentOrPrevious)) {
            recordLocationAsDeleted(visit, cancelledLocationId, false, true, cancellationTime, storedFrom);
            logger.debug("CancelTransfer message: visit to cancel was not found");
            return;
        }
        LocationVisit retiringLocation = visitLocations.get(indexCurrentOrPrevious.intValue());
        if (retiringLocation.getLocationId() != cancelledLocationId) {
            recordLocationAsDeleted(visit, cancelledLocationId, false, true, cancellationTime, storedFrom);
            logger.debug("CancelTransfer message: visit to cancel was not found, marking as cancelled in audit log");
            return;
        }
        updatePreviousAndMergeWithNextVisitIfRequired(visitLocations, indexAndNextLocation, cancellationTime, retiringLocation, storedFrom);
        deleteLocationVisit(cancellationTime, storedFrom, retiringLocation);
    }

    private void updatePreviousAndMergeWithNextVisitIfRequired(
            List<LocationVisit> visitLocations, Pair<Long, RowState<LocationVisit, LocationVisitAudit>> indexAndNextLocation,
            Instant cancellationTime, LocationVisit retiringLocation, Instant storedFrom) {
        Long previousIndex = indexAndNextLocation.getLeft() + 1;

        if (!indexInRange(visitLocations, previousIndex)) {
            return;
        }
        Instant previousDischargeTime = retiringLocation.getDischargeTime();
        LocationVisit previousLocation = visitLocations.get(previousIndex.intValue());
        RowState<LocationVisit, LocationVisitAudit> nextLocationState = indexAndNextLocation.getRight();
        if (nextLocationState != null) {
            LocationVisit nextLocation = nextLocationState.getEntity();
            if (nextLocation.getLocationId().equals(previousLocation.getLocationId())) {
                logger.debug("Previous location and next location match - deleting next location and updating discharge of previous");
                previousDischargeTime = nextLocation.getDischargeTime();
                deleteLocationVisit(cancellationTime, storedFrom, nextLocation);
            }
        }
        RowState<LocationVisit, LocationVisitAudit> previousLocationState = new RowState<>(
                visitLocations.get(previousIndex.intValue()), cancellationTime, storedFrom, false);
        setInferredDischargeAndTime(true, previousDischargeTime, previousLocationState);
        previousLocationState.saveEntityOrAuditLogIfRequired(locationVisitRepo, locationVisitAuditRepo);

    }

    private void rollbackDischargeToPreviousValue(
            HospitalVisit visit, Instant storedFrom, Location locationId, Instant validFrom, Instant cancellationTime, LocationVisit incorrectVisit) {
        List<LocationVisit> visitLocations = locationVisitRepo.findAllByHospitalVisitIdOrderByAdmissionTimeDesc(visit);
        Pair<Long, RowState<LocationVisit, LocationVisitAudit>> indexAndNextLocation = getIndexOfCurrentAndNextLocationVisit(
                visitLocations, locationId, validFrom, storedFrom);
        RowState<LocationVisit, LocationVisitAudit> nextLocation = indexAndNextLocation.getRight();
        if (nextLocation != null) {
            logger.debug("CancelDischarge, but locations after discharge for visit - not doing anything");
            return;
        }
        RowState<LocationVisit, LocationVisitAudit> rollbackDischarge = new RowState<>(
                incorrectVisit, cancellationTime, storedFrom, false);

        Instant previousDischargeTime = locationVisitAuditRepo
                .findPreviousLocationVisitAuditForDischarge(incorrectVisit.getLocationVisitId(), cancellationTime)
                .map(LocationVisitAudit::getDischargeTime)
                .orElse(null);
        // find previous state of the location visit discharge
        logger.debug("CancelDischarge, no locations after discharge for visit so rolling back discharge time to {}", previousDischargeTime);

        setInferredDischargeAndTime(false, previousDischargeTime, rollbackDischarge);
    }

    /**
     * Get cancellation time.
     * @param msg Cancellation message
     * @return the cancellation time
     * @throws RequiredDataMissingException if the cancellation time is missing
     */
    private Instant getCancellationTime(AdtCancellation msg) throws RequiredDataMissingException {
        Instant cancellationTime = msg.getCancelledDateTime();
        if (cancellationTime == null) {
            throw new RequiredDataMissingException("Cancellation message missing cancellation time");
        }
        return cancellationTime;
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
     * Is message outcome a simple move?
     * @param msg AdtMessage
     * @return true if a message outcome is moving from one location to another.
     */
    private boolean messageOutcomeIsSimpleMove(AdtMessage msg) {
        return msg instanceof TransferPatient || msg instanceof UpdatePatientInfo || msg instanceof AdmitPatient || msg instanceof RegisterPatient;
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
