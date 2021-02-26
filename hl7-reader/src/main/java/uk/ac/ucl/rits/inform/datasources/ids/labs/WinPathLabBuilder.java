package uk.ac.ucl.rits.inform.datasources.ids.labs;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.v26.group.ORM_O01_ORDER;
import ca.uhn.hl7v2.model.v26.group.ORM_O01_PATIENT;
import ca.uhn.hl7v2.model.v26.group.ORU_R01_OBSERVATION;
import ca.uhn.hl7v2.model.v26.group.ORU_R01_ORDER_OBSERVATION;
import ca.uhn.hl7v2.model.v26.group.ORU_R01_PATIENT_RESULT;
import ca.uhn.hl7v2.model.v26.message.ORM_O01;
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
import uk.ac.ucl.rits.inform.interchange.OrderCodingSystem;
import uk.ac.ucl.rits.inform.interchange.lab.LabIsolateMsg;
import uk.ac.ucl.rits.inform.interchange.lab.LabOrderMsg;
import uk.ac.ucl.rits.inform.interchange.lab.LabResultMsg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * Build WinPath LabOrders.
 * @author Jeremy Stein
 * @author Stef Piatek
 */
public final class WinPathLabBuilder extends LabOrderBuilder {
    private static final Collection<String> ALLOWED_OCIDS = new HashSet<>(Arrays.asList("SC", "RE")); // other winpath: CA, SN, NW, NA, CR, OC
    private static final Logger logger = LoggerFactory.getLogger(WinPathLabBuilder.class);


    /**
     * Build a lab order structure from a lab order (ORM) message.
     * @param subMessageSourceId unique Id from the IDS
     * @param order              one of the order groups in the message that is to be converted into an order structure
     * @param ormO01             the ORM^O01 message (can contain multiple orders) for extracting data common to the whole message
     * @param codingSystem       coding system
     * @throws HL7Exception              if HAPI does
     * @throws Hl7InconsistencyException if something about the HL7 message doesn't make sense
     */
    private WinPathLabBuilder(String subMessageSourceId, ORM_O01_ORDER order, ORM_O01 ormO01, OrderCodingSystem codingSystem)
            throws HL7Exception, Hl7InconsistencyException {
        setBatteryCodingSystem(codingSystem);

        MSH msh = (MSH) ormO01.get("MSH");
        ORM_O01_PATIENT patient = ormO01.getPATIENT();
        PID pid = patient.getPID();
        PV1 pv1 = patient.getPATIENT_VISIT().getPV1();
        setSourceAndPatientIdentifiers(subMessageSourceId, msh, pid, pv1);
        ORC orc = order.getORC();
        OBR obr = order.getORDER_DETAIL().getOBR();
        populateObrFields(obr);
        populateOrderInformation(orc, obr);
        validateAndSetEpicOrderNumber();
    }


