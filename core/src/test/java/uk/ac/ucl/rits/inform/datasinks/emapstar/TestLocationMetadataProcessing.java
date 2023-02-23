package uk.ac.ucl.rits.inform.datasinks.emapstar;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.jdbc.Sql;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.IncompatibleDatabaseStateException;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.locations.BedFacilityRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.locations.BedRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.locations.BedStateRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.locations.DepartmentRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.locations.DepartmentStateRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.locations.LocationRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.locations.RoomRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.locations.RoomStateRepository;
import uk.ac.ucl.rits.inform.informdb.movement.Bed;
import uk.ac.ucl.rits.inform.informdb.movement.BedFacility;
import uk.ac.ucl.rits.inform.informdb.movement.BedState;
import uk.ac.ucl.rits.inform.informdb.movement.Department;
import uk.ac.ucl.rits.inform.informdb.movement.DepartmentState;
import uk.ac.ucl.rits.inform.informdb.movement.Location;
import uk.ac.ucl.rits.inform.informdb.movement.Room;
import uk.ac.ucl.rits.inform.informdb.movement.RoomState;
import uk.ac.ucl.rits.inform.interchange.EpicRecordStatus;
import uk.ac.ucl.rits.inform.interchange.LocationMetadata;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
    @Autowired
    private BedFacilityRepository bedFacilityRepo;

    private static final String ACUN_DEPT_HL7_STRING = "ACUN";
    private static final String ACUN_ROOM_HL7_STRING = "E03ACUN BY12";
    private static final String ACUN_BED_HL7_STRING = "BY12-C49";
    private static final String ACUN_LOCATION_HL7_STRING = String.join("^", ACUN_DEPT_HL7_STRING, ACUN_ROOM_HL7_STRING, ACUN_BED_HL7_STRING);
    private static final Instant CONTACT_TIME = Instant.parse("2016-02-09T00:00:00Z");
    private static final Instant LATER_TIME = CONTACT_TIME.plusSeconds(20);
    private static final Instant EARLIER_TIME = CONTACT_TIME.minusSeconds(20);
    private static final long ACUN_ROOM_CSN = 1158;
    private static final long ACUN_BED_CSN = 4417L;
    private static final String ACUN_BED_FACILITY = "Cot";

    private static final long MEDSURG_BED_CSN = 11L;

    private static final String DENTAL_HL7_STRING = "1000000059^null^null";


    private LocationMetadata acunCensusBed;
    private LocationMetadata medSurgPoolBed;
    private LocationMetadata dentalDepOnly;

    TestLocationMetadataProcessing() throws IOException {
        acunCensusBed = messageFactory.getLocationMetadata("acun_census_bed.yaml");
        medSurgPoolBed = messageFactory.getLocationMetadata("medsurg_active_pool_bed.yaml");
        dentalDepOnly = messageFactory.getLocationMetadata("dental_department_only.yaml");
    }

    /**
     * @return get location from string
     */
    private Location getLocation(String locationString) {
        return locationRepo.findByLocationStringEquals(locationString).orElseThrow();
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
        Location location = getLocation(ACUN_LOCATION_HL7_STRING);
        assertNotNull(location);
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

        getLocation(ACUN_LOCATION_HL7_STRING);
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

        Location location = getLocation(ACUN_LOCATION_HL7_STRING);
        Department dep = location.getDepartmentId();
        assertEquals(ACUN_DEPT_HL7_STRING, dep.getHl7String());
        assertEquals("EGA E03 ACU NURSERY", dep.getName());
        assertEquals("Maternity - Well Baby", dep.getSpeciality());

        DepartmentState depState = departmentStateRepo.findByDepartmentIdAndStatus(dep, EpicRecordStatus.ACTIVE.toString()).orElseThrow();
        assertNotNull(depState.getStoredFrom());
        assertNull(depState.getValidUntil());
        assertNotNull(depState.getValidFrom());
        assertNull(depState.getStoredUntil());
    }

    /**
     * Given no departments exist in the database
     * When a location with full hl7 string (dep^room^bed) is processed
     * Then a full location entity is created, and a department only location is created
     * @throws Exception shouldn't happen
     */
    @Test
    void testDepartmentOnlyLocationCreated() throws Exception {
        processSingleMessage(acunCensusBed);

        assertTrue(locationRepo.findByLocationStringEquals(ACUN_LOCATION_HL7_STRING).isPresent());

        String departmentOnlyHl7 = String.format("%s^null^null", ACUN_DEPT_HL7_STRING);
        Location depOnly = locationRepo.findByLocationStringEquals(departmentOnlyHl7).orElseThrow();
        assertNotNull(depOnly.getDepartmentId());
        assertNull(depOnly.getRoomId());
        assertNull(depOnly.getBedId());
    }

    /**
     * Given no departments exist in the database
     * When a location with full hl7 string (dep^room^bed) is processed with a null department hl7 string
     * Then only a full location entity is created
     * @throws Exception shouldn't happen
     */
    @Test
    void testNullDepartmentOnlyCreatedFullLocation() throws Exception {
        String nullDept = String.join("^", "null", ACUN_ROOM_HL7_STRING, ACUN_BED_HL7_STRING);
        acunCensusBed.setHl7String(nullDept);
        acunCensusBed.setDepartmentHl7("null");

        processSingleMessage(acunCensusBed);

        assertTrue(locationRepo.findByLocationStringEquals(nullDept).isPresent());
        assertTrue(locationRepo.findByLocationStringEquals("null^null^null").isEmpty());
    }

    /**
     * Given full hl7 location and department only already exists in the database
     * When a location with full hl7 string (dep^room^bed) is processed
     * Then no new locations should be created
     * @throws Exception shouldn't happen
     */
    @Test
    @Sql("/populate_db.sql")
    void testFullLocationAndDepartmentOnlyLocationExistsAlready() throws Exception {
        long preProcessingCount = locationRepo.count();

        processSingleMessage(acunCensusBed);

        long postProcessingCount = locationRepo.count();
        assertEquals(preProcessingCount, postProcessingCount);
    }

    /**
     * Given no departments exist in the database
     * When a location with only a department is processed
     * Then the department only location is created
     * @throws Exception shouldn't happen
     */
    @Test
    void testDepartmentOnlyLocationParsed() throws Exception {
        processSingleMessage(dentalDepOnly);

        Location depOnly = locationRepo.findByLocationStringEquals(DENTAL_HL7_STRING).orElseThrow();
        assertNotNull(depOnly.getDepartmentId());
        assertNull(depOnly.getRoomId());
        assertNull(depOnly.getBedId());
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
        EpicRecordStatus newStatus = EpicRecordStatus.INACTIVE;
        acunCensusBed.setDepartmentRecordStatus(newStatus);
        acunCensusBed.setDepartmentUpdateDate(LATER_TIME);
        processSingleMessage(acunCensusBed);

        Location location = getLocation(ACUN_LOCATION_HL7_STRING);

        // previous state is invalidated
        DepartmentState previousState = departmentStateRepo
                .findByDepartmentIdAndStatus(location.getDepartmentId(), EpicRecordStatus.ACTIVE.toString())
                .orElseThrow();
        assertEquals(LATER_TIME, previousState.getValidUntil());
        assertNotNull(previousState.getStoredUntil());

        // current state is active
        DepartmentState currentState = departmentStateRepo.findByDepartmentIdAndStatus(location.getDepartmentId(), newStatus.toString()).orElseThrow();
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
                metadata -> metadata.setDepartmentSpeciality(newData)
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
        if ("NEW".equals(acunCensusBed.getDepartmentHl7())) {
            // data integrity violation will be the thrown exception, even if our own custom exception is thrown beforehand
            assertThrows(DataIntegrityViolationException.class, () -> processSingleMessage(acunCensusBed));
        } else {
            assertThrows(IncompatibleDatabaseStateException.class, () -> processSingleMessage(acunCensusBed));
        }
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

        Location location = getLocation(ACUN_LOCATION_HL7_STRING);
        Room room = location.getRoomId();
        assertEquals("BY12", room.getName());
        assertEquals(ACUN_ROOM_HL7_STRING, room.getHl7String());
        assertEquals(location.getDepartmentId(), room.getDepartmentId());

        RoomState roomState = roomStateRepo.findByCsn(1158L).orElseThrow();
        assertEquals(EpicRecordStatus.ACTIVE.toString(), roomState.getStatus());
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
    @Test
    @Sql("/populate_db.sql")
    void testNewRoomStateWithLaterTimeUpdates() throws Exception {
        acunCensusBed.setRoomCsn(1L);
        acunCensusBed.setRoomContactDate(LATER_TIME);
        processSingleMessage(acunCensusBed);

        RoomState previousState = roomStateRepo.findByCsn(ACUN_ROOM_CSN).orElseThrow();
        assertEquals(LATER_TIME, previousState.getValidUntil());
        assertNotNull(previousState.getStoredUntil());

        RoomState currentState = roomStateRepo.findByCsn(1L).orElseThrow();
        assertEquals(LATER_TIME, currentState.getValidFrom());
        assertNull(currentState.getValidUntil());
        assertNotNull(currentState.getStoredFrom());
        assertNull(currentState.getStoredUntil());
    }

    /**
     * Given room exists in database
     * when a location metadata message with matching hl7 string (different CSN and same time) is processed
     * then a new active state should be created, invalidating the previous state
     */
    @Test
    @Sql("/populate_db.sql")
    void testNewRoomStateWithSameTimeUpdates() throws Exception {
        acunCensusBed.setRoomCsn(1L);
        processSingleMessage(acunCensusBed);

        RoomState previousState = roomStateRepo.findByCsn(ACUN_ROOM_CSN).orElseThrow();
        assertEquals(CONTACT_TIME, previousState.getValidUntil());
        assertNotNull(previousState.getStoredUntil());

        RoomState currentState = roomStateRepo.findByCsn(1L).orElseThrow();
        assertEquals(CONTACT_TIME, currentState.getValidFrom());
        assertNull(currentState.getValidUntil());
        assertNotNull(currentState.getStoredFrom());
        assertNull(currentState.getStoredUntil());
    }

    /**
     * Given room exists in database
     * when a location metadata message with matching hl7 string (different CSN and earlier time) is processed
     * an exception should be thrown
     */
    @Test
    @Sql("/populate_db.sql")
    void testNewRoomStateWithEarlierTimeThrows() {
        acunCensusBed.setRoomCsn(1L);
        acunCensusBed.setRoomContactDate(EARLIER_TIME);
        assertThrows(IncompatibleDatabaseStateException.class, () -> processSingleMessage(acunCensusBed));
    }


    /**
     * Given that location exists with a linked room.
     * When a message with the same full hl7 string has a different room name
     * An exception should be thrown
     */
    @Test
    @Sql("/populate_db.sql")
    void testRoomCantChangeName() {
        acunCensusBed.setRoomName("new_name");
        assertThrows(IncompatibleDatabaseStateException.class, () -> processSingleMessage(acunCensusBed));
    }

    /**
     * Given that location exists with a linked room.
     * When a message with the same full hl7 string has a different room hl7 string
     * An exception should be thrown
     */
    @Test
    @Sql("/populate_db.sql")
    void testRoomCantChangeHl7String() {
        acunCensusBed.setRoomHl7("new_name");
        assertThrows(DataIntegrityViolationException.class, () -> processSingleMessage(acunCensusBed));
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

        Location location = getLocation(ACUN_LOCATION_HL7_STRING);
        Bed bed = location.getBedId();

        assertEquals(ACUN_BED_HL7_STRING, bed.getHl7String());
        assertEquals(location.getRoomId(), bed.getRoomId());

        BedState state = bedStateRepo.findByCsn(ACUN_BED_CSN).orElseThrow();
        assertEquals(EpicRecordStatus.ACTIVE.toString(), state.getStatus());
        assertFalse(state.getIsBunk());
        assertTrue(state.getIsInCensus());
        assertNull(state.getPoolBedCount());
    }

    /**
     * Given no beds exist in database
     * when two location metadata messages for pool beds are processed
     * a new pool bed should be created and the pool count should be 2
     */
    @Test
    void poolBedIncrements() throws Exception {
        processSingleMessage(medSurgPoolBed);
        medSurgPoolBed.setBedCsn(1L);
        processSingleMessage(medSurgPoolBed);

        BedState state = bedStateRepo.findByCsn(MEDSURG_BED_CSN).orElseThrow();
        assertEquals(2, state.getPoolBedCount());
    }

    /**
     * Given bed exists in database
     * when a location metadata message with matching hl7 string (different CSN and later time) is processed
     * then a new active state should be created, invalidating the previous state
     */
    @Test
    @Sql("/populate_db.sql")
    void testNewBedStateWithLaterTimeUpdates() throws Exception {
        acunCensusBed.setBedCsn(1L);
        acunCensusBed.setBedContactDate(LATER_TIME);
        processSingleMessage(acunCensusBed);

        BedState previousState = bedStateRepo.findByCsn(ACUN_BED_CSN).orElseThrow();
        assertEquals(LATER_TIME, previousState.getValidUntil());
        assertNotNull(previousState.getStoredUntil());

        BedState currentState = bedStateRepo.findByCsn(1L).orElseThrow();
        assertEquals(LATER_TIME, currentState.getValidFrom());
        assertNull(currentState.getValidUntil());
        assertNotNull(currentState.getStoredFrom());
        assertNull(currentState.getStoredUntil());
    }

    /**
     * Given bed exists in database
     * when a location metadata message with matching hl7 string (different CSN and same time) is processed
     * then a new active state should be created, invalidating the previous state
     */
    @Test
    @Sql("/populate_db.sql")
    void testNewBedStateWithSameTimeUpdates() throws Exception {
        acunCensusBed.setBedCsn(1L);
        processSingleMessage(acunCensusBed);

        BedState previousState = bedStateRepo.findByCsn(ACUN_BED_CSN).orElseThrow();
        assertEquals(CONTACT_TIME, previousState.getValidUntil());
        assertNotNull(previousState.getStoredUntil());

        BedState currentState = bedStateRepo.findByCsn(1L).orElseThrow();
        assertEquals(CONTACT_TIME, currentState.getValidFrom());
        assertNull(currentState.getValidUntil());
        assertNotNull(currentState.getStoredFrom());
        assertNull(currentState.getStoredUntil());
    }

    /**
     * Given bed exists in database
     * when a location metadata message with matching hl7 string (different CSN and earlier time) is processed that doesn't exist in database
     * then an exception should be thrown
     */
    @Test
    @Sql("/populate_db.sql")
    void testNewBedStateWithEarlierTimeThrows() {
        acunCensusBed.setBedCsn(1L);
        acunCensusBed.setBedContactDate(EARLIER_TIME);
        assertThrows(IncompatibleDatabaseStateException.class, () -> processSingleMessage(acunCensusBed));
    }


    /**
     * Given that location exists with a linked bed.
     * When a message with the same full hl7 string has a different bed hl7 string
     * An exception should be thrown
     */
    @Test
    @Sql("/populate_db.sql")
    void testLocationBedCantChange() {
        acunCensusBed.setBedHl7("NEW");
        assertThrows(DataIntegrityViolationException.class, () -> processSingleMessage(acunCensusBed));
    }

    /**
     * Given pool bed exist in database
     * when two location metadata messages for existing pool beds at a different contact time are processed
     * a new pool bed should be created and the pool count should be 2
     */
    @Test
    @Sql("/populate_db.sql")
    void testLaterPoolBedProcessed() throws Exception {
        // setup and process
        medSurgPoolBed.setBedCsn(1L);
        medSurgPoolBed.setBedContactDate(LATER_TIME);
        processSingleMessage(medSurgPoolBed);
        medSurgPoolBed.setBedCsn(2L);
        processSingleMessage(medSurgPoolBed);

        // check previous state
        BedState previousPoolState = bedStateRepo.findByCsn(MEDSURG_BED_CSN).orElseThrow();
        assertEquals(LATER_TIME, previousPoolState.getValidUntil());
        assertNotNull(previousPoolState.getStoredUntil());

        // check current state
        BedState currentPoolState = bedStateRepo.findByCsn(1L).orElseThrow();
        assertEquals(2, currentPoolState.getPoolBedCount());
        assertEquals(LATER_TIME, currentPoolState.getValidFrom());
        assertNull(currentPoolState.getValidUntil());
        assertNotNull(currentPoolState.getStoredFrom());
        assertNull(currentPoolState.getStoredUntil());
    }


    // ROOM FACILITY

    /**
     * Given no beds exist in database
     * when a location metadata message with bed facility is processed
     * then a new bed facility should be created
     */
    @Test
    void testRoomFacilityCreated() throws Exception {
        processSingleMessage(acunCensusBed);
        BedState bedState = bedStateRepo.findByCsn(ACUN_BED_CSN).orElseThrow();
        assertDoesNotThrow(() -> bedFacilityRepo.findByBedStateIdAndType(bedState, ACUN_BED_FACILITY).orElseThrow());
    }

    /**
     * Given no beds exist in database
     * when two location metadata messages for the same CSN are processed with different bed facilities
     * then two new bed facilities should be created
     */
    @Test
    void testTwoRoomFacilitiesForSameBed() throws Exception {
        processSingleMessage(acunCensusBed);
        String newFacility = "Near Nurse Station";
        acunCensusBed.setBedFacility(newFacility);
        processSingleMessage(acunCensusBed);


        BedState bedState = bedStateRepo.findByCsn(ACUN_BED_CSN).orElseThrow();
        List<BedFacility> bedFacilities = bedFacilityRepo.findAllByBedStateIdOrderByType(bedState);
        assertEquals(2, bedFacilities.size());

        assertEquals(ACUN_BED_FACILITY, bedFacilities.get(0).getType());
        assertEquals(newFacility, bedFacilities.get(1).getType());
    }
}
