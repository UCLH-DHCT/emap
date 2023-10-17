package uk.ac.ucl.rits.inform.datasinks.emapstar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.test.context.jdbc.Sql;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.IncompatibleDatabaseStateException;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.locations.BedFacilityRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.locations.BedStateRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.locations.DepartmentRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.locations.DepartmentStateRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.locations.LocationRepository;
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
import uk.ac.ucl.rits.inform.interchange.location.DepartmentMetadata;
import uk.ac.ucl.rits.inform.interchange.location.LocationMetadata;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Component
class LocationHelper {
    @Autowired
    private LocationRepository locationRepo;

    Location getLocation(String locationString) {
        return locationRepo.findByLocationStringEquals(locationString).orElseThrow();
    }

    long count() {
        return locationRepo.count();
    }

    Location find(String location){
        return locationRepo.findByLocationStringEquals(location).orElseThrow();
    }
}

class TestLocationMetadataProcessing extends MessageProcessingBase {
    @Autowired
    private LocationHelper locationHelper;

    private static final String ACUN_DEPT_HL7_STRING = "ACUN";
    private static final String ACUN_ROOM_HL7_STRING = "E03ACUN BY12";
    private static final String ACUN_BED_HL7_STRING = "BY12-C49";
    private static final String ACUN_LOCATION_HL7_STRING = String.join("^", ACUN_DEPT_HL7_STRING, ACUN_ROOM_HL7_STRING, ACUN_BED_HL7_STRING);

    private LocationMetadata acunCensusBed;

    @BeforeEach
    void setup() throws IOException {
        acunCensusBed = messageFactory.getLocationMetadata("acun_census_bed.yaml");
    }

    /**
     * Given no locations exist in database
     * when a location metadata message is processed
     * then a new location should be created
     */
    @Test
    void testLocationCreated() throws Exception {
        processSingleMessage(acunCensusBed);
        Location location = locationHelper.getLocation(ACUN_LOCATION_HL7_STRING);
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
        Long preProcessingCount = locationHelper.count();

        processSingleMessage(acunCensusBed);

        locationHelper.getLocation(ACUN_LOCATION_HL7_STRING);
        assertEquals(preProcessingCount, locationHelper.count());
    }

    // DEPARTMENT

    /**
     * Given no departments exist in database
     * when a location metadata message is processed
     * then a new department should be created
     */
    @Test
    void testDepartmentCreated() throws Exception {
        processSingleMessage(acunCensusBed);

        Location location = locationHelper.getLocation(ACUN_LOCATION_HL7_STRING);
        Department dep = location.getDepartmentId();
        assertEquals(ACUN_DEPT_HL7_STRING, dep.getHl7String());
        assertEquals("EGA E03 ACU NURSERY", dep.getName());
    }
}


class TestDepartmentMetadata extends MessageProcessingBase {
    @Autowired
    private LocationHelper locationHelper;
    @Autowired
    private DepartmentRepository departmentRepo;
    @Autowired
    private DepartmentStateRepository departmentStateRepo;


    private static final String ACUN_DEPT_HL7_STRING = "ACUN";
    private static final Instant SPECIALITY_UPDATE_TIME = Instant.parse("2022-02-09T00:00:20Z");


    private DepartmentMetadata acunDepartment;


    @BeforeEach
    void setup() throws IOException {
        acunDepartment = messageFactory.getDepartmentMetadata("acun_dept.yaml");
    }


    private Department getAcunDept() {
        return departmentRepo.findByHl7String(ACUN_DEPT_HL7_STRING).orElseThrow();
    }


    /**
     * Given no departments exist in the database
     * When a department metadata is processed
     * Then a department only location is created (hl7 string is {dep}^null^null)
     * @throws Exception shouldn't happen
     */
    @Test
    void testDepartmentOnlyLocationCreated() throws Exception {
        processSingleMessage(acunDepartment);

        String departmentOnlyHl7 = String.format("%s^null^null", ACUN_DEPT_HL7_STRING);
        Location depOnly = locationHelper.find(departmentOnlyHl7);
        assertNotNull(depOnly.getDepartmentId());
        assertNull(depOnly.getRoomId());
        assertNull(depOnly.getBedId());
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
        long preProcessingCount = locationHelper.count();

        processSingleMessage(acunDepartment);

        long postProcessingCount = locationHelper.count();
        assertEquals(preProcessingCount, postProcessingCount);
    }


