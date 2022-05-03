package uk.ac.ucl.rits.inform.datasinks.emapstar.labs;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ucl.rits.inform.datasinks.emapstar.MessageProcessingBase;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabBatteryElementRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabBatteryRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabTestDefinitionRepository;
import uk.ac.ucl.rits.inform.informdb.labs.LabBattery;
import uk.ac.ucl.rits.inform.informdb.labs.LabTestDefinition;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.lab.LabMetadataMsg;
import uk.ac.ucl.rits.inform.interchange.lab.LabOrderMsg;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class TestLabMetadata extends MessageProcessingBase {
    @Autowired
    LabBatteryRepository labBatteryRepository;
    @Autowired
    LabBatteryElementRepository labBatteryElementRepository;
    @Autowired
    LabTestDefinitionRepository labTestDefinitionRepository;

    /*
     I don't think we get information about *which* tests are in which batteries from Clarity.
     Are we left to infer this from the test results we get?
     So Lab Metadata is going to be about fleshing out the battery and test data we do have,
     but the ordering of Metadata vs Orders may vary, so important to use getOrCreate idiom always,
     and update fields (with auditing?) when finished.
     (What is in these tables presently? Have a look when DSD is fixed.)
     */

    @Test
    public void testImpliedMetadata() throws IOException, EmapOperationMessageProcessingException {
        List<LabOrderMsg> messages = messageFactory.getLabOrders("winpath/ORU_R01.yaml", "0000040");
        LabOrderMsg fourResults = messages.get(0);
        processSingleMessage(fourResults);
        assertEquals(2, labBatteryRepository.count());
        assertEquals(4, labBatteryElementRepository.count());
        assertEquals(4, labTestDefinitionRepository.count());

        List<LabMetadataMsg> labMetadataMsgs = messageFactory.getLabMetadataMsgs("labs_metadata.yaml");
        for (var m : labMetadataMsgs) {
            processSingleMessage(m);
        }

        assertEquals(2 + 5, labBatteryRepository.count());

        // unchanged
        assertEquals(4, labBatteryElementRepository.count());

        assertEquals(4 + 5, labTestDefinitionRepository.count());
        LabTestDefinition aml = labTestDefinitionRepository.findByTestLabCode("AML").get();
//        aml.getTestStandardCode();
        LabBattery battery = labBatteryRepository.findByBatteryCodeAndLabProvider("GYNAE", "WIN_PATH").get();
        assertEquals("Gynaecological Smear", battery.getDescription());
    }
}
