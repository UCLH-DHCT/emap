package uk.ac.ucl.rits.inform.datasinks.emapstar.visit_observations;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import uk.ac.ucl.rits.inform.datasinks.emapstar.MessageProcessingBase;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.vist_observations.VisitObservationTypeAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.vist_observations.VisitObservationTypeRepository;
import uk.ac.ucl.rits.inform.informdb.visit_recordings.VisitObservationType;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.visit_observations.FlowsheetMetadata;

import java.io.IOException;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestVisitObservationTypeProcessing extends MessageProcessingBase {
    FlowsheetMetadata flowsheetMetadata;

    @Autowired
    VisitObservationTypeRepository visitObservationTypeRepository;
    @Autowired
    VisitObservationTypeAuditRepository visitObservationTypeAuditRepository;
    private static final String CABOODLE_APPLICATION = "caboodle";
    private static final String FLOWSHEET = "flowsheet";
    private static final String FLOWSHEET_ID = "5";
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
        flowsheetMetadata = messageFactory.getFlowsheetMetadata("flowsheet_metadata.yaml").get(0);
    }

    /**
     * Given no metadata types exist.
     * When a single metadata message is processed
     * Then a visit observation type should be created
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    void testEntityCreated() throws EmapOperationMessageProcessingException {
        processSingleMessage(flowsheetMetadata);
        assertEquals(1, visitObservationTypeRepository.count());

        VisitObservationType type = visitObservationTypeRepository.findAll().iterator().next();
        assertMetadataFields(type, flowsheetMetadata.getLastUpdatedInstant());
    }

    /**
     * Given flowsheet visit observation type already exists
     * When a new metadata message is processed
     * Non-minimal fields should be updated
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    @Sql("/populate_db.sql")
    void testFieldsUpdated() throws EmapOperationMessageProcessingException {
        // Set existing data to be a flowsheet which exists already
        FlowsheetMetadata metadata = flowsheetMetadata;
        metadata.setLastUpdatedInstant(LATER_TIME);
        metadata.setFlowsheetId(FLOWSHEET_ID);
        metadata.setInterfaceId(FLOWSHEET_ID);
        processSingleMessage(metadata);

        VisitObservationType type = visitObservationTypeRepository
                .findByIdInApplicationAndSourceSystemAndSourceObservationType(FLOWSHEET_ID, CABOODLE_APPLICATION, FLOWSHEET)
                .orElseThrow();

        assertMetadataFields(type, LATER_TIME);
    }

    /**
     * Given flowsheet visit observation type already exists
     * When an older metadata message is processed
     * Non-minimal fields should be updated
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    @Sql("/populate_db.sql")
    void testOldMessageUpdates() throws EmapOperationMessageProcessingException {
        // Set existing data to be a flowsheet which exists already
        FlowsheetMetadata metadata = flowsheetMetadata;
        metadata.setLastUpdatedInstant(EARLIER_TIME);
        metadata.setFlowsheetId(FLOWSHEET_ID);
        metadata.setInterfaceId(FLOWSHEET_ID);
        processSingleMessage(metadata);

        System.out.println(metadata);

        VisitObservationType type = visitObservationTypeRepository
                .findByIdInApplicationAndSourceSystemAndSourceObservationType(FLOWSHEET_ID, CABOODLE_APPLICATION, FLOWSHEET)
                .orElseThrow();

        // shouldn't update as already has data in, and message is older
        assertEquals("R HPSC IDG SW PRESENT", type.getName());
        // should update because was null when older message is processed
        // assertEquals("Social Work Team Member", type.getDisplayName());
    }
}
