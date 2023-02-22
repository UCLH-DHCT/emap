package uk.ac.ucl.rits.inform.datasinks.emapstar.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.IncompatibleDatabaseStateException;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.locations.BedFacilityRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.locations.BedRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.locations.BedStateRepository;
import uk.ac.ucl.rits.inform.informdb.TemporalFrom;
import uk.ac.ucl.rits.inform.informdb.movement.Bed;
import uk.ac.ucl.rits.inform.informdb.movement.BedFacility;
import uk.ac.ucl.rits.inform.informdb.movement.BedState;
import uk.ac.ucl.rits.inform.informdb.movement.Room;
import uk.ac.ucl.rits.inform.interchange.LocationMetadata;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Adds or updates bed information.
 * @author Stef Piatek
 */
@Component
public class BedController {
    private static final Logger logger = LoggerFactory.getLogger(BedController.class);

    private final BedRepository bedRepo;
    private final BedStateRepository bedStateRepo;
    private final BedFacilityRepository bedFacilityRepo;


    /**
     * @param bedRepo         bed repository
     * @param bedStateRepo    bed state repository
     * @param bedFacilityRepo bed facility repository
     */
    public BedController(BedRepository bedRepo, BedStateRepository bedStateRepo, BedFacilityRepository bedFacilityRepo) {
        this.bedRepo = bedRepo;
        this.bedStateRepo = bedStateRepo;
        this.bedFacilityRepo = bedFacilityRepo;
    }

    /**
     * Create Bed if it doesn't exist and update state and create facilities if required.
     * <p>
     * We should receive beds in order of their valid from, so if a bed doesn't exist (by CSN) then it should be created.
     * <p>
     * For pool beds, we create a single bed and in the state entity, increment the number of pool beds found at the contact time.
     * Because the CSN is only of the first encountered, an existing pool bed is found by those which have a pool bed count and the same
     * contact time. This means that if the locations are processed from the beginning of epic time again then the pool bed count will
     * be larger than the real value. This is fine because we shouldn't be removing the current progress from the locations hoover.
     * @param room       room entity that the bed is associated with
     * @param msg        message to be processed
     * @param storedFrom time that emap core started processing the message
     * @return bed
     * @throws IncompatibleDatabaseStateException if a new state is encountered which is has an earlier valid from than the most recent state
     */
    public Bed processBedStateAndFacility(Room room, LocationMetadata msg, Instant storedFrom) throws IncompatibleDatabaseStateException {
        Bed bed = bedRepo
                .findByHl7StringAndRoomId(msg.getBedHl7(), room)
                .orElseGet(() -> bedRepo.save(new Bed(msg.getBedHl7(), room)));

        BedState bedState = processBedState(bed, msg, storedFrom);

        if (msg.getBedFacility() != null) {
            createBedFacilityIfNotExists(bedState, msg.getBedFacility());
        }

        return bed;
    }

    private BedState processBedState(Bed bed, LocationMetadata msg, Instant storedFrom) throws IncompatibleDatabaseStateException {


        List<BedState> states = bedStateRepo.findAllByBedIdOrderByValidFromDesc(bed);

        // if we already know about the bed pool, increment it and don't do any further processing
        if (msg.getIsPoolBed() != null && msg.getIsPoolBed()) {
            Optional<BedState> existingPoolBed = findExistingPoolBedByValidFrom(msg.getBedContactDate(), states);
            if (existingPoolBed.isPresent()) {
                incrementPoolBedAndSave(existingPoolBed.get());
                return existingPoolBed.get();
            }
        }

        // if we already know about the bed CSN, don't do any further processing
        Optional<BedState> existingState = states.stream()
                .filter(state -> state.getCsn().equals(msg.getBedCsn()))
                .findFirst();
        if (existingState.isPresent()) {
            return existingState.get();
        }

        return createCurrentStateAndInvalidatePrevious(msg, bed, states, new TemporalFrom(msg.getBedContactDate(), storedFrom));
    }

    private Optional<BedState> findExistingPoolBedByValidFrom(Instant bedContactDate, Collection<BedState> states) {
        return states.stream()
                .filter(state -> state.getPoolBedCount() != null && state.getValidFrom().equals(bedContactDate))
                .findFirst();
    }

    private void incrementPoolBedAndSave(BedState existingPoolBed) {
        existingPoolBed.incrementPoolBedCount();
        bedStateRepo.save(existingPoolBed);
    }

    /**
     * Create new state from current message, invalidating the previous state and saving if required.
     * @param msg          message to process
     * @param bed          bed entity
     * @param states       previous states sorted by descending valid from dates
     * @param temporalFrom valid and stored from
     * @return Current bed state for message
     * @throws IncompatibleDatabaseStateException if a novel, non-pool CSN is found with a contact date earlier than the latest state
     */
    private BedState createCurrentStateAndInvalidatePrevious(
            LocationMetadata msg, Bed bed, Collection<BedState> states, TemporalFrom temporalFrom) throws IncompatibleDatabaseStateException {
        BedState currentState = new BedState(
                bed, msg.getBedCsn(), msg.getBedIsInCensus(), msg.getIsBunkBed(),
                msg.getBedRecordState().toString(), msg.getIsPoolBed(), temporalFrom
        );

        if (msg.getIsPoolBed()) {
            incrementPoolBedAndSave(currentState);
        }

        // if the bed doesn't have any existing states we don't need to invalidate any previous states
        if (states.isEmpty()) {
            return bedStateRepo.save(currentState);
        }

        // assuming the current message is after the most recent state, we should invalidate it and save the new state
        BedState previousState = states.stream().findFirst().orElseThrow();
        if (currentState.getValidFrom().isBefore(previousState.getValidFrom())) {
            throw new IncompatibleDatabaseStateException("New bed state is valid before the most current bed state");
        }

        previousState.setValidUntil(temporalFrom.getValid());
        previousState.setStoredUntil(temporalFrom.getStored());

        bedStateRepo.saveAll(List.of(previousState, currentState));
        return currentState;
    }

    private void createBedFacilityIfNotExists(BedState bedState, String bedFacility) {
        bedFacilityRepo.findByBedStateIdAndType(bedState, bedFacility)
                .orElseGet(() -> bedFacilityRepo.save(new BedFacility(bedState, bedFacility)));
    }

}
