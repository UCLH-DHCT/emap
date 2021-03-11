package uk.ac.ucl.rits.inform.datasources.ids.labs;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.v26.group.ORM_O01_ORDER;
import ca.uhn.hl7v2.model.v26.group.ORM_O01_PATIENT;
import ca.uhn.hl7v2.model.v26.group.ORR_O02_ORDER;
import ca.uhn.hl7v2.model.v26.group.ORR_O02_PATIENT;
import ca.uhn.hl7v2.model.v26.group.ORU_R01_OBSERVATION;
import ca.uhn.hl7v2.model.v26.group.ORU_R01_ORDER_OBSERVATION;
import ca.uhn.hl7v2.model.v26.group.ORU_R01_PATIENT_RESULT;
import ca.uhn.hl7v2.model.v26.message.ORM_O01;
import ca.uhn.hl7v2.model.v26.message.ORR_O02;
import ca.uhn.hl7v2.model.v26.message.ORU_R01;
import ca.uhn.hl7v2.model.v26.segment.MSH;
import ca.uhn.hl7v2.model.v26.segment.NTE;
import ca.uhn.hl7v2.model.v26.segment.OBR;
import ca.uhn.hl7v2.model.v26.segment.OBX;
import ca.uhn.hl7v2.model.v26.segment.ORC;
import ca.uhn.hl7v2.model.v26.segment.PID;
import ca.uhn.hl7v2.model.v26.segment.PV1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ucl.rits.inform.datasources.ids.exceptions.Hl7InconsistencyException;
import uk.ac.ucl.rits.inform.datasources.ids.exceptions.Hl7MessageIgnoredException;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.OrderCodingSystem;
import uk.ac.ucl.rits.inform.interchange.lab.LabOrderMsg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Build CoPath LabOrders.
 * @author Stef Piatek
 */
public final class CoPathLabBuilder extends LabOrderBuilder {
    /**
     * Allowed order control IDs for parsing.
     * <p>
     * ORM O01: NW (New sample and order), SC (status change), SN (send new order for existing sample), CA (cancel order), OC (order cancelled)
     * ORU R01: RE (results)
     * ORR R02: NA (response to SN), CR (response to CA)
     */
    private static final Collection<String> CANCEL_OC_IDS = new HashSet<>(Arrays.asList("CA", "CR", "OC"));
    private static final String[] ALLOWED_OC_IDS = {"RE", "NW", "SC", "SN", "NA", "CA", "CR", "OC"};
    private static final Logger logger = LoggerFactory.getLogger(CoPathLabBuilder.class);


    /**
     * Build a lab order structure from a lab order (no results).
     * @param subMessageSourceId unique Id from the IDS
     * @param msh                MSH segment
     * @param pid                PID segment
     * @param pv1                PV1 segment
     * @param obr                OBR segment
     * @param orc                ORC segment
     * @param notes
     * @param codingSystem       coding system
     * @throws HL7Exception              if HAPI does
     * @throws Hl7InconsistencyException if something about the HL7 message doesn't make sense
     */
    private CoPathLabBuilder(String subMessageSourceId, MSH msh, PID pid, PV1 pv1, OBR obr, ORC orc, List<NTE> notes, OrderCodingSystem codingSystem)
            throws HL7Exception, Hl7InconsistencyException {
        super(ALLOWED_OC_IDS);
        setBatteryCodingSystem(codingSystem);
        setSourceAndPatientIdentifiers(subMessageSourceId, msh, pid, pv1);
        populateObrFields(obr);
        populateOrderInformation(orc, obr);
        setEpicOrderNumberFromORC();
    }

    private void setEpicOrderNumberFromORC() {
        getMsg().setEpicCareOrderNumber(InterchangeValue.buildFromHl7(getEpicCareOrderNumberOrc()));
    }


    /**
     * Construct order details from a CoPath results (ORU) message. Most/all of the details of the order are contained in the
     * results message - at least the order numbers are present so we can look up the order
     * which we should already know about from a preceding ORM message.
     * @param subMessageSourceId unique Id from the IDS
     * @param obs                the result group from HAPI (ORU_R01_ORDER_OBSERVATION)
     * @param msh                the MSH segment
     * @param pid                the PID segment
     * @param pv1                the PV1 segment
     * @param codingSystem       order coding system
     * @throws HL7Exception              if HAPI does
     * @throws Hl7InconsistencyException if, according to my understanding, the HL7 message contains errors
     */
    private CoPathLabBuilder(String subMessageSourceId, ORU_R01_ORDER_OBSERVATION obs, MSH msh, PID pid, PV1 pv1, OrderCodingSystem codingSystem)
            throws HL7Exception, Hl7InconsistencyException {
        super(ALLOWED_OC_IDS);
        setBatteryCodingSystem(codingSystem);
        setSourceAndPatientIdentifiers(subMessageSourceId, msh, pid, pv1);
        OBR obr = obs.getOBR();
        populateObrFields(obr);
        populateOrderInformation(obs.getORC(), obr);
        setEpicOrderNumberFromORC();

        List<CoPathResultBuilder> tempResults = new ArrayList<>(obs.getOBSERVATIONAll().size());
        List<ORU_R01_OBSERVATION> observationAll = obs.getOBSERVATIONAll();
        for (ORU_R01_OBSERVATION ob : observationAll) {
            OBX obx = ob.getOBX();
            List<NTE> notes = ob.getNTEAll();
            CoPathResultBuilder labResult = new CoPathResultBuilder(obx, obr, notes);
            labResult.constructMsg();
            tempResults.add(labResult);
        }
        getMsg().setLabResultMsgs(tempResults.stream().map(LabResultBuilder::getMessage).collect(Collectors.toList()));
    }