    /**
     * Given department exists in database
     * when a location metadata message with the same department (with same data) is processed
     * then there should be no changes to the department table
     */
    @Test
    @Sql("/populate_db.sql")
    void testDepartmentNotDuplicated() throws Exception {
        Iterable<Department> preProcessingDept = departmentRepo.findAll();
        processSingleMessage(acunDepartment);
        assertEquals(preProcessingDept, departmentRepo.findAll());
    }

    /**
     * Given department exists in database
     * when a location metadata message with matching hl7 string (different status and later time) is processed
     * then a new active state should be created, invalidating the previous state at the speciality update time
     */
    @Test
    @Sql("/populate_db.sql")
    void testDepartmentStateAdded() throws Exception {
        EpicRecordStatus newStatus = EpicRecordStatus.INACTIVE;
        acunDepartment.setDepartmentRecordStatus(newStatus);
        acunDepartment.setDepartmentSpeciality("new");
        processSingleMessage(acunDepartment);

        getAcunDept();

        // previous state is invalidated
        DepartmentState previousState = departmentStateRepo
                .findByDepartmentIdAndStatus(getAcunDept(), EpicRecordStatus.ACTIVE.toString())
                .orElseThrow();
        assertEquals(SPECIALITY_UPDATE_TIME, previousState.getValidUntil());
        assertNotNull(previousState.getStoredUntil());

        // current state is active
        DepartmentState currentState = departmentStateRepo.findByDepartmentIdAndStatus(getAcunDept(), newStatus.toString()).orElseThrow();
        assertNotNull(currentState.getStoredFrom());
        assertNull(currentState.getStoredUntil());
        assertEquals(SPECIALITY_UPDATE_TIME, currentState.getValidFrom());
        assertNull(currentState.getValidUntil());
    }



    /**
     * Given nothing in the database
     * When two location metadata messages for the same department (different speciality update time, but same information) are processed out of order
     * The final department state should have the earlier speciality validFrom
     */
    @Test
    void testSameDepartmentStateOutOfOrderMerged() throws Exception {
        acunDepartment.setPreviousDepartmentSpeciality(null);
        acunDepartment.setSpecialityUpdate(SPECIALITY_UPDATE_TIME.plusSeconds(1));
        processSingleMessage(acunDepartment);
        acunDepartment.setSpecialityUpdate(SPECIALITY_UPDATE_TIME);
        processSingleMessage(acunDepartment);


        // only single state for the department and status, and uses the earlier time (that was received second)
        DepartmentState currentState = departmentStateRepo.findByDepartmentIdAndStatus(getAcunDept(), EpicRecordStatus.ACTIVE.toString()).orElseThrow();
        assertEquals(SPECIALITY_UPDATE_TIME, currentState.getValidFrom());
    }

    /**
     * Given nothing in the database
     * When two location metadata messages for the same department (different speciality update time, but same information) are processed in order
     * The final department state should have the earlier speciality validFrom
     */
    @Test
    void testSameDepartmentStateInOrderMerged() throws Exception {
        acunDepartment.setPreviousDepartmentSpeciality(null);
        acunDepartment.setSpecialityUpdate(SPECIALITY_UPDATE_TIME);
        processSingleMessage(acunDepartment);
        acunDepartment.setSpecialityUpdate(SPECIALITY_UPDATE_TIME.plusSeconds(1));
        processSingleMessage(acunDepartment);


        // only single state for the department and status, and uses the earlier time (that was received second)
        DepartmentState currentState = departmentStateRepo.findByDepartmentIdAndStatus(getAcunDept(), EpicRecordStatus.ACTIVE.toString()).orElseThrow();
        assertEquals(SPECIALITY_UPDATE_TIME, currentState.getValidFrom());
    }

