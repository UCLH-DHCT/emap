package uk.ac.ucl.rits.inform.datasinks.emapstar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.informdb.identity.MrnToLive;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.PathologyOrder;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TestPathologyProcessing extends MessageProcessingBase {
    List<PathologyOrder> messages;


    @BeforeEach
    public void setup() {
        messages = messageFactory.getPathologyOrders("ORU_R01.yaml", "0000040");
    }

    /**
     * no existing mrns, so new mrn, mrn_to_live core_demographics rows should be created
     */
    @Test
    public void testCreateNewPatient() throws EmapOperationMessageProcessingException {
        for (PathologyOrder msg : messages) {
            processSingleMessage(msg);
        }
        List<Mrn> mrns = getAllMrns();
        assertEquals(1, mrns.size());
        assertEquals("Corepoint", mrns.get(0).getSourceSystem());

        MrnToLive mrnToLive = mrnToLiveRepo.getByMrnIdEquals(mrns.get(0));
        assertNotNull(mrnToLive);
        // then new encounter and results...
    }


}