    /**
     * Build order from ORM O01.
     * @param idsUnid      unique Id from the IDS
     * @param ormO01       message
     * @param codingSystem coding system
     * @return interchange messages
     * @throws HL7Exception              if HAPI does
     * @throws Hl7InconsistencyException if the HL7 message contains errors
     */
    public static List<LabOrderMsg> build(String idsUnid, ORM_O01 ormO01, OrderCodingSystem codingSystem)
            throws HL7Exception, Hl7InconsistencyException {
        List<ORM_O01_ORDER> hl7Orders = ormO01.getORDERAll();
        MSH msh = ormO01.getMSH();
        ORM_O01_PATIENT patient = ormO01.getPATIENT();
        PID pid = patient.getPID();
        PV1 pv1 = patient.getPATIENT_VISIT().getPV1();


        List<LabOrderMsg> interchangeOrders = new ArrayList<>(hl7Orders.size());
        int msgSuffix = 0;
        for (ORM_O01_ORDER order : hl7Orders) {
            msgSuffix++;
            String subMessageSourceId = String.format("%s_%02d", idsUnid, msgSuffix);
            ORC orc = order.getORC();
            OBR obr = order.getORDER_DETAIL().getOBR();
            List<NTE> notes = order.getORDER_DETAIL().getNTEAll();
            LabOrderBuilder labOrderBuilder = new CoPathLabBuilder(subMessageSourceId, msh, pid, pv1, obr, orc, notes, codingSystem);
            labOrderBuilder.addMsgIfAllowedOcId(interchangeOrders);
        }
        return interchangeOrders;
    }

    /**
     * Build lab order messages from ORR O02.
     * @param idsUnid      unique Id from the IDS
     * @param msg          hl7 message
     * @param codingSystem coding system
     * @return interchange messages
     * @throws HL7Exception              if HAPI does
     * @throws Hl7InconsistencyException if the HL7 message contains errors
     */
    public static Collection<LabOrderMsg> build(String idsUnid, ORR_O02 msg, OrderCodingSystem codingSystem)
            throws HL7Exception, Hl7InconsistencyException {
        List<ORR_O02_ORDER> hl7Orders = msg.getRESPONSE().getORDERAll();
        MSH msh = msg.getMSH();
        ORR_O02_PATIENT patient = msg.getRESPONSE().getPATIENT();
        PID pid = patient.getPID();
        PV1 emptyPV1 = new PV1(msg.getParent(), null);

        List<LabOrderMsg> interchangeOrders = new ArrayList<>(hl7Orders.size());
        int msgSuffix = 0;
        for (ORR_O02_ORDER order : hl7Orders) {
            msgSuffix++;
            String subMessageSourceId = String.format("%s_%02d", idsUnid, msgSuffix);
            ORC orc = order.getORC();
            OBR obr = order.getOBR();
            List<NTE> notes = order.getNTEAll();
            LabOrderBuilder labOrderBuilder = new CoPathLabBuilder(subMessageSourceId, msh, pid, emptyPV1, obr, orc, notes, codingSystem);
            labOrderBuilder.addMsgIfAllowedOcId(interchangeOrders);
        }
        return interchangeOrders;
    }

    /**
     * Build order with results from ORU R01.
     * @param idsUnid      unique Id from the IDS
     * @param oruR01       hl7 message
     * @param codingSystem coding system to use.
     * @return interchange messages
     * @throws HL7Exception               if HAPI does
     * @throws Hl7InconsistencyException  if the HL7 message contains errors
     * @throws Hl7MessageIgnoredException if message is ignored
     */
    public static Collection<LabOrderMsg> build(String idsUnid, ORU_R01 oruR01, OrderCodingSystem codingSystem)
            throws HL7Exception, Hl7InconsistencyException, Hl7MessageIgnoredException {
        if (oruR01.getPATIENT_RESULTReps() != 1) {
            throw new Hl7MessageIgnoredException("Not expecting CoPath to have multiple patient results in one message");
        }
        ORU_R01_PATIENT_RESULT patientResults = oruR01.getPATIENT_RESULT();
        List<ORU_R01_ORDER_OBSERVATION> orderObservations = patientResults.getORDER_OBSERVATIONAll();
        MSH msh = (MSH) oruR01.get("MSH");
        PID pid = patientResults.getPATIENT().getPID();
        PV1 pv1 = patientResults.getPATIENT().getVISIT().getPV1();

        List<LabOrderMsg> orders = new ArrayList<>(orderObservations.size());
        int msgSuffix = 0;
        for (ORU_R01_ORDER_OBSERVATION obs : orderObservations) {
            msgSuffix++;
            String subMessageSourceId = String.format("%s_%02d", idsUnid, msgSuffix);
            LabOrderBuilder labOrderBuilder = new CoPathLabBuilder(subMessageSourceId, obs, msh, pid, pv1, codingSystem);
            labOrderBuilder.addMsgIfAllowedOcId(orders);
        }
        return orders;
    }


}

