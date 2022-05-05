package uk.ac.ucl.rits.inform.datasinks.emapstar.adt;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ucl.rits.inform.datasinks.emapstar.MessageProcessingBase;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.CoreDemographicRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.HospitalVisitRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.MrnRepository;
import uk.ac.ucl.rits.inform.interchange.adt.PendingTransfer;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestPendingAdt extends MessageProcessingBase {
    @Autowired
    private MrnRepository mrnRepository;
    @Autowired
    private CoreDemographicRepository coreDemographicRepository;
    @Autowired
    private HospitalVisitRepository hospitalVisitRepository;


    /**
     * Given that no entities exist in the database
     * When a pending transfer message is processed
     * Mrn, core demographics and hospital visit entities should be created
     * @throws Exception shouldn't happen
     */
    @Test
    void testPendingCreatesOtherEntities() throws Exception {
        PendingTransfer msg = messageFactory.getAdtMessage("pending/A15.yaml");
        dbOps.processMessage(msg);

        assertEquals(1, mrnRepository.count());
        assertEquals(1, coreDemographicRepository.count());
        assertEquals(1, hospitalVisitRepository.count());
    }



}

