package uk.ac.ucl.rits.inform.datasinks.emapstar.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
import java.util.Objects;
import java.util.Optional;

/**
 * Adds or updates location metadata (department, room, bed pool, bed, and their states).
 * @author Stef Piatek
 */
@Component
public class LocationController {
    private static final Logger logger = LoggerFactory.getLogger(LocationController.class);

    private final LocationRepository locationRepo;
    private final DepartmentRepository departmentRepo;
    private final DepartmentStateRepository departmentStateRepo;
    private final RoomRepository roomRepo;
    private final RoomStateRepository roomStateRepo;
    private final BedController bedController;


    /**
     * Interaction with hospital locations.
     * @param locationRepo        repository for Location
     * @param departmentRepo      repository for Department
     * @param departmentStateRepo repository for DepartmentState
     * @param roomRepo            repository for Room
     * @param roomStateRepo       repository for RoomState
     * @param bedController       controller for Bed tables
     */
    public LocationController(
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
     * Gets location entity by string if it exists, otherwise creates it.
     * @param locationString full location string.
     * @return Location entity
     */
    @Cacheable(value = "location", key = "{#locationString}")
    public Location getOrCreateLocation(String locationString) {
        logger.trace("** Querying for location {}", locationString);
        return locationRepo.findByLocationStringEquals(locationString)
                .orElseGet(() -> {
                    Location location = new Location(locationString);
                    return locationRepo.save(location);
                });
    }

    /**
     * Process location metadata, saving and updating states for department, room and bed.
     * Updates location ForeignKeys if they don't already exist.
     * @param msg        message to process
     * @param storedFrom time that emap core started processing the message
     * @throws IncompatibleDatabaseStateException if static entities (department, room and bed) change from what the database knows about
     */
    @Transactional
    @CacheEvict(value = "location", key = "{#msg.hl7String}")
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

    /**
     * Create department if it doesn't exist and update state.
     * <p>
     * Status is the only thing that can change for a department state and we're not expecting them to start with a valid from.
     * This means that the best we can do is order them in the order that we receive them and if the state has changed, make this the active state.
     * @param msg        message to be processed
     * @param storedFrom time that emap core started processing the message
     * @return department entity
     * @throws IncompatibleDatabaseStateException if the previous department speciality is inconsistent with what is currently in EMAP
     */
    private Department updateOrCreateDepartmentAndState(LocationMetadata msg, Instant storedFrom) throws IncompatibleDatabaseStateException {
        Department dep = departmentRepo
                .findByHl7StringAndName(msg.getDepartmentHl7(), msg.getDepartmentName())
                .orElseGet(() -> departmentRepo.save(
                        new Department(msg.getDepartmentHl7(), msg.getDepartmentName())));

        createDepartmentOnlyLocationIfRequired(dep, msg.getHl7String());
        createCurrentStateAndUpdatePreviousIfRequired(msg, dep, storedFrom);

        return dep;
    }

    /**
     * Create location string with no room and bed if it doesn't already exist.
     * <p>
     * Skipping:
     * - locations without room or department (as these will already create a department only location)
     * - departments that have no interface ID (so some through with hl7 string of "null"). As these won't be unique.
     * <p>
     * SQL query will not return department^null^null if there are rooms and locations linked from the query.
     * Correcting this here which isn't that clean but seems justifiable to be able to link patients who
     * have just turned up to the department and are not in a specific room/bed.
     * @param department        department entity
     * @param locationHl7String full hl7 location string
     */
    private void createDepartmentOnlyLocationIfRequired(Department department, String locationHl7String) {
        if ("null".equals(department.getHl7String()) || locationHl7String.endsWith("^null^null")) {
            return;
        }
        String departmentOnlyHl7 = String.format("%s^null^null", department.getHl7String());
        Location departmentLocation = getOrCreateLocation(departmentOnlyHl7);
        if (departmentLocation.getDepartmentId() == null) {
            departmentLocation.setDepartmentId(department);
            locationRepo.save(departmentLocation);
        }
    }

    private boolean notNullAndDifferent(String msg, String dep) {
        return dep != null && !dep.equals(msg);
    }

    /**
     * Create state from department and if it's different from an existing state, invalidate the existing state.
     * @param msg        message to process
     * @param department parent department entity
     * @param storedFrom time that emap core started processing the message
     * @throws IncompatibleDatabaseStateException if the previous department speciality is inconsistent with what is currently in EMAP
     */
    private void createCurrentStateAndUpdatePreviousIfRequired(LocationMetadata msg, Department department, Instant storedFrom)
            throws IncompatibleDatabaseStateException {
        Instant validFrom = msg.getSpecialityUpdate() == null ? msg.getDepartmentUpdateDate() : msg.getSpecialityUpdate();
        DepartmentState currentState = new DepartmentState(
                department, msg.getDepartmentRecordStatus().toString(), msg.getDepartmentSpeciality(), validFrom, storedFrom);

        Optional<DepartmentState> possiblePreviousState = departmentStateRepo.findFirstByDepartmentIdOrderByStoredFromDesc(department);

        // if a state already exists and is different from the current state then we should make a new valid state from the current message
        if (possiblePreviousState.isPresent()) {
            DepartmentState previousState = possiblePreviousState.get();

            if (stateIsDifferentOrMessageIsLater(currentState, previousState)) {
                // Throw an exception if the previous speciality doesn't align with what is currently in EMAP
                if (!Objects.equals(previousState.getSpeciality(), msg.getPreviousDepartmentSpeciality())) {
                    throw new IncompatibleDatabaseStateException("Department speciality does not match what is already in the database");
                }
                // Add new department states to EMAP
                previousState.setStoredUntil(currentState.getStoredFrom());
                previousState.setValidUntil(currentState.getValidFrom());
                departmentStateRepo.saveAll(List.of(previousState, currentState));
            }
        // if the previous department speciality is not in the database
        } else if (msg.getPreviousDepartmentSpeciality() != null) {
            DepartmentState previousState = new DepartmentState(
                    department, msg.getDepartmentRecordStatus().toString(), msg.getPreviousDepartmentSpeciality(),
                    msg.getDepartmentContactDate(), storedFrom);
            previousState.setStoredUntil(currentState.getStoredFrom());
            previousState.setValidUntil(currentState.getValidFrom());
            departmentStateRepo.saveAll(List.of(previousState, currentState));
        // if no state already exists then just save the state as is
        } else {
            departmentStateRepo.save(currentState);
        }
    }

    private boolean stateIsDifferentOrMessageIsLater(DepartmentState currentState, DepartmentState previousState) {
        return !previousState.getStatus().equals(currentState.getStatus())
                || previousState.getValidFrom().isBefore(currentState.getValidFrom())
                || !Objects.equals(currentState.getSpeciality(), previousState.getSpeciality());
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
