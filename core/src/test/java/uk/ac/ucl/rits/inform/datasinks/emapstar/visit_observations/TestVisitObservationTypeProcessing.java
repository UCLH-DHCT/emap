package uk.ac.ucl.rits.inform.datasinks.emapstar.visit_observations;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ucl.rits.inform.datasinks.emapstar.MessageProcessingBase;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.HospitalVisitRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.vist_observations.VisitObservationRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.vist_observations.VisitObservationTypeAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.vist_observations.VisitObservationTypeRepository;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.visit_recordings.VisitObservation;
import uk.ac.ucl.rits.inform.informdb.visit_recordings.VisitObservationType;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.visit_observations.Flowsheet;
import uk.ac.ucl.rits.inform.interchange.visit_observations.FlowsheetMetadata;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TestVisitObservationTypeProcessing extends MessageProcessingBase {
    FlowsheetMetadata flowsheetMetadata;
    FlowsheetMetadata flowsheetMpiMetadata;
    Flowsheet flowsheetEpic;
    Flowsheet flowsheetClarity;

    @Autowired
    HospitalVisitRepository hospitalVisitRepository;
    @Autowired
    VisitObservationRepository visitObservationRepository;
    @Autowired
    VisitObservationTypeRepository visitObservationTypeRepository;
    @Autowired
    VisitObservationTypeAuditRepository visitObservationTypeAuditRepository;
    private static final String ID_IN_APPLICATION = "449876";
    private static final String INTERFACE_ID = "331258";
    private static final String FLOWSHEET = "flowsheet";
    private static final Instant EARLIER_TIME = Instant.parse("1991-01-01T00:00:00Z");
    private static final Instant LATER_TIME = Instant.parse("2021-12-23T00:00:00Z");


    private void assertMetadataFields(VisitObservationType type, Instant validFrom) {
        assertEquals("R HPSC IDG SW PRESENT", type.getName());
        assertEquals("Social Work Team Member", type.getDisplayName());
        assertEquals("*Unknown", type.getDescription());
        assertEquals("String Type", type.getPrimaryDataType());
        assertEquals(validFrom, type.getValidFrom());
    }

    @BeforeEach
    void setup() throws IOException {
        flowsheetMetadata = messageFactory.getFlowsheetMetadata("flowsheet_metadata.yaml").get(3);
        flowsheetMpiMetadata = messageFactory.getFlowsheetMetadata("flowsheet_mpi_metadata.yaml").get(5);
        flowsheetEpic = messageFactory.getFlowsheets("hl7_flowsheet_metadata.yaml", "0000040").get(0);
        flowsheetClarity = messageFactory.getFlowsheets("hl7_flowsheet_metadata.yaml", "0000040").get(1);
    }

    /**
     * Given no visit observation type exist.
     * When an EPIC flowsheet message arrives
     * Then a visit observation type is created with only interface_id, but no id_in_application
     * @throws EmapOperationMessageProcessingException should never happen
     */
    @Test
    void testCreateVisitObservationTypeFromEpic() throws EmapOperationMessageProcessingException {
        processSingleMessage(flowsheetEpic);

        VisitObservationType visitObservationType = visitObservationTypeRepository.find(INTERFACE_ID,
                null, FLOWSHEET).orElseThrow();

        assertNull(visitObservationType.getIdInApplication());
        assertNotNull(visitObservationType.getInterfaceId());
    }


    /**
     * Given no visit observation type exist.
     * When a caboodle flowsheet metadata message arrives
     * Then a visit observation type is created with only id_in_application, but no interface_id
     */
    @Test
    void testCreateVisitObservationTypeFromCaboodleMetadata() throws EmapOperationMessageProcessingException {
        processSingleMessage(flowsheetMetadata);

        VisitObservationType visitObservationType = visitObservationTypeRepository.find(null, ID_IN_APPLICATION,
                FLOWSHEET).orElseThrow();
        assertNull(visitObservationType.getInterfaceId());
        assertNotNull(visitObservationType.getIdInApplication());
    }

    /**
     * Given no visit observation type exist.
     * When a clarity flowsheet metadata message arrives
     * Then a visit observation type is created with both id_in_application and interface_id
     */
    @Test
    void testCreateVisitObservationTypeFromClarityMetadata() throws EmapOperationMessageProcessingException {
        processSingleMessage(flowsheetMpiMetadata);

        VisitObservationType visitObservationType = visitObservationTypeRepository.find(INTERFACE_ID, ID_IN_APPLICATION,
                FLOWSHEET).orElseThrow();
        assertNotNull(visitObservationType.getInterfaceId());
        assertNotNull(visitObservationType.getIdInApplication());
    }

    /**
     * Given a visit observation type with id_in_application but no interface_id exists
     * When a EPIC flowsheet message arrives
     * Then another visit observation type is created with interface_id but no id_in_application
     */
    @Test
    void testCreateVisitObservationTypeFromEpicAndCaboodle() throws EmapOperationMessageProcessingException {
        processSingleMessage(flowsheetEpic);
        processSingleMessage(flowsheetMetadata);

        List vots = getAllEntities(visitObservationTypeRepository);
        assertEquals(2, vots.size());
    }

    /**
     * Given a visit observation type with interface_id but no id_in_application exists
     * When an EPIC flowsheet message arrives
     * Then the existing visit observation type is not changed
     */
    @Test
    void testNotCreateVisitObservationTypeIfExistsEpic() throws EmapOperationMessageProcessingException {
        processSingleMessage(flowsheetEpic);
        List vots = getAllEntities(visitObservationTypeRepository);
        assertEquals(1, vots.size());

        processSingleMessage(flowsheetEpic);
        vots = getAllEntities(visitObservationTypeRepository);
        assertEquals(1, vots.size());
    }

    /**
     * Given a visit observation type with id_in_application but no interface_id exists
     * When a clarity flowsheet metadata message arrives
     * Then the missing id_in_application for visit observation type is filled in
     */
    @Test
    void testFillIdInApplication() throws EmapOperationMessageProcessingException {
        processSingleMessage(flowsheetEpic);
        processSingleMessage(flowsheetMpiMetadata);

        List vots = getAllEntities(visitObservationTypeRepository);
        assertEquals(1, vots.size());

        assertEquals(INTERFACE_ID, ((VisitObservationType)vots.get(0)).getInterfaceId());
        assertEquals(ID_IN_APPLICATION, ((VisitObservationType)vots.get(0)).getIdInApplication());
    }

    /**
     * Given:
     * - a visit observation type with id_in_application
     * - a second visit observation type with interface_id
     * - a visit observation referring to the first visit observation type
     * - another visit observation referring to the second visit observation type
     * When a clarity flowsheet metadata message is processed that links id_in_application with interface_id
     * Then 
     * - the visit observation type with id_in_application is updated to hold the interface_id from second visit observation type
     * - the id is updated for the second visit observation referring to second visit observation type
     * - the second visit observation type is deleted
     */
    @Test
    void testMappingClaritySecondOT() throws EmapOperationMessageProcessingException {
        processSingleMessage(flowsheetEpic);
        processSingleMessage(flowsheetClarity);

        List vots = getAllEntities(visitObservationTypeRepository);
        assertEquals(2, vots.size());
        assertEquals(((VisitObservationType)vots.get(0)).getInterfaceId(), INTERFACE_ID);
        assertEquals(((VisitObservationType)vots.get(1)).getIdInApplication(), ID_IN_APPLICATION);

        processSingleMessage(flowsheetMpiMetadata);
        vots = getAllEntities(visitObservationTypeRepository);
        // check that ID replacements on observation types worked
        assertEquals(1, vots.size());
        assertEquals(((VisitObservationType)vots.get(0)).getInterfaceId(), INTERFACE_ID);
        assertEquals(((VisitObservationType)vots.get(0)).getIdInApplication(), ID_IN_APPLICATION);

        HospitalVisit visit = hospitalVisitRepository.findByEncounter(defaultEncounter).orElseThrow(NullPointerException::new);
        List<VisitObservation> vos = visitObservationRepository.findAllByHospitalVisitId(visit);
        assertEquals(vos.get(1).getVisitObservationTypeId().getVisitObservationTypeId(), ((VisitObservationType)vots.get(0)).getVisitObservationTypeId());
    }

    /**
     * Given a visit observation type with both id_in_application and interface_id
     * When a newer caboodle flowsheet metadata message arrives with different names
     * Then the names are updated accordingly but ids are kept as they are
     */
    @Test
    void testNewCaboodleUpdate() throws EmapOperationMessageProcessingException {
        String newDescription = "new description";
        processSingleMessage(flowsheetMetadata);
        processSingleMessage(flowsheetMpiMetadata);
        List vots = getAllEntities(visitObservationTypeRepository);
        assertEquals(1, vots.size());

        assertEquals(INTERFACE_ID, ((VisitObservationType)vots.get(0)).getInterfaceId());
        assertEquals(ID_IN_APPLICATION, ((VisitObservationType)vots.get(0)).getIdInApplication());

        flowsheetMetadata.setDescription(newDescription);
        flowsheetMetadata.setLastUpdatedInstant(LATER_TIME);
        processSingleMessage(flowsheetMetadata);

        vots = getAllEntities(visitObservationTypeRepository);
        assertEquals(1, vots.size());
        assertEquals(newDescription, ((VisitObservationType) vots.get(0)).getDescription());
    }

    /**
     * Given a visit observation type with both id_in_application and interface_id
     * When an older caboodle flowsheet metadata message arrives with different names
     * Then the names are not updated and ids are kept as they are
     */
    @Test
    void testOlderCaboodleUpdate() throws EmapOperationMessageProcessingException {
        String newDescription = "new description";
        processSingleMessage(flowsheetMetadata);
        processSingleMessage(flowsheetMpiMetadata);
        List vots = getAllEntities(visitObservationTypeRepository);
        assertEquals(1, vots.size());

        assertEquals(INTERFACE_ID, ((VisitObservationType)vots.get(0)).getInterfaceId());
        assertEquals(ID_IN_APPLICATION, ((VisitObservationType)vots.get(0)).getIdInApplication());

        flowsheetMetadata.setDescription(newDescription);
        flowsheetMetadata.setLastUpdatedInstant(EARLIER_TIME);
        processSingleMessage(flowsheetMetadata);

        vots = getAllEntities(visitObservationTypeRepository);
        assertEquals(1, vots.size());
        assertNotEquals(newDescription, ((VisitObservationType) vots.get(0)).getDescription());
    }
}
