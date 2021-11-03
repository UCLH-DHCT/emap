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
        // flowsheetMetadata = messageFactory.getFlowsheetMetadata("flowsheet_metadata.yaml").get(0);

        // need flowsheet meta EPIC
        // need flowsheet meta caboodle
        // need flowsheet that meta refers to
    }

    /**
     * Given no metadata types exist.
     * When a single CLARITY/CABOODLE flowsheet message is processed
     * Then a visit observation type is created and the in_application_id is null
     */

    /**
     * Given no metadata types exist.
     * When a single EPIC flowsheet message is processed
     * Then a largely empty visit observation type is created and the in_application_id is null
     */

    /**
     * Given an EPIC visit observation type exists
     * When a newer EPIC flowsheet message is processed
     * Non-minimal fields should be updated, but type not linked through in_application_id
     */

    /**
     * Given an EPIC visit observation type exists
     * When a newer CLARITY/CABOODLE flowsheet message is processed
     * Non-minimal fields and null fields should be updated and the link through in_application_id created
     */

    /**
     * Given a CLARITY/CABOODLE visit observation type exists
     * When a newer CLARITY/CABOODLE flowsheet message is processed
     * Non-minimal fields and null fields should be updated, link through in_application_id should not be changed
     */

    /**
     * Given a CLARITY/CABOODLE visit observation type exists
     * When a newer EPIC flowsheet message is processed
     * Non-minimal fields and null fields should be updated, link through in_application_id should not be changed
     */

    /**
     * Given a CLARITY/CABOODLE visit observation type exists
     * When an older CLARITY/CABOODLE flowsheet message is processed
     * Null fields should be updated, link through in_application_id should not be changed
     */

    /**
     * Given a CLARITY/CABOODLE visit observation type exists
     * When an older EPIC flowsheet message is processed
     * Null fields should be updated, link through in_application_id should not be changed
     */

    /**
     * Given an EPIC visit observation type exists
     * When an older EPIC flowsheet message is processed
     * Null fields should be updated, link through in_application_id should still be null
     */

    /**
     * Given an EPIC visit observation type exists
     * When an older CLARITY/CABOODLE flowsheet message is processed
     * Null fields should be updated, link through in_application_id should still be set to link
     */

}
