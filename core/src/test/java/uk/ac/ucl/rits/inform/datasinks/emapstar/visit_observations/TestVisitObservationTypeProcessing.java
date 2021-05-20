package uk.ac.ucl.rits.inform.datasinks.emapstar.visit_observations;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ucl.rits.inform.datasinks.emapstar.MessageProcessingBase;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.vist_observations.VisitObservationTypeAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.vist_observations.VisitObservationTypeRepository;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.visit_observations.FlowsheetMetadata;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestVisitObservationTypeProcessing extends MessageProcessingBase {
    List<FlowsheetMetadata> flowsheetMetadata;

    @Autowired
    VisitObservationTypeRepository visitObservationTypeRepository;
    @Autowired
    VisitObservationTypeAuditRepository visitObservationTypeAuditRepository;
    private static final String CABOODLE_APPLICATION = "caboodle";
    private static final String FLOWSHEET = "flowsheet";

    @BeforeEach
    void setup() throws IOException {
        flowsheetMetadata = messageFactory.getFlowsheetMetadata("flowsheet_metadata.yaml");
    }


    /**
     * Given no metadata types exist.
     * When a single metadata message is processed
     * Then a visit observation type should be created
     * @throws EmapOperationMessageProcessingException shouldn't happen
     */
    @Test
    void testEntityCreated() throws EmapOperationMessageProcessingException {
        processSingleMessage(flowsheetMetadata.get(0));
        assertEquals(1, visitObservationTypeRepository.count());
    }
}
