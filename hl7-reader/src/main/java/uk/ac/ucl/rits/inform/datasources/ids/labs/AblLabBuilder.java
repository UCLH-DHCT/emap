package uk.ac.ucl.rits.inform.datasources.ids.labs;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.v26.group.ORU_R30_OBSERVATION;
import ca.uhn.hl7v2.model.v26.message.ORU_R30;
import ca.uhn.hl7v2.model.v26.segment.NTE;
import ca.uhn.hl7v2.model.v26.segment.OBR;
import ca.uhn.hl7v2.model.v26.segment.OBX;
import uk.ac.ucl.rits.inform.datasources.ids.exceptions.Hl7InconsistencyException;
import uk.ac.ucl.rits.inform.datasources.ids.exceptions.Hl7MessageIgnoredException;
import uk.ac.ucl.rits.inform.datasources.ids.hl7parser.PatientInfoHl7;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.OrderCodingSystem;
import uk.ac.ucl.rits.inform.interchange.lab.LabOrderMsg;
import uk.ac.ucl.rits.inform.interchange.lab.LabResultMsg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Build ABL90 Flex Plus LabOrders.
 * @author Stef Piatek
 */
public final class AblLabBuilder extends LabOrderBuilder {

    /**
     * Construct builder from ABL ORU R30 message.
     * @param subMessageSourceId unique Id from the IDS
     * @param oruR30             ORU R30 message
     * @param codingSystem       coding system
     * @throws HL7Exception               if HAPI does
     * @throws Hl7MessageIgnoredException if it's a calibration or test message
     * @throws Hl7InconsistencyException  if hl7 message is incorrectly formed
     */
    private AblLabBuilder(String subMessageSourceId, ORU_R30 oruR30, OrderCodingSystem codingSystem)
            throws HL7Exception, Hl7InconsistencyException, Hl7MessageIgnoredException {
        super(new String[]{"No ORC"});
        PatientInfoHl7 patientHl7 = new PatientInfoHl7(oruR30.getMSH(), oruR30.getPID(), oruR30.getVISIT().getPV1());
        setSourceAndPatientIdentifiers(subMessageSourceId, patientHl7);
        setBatteryCodingSystem(codingSystem);

        OBR obr = oruR30.getOBR();
        // skip message if it is "Proficiency Testing"
        populateSpecimenTypeOrIgnoreMessage(obr);
        populateObrFields(obr);
        populateOrderInformation(obr);
        getMsg().setLabSpecimenNumber(obr.getObr3_FillerOrderNumber().getEi1_EntityIdentifier().getValueOrEmpty());

        List<ORU_R30_OBSERVATION> observations = oruR30.getOBSERVATIONAll();
        List<LabResultMsg> results = new ArrayList<>(observations.size());
        for (ORU_R30_OBSERVATION ob : observations) {
            OBX obx = ob.getOBX();
            List<NTE> notes = ob.getNTEAll();
            LabResultBuilder resultBuilder = new AblResultBuilder(obx, notes, getMsg().getSourceSystem());
            resultBuilder.constructMsg();
            results.add(resultBuilder.getMessage());
        }
        getMsg().setLabResultMsgs(results);
    }

    /**
     * Build order with results.
     * @param idsUnid      unique Id from the IDS
     * @param oruR30       hl7 message
     * @param codingSystem coding system to use.
     * @return interchange messages
     * @throws HL7Exception               if HAPI does
     * @throws Hl7InconsistencyException  if the HL7 message contains errors
     * @throws Hl7MessageIgnoredException if message is ignored
     */
    public static Collection<LabOrderMsg> build(String idsUnid, ORU_R30 oruR30, OrderCodingSystem codingSystem)
            throws Hl7MessageIgnoredException, Hl7InconsistencyException, HL7Exception {
        List<LabOrderMsg> orders = new ArrayList<>(1);
        LabOrderMsg labOrder = new AblLabBuilder(idsUnid, oruR30, codingSystem).getMsg();
        // only one observation per message
        orders.add(labOrder);
        return orders;
    }

    /**
     * Populate the sample type information for ABL 90 flex.
     * @param obr OBR segment
     * @throws Hl7MessageIgnoredException if testing/calibration reading
     */
    private void populateSpecimenTypeOrIgnoreMessage(OBR obr) throws Hl7MessageIgnoredException {
        String sampleType = obr.getObr15_SpecimenSource().getSps1_SpecimenSourceNameOrCode().getCwe1_Identifier().getValueOrEmpty();

        if ("Proficiency Testing".equals(sampleType)) {
            throw new Hl7MessageIgnoredException("Test/Calibration reading, skipping processing");
        }
        getMsg().setSpecimenType(InterchangeValue.buildFromHl7(sampleType));
    }
}
