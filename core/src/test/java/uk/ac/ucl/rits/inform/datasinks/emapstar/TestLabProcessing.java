package uk.ac.ucl.rits.inform.datasinks.emapstar;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.HospitalVisitRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LabBatteryTypeRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LabNumberRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LabOrderRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LabResultRepository;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.informdb.identity.MrnToLive;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.LabOrder;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

class TestLabProcessing extends MessageProcessingBase {
    private List<LabOrder> messages = messageFactory.getLabOrders("ORU_R01.yaml", "0000040");
    @Autowired
    private HospitalVisitRepository hospitalVisitRepository;
    @Autowired
    LabBatteryTypeRepository labBatteryTypeRepository;
    @Autowired
    LabNumberRepository labNumberRepository;
    @Autowired
    LabOrderRepository labOrderRepository;
    @Autowired
    LabResultRepository labResultRepository;



    /**
     * no existing data. rows should be created for: mrns, so new mrn, mrn_to_live, core_demographics, hospital visit
     */
    @Test
    void testCreateNew() throws EmapOperationMessageProcessingException {
        processSingleMessage(messages.get(0));

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
        // then lab results:
        // lab result
        Assertions.assertEquals(1, labResultRepository.count());
        Assertions.assertEquals(1, labNumberRepository.count());
        Assertions.assertEquals(1, labBatteryTypeRepository.count());
        Assertions.assertEquals(1, labOrderRepository.count());


        // -- for now skipping over, will also need to do the foreign keys for these when we get there
        // lab collection
        // LabResultSensitivity

    }

}
