package uk.ac.ucl.rits.inform.datasinks.emapstar.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.IncompatibleDatabaseStateException;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.locations.BedRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.locations.BedStateRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.locations.DepartmentRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.locations.DepartmentStateRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.locations.LocationRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.locations.RoomRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.locations.RoomStateRepository;
import uk.ac.ucl.rits.inform.informdb.movement.Bed;
import uk.ac.ucl.rits.inform.informdb.movement.BedState;
import uk.ac.ucl.rits.inform.informdb.movement.Department;
import uk.ac.ucl.rits.inform.informdb.movement.DepartmentState;
import uk.ac.ucl.rits.inform.informdb.movement.Location;
import uk.ac.ucl.rits.inform.informdb.movement.Room;
import uk.ac.ucl.rits.inform.informdb.movement.RoomState;
import uk.ac.ucl.rits.inform.interchange.LocationMetadata;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Adds or updates location metadata (department, room, bed pool, bed, and their states).
 * @author Stef Piatek
 */
@Component
public class LocationMetadataController {
    private static final Logger logger = LoggerFactory.getLogger(LocationMetadataController.class);

    private final LocationRepository locationRepo;
    private final DepartmentRepository departmentRepo;
    private final DepartmentStateRepository departmentStateRepo;
    private final RoomRepository roomRepo;
    private final RoomStateRepository roomStateRepo;
    private final BedRepository bedRepo;
    private final BedStateRepository bedStateRepo;


    public LocationMetadataController(
            LocationRepository locationRepo, DepartmentRepository departmentRepo, DepartmentStateRepository departmentStateRepo,
            RoomRepository roomRepo, RoomStateRepository roomStateRepo,
            BedRepository bedRepo, BedStateRepository bedStateRepo) {
        this.locationRepo = locationRepo;
        this.departmentRepo = departmentRepo;
        this.departmentStateRepo = departmentStateRepo;
        this.roomRepo = roomRepo;
        this.roomStateRepo = roomStateRepo;
        this.bedRepo = bedRepo;
        this.bedStateRepo = bedStateRepo;
    }

    /**
     * Process location metadata, saving and updating states for department, room and bed.
     * Updates location ForeignKeys if they don't already exist.
     * @param msg        message to process
     * @param storedFrom time that emap core started processing the message
     * @throws IncompatibleDatabaseStateException if static entities (department, room and bed) change from what the database knows about
     */
    @Transactional
    public void processMessage(LocationMetadata msg, Instant storedFrom) throws IncompatibleDatabaseStateException {
        Location location = getOrCreateLocation(msg.getHl7String());
        Department department = updateOrCreateDepartmentAndState(msg, storedFrom);

        Room room = null;
        if (msg.getRoomCsn() != null) {
            room = updateOrCreateRoomAndState(department, msg, storedFrom);
        }
        Bed bed = null;
        if (msg.getBedCsn() != null) {
            bed = updateOrCreateBedAndState(room, msg, storedFrom);
        }


        addLocationForeignKeys(location, department, room, bed);
    }

    private Location getOrCreateLocation(String hl7String) {
        return locationRepo.findByLocationStringEquals(hl7String)
                .orElseGet(() -> locationRepo.save(new Location(hl7String)));
    }


    /**
     * Create department if it doesn't exist and update state.
     * <p>
     * Status is the only thing that can change for a department state and we're not expecting them to start with a valid from.
     * This means that the best we can do is order them in the order that we receive them and if the state has changed, make this the active state.
     * @param msg        message to be processed
     * @param storedFrom time that emap core started processing the message
     * @return department entity
     * @throws IncompatibleDatabaseStateException if the department name or speciality changes
     */
    private Department updateOrCreateDepartmentAndState(LocationMetadata msg, Instant storedFrom) throws IncompatibleDatabaseStateException {
        Department dep = departmentRepo
                .findByHl7String(msg.getDepartmentHl7())
                .orElseGet(() -> departmentRepo.save(
                        new Department(msg.getDepartmentHl7(), msg.getDepartmentName(), msg.getDepartmentSpeciality())));

        if (notNullAndDifferent(dep.getName(), msg.getDepartmentName()) || notNullAndDifferent(msg.getDepartmentSpeciality(), dep.getSpeciality())) {
            throw new IncompatibleDatabaseStateException("Department can't change it's name or speciality");
        }

        createCurrentStateAndUpdatePreviousIfRequired(msg, dep, storedFrom);

        return dep;
    }

