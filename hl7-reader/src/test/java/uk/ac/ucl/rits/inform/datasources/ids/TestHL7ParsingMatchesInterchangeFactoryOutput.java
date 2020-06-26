package uk.ac.ucl.rits.inform.datasources.ids;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.junit.Before;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import uk.ac.ucl.rits.inform.interchange.AdtMessage;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessage;
import uk.ac.ucl.rits.inform.interchange.InterchangeMessageFactory;
import uk.ac.ucl.rits.inform.interchange.PathologyOrder;
import uk.ac.ucl.rits.inform.interchange.VitalSigns;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 */
@ActiveProfiles("test")
public class TestHL7ParsingMatchesInterchangeFactoryOutput extends TestHl7MessageStream {
    InterchangeMessageFactory interchangeFactory = new InterchangeMessageFactory();

    private void testAdtMessage(String adtFileStem) throws Exception {
        System.out.println("Testing ADT message with stem:" + adtFileStem);
        List<? extends EmapOperationMessage> messagesFromHl7Message = processSingleMessage("Adt/" + adtFileStem +".txt");
        AdtMessage expectedOrders = interchangeFactory.getAdtMessage(adtFileStem + ".yaml", "0000000042");
        assertEquals(1, messagesFromHl7Message.size());
        assertEquals(messagesFromHl7Message.get(0), expectedOrders);
    }

    @Test
    public void testGenericAdtA01() throws Exception {
        testAdtMessage("generic/A01");
        testAdtMessage("generic/A01_b");

    }

    @Test
    public void testGenericAdtA02() throws Exception {
        testAdtMessage("generic/A02");
    }

    @Test
    public void testGenericAdtA03() throws Exception {
        testAdtMessage("generic/A03");
        testAdtMessage("generic/A03_death");
        testAdtMessage("generic/A03_death_2");
        testAdtMessage("generic/A03_death_3");

    }

    @Test
    public void testGenericAdtA04() throws Exception {
        testAdtMessage("generic/A04");
    }

    @Test
    public void testGenericAdtA06() throws Exception {
        testAdtMessage("generic/A06");
    }

    @Test
    public void testGenericAdtA08() throws Exception {
        testAdtMessage("generic/A08_v1");
        testAdtMessage("generic/A08_v2");
    }

    @Test
    public void testGenericAdtA40() throws Exception {
        testAdtMessage("generic/A40");
    }


    @Test
    public void testPathologyIncrementalLoad() throws Exception {
        List<? extends EmapOperationMessage> messagesFromHl7Message = processMultiplePathologyOrderMessages("PathologyOrder/Incremental.txt");
        List<PathologyOrder> expectedOrders = interchangeFactory.getPathologyOrders("incremental.yaml", "0000000042");
        assertEquals(messagesFromHl7Message, expectedOrders);
    }

    @Test
    public void testPathologyIncrementalDuplicateResultSegment() throws Exception {
        List<? extends EmapOperationMessage> messagesFromHl7Message = processMultiplePathologyOrderMessages("PathologyOrder/PathologyDuplicateResultSegment.txt");
        List<PathologyOrder> expectedOrders = interchangeFactory.getPathologyOrders("incremental_duplicate_result_segment.yaml", "0000000042");
        for (int i = 0; i < expectedOrders.size(); i++) {
            assertEquals(messagesFromHl7Message.get(i), expectedOrders.get(i));

        }
        assertEquals(messagesFromHl7Message, expectedOrders);
    }

    @Test
    public void testPathologyOrder() throws Exception {
        List<? extends EmapOperationMessage> messagesFromHl7Message = processSingleMessage("PathologyOrder/ORU_R01.txt");
        List<PathologyOrder> expectedOrders = interchangeFactory.getPathologyOrders("ORU_R01.yaml", "0000000042");
        assertEquals(messagesFromHl7Message, expectedOrders);
    }

    @Test
    public void testPathologySensitivity() throws Exception {
        List<? extends EmapOperationMessage> messagesFromHl7Message = processSingleMessage("PathologyOrder/Sensitivity.txt");
        List<PathologyOrder> expectedOrders = interchangeFactory.getPathologyOrders("sensitivity.yaml", "0000000042");
        assertEquals(messagesFromHl7Message, expectedOrders);
    }


    @Test
    public void testVitalSigns() throws Exception {
        List<? extends EmapOperationMessage> messagesFromHl7Message = processSingleMessage("VitalSigns/MixedHL7Message.txt");
        List<VitalSigns> expectedOrders = interchangeFactory.getVitalSigns("hl7.yaml", "0000000042");
        assertEquals(messagesFromHl7Message, expectedOrders);
    }
}
