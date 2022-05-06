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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TestLabMetadata extends MessageProcessingBase {
    @Autowired
    LabBatteryRepository labBatteryRepository;
    @Autowired
    LabBatteryElementRepository labBatteryElementRepository;
    @Autowired
    LabTestDefinitionRepository labTestDefinitionRepository;

    /*
     We don't get information about *which* tests are in which batteries from Clarity.
     We can only infer this from the test results we get.
     So Lab Metadata is going to be about fleshing out the battery and test data we do have,
     but the ordering of Metadata vs Orders may vary, so important to use getOrCreate idiom always,
     and update fields (with auditing?) when finished.
     */

    @Test
    public void testImpliedMetadata() throws IOException, EmapOperationMessageProcessingException {
        processLabOrderMessage();
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

        LabTestDefinition amlTest = labTestDefinitionRepository.findByTestLabCode("AML").get();
        assertEquals("WINPATH AMOXICILLIN", amlTest.getName());

        LabBattery gynaeBattery = labBatteryRepository.findByBatteryCodeAndLabProvider("GYNAE", "WIN_PATH").get();
        assertEquals("Gynaecological Smear", gynaeBattery.getBatteryName());
    }

    @Test
    public void testUpdatedTestMetadata() throws IOException, EmapOperationMessageProcessingException {
        processLabOrderMessage();

        // We've inferred the existence of a test called "ALP" already, but we don't have much metadata yet
        LabTestDefinition amlTestBefore = labTestDefinitionRepository.findByTestLabCode("ALP").get();
        assertNull(amlTestBefore.getName());

        processMessages(messageFactory.getLabMetadataMsgs("labs_metadata_update_existing_test.yaml"));

        // verify name has now been filled in
        LabTestDefinition amlTestAfter = labTestDefinitionRepository.findByTestLabCode("ALP").get();
        assertEquals("Alkaline phosphatase", amlTestAfter.getName());
    }

    @Test
    public void testUpdatedBatteryMetadata() throws IOException, EmapOperationMessageProcessingException {
        processLabOrderMessage();

        // We've inferred the existence of a battery called "BON" already, but we don't have much metadata yet
        LabBattery bonTestBefore = labBatteryRepository.findByBatteryCodeAndLabProvider("BON", "WIN_PATH").get();
        assertNull(bonTestBefore.getBatteryName());

        processMessages(messageFactory.getLabMetadataMsgs("labs_metadata_update_existing_battery.yaml"));

        // verify name has now been filled in
        LabBattery bonTestAfter = labBatteryRepository.findByBatteryCodeAndLabProvider("BON", "WIN_PATH").get();
        assertEquals("Bone Profile", bonTestAfter.getBatteryName());
    }

    private void processLabOrderMessage() throws IOException, EmapOperationMessageProcessingException {
        List<LabOrderMsg> messages = messageFactory.getLabOrders("winpath/ORU_R01.yaml", "0000040");
        LabOrderMsg fourResults = messages.get(0);
        processSingleMessage(fourResults);
    }

}
