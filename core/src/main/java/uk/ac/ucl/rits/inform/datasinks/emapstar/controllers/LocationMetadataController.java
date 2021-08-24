package uk.ac.ucl.rits.inform.datasinks.emapstar.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.IncompatibleDatabaseStateException;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.locations.DepartmentRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.locations.DepartmentStateRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.locations.LocationRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.locations.RoomRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.locations.RoomStateRepository;
import uk.ac.ucl.rits.inform.informdb.movement.Bed;
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
    private final BedController bedController;


    /**
     * @param locationRepo repository for Location
     * @param departmentRepo repository for Department
     * @param departmentStateRepo  repository for DepartmentState
     * @param roomRepo repository for Room
     * @param roomStateRepo  repository for RoomState
     * @param bedController controller for Bed tables
     */
    public LocationMetadataController(
            LocationRepository locationRepo, DepartmentRepository departmentRepo, DepartmentStateRepository departmentStateRepo,
            RoomRepository roomRepo, RoomStateRepository roomStateRepo, BedController bedController) {
        this.locationRepo = locationRepo;
        this.departmentRepo = departmentRepo;
        this.departmentStateRepo = departmentStateRepo;
        this.roomRepo = roomRepo;
        this.roomStateRepo = roomStateRepo;
        this.bedController = bedController;
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
            bed = bedController.processBedStateAndFacility(room, msg, storedFrom);
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
                .findByHl7StringAndName(msg.getDepartmentHl7(), msg.getDepartmentName())
                .orElseGet(() -> departmentRepo.save(
                        new Department(msg.getDepartmentHl7(), msg.getDepartmentName(), msg.getDepartmentSpeciality())));

        if (notNullAndDifferent(msg.getDepartmentSpeciality(), dep.getSpeciality())) {
            throw new IncompatibleDatabaseStateException("Department can't change it's speciality");
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
        DepartmentState currentState = new DepartmentState(
                department, msg.getDepartmentRecordStatus().toString(), msg.getDepartmentUpdateDate(), storedFrom);

        Optional<DepartmentState> possiblePreviousState = departmentStateRepo.findFirstByDepartmentIdOrderByStoredFromDesc(department);

        // if a state already exists and is different from current then we should make a new valid state from the current message
        if (possiblePreviousState.isPresent()) {
            DepartmentState previousState = possiblePreviousState.get();
            if (stateIsDifferentOrMessageIsLater(currentState, previousState)) {
                previousState.setStoredUntil(currentState.getStoredFrom());
                previousState.setValidUntil(currentState.getValidFrom());
                departmentStateRepo.saveAll(List.of(previousState, currentState));
            }
        } else {
            // if no state state exists already then just save the state
            departmentStateRepo.save(currentState);
        }
    }

    private boolean stateIsDifferentOrMessageIsLater(DepartmentState currentState, DepartmentState previousState) {
        return !previousState.getStatus().equals(currentState.getStatus()) || previousState.getValidFrom().isBefore(currentState.getValidFrom());
    }

    /**
     * Create Room if it doesn't exist and update state.
     * <p>
     * We should receive rooms in order of their valid from, so if a room doesn't exist (by CSN) then it should be created.
     * @param department department that the room belongs to
     * @param msg        message to be processed
     * @param storedFrom time that emap core started processing the message
     * @return room
     * @throws IncompatibleDatabaseStateException if room name changes
     */
    private Room updateOrCreateRoomAndState(Department department, LocationMetadata msg, Instant storedFrom)
            throws IncompatibleDatabaseStateException {
        Room room = roomRepo
                .findByHl7StringAndDepartmentId(msg.getRoomHl7(), department)
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
                room, msg.getRoomCsn(), msg.getRoomRecordState().toString(), msg.getIsRoomReady(), msg.getRoomContactDate(), storedFrom);


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
