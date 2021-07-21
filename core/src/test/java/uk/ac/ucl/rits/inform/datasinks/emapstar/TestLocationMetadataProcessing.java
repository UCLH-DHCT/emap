package uk.ac.ucl.rits.inform.datasinks.emapstar;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
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

import java.io.IOException;
import java.time.Instant;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestLocationMetadataProcessing extends MessageProcessingBase {
    @Autowired
    private LocationRepository locationRepo;
    @Autowired
    private DepartmentRepository departmentRepo;
    @Autowired
    private DepartmentStateRepository departmentStateRepo;
    @Autowired
    private RoomRepository roomRepo;
    @Autowired
    private RoomStateRepository roomStateRepo;
    @Autowired
    private BedRepository bedRepo;
    @Autowired
    private BedStateRepository bedStateRepo;

    private static final String ACUN_DEPT_HL7_STRING = "ACUN";
    private static final String ACUN_ROOM_HL7_STRING = "E03ACUN BY12";
    private static final String ACUN_BED_HL7_STRING = "BY12-C49";
    private static final String ACUN_LOCATION_HL7_STRING = String.join("^", ACUN_DEPT_HL7_STRING, ACUN_ROOM_HL7_STRING, ACUN_BED_HL7_STRING);
    private static final String ACTIVE = "Active";
    private static final Instant CONTACT_TIME = Instant.parse("2016-02-09T00:00:00Z");
    private static final Instant LATER_TIME = CONTACT_TIME.plusSeconds(20);

    private LocationMetadata acunCensusBed;

    TestLocationMetadataProcessing() throws IOException {
        acunCensusBed = messageFactory.getLocationMetadata("acun_census_bed.yaml");
    }

    /**
     * @return default location for acun used in tests.
     */
    private Location getAcunLocation() {
        return locationRepo.findByLocationStringEquals(ACUN_LOCATION_HL7_STRING).orElseThrow();
    }

    // LOCATION

    /**
     * Given no locations exist in database
     * when a location metadata message is processed
     * then a new location should be created
     */
    @Test
    void testLocationCreated() throws Exception {
        processSingleMessage(acunCensusBed);
        getAcunLocation();
        assertEquals(1, locationRepo.count());
    }

    /**
     * Given location already exists in database
     * when a location metadata message is processed with the same string
     * no new locations should be created
     */
    @Test
    @Sql("/populate_db.sql")
    void testLocationNotDuplicated() throws Exception {
        Long preProcessingCount = locationRepo.count();

        processSingleMessage(acunCensusBed);

        getAcunLocation();
        assertEquals(preProcessingCount, locationRepo.count());
    }

    // DEPARTMENT

    /**
     * Given no departments exist in database
     * when a location metadata message is processed
     * then a new department and department state should be created
     */
    @Test
    void testDepartmentCreated() throws Exception {
        processSingleMessage(acunCensusBed);

        Location location = getAcunLocation();
        Department dep = location.getDepartmentId();
        assertEquals(ACUN_DEPT_HL7_STRING, dep.getHl7String());
        assertEquals("EGA E03 ACU NURSERY", dep.getName());
        assertEquals("Maternity - Well Baby", dep.getSpeciality());

        DepartmentState depState = departmentStateRepo.findByDepartmentIdAndStatus(dep, ACTIVE).orElseThrow();
        assertNotNull(depState.getStoredFrom());
        assertNull(depState.getValidUntil());
        assertNull(depState.getValidFrom());
        assertNull(depState.getStoredUntil());
    }


    /**
     * Given department exists in database
     * when a location metadata message with the same department (with same data) is processed
     * then there should be no changes to the department tables
     */
    @Test
    @Sql("/populate_db.sql")
    void testDepartmentNotDuplicated() throws Exception {
        Iterable<Department> preProcessingDept = departmentRepo.findAll();
        Iterable<DepartmentState> preProcessingDeptState = departmentStateRepo.findAll();

        processSingleMessage(acunCensusBed);

        assertEquals(preProcessingDept, departmentRepo.findAll());
        assertEquals(preProcessingDeptState, departmentStateRepo.findAll());
    }

    /**
     * Given department exists in database
     * when a location metadata message with matching hl7 string (different status and later time) is processed
     * then a new active state should be created, invalidating the previous state
     */
    @Test
    @Sql("/populate_db.sql")
    void testDepartmentStateAdded() throws Exception {
        String newStatus = "Inactive";
        acunCensusBed.setDepartmentRecordStatus(newStatus);
        acunCensusBed.setDepartmentUpdateDate(LATER_TIME);
        processSingleMessage(acunCensusBed);

        Location location = getAcunLocation();

        // previous state is invalidated
        DepartmentState previousState = departmentStateRepo.findByDepartmentIdAndStatus(location.getDepartmentId(), ACTIVE).orElseThrow();
        assertEquals(LATER_TIME, previousState.getValidUntil());
        assertNotNull(previousState.getStoredUntil());

        // current state is active
        DepartmentState currentState = departmentStateRepo.findByDepartmentIdAndStatus(location.getDepartmentId(), newStatus).orElseThrow();
        assertNotNull(currentState.getStoredFrom());
        assertNull(currentState.getStoredUntil());
        assertEquals(LATER_TIME, currentState.getValidFrom());
        assertNull(currentState.getValidUntil());
    }

    /**
     * Set fields which should never change to new value.
     */
    static Stream<Consumer<LocationMetadata>> departmentFieldsWithChangedData() {
        String newData = "NEW";
        return Stream.of(
                metadata -> metadata.setDepartmentHl7(newData),
                metadata -> metadata.setDepartmentSpeciality(newData),
                metadata -> metadata.setDepartmentName(newData)
        );
    }

    /**
     * Given that location exists with a linked department.
     * When a message with the same full hl7 string has a different department data (excluding status and updatedDate)
     * An exception should be thrown
     */
    @ParameterizedTest
    @MethodSource("departmentFieldsWithChangedData")
    @Sql("/populate_db.sql")
    void testDepartmentCantChange(Consumer<LocationMetadata> metadataConsumer) {
        metadataConsumer.accept(acunCensusBed);
        assertThrows(IncompatibleDatabaseStateException.class, () -> processSingleMessage(acunCensusBed));
    }

    // ROOM

    /**
     * Given no rooms exist in database
     * when a location metadata message with room is processed
     * then a new room and room state should be created
     */
    @Test
    void testRoomCreated() throws Exception {
        processSingleMessage(acunCensusBed);

        Location location = getAcunLocation();
        Room room = location.getRoomId();
        assertEquals("BY12", room.getName());
        assertEquals(ACUN_ROOM_HL7_STRING, room.getHl7String());
        assertEquals(location.getDepartmentId(), room.getDepartmentId());

        RoomState roomState = roomStateRepo.findByCsn(1158L).orElseThrow();
        assertEquals(ACTIVE, roomState.getStatus());
        assertTrue(roomState.getIsReady());
        assertEquals(CONTACT_TIME, roomState.getValidFrom());
        assertNull(roomState.getValidUntil());
        assertNotNull(roomState.getStoredFrom());
        assertNull(roomState.getStoredUntil());
    }

    /**
     * Given room exists in database
     * when a location metadata message with matching hl7 string (different CSN and later time) is processed
     * then a new active state should be created, invalidating the previous state
     */

    /**
     * Given room exists in database
     * when a location metadata message with matching hl7 string (different CSN and same time) is processed
     * then a new active state should be created, invalidating the previous state
     */

    /**
     * Given room exists in database
     * when a location metadata message with matching hl7 string (different CSN and earlier time) is processed
     * then the previous temporal until data should be updated, processed message temporal until data should be set to the next message from times.
     */

    /**
     * Set fields which should never change to new value.
     */
    static Stream<Consumer<LocationMetadata>> roomFieldsWithChangedData() {
        String newData = "NEW";
        return Stream.of(
                metadata -> metadata.setRoomHl7(newData),
                metadata -> metadata.setRoomName(newData)
        );
    }

    /**
     * Given that location exists with a linked room.
     * When a message with the same full hl7 string has a different room data (excluding status and updatedDate)
     * An exception should be thrown
     */
    @ParameterizedTest
    @MethodSource("roomFieldsWithChangedData")
    @Sql("/populate_db.sql")
    void testRoomCantChange(Consumer<LocationMetadata> metadataConsumer) {
        metadataConsumer.accept(acunCensusBed);
        assertThrows(IncompatibleDatabaseStateException.class, () -> processSingleMessage(acunCensusBed));
    }

    // BED

    /**
     * Given no beds exist in database
     * when a location metadata message with bed is processed
     * then a new bed and bed state should be created
     */
    @Test
    void testBedCreated() throws Exception {
        processSingleMessage(acunCensusBed);

        Location location = getAcunLocation();
        Bed bed = location.getBedId();

        assertEquals(ACUN_BED_HL7_STRING, bed.getHl7String());
        assertEquals(location.getRoomId(), bed.getRoomId());

        BedState state = bedStateRepo.findByCsn(4417L).orElseThrow();
        assertEquals(ACTIVE, state.getStatus());
        assertFalse(state.getIsBunk());
        assertTrue(state.getIsInCensus());
        assertEquals(0, state.getPoolBedCount());
    }

    /**
     * Given no beds exist in database
     * when two location metadata messages for pool beds are processed
     * a new pool bed should be created and the pool count should be 2
     */

    /**
     * Given bed exists in database
     * when a location metadata message with matching hl7 string (different CSN and later time) is processed
     * then a new active state should be created, invalidating the previous state
     */

    /**
     * Given bed exists in database
     * when a location metadata message with matching hl7 string (different CSN and same time) is processed
     * then a new active state should be created, invalidating the previous state
     */

    /**
     * Given bed exists in database
     * when a location metadata message with matching hl7 string (different CSN and earlier time) is processed
     * then the previous temporal until data should be updated, processed message temporal until data should be set to the next message from times.
     */


    /**
     * Given that location exists with a linked bed.
     * When a message with the same full hl7 string has a different bed hl7 string
     * An exception should be thrown
     */
    @Test
    @Sql("/populate_db.sql")
    void testLocationBedCantChange() {
        acunCensusBed.setBedHl7("NEW");
        assertThrows(IncompatibleDatabaseStateException.class, () -> processSingleMessage(acunCensusBed));
    }

    /**
     * Given pool bed exist in database
     * when two location metadata messages for existing pool beds at a different contact time are processed
     * a new pool bed should be created and the pool count should be 2
     */



    // ROOM FACILITY
    /**
     * Given no beds exist in database
     * when a location metadata message with bed facility is processed
     * then a new bed facility should be created
     */

    /**
     * Given no beds exist in database
     * when two location metadata messages for the same CSN are processed with different bed facilities
     * then two new bed facilities should be created
     */


}