    /**
     * Construct order details from a WinPath results (ORU) message. Most/all of the details of the order are contained in the
     * results message - at least the order numbers are present so we can look up the order
     * which we should already know about from a preceding ORM message.
     * <p>
     * ORU_R01_PATIENT_RESULT repeating
     * ------ORU_R01_PATIENT optional
     * --------- PID (Patient Identification)
     * --------- PRT (Participation Information) optional repeating
     * --------- PD1 (Patient Additional Demographic) optional
     * --------- NTE (Notes and Comments) optional repeating
     * --------- NK1 (Next of Kin / Associated Parties) optional repeating
     * --------- ORU_R01_PATIENT_OBSERVATION (a Group object) optional repeating
     * ------------- OBX (Observation/Result)
     * ------------- PRT (Participation Information) optional repeating
     * --------- ORU_R01_VISIT (a Group object) optional
     * ------------- PV1 (Patient Visit)
     * ------------- PV2 (Patient Visit - Additional Information) optional
     * ------------- PRT (Participation Information) optional repeating
     * ------ORU_R01_ORDER_OBSERVATION repeating
     * --------- ORC (Common Order) optional
     * --------- OBR (Observation Request)
     * --------- NTE (Notes and Comments) optional repeating
     * --------- PRT (Participation Information) optional repeating
     * --------- ORU_R01_TIMING_QTY (a Group object) optional repeating
     * --------- CTD (Contact Data) optional
     * --------- ORU_R01_OBSERVATION (a Group object) optional repeating
     * ------------- OBX (Observation/Result)
     * ------------- PRT (Participation Information) optional repeating
     * ------------- NTE (Notes and Comments) optional repeating
     * --------- FT1 (Financial Transaction) optional repeating
     * --------- CTI (Clinical Trial Identification) optional repeating
     * --------- ORU_R01_SPECIMEN (a Group object) optional repeating
     * ------------- SPM (Specimen)
     * ------------- ORU_R01_SPECIMEN_OBSERVATION (a Group object) optional repeating
     * ------------------OBX (Observation/Result)
     * ------------------PRT (Participation Information) optional repeating
     * @param subMessageSourceId unique Id from the IDS
     * @param obs                the result group from HAPI (ORU_R01_ORDER_OBSERVATION)
     * @param msh                the MSH segment
     * @param pid                the PID segment
     * @param pv1                the PV1 segment
     * @param codingSystem       order coding system
     * @throws HL7Exception              if HAPI does
     * @throws Hl7InconsistencyException if, according to my understanding, the HL7 message contains errors
     */
    private WinPathLabBuilder(String subMessageSourceId, ORU_R01_ORDER_OBSERVATION obs, MSH msh, PID pid, PV1 pv1, OrderCodingSystem codingSystem)
            throws HL7Exception, Hl7InconsistencyException {
        setBatteryCodingSystem(codingSystem);
        setSourceAndPatientIdentifiers(subMessageSourceId, msh, pid, pv1);
        OBR obr = obs.getOBR();
        populateObrFields(obr);
        populateOrderInformation(obs.getORC(), obr);
        validateAndSetEpicOrderNumber();

        List<WinPathResultBuilder> tempResults = new ArrayList<>(obs.getOBSERVATIONAll().size());
        List<ORU_R01_OBSERVATION> observationAll = obs.getOBSERVATIONAll();
        for (ORU_R01_OBSERVATION ob : observationAll) {
            OBX obx = ob.getOBX();
            List<NTE> notes = ob.getNTEAll();
            WinPathResultBuilder labResult = new WinPathResultBuilder(obx, obr, notes);
            labResult.constructMsg();
            tempResults.add(labResult);
        }
        // merge isolate results
        mergeOrFilterResults(tempResults);
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

        List<LabOrderMsg> interchangeOrders = new ArrayList<>(hl7Orders.size());
        int msgSuffix = 0;
        for (ORM_O01_ORDER order : hl7Orders) {
            msgSuffix++;
            String subMessageSourceId = String.format("%s_%02d", idsUnid, msgSuffix);
            LabOrderMsg labOrder;
            labOrder = new WinPathLabBuilder(subMessageSourceId, order, ormO01, codingSystem).getMsg();
            if (ALLOWED_OCIDS.contains(labOrder.getOrderControlId())) {
                interchangeOrders.add(labOrder);
            } else {
                logger.trace("Ignoring order control ID ='{}'", labOrder.getOrderControlId());
            }
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
            throw new Hl7MessageIgnoredException("Not expecting WinPath to have multiple patient results in one message");
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
            LabOrderMsg labOrder = new WinPathLabBuilder(subMessageSourceId, obs, msh, pid, pv1, codingSystem).getMsg();
            if (ALLOWED_OCIDS.contains(labOrder.getOrderControlId())) {
                orders.add(labOrder);
            } else {
                logger.trace("Ignoring order control ID = '{}'", labOrder.getOrderControlId());
            }
        }
        mergeSensitivitiesIntoIsolate(orders);
        return orders;
    }

