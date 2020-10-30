package uk.ac.ucl.rits.inform.datasinks.emapstar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.AuditHospitalVisitRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.HospitalVisitRepository;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.informdb.identity.MrnToLive;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.VitalSigns;

public class TestVitalSignProcessing extends MessageProcessingBase {
    private List<VitalSigns> messages;
    private String vitalsMrn = "21014099";
    @Autowired
    private HospitalVisitRepository hospitalVisitRepository;
    @Autowired
    private AuditHospitalVisitRepository auditHospitalVisitRepository;


    @BeforeEach
    public void setup() {
        messages = messageFactory.getVitalSigns("hl7.yaml", "0000040");
    }

    /**
     * no existing mrns, so new mrn, mrn_to_live core_demographics rows should be created.
     */
    @Test
    public void testCreateNewPatient() throws EmapOperationMessageProcessingException {
        for (VitalSigns msg : messages) {
            msg.setVisitNumber(defaultEncounter);
            processSingleMessage(msg);
        }
        List<Mrn> mrns = getAllMrns();
        assertEquals(1, mrns.size());
        assertEquals("EPIC", mrns.get(0).getSourceSystem());

        MrnToLive mrnToLive = mrnToLiveRepo.getByMrnIdEquals(mrns.get(0));
        assertNotNull(mrnToLive);

        HospitalVisit visit = hospitalVisitRepository.findByEncounter(defaultEncounter).orElseThrow(NullPointerException::new);

        // then new results

    }


}
