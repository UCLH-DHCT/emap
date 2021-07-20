package uk.ac.ucl.rits.inform.datasinks.emapstar.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.locations.BedRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.locations.BedStateRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.locations.DepartmentRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.locations.DepartmentStateRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.locations.LocationRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.locations.RoomRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.locations.RoomStateRepository;
import uk.ac.ucl.rits.inform.informdb.movement.Department;
import uk.ac.ucl.rits.inform.informdb.movement.Location;
import uk.ac.ucl.rits.inform.informdb.movement.Room;
import uk.ac.ucl.rits.inform.interchange.LocationMetadata;

import java.time.Instant;

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
    public void processMessage(LocationMetadata locationMetadata, Instant storedFrom) {
        Location location = getOrCreateLocation(locationMetadata.getHl7String());
        Department department = updateOrCreateDepartmentAndState(location, locationMetadata, storedFrom);
        Room room = updateOrCreateRoomAndState(location, department, locationMetadata, storedFrom);
        updateOrCreateBedAndState(location, room, locationMetadata, storedFrom);
    }

    private Location getOrCreateLocation(String hl7String) {
        return locationRepo.findByLocationStringEquals(hl7String)
                .orElseGet(() -> locationRepo.save(new Location(hl7String)));
    }

    /**
     * Create department if it doesn't exist and update state.
     * @param location         location entity
     * @param locationMetadata message to be processed
     * @param storedFrom       time that emap core started processing the message
     * @return department entity
     */
    private Department updateOrCreateDepartmentAndState(Location location, LocationMetadata locationMetadata, Instant storedFrom) {
        return null;
    }

    /**
     * Create Room if it doesn't exist and update state.
     * @param location         location entity
     * @param department       department entity that the room is associated with
     * @param locationMetadata message to be processed
     * @param storedFrom       time that emap core started processing the message
     * @return room
     */
    private Room updateOrCreateRoomAndState(Location location, Department department, LocationMetadata locationMetadata, Instant storedFrom) {
        return null;
    }

    /**
     * Create Bed if it doesn't exist and update state.
     * For pool beds, we create a single bed and in the state entity, increment the number of pool beds found at the contact time.
     * @param location         location entity
     * @param room             room entity that the bed is associated with
     * @param locationMetadata message to be processed
     * @param storedFrom       time that emap core started processing the message
     */
    private void updateOrCreateBedAndState(Location location, Room room, LocationMetadata locationMetadata, Instant storedFrom) {
    }
}
