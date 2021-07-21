package uk.ac.ucl.rits.inform.datasinks.emapstar.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import uk.ac.ucl.rits.inform.informdb.movement.Department;
import uk.ac.ucl.rits.inform.informdb.movement.DepartmentState;
import uk.ac.ucl.rits.inform.informdb.movement.Location;
import uk.ac.ucl.rits.inform.informdb.movement.Room;
import uk.ac.ucl.rits.inform.interchange.LocationMetadata;

import java.time.Instant;
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


        addLocationForeignkeys(location, department, room, bed);
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

        if (!msg.getDepartmentName().equals(dep.getName()) || !msg.getDepartmentSpeciality().equals(dep.getSpeciality())) {
            throw new IncompatibleDatabaseStateException("Department can't change it's name or speciality");
        }

        DepartmentState currentState = new DepartmentState(dep, msg.getDepartmentRecordStatus(), msg.getDepartmentUpdateDate(), storedFrom);
        Optional<DepartmentState> possiblePreviousState = departmentStateRepo
                .findFirstByDepartmentIdOrderByStoredFromDesc(dep);

        if (possiblePreviousState.isPresent()) {
            DepartmentState previousState = possiblePreviousState.get();
            if (!previousState.getStatus().equals(msg.getDepartmentRecordStatus())) {
                // previous state is different so update current state and save new state as well
                previousState.setStoredUntil(currentState.getStoredFrom());
                previousState.setValidUntil(currentState.getValidFrom());
                departmentStateRepo.saveAll(List.of(previousState, currentState));
            }
        } else {
            // no previous states exist so just save
            departmentStateRepo.save(currentState);
        }

        return dep;
    }

    /**
     * Create Room if it doesn't exist and update state.
     * @param department       department entity that the room is associated with
     * @param locationMetadata message to be processed
     * @param storedFrom       time that emap core started processing the message
     * @return room
     */
    private Room updateOrCreateRoomAndState(Department department, LocationMetadata locationMetadata, Instant storedFrom) {
        return null;
    }

    /**
     * Create Bed if it doesn't exist and update state.
     * For pool beds, we create a single bed and in the state entity, increment the number of pool beds found at the contact time.
     * @param room             room entity that the bed is associated with
     * @param locationMetadata message to be processed
     * @param storedFrom       time that emap core started processing the message
     * @return bed
     */
    private Bed updateOrCreateBedAndState(Room room, LocationMetadata locationMetadata, Instant storedFrom) {
        return null;
    }

    private void addLocationForeignkeys(Location location, Department department, Room room, Bed bed) throws IncompatibleDatabaseStateException {
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
