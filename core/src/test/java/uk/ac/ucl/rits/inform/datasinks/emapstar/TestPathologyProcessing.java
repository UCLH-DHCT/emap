package uk.ac.ucl.rits.inform.datasinks.emapstar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.HospitalVisitAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.HospitalVisitRepository;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.informdb.identity.MrnToLive;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.PathologyOrder;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

class TestPathologyProcessing extends MessageProcessingBase {
    private List<PathologyOrder> messages;
    @Autowired
    private HospitalVisitRepository hospitalVisitRepository;
    @Autowired
    private HospitalVisitAuditRepository hospitalVisitAuditRepository;


    @BeforeEach
    void setup() {
        messages = messageFactory.getPathologyOrders("ORU_R01.yaml", "0000040");
    }

    /**
     * no existing data. rows should be created for: mrns, so new mrn, mrn_to_live, core_demographics, hospital visit
     */
    @Test
    void testCreateNewPatient() throws EmapOperationMessageProcessingException {
        for (PathologyOrder msg : messages) {
            processSingleMessage(msg);
        }
        List<Mrn> mrns = getAllMrns();
        assertEquals(1, mrns.size());
        assertEquals("Corepoint", mrns.get(0).getSourceSystem());

        MrnToLive mrnToLive = mrnToLiveRepo.getByMrnIdEquals(mrns.get(0));
        assertNotNull(mrnToLive);

        HospitalVisit visit = hospitalVisitRepository.findByEncounter(defaultEncounter).orElseThrow(NullPointerException::new);
        assertEquals("Corepoint", visit.getSourceSystem());
        assertNull(visit.getPatientClass());
        assertNull(visit.getArrivalMethod());
        assertNull(visit.getAdmissionTime());
        // then new results

    }


}
