package uk.ac.ucl.rits.inform.datasinks.emapstar;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.HospitalVisitAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.HospitalVisitRepository;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.informdb.identity.MrnToLive;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.VitalSigns;

import java.util.List;

class TestVitalSignProcessing extends MessageProcessingBase {
    private List<VitalSigns> messages;
    private String vitalsMrn = "21014099";
    @Autowired
    private HospitalVisitRepository hospitalVisitRepository;
    @Autowired
    private HospitalVisitAuditRepository hospitalVisitAuditRepository;


    @BeforeEach
    void setup() {
        messages = messageFactory.getVitalSigns("hl7.yaml", "0000040");
    }

    /**
     * no existing mrns, so new mrn, mrn_to_live core_demographics rows should be created.
     */
    @Test
    void testCreateNewPatient() throws EmapOperationMessageProcessingException {
        for (VitalSigns msg : messages) {
            msg.setVisitNumber(defaultEncounter);
            processSingleMessage(msg);
        }
        List<Mrn> mrns = getAllMrns();
        Assertions.assertEquals(1, mrns.size());
        Assertions.assertEquals("EPIC", mrns.get(0).getSourceSystem());

        MrnToLive mrnToLive = mrnToLiveRepo.getByMrnIdEquals(mrns.get(0));
        Assertions.assertNotNull(mrnToLive);

        HospitalVisit visit = hospitalVisitRepository.findByEncounter(defaultEncounter).orElseThrow(NullPointerException::new);

        // then new results

    }


}