    /**
     * Given no department states existing in the database
     * when an old and a new department speciality are both given in the hl7 message
     * then two department state rows must be created
     */
    @Test
    void testDepartmentPreviousSpecialityNotInDB() throws Exception {
        // Process message
        processSingleMessage(acunDepartment);

        // Checking the original department speciality
        Department dep = getAcunDept();
        DepartmentState prevState = departmentStateRepo.findByDepartmentIdAndSpeciality(dep, "Dental - Oral Medicine").orElseThrow();
        assertNotNull(prevState.getValidFrom());
        assertNotNull(prevState.getValidUntil());
        assertNotNull(prevState.getStoredFrom());
        assertNotNull(prevState.getStoredUntil());

        // Check that the current department start has starting dates but not end dates
        DepartmentState currentState = departmentStateRepo.findByDepartmentIdAndSpeciality(dep, "Maternity - Well Baby").orElseThrow();
        assertNotNull(currentState.getValidFrom());
        assertNull(currentState.getValidUntil());
        assertNotNull(currentState.getStoredFrom());
        assertNull(currentState.getStoredUntil());

        // Check that there are two department states
        assertEquals(2, departmentStateRepo.findAllByDepartmentId(dep).size());
    }

    /**
     * Given a department already has a speciality
     * when a message changes the department to have a new speciality
     * then a new DepartmentState should be created and the old one updated
     */
    @Test
    @Sql("/populate_db.sql")
    void testDepartmentPreviousSpecialityIsInDB() throws Exception {
        // Process message
        processSingleMessage(acunDepartment);

        // Checking the original department speciality
        Department dep = getAcunDept();
        DepartmentState prevState = departmentStateRepo.findByDepartmentIdAndSpeciality(dep, "Dental - Oral Medicine").orElseThrow();
        assertNotNull(prevState.getValidFrom());
        assertNotNull(prevState.getValidUntil());
        assertNotNull(prevState.getStoredFrom());
        assertNotNull(prevState.getStoredUntil());

        // Check that the current department start has starting dates but not end dates
        DepartmentState currentState = departmentStateRepo.findByDepartmentIdAndSpeciality(dep, "Maternity - Well Baby").orElseThrow();
        assertNotNull(currentState.getValidFrom());
        assertNull(currentState.getValidUntil());
        assertNotNull(currentState.getStoredFrom());
        assertNull(currentState.getStoredUntil());

        // Check that there are two department states
        assertEquals(2, departmentStateRepo.findAllByDepartmentId(dep).size());
    }

    /**
     * Given no department states existing in the database
     * When a message with no previous speciality is processed, then a later message that also has no previous speciality is updated
     * Then only one department state should exist
     */
    @Test
    void testNoPreviousSpecialityForFirstAndSecondMessages() throws Exception {
        // Process message
        acunDepartment.setPreviousDepartmentSpeciality(null);
        acunDepartment.setSpecialityUpdate(null);
        processSingleMessage(acunDepartment);
        acunDepartment.setDepartmentContactDate(acunDepartment.getDepartmentContactDate().plusSeconds(1));
        processSingleMessage(acunDepartment);

        // Check that there is one department state
        assertEquals(1L, departmentStateRepo.count());
    }

}

class TestRoomMetadata extends MessageProcessingBase {
    @Autowired
    private LocationHelper locationHelper;
    @Autowired
    private RoomStateRepository roomStateRepo;

    private static final String ACUN_DEPT_HL7_STRING = "ACUN";
    private static final String ACUN_ROOM_HL7_STRING = "E03ACUN BY12";
    private static final String ACUN_BED_HL7_STRING = "BY12-C49";
    private static final String ACUN_LOCATION_HL7_STRING = String.join("^", ACUN_DEPT_HL7_STRING, ACUN_ROOM_HL7_STRING, ACUN_BED_HL7_STRING);
    private static final Instant CONTACT_TIME = Instant.parse("2016-02-09T00:00:00Z");
    private static final Instant LATER_TIME = CONTACT_TIME.plusSeconds(20);
    private static final Instant EARLIER_TIME = CONTACT_TIME.minusSeconds(20);
    private static final long ACUN_ROOM_CSN = 1158;

    private LocationMetadata acunCensusBed;

    @BeforeEach
    void setup() throws IOException {
        acunCensusBed = messageFactory.getLocationMetadata("acun_census_bed.yaml");
    }