    private boolean notNullAndDifferent(String msg, String dep) {
        return dep != null && !dep.equals(msg);
    }

    /**
     * Create state from department and if it's different from an existing state, invalidate the existing state.
     * @param msg        message to process
     * @param department parent department entity
     * @param storedFrom time that emap core started processing the message
     */
    private void createCurrentStateAndUpdatePreviousIfRequired(LocationMetadata msg, Department department, Instant storedFrom) {
        DepartmentState currentState = new DepartmentState(department, msg.getDepartmentRecordStatus(), msg.getDepartmentUpdateDate(), storedFrom);
        Optional<DepartmentState> possiblePreviousState = departmentStateRepo
                .findFirstByDepartmentIdOrderByStoredFromDesc(department);

        // if a state already exists and is different from current then we should make a new valid state from the current message
        if (possiblePreviousState.isPresent()) {
            DepartmentState previousState = possiblePreviousState.get();
            if (!previousState.getStatus().equals(msg.getDepartmentRecordStatus())) {
                previousState.setStoredUntil(currentState.getStoredFrom());
                previousState.setValidUntil(currentState.getValidFrom());
                departmentStateRepo.saveAll(List.of(previousState, currentState));
            }
        } else {
            // if no state state exists already then just save the state
            departmentStateRepo.save(currentState);
        }
    }

    /**
     * Create Room if it doesn't exist and update state.
     * <p>
     * We should receive rooms in order of their valid from, so if a room doesn't exist (by CSN) then it should be created.
     * @param msg        message to be processed
     * @param storedFrom time that emap core started processing the message
     * @return room
     * @throws IncompatibleDatabaseStateException if room name changes
     */
    private Room updateOrCreateRoomAndState(Department department, LocationMetadata msg, Instant storedFrom)
            throws IncompatibleDatabaseStateException {
        Room room = roomRepo
                .findByHl7String(msg.getRoomHl7())
                .orElseGet(() -> roomRepo.save(new Room(msg.getRoomHl7(), msg.getRoomName(), department)));

        if (notNullAndDifferent(room.getName(), msg.getRoomName())) {
            throw new IncompatibleDatabaseStateException("Room can't change it's name");
        }
        List<RoomState> states = roomStateRepo.findAllByRoomIdOrderByValidFromDesc(room);

        Optional<RoomState> existingState = states.stream()
                .filter(state -> state.getCsn().equals(msg.getRoomCsn()))
                .findFirst();

        if (existingState.isPresent()) {
            return room;
        }
        createCurrentStateAndInvalidatePrevious(msg, storedFrom, room, states);

        return room;
    }

    /**
     * Create new state from current message, invalidating the previous state and saving if required.
     * @param msg        message to process
     * @param storedFrom time that emap-core started processing the message
     * @param room       room entity
     * @param states     previous states sorted by descending valid from dates
     * @throws IncompatibleDatabaseStateException if a novel CSN is found with a contact date earlier than the latest state
     */
    private void createCurrentStateAndInvalidatePrevious(
            LocationMetadata msg, Instant storedFrom, Room room, Collection<RoomState> states) throws IncompatibleDatabaseStateException {
        RoomState currentState = new RoomState(
                room, msg.getRoomCsn(), msg.getRoomRecordState(), msg.getIsRoomReady(), msg.getRoomContactDate(), storedFrom);


        // if the room doesn't have any existing states we don't need to invalidate any previous states
        if (states.isEmpty()) {
            roomStateRepo.save(currentState);
            return;
        }

        // assuming the current message is after the most recent state, we should invalidate it and save the new state
        RoomState previousState = states.stream().findFirst().orElseThrow();
        if (currentState.getValidFrom().isBefore(previousState.getValidFrom())) {
            throw new IncompatibleDatabaseStateException("New room state is valid before the most current room state");
        }

        previousState.setValidUntil(currentState.getValidFrom());
        previousState.setStoredUntil(currentState.getStoredFrom());

        roomStateRepo.saveAll(List.of(previousState, currentState));
    }

