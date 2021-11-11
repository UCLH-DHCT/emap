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

        // flowsheet meta clarity (has the mapping information)
        // flowsheet meta caboodle (all of the naming data)
        // flowsheet clarity -- flowsheet that needs visit observation type linked
        // flowsheet EPIC -- flowsheet that needs visit observation type linked
    }


    /**
     * Given no visit observation type exist.
     * When an EPIC flowsheet message arrives
     * Then a visit observation type is created with only interface_id, but no id_in_application
     */


    /**
     * Given no visit observation type exist.
     * When a caboodle flowsheet metadata message arrives
     * Then a visit observation type is created with only id_in_application, but no interface_id
     */


    /**
     * Given no visit observation type exist.
     * When a clarity flowsheet metadata message arrives
     * Then a visit observation type is created with both id_in_application and interface_id
     */


    /**
     * Given a visit observation type with id_in_application but no interface_id exists
     * When a clarity flowsheet metadata message arrives
     * Then another visit observation type is created with id_in_application but no interface_id
     */


    /**
     * Given a visit observation type with interface_id but no id_in_application exists
     * When an EPIC flowsheet message arrives
     * Then the existing visit observation type is not changed
     */


    /**
     * Given a visit observation type with id_in_application but no interface_id exists
     * When a clarity flowsheet metadata message arrives
     * Then the missing id_in_application for visit observation type is filled in
     */

    /**
     * Given a visit observation type with id_in_application, a second visit observation type with interface_id,
     *   a visit observation referring to the first visit observation type and another visit observation referring to the second visit observation type
     * When a clarity flowsheet metadata message is processed that links id_in_application with interface_id
     * Then the visit observation type with id_in_application is updated to hold the interface_id from second visit
     *   observation type and the id is updated for the second visit observation referring to second visit observation type
     */

    /**
     * Given a visit observation type with both id_in_application and interface_id
     * When a newer caboodle flowsheet metadata message arrives with different names
     * Then the names are updated accordingly but ids are kept as they are
     */

    /**
     * Given a visit observation type with both id_in_application and interface_id
     * When an older caboodle flowsheet metadata message arrives with different names
     * Then the names are not updated and ids are kept as they are
     */



}