    /**
     * Given no rooms exist in database
     * when a location metadata message with room is processed
     * then a new room and room state should be created
     */
    @Test
    void testRoomCreated() throws Exception {
        processSingleMessage(acunCensusBed);

        Location location = locationHelper.getLocation(ACUN_LOCATION_HL7_STRING);
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
        acunCensusBed.getRoomMetadata().setRoomCsn(1L);
        acunCensusBed.getRoomMetadata().setRoomContactDate(LATER_TIME);
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
        acunCensusBed.getRoomMetadata().setRoomCsn(1L);
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
        acunCensusBed.getRoomMetadata().setRoomCsn(1L);
        acunCensusBed.getRoomMetadata().setRoomContactDate(EARLIER_TIME);
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
        acunCensusBed.getRoomMetadata().setRoomName("new_name");
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
        acunCensusBed.getRoomMetadata().setRoomHl7("new_name");
        assertThrows(DataIntegrityViolationException.class, () -> processSingleMessage(acunCensusBed));
    }


}

class TestBedMetadata extends MessageProcessingBase {
    @Autowired
    private LocationHelper locationHelper;
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
    private static final long ACUN_BED_CSN = 4417L;
    private static final String ACUN_BED_FACILITY = "Cot";

    private static final long MEDSURG_BED_CSN = 11L;


    private LocationMetadata acunCensusBed;
    private LocationMetadata medSurgPoolBed;

    @BeforeEach
    void setup() throws IOException {
        acunCensusBed = messageFactory.getLocationMetadata("acun_census_bed.yaml");
        medSurgPoolBed = messageFactory.getLocationMetadata("medsurg_active_pool_bed.yaml");
    }



    /**
     * Given no beds exist in database
     * when a location metadata message with bed facility is processed
     * then a new bed facility should be created
     */
    @Test
    void testBedFacilityCreated() throws Exception {
        processSingleMessage(acunCensusBed);
        BedState bedState = bedStateRepo.findByCsn(ACUN_BED_CSN).orElseThrow();
        assertDoesNotThrow(() -> bedFacilityRepo.findByBedStateIdAndType(bedState, ACUN_BED_FACILITY).orElseThrow());
    }

    /**
     * Given no beds exist in database
     * when a location metadata message with bed is processed
     * then a new bed and bed state should be created
     */
    @Test
    void testBedCreated() throws Exception {
        processSingleMessage(acunCensusBed);

        Location location = locationHelper.getLocation(ACUN_LOCATION_HL7_STRING);
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
        medSurgPoolBed.getBedMetadata().setBedCsn(1L);
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
        acunCensusBed.getBedMetadata().setBedCsn(1L);
        acunCensusBed.getBedMetadata().setBedContactDate(LATER_TIME);
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
        acunCensusBed.getBedMetadata().setBedCsn(1L);
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
        acunCensusBed.getBedMetadata().setBedCsn(1L);
        acunCensusBed.getBedMetadata().setBedContactDate(EARLIER_TIME);
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
        acunCensusBed.getBedMetadata().setBedHl7("NEW");
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
        var bedData = medSurgPoolBed.getBedMetadata();
        bedData.setBedCsn(1L);
        bedData.setBedContactDate(LATER_TIME);
        processSingleMessage(medSurgPoolBed);
        bedData.setBedCsn(2L);
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

    /**
     * Given no beds exist in database
     * when two location metadata messages for the same CSN are processed with different bed facilities
     * then two new bed facilities should be created
     */
    @Test
    void testTwoRoomFacilitiesForSameBed() throws Exception {
        processSingleMessage(acunCensusBed);
        String newFacility = "Near Nurse Station";
        acunCensusBed.getBedMetadata().setBedFacility(newFacility);
        processSingleMessage(acunCensusBed);


        BedState bedState = bedStateRepo.findByCsn(ACUN_BED_CSN).orElseThrow();
        List<BedFacility> bedFacilities = bedFacilityRepo.findAllByBedStateIdOrderByType(bedState);
        assertEquals(2, bedFacilities.size());

        assertEquals(ACUN_BED_FACILITY, bedFacilities.get(0).getType());
        assertEquals(newFacility, bedFacilities.get(1).getType());
    }

}