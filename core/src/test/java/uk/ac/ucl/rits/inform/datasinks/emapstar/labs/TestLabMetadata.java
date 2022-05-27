package uk.ac.ucl.rits.inform.datasinks.emapstar.labs;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ucl.rits.inform.datasinks.emapstar.MessageProcessingBase;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabBatteryAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabBatteryElementRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabBatteryRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabTestDefinitionAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabTestDefinitionRepository;
import uk.ac.ucl.rits.inform.informdb.labs.LabBattery;
import uk.ac.ucl.rits.inform.informdb.labs.LabBatteryAudit;
import uk.ac.ucl.rits.inform.informdb.labs.LabTestDefinition;
import uk.ac.ucl.rits.inform.informdb.labs.LabTestDefinitionAudit;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.lab.LabMetadataMsg;
import uk.ac.ucl.rits.inform.interchange.lab.LabOrderMsg;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TestLabMetadata extends MessageProcessingBase {
    @Autowired
    LabBatteryRepository labBatteryRepository;
    @Autowired
    LabBatteryAuditRepository labBatteryAuditRepository;
    @Autowired
    LabBatteryElementRepository labBatteryElementRepository;
    @Autowired
    LabTestDefinitionRepository labTestDefinitionRepository;
    @Autowired
    LabTestDefinitionAuditRepository labTestDefinitionAuditRepository;

    /*
     We don't get information about *which* tests are in which batteries from Clarity.
     We can only infer this from the test results we get.
     So Lab Metadata is going to be about fleshing out the battery and test data we do have,
     but the ordering of Metadata vs Orders may vary, so important to use getOrCreate idiom always,
     and update fields (with auditing?) when finished.
     */

    @Test
    public void testImpliedMetadata() throws Exception {
        processLabOrderMessage();
        assertEquals(2, labBatteryRepository.count());
        assertEquals(4, labBatteryElementRepository.count());
        assertEquals(4, labTestDefinitionRepository.count());

        assertEquals(0, labBatteryAuditRepository.count());
        assertEquals(0, labTestDefinitionAuditRepository.count());

        List<LabMetadataMsg> labMetadataMsgs = messageFactory.getLabMetadataMsgs("labs_metadata.yaml");
        for (var m : labMetadataMsgs) {
            processSingleMessage(m);
        }

        assertEquals(2 + 6, labBatteryRepository.count());

        // unchanged
        assertEquals(4, labBatteryElementRepository.count());
        assertEquals(4 + 6, labTestDefinitionRepository.count());

        LabTestDefinition amlTest = labTestDefinitionRepository.findByTestLabCode("AML").orElseThrow();
        assertEquals("WINPATH AMOXICILLIN", amlTest.getName());

        LabBattery gynaeBattery = labBatteryRepository.findByBatteryCodeAndLabProvider("GYNAE", "WIN_PATH").orElseThrow();
        assertEquals("Gynaecological Smear", gynaeBattery.getBatteryName());
    }

    @Test
    public void testUpdatedTestMetadata() throws Exception {
        processLabOrderMessage();
        assertEquals(0, labBatteryAuditRepository.count());
        assertEquals(0, labTestDefinitionAuditRepository.count());

        // We've inferred the existence of a test called "ALP" already, but we don't have much metadata yet
        LabTestDefinition amlTestBefore = labTestDefinitionRepository.findByTestLabCode("ALP").orElseThrow();
        assertNull(amlTestBefore.getName());
        long deletedLabTestDefinitionId = amlTestBefore.getLabTestDefinitionId();

        processMessages(messageFactory.getLabMetadataMsgs("labs_metadata_update_existing_test.yaml"));

        // verify name has now been filled in
        LabTestDefinition amlTestAfter = labTestDefinitionRepository.findByTestLabCode("ALP").orElseThrow();
        Instant changeDatetime = Instant.parse("2019-09-13T16:00:00Z");
        assertEquals(changeDatetime, amlTestAfter.getValidFrom());
        assertEquals("Alkaline phosphatase", amlTestAfter.getName());
        ArrayList<LabTestDefinitionAudit> allAuditRows = new ArrayList<>();
        labTestDefinitionAuditRepository.findAll().iterator().forEachRemaining(allAuditRows::add);

        assertEquals(1, allAuditRows.size());
        LabTestDefinitionAudit labTestDefinitionAudit = allAuditRows.get(0);
        assertEquals(deletedLabTestDefinitionId, labTestDefinitionAudit.getLabTestDefinitionId());
        assertNotNull(labTestDefinitionAudit.getStoredUntil());
        assertEquals(changeDatetime, labTestDefinitionAudit.getValidUntil());
    }

    @Test
    public void testUpdatedBatteryMetadata() throws Exception {
        processLabOrderMessage();
        assertEquals(0, labBatteryAuditRepository.count());
        assertEquals(0, labTestDefinitionAuditRepository.count());

        // We've inferred the existence of a battery called "BON" already, but we don't have much metadata yet
        LabBattery bonTestBefore = labBatteryRepository.findByBatteryCodeAndLabProvider("BON", "WIN_PATH").orElseThrow();
        assertNull(bonTestBefore.getBatteryName());
        long deletedlabBatteryId = bonTestBefore.getLabBatteryId();

        processMessages(messageFactory.getLabMetadataMsgs("labs_metadata_update_existing_battery.yaml"));

        // verify name has now been filled in
        LabBattery bonTestAfter = labBatteryRepository.findByBatteryCodeAndLabProvider("BON", "WIN_PATH").orElseThrow();
        Instant changeDatetime = Instant.parse("2019-04-05T09:23:16Z");
        assertEquals(changeDatetime, bonTestAfter.getValidFrom());
        assertEquals("Bone Profile", bonTestAfter.getBatteryName());
        ArrayList<LabBatteryAudit> allAuditRows = new ArrayList<>();
        labBatteryAuditRepository.findAll().iterator().forEachRemaining(allAuditRows::add);
        assertEquals(1, allAuditRows.size());
        LabBatteryAudit labBatteryAudit = allAuditRows.get(0);
        assertEquals(deletedlabBatteryId, labBatteryAudit.getLabBatteryId());
        assertNotNull(labBatteryAudit.getStoredUntil());
        assertEquals(changeDatetime, labBatteryAudit.getValidUntil());
    }

    private void processLabOrderMessage() throws IOException, EmapOperationMessageProcessingException {
        List<LabOrderMsg> messages = messageFactory.getLabOrders("winpath/ORU_R01.yaml", "0000040");
        LabOrderMsg fourResults = messages.get(0);
        processSingleMessage(fourResults);
    }

}
