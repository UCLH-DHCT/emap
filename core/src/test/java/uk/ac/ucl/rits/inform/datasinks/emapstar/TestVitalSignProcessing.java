package uk.ac.ucl.rits.inform.datasinks.emapstar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.informdb.identity.MrnToLive;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.VitalSigns;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TestVitalSignProcessing extends MessageProcessingBase {
    List<VitalSigns> messages;
    String vitalsMrn = "21014099";


    @BeforeEach
    public void setup() {
        messages = messageFactory.getVitalSigns("hl7.yaml", "0000040");
    }

    /**
     * no existing mrns, so new mrn, mrn_to_live core_demographics rows should be created
     */
    @Test
    public void testCreateNewPatient() throws EmapOperationMessageProcessingException {
        for (VitalSigns msg : messages) {
            processSingleMessage(false, msg);
        }
        List<Mrn> mrns = getAllMrns();
        assertEquals(1, mrns.size());
        assertEquals("EPIC", mrns.get(0).getSourceSystem());

        MrnToLive mrnToLive = mrnToLiveRepo.getByMrnIdEquals(mrns.get(0));
        assertNotNull(mrnToLive);
        // then new encounter and results...
    }


}
