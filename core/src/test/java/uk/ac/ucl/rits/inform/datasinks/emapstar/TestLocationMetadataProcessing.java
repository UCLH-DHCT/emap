package uk.ac.ucl.rits.inform.datasinks.emapstar;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.locations.BedRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.locations.BedStateRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.locations.DepartmentRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.locations.DepartmentStateRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.locations.LocationRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.locations.RoomRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.locations.RoomStateRepository;
import uk.ac.ucl.rits.inform.informdb.movement.Department;
import uk.ac.ucl.rits.inform.informdb.movement.DepartmentState;
import uk.ac.ucl.rits.inform.informdb.movement.Room;
import uk.ac.ucl.rits.inform.informdb.movement.RoomState;
import uk.ac.ucl.rits.inform.interchange.LocationMetadata;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

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

    private LocationMetadata acunCensusBed;

    TestLocationMetadataProcessing() throws IOException {
        acunCensusBed = messageFactory.getLocationMetadata("acun_census_bed.yaml");
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
        locationRepo.findByLocationStringEquals(ACUN_LOCATION_HL7_STRING).orElseThrow();
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

        locationRepo.findByLocationStringEquals(ACUN_LOCATION_HL7_STRING).orElseThrow();
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

        Department dep = departmentRepo.findByHl7String(ACUN_DEPT_HL7_STRING).orElseThrow();
        assertEquals("EGA E03 ACU NURSERY", dep.getName());
        assertEquals("Maternity - Well Baby", dep.getSpeciality());

        DepartmentState depState = departmentStateRepo.findAllByDepartmentIdAndStatus(dep, ACTIVE).orElseThrow();
        assertEquals("Active", depState.getStatus());
        assertNotNull(depState.getStoredFrom());
        assertNull(depState.getValidUntil());
        assertNull(depState.getValidFrom());
        assertNull(depState.getStoredUntil());
    }

    /**
     * Given department exists in database
     * when a location metadata message with the same department CSN (and same data) is processed
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

    /**
     * Given department exists in database
     * when a location metadata message with matching hl7 string (different status and same time) is processed
     * then a new active state should be created, invalidating the previous state
     */

    /**
     * Given department exists in database
     * when a location metadata message with matching hl7 string (different status and earlier time) is processed
     * then the previous temporal until data should be updated, processed message temporal until data should be set to the next message from times.
     */

    // ROOM
    /**
     * Given no rooms exist in database
     * when a location metadata message with room is processed
     * then a new room and room state should be created
     */
    @Test
    void testRoomCreated() throws Exception {
        processSingleMessage(acunCensusBed);
        Room room = roomRepo.findByHl7String(ACUN_ROOM_HL7_STRING).orElseThrow();

        RoomState roomState = roomStateRepo.findByCsn(1158L).orElseThrow();

    }

    /**
     * Given no beds exist in database
     * when a location metadata message with bed is processed
     * then a new bed and bed state should be created
     */

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