    /**
     * Use the HL7 fields to re-parent the sensitivities in this list so they are added to their parent isolate.
     * Parents and children must all be in the list supplied.
     * @param orders the list of all orders from a HL7 message. This list will have items modified and/or deleted.
     * @throws Hl7InconsistencyException if no parent found for a message which has a subId.
     */
    private static void mergeSensitivitiesIntoIsolate(List<LabOrderMsg> orders) throws Hl7InconsistencyException {
        // we may have multiple orders that are unrelated to each other (apart from being for the same patient).
        ListIterator<LabOrderMsg> iter = orders.listIterator();
        while (iter.hasNext()) {
            LabOrderMsg orderToReparent = iter.next();
            if (orderToReparent.getParentSubId().isEmpty()) {
                continue;
            }
            // Parent order is always first in HL7 from WinPath (single case in IDS which looks like a malformed message)
            LabOrderMsg possibleOrder = orders.get(0);
            try {
                LabResultMsg foundParentResult = possibleOrder.getLabResultMsgs().stream()
                        .filter(par -> isChildOf(orderToReparent, par))
                        .findFirst().orElseThrow();
                // add the order to the list of sensitivities and delete from the original list
                logger.debug("Reparenting sensitivity {} onto {}", orderToReparent, foundParentResult);
                LabIsolateMsg parentIsolate = foundParentResult.getLabIsolate();
                parentIsolate.setSensitivities(orderToReparent.getLabResultMsgs());
                parentIsolate.setClinicalInformation(orderToReparent.getClinicalInformation());
                iter.remove();
            } catch (NoSuchElementException e) {
                throw new Hl7InconsistencyException("No parent order found for sensitivity", e);
            }
        }
    }

    /**
     * Use the sub IDs to see which observations (results) belong together and should be combined.
     * <p>
     * Eg. microbiology ISOLATE + CFU conc. appear in different OBX segments, linked by a sub ID.
     * @param labResults the list of lab results to merge. This elements of the list will be modified and/or removed.
     */
    private static void mergeOrFilterResults(List<WinPathResultBuilder> labResults) throws Hl7InconsistencyException {
        Map<String, WinPathResultBuilder> subIdMapping = new HashMap<>(labResults.size());
        ListIterator<WinPathResultBuilder> iterator = labResults.listIterator();
        while (iterator.hasNext()) {
            WinPathResultBuilder builder = iterator.next();
            String subId = builder.getMessage().getObservationSubId();

            if (subId.isEmpty()) {
                continue;
            } else if (builder.getMessage().getLabIsolate() == null) {
                throw new Hl7InconsistencyException("Lab isolate from result not found from winpath with subId");
            }

            WinPathResultBuilder existing = subIdMapping.get(subId);
            if (existing == null) {
                // save it for future results that will need to refer back to it
                subIdMapping.put(subId, builder);
            } else {
                // the sub ID has already been seen, so merge this result into the existing result, and delete this result
                existing.mergeIsolatesSetMimeTypeAndClearValue(builder.getMessage());
                iterator.remove();
            }
        }
    }

    /**
     * EpicCareOrderNumber duplicated between the ORC and OBR segments.
     * Check that everything matches as expected. If it doesn't we might have to do some integration
     * here.
     * @throws Hl7InconsistencyException if anything doesn't match
     */
    private void validateAndSetEpicOrderNumber() throws Hl7InconsistencyException {
        String orcNumber = getEpicCareOrderNumberOrc();
        String obrNumber = getEpicCareOrderNumberObr();
        // check we're not confused and these order numbers match - they can be empty though (eg. if ORC-1 = "SN")
        if (!orcNumber.equals(obrNumber)) {
            throw new Hl7InconsistencyException(String.format("ORC-2 %s does not match OBR-2 %s", orcNumber, obrNumber));
        }
        //once we've established they're identical, set the definitive value to be one of them
        getMsg().setEpicCareOrderNumber(orcNumber);
    }


    /**
     * HL7-specific way of determining parentage. The workings of this shouldn't be
     * exposed to the interchange format (ie. LabOrder).
     * @param possibleChild  the order to test whether possibleParent is a parent of it
     * @param possibleParent the result to test whether possibleChild is a child of it
     * @return whether possibleChild is a child (ie. a sensitivity order/result) of possibleParent
     */
    private static boolean isChildOf(LabOrderMsg possibleChild, LabResultMsg possibleParent) {
        if (possibleChild.getEpicCareOrderNumber().isEmpty()
                || possibleChild.getParentObservationIdentifier().isEmpty()
                || possibleChild.getParentSubId().isEmpty()) {
            return false;
        }

        return possibleChild.getEpicCareOrderNumber().equals(possibleParent.getEpicCareOrderNumber())
                && possibleChild.getParentObservationIdentifier().equals(possibleParent.getTestItemLocalCode())
                && possibleChild.getParentSubId().equals(possibleParent.getObservationSubId());
    }

}