    /**
     * Create Bed if it doesn't exist and update state.
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
    private Bed updateOrCreateBedAndState(Room room, LocationMetadata msg, Instant storedFrom) throws IncompatibleDatabaseStateException {
        Bed bed = bedRepo
                .findByHl7String(msg.getBedHl7())
                .orElseGet(() -> bedRepo.save(new Bed(msg.getBedHl7(), room)));

        List<BedState> states = bedStateRepo.findAllByBedIdOrderByValidFromDesc(bed);

        // if we already know about the bed pool, increment it and don't do any further processing
        if (msg.getIsPoolBed()) {
            Optional<BedState> existingPoolBed = findExistingPoolBedByValidFrom(msg.getBedContactDate(), states);
            if (existingPoolBed.isPresent()) {
                incrementPoolBedAndSave(existingPoolBed.get());
                return bed;
            }
        }

        // if we already know about the bed CSN, don't do any further processing
        Optional<BedState> existingState = states.stream()
                .filter(state -> state.getCsn().equals(msg.getBedCsn()))
                .findFirst();
        if (existingState.isPresent()) {
            return bed;
        }
        createCurrentStateAndInvalidatePrevious(msg, storedFrom, bed, states);

        return bed;
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
     * @param msg        message to process
     * @param storedFrom time that emap-core started processing the message
     * @param bed        bed entity
     * @param states     previous states sorted by descending valid from dates
     * @throws IncompatibleDatabaseStateException if a novel, non-pool CSN is found with a contact date earlier than the latest state
     */
    private void createCurrentStateAndInvalidatePrevious(
            LocationMetadata msg, Instant storedFrom, Bed bed, Collection<BedState> states) throws IncompatibleDatabaseStateException {
        BedState currentState = new BedState(
                bed, msg.getBedCsn(), msg.getBedIsInCensus(), msg.getIsBunkBed(), msg.getBedRecordState(), msg.getIsPoolBed(),
                msg.getBedContactDate(), storedFrom
        );

        if (msg.getIsPoolBed()) {
            incrementPoolBedAndSave(currentState);
        }

        // if the bed doesn't have any existing states we don't need to invalidate any previous states
        if (states.isEmpty()) {
            bedStateRepo.save(currentState);
            return;
        }

        // assuming the current message is after the most recent state, we should invalidate it and save the new state
        BedState previousState = states.stream().findFirst().orElseThrow();
        if (currentState.getValidFrom().isBefore(previousState.getValidFrom())) {
            throw new IncompatibleDatabaseStateException("New bed state is valid before the most current bed state");
        }

        previousState.setValidUntil(currentState.getValidFrom());
        previousState.setStoredUntil(currentState.getStoredFrom());

        bedStateRepo.saveAll(List.of(previousState, currentState));
    }

    /**
     * Add foreign keys to location if they are currently null and save.
     * @param location   location entity
     * @param department department entity
     * @param room       nullable room entity
     * @param bed        nullable bed entity
     * @throws IncompatibleDatabaseStateException if an existing foreign key has changed
     */
    private void addLocationForeignKeys(Location location, Department department, @Nullable Room room, @Nullable Bed bed)
            throws IncompatibleDatabaseStateException {
        boolean changed = false;
        if (location.getDepartmentId() != department) {
            if (location.getDepartmentId() != null) {
                throw new IncompatibleDatabaseStateException("A location string can't change it's Department");
            }
            changed = true;
            location.setDepartmentId(department);
        }

        if (room != null && room != location.getRoomId()) {
            if (location.getRoomId() != null) {
                throw new IncompatibleDatabaseStateException("A location string can't change it's Room");
            }
            changed = true;
            location.setRoomId(room);
        }
        if (bed != null && bed != location.getBedId()) {
            if (location.getBedId() != null) {
                throw new IncompatibleDatabaseStateException("A location string can't change it's Bed");
            }
            changed = true;
            location.setBedId(bed);
        }

        if (changed) {
            locationRepo.save(location);
        }
    }
}
