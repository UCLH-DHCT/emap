package uk.ac.ucl.rits.inform.datasources.ids;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.v26.datatype.CWE;
import ca.uhn.hl7v2.model.v26.datatype.PRL;
import ca.uhn.hl7v2.model.v26.group.ORM_O01_ORDER;
import ca.uhn.hl7v2.model.v26.group.ORM_O01_PATIENT;
import ca.uhn.hl7v2.model.v26.group.ORU_R01_OBSERVATION;
import ca.uhn.hl7v2.model.v26.group.ORU_R01_ORDER_OBSERVATION;
import ca.uhn.hl7v2.model.v26.group.ORU_R01_PATIENT_RESULT;
import ca.uhn.hl7v2.model.v26.group.ORU_R30_OBSERVATION;
import ca.uhn.hl7v2.model.v26.message.ORM_O01;
import ca.uhn.hl7v2.model.v26.message.ORU_R01;
import ca.uhn.hl7v2.model.v26.message.ORU_R30;
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
import uk.ac.ucl.rits.inform.interchange.lab.LabOrderMsg;
import uk.ac.ucl.rits.inform.interchange.lab.LabResultMsg;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Build one or more LabOrder object(s) from an HL7 message.
 * @author Jeremy Stein
 * @author Stef Piatek
 */
final class LabParser {
    private static Set<String> allowedOCIDs = new HashSet<>(Arrays.asList("SC", "RE"));

    private static final Logger logger = LoggerFactory.getLogger(LabParser.class);
    private static final String ignoredABLFlag = "N";

    private String epicCareOrderNumberOrc;
    private String epicCareOrderNumberObr;

    private final LabOrderMsg msg = new LabOrderMsg();

    /**
     * Build a lab order structure from a lab order (ORM) message.
     * @param subMessageSourceId unique Id from the IDS
     * @param order              one of the order groups in the message that is to be converted into an order structure
     * @param ormO01             the ORM^O01 message (can contain multiple orders) for extracting data common to the whole message
     * @throws HL7Exception               if HAPI does
     * @throws Hl7InconsistencyException  if something about the HL7 message doesn't make sense
     * @throws Hl7MessageIgnoredException if the entire message should be ignored
     */
    private LabParser(String subMessageSourceId, ORM_O01_ORDER order, ORM_O01 ormO01)
            throws HL7Exception, Hl7InconsistencyException, Hl7MessageIgnoredException {
        MSH msh = (MSH) ormO01.get("MSH");
        ORM_O01_PATIENT patient = ormO01.getPATIENT();
        PID pid = patient.getPID();
        PV1 pv1 = patient.getPATIENT_VISIT().getPV1();
        setSourceAndPatientIdentifiers(subMessageSourceId, msh, pid, pv1);
        String sendingApplication = msg.getSourceSystem();
        if (!"WinPath".equals(sendingApplication)) {
            throw new Hl7MessageIgnoredException("Only processing messages from WinPath, not \"" + sendingApplication + "\"");
        }
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
     * @throws HL7Exception              if HAPI does
     * @throws Hl7InconsistencyException if, according to my understanding, the HL7 message contains errors
     */
    private LabParser(String subMessageSourceId, ORU_R01_ORDER_OBSERVATION obs, MSH msh, PID pid, PV1 pv1)
            throws HL7Exception, Hl7InconsistencyException {
        setSourceAndPatientIdentifiers(subMessageSourceId, msh, pid, pv1);
        OBR obr = obs.getOBR();
        populateObrFields(obr);
        populateOrderInformation(obs.getORC(), obr);
        validateAndSetEpicOrderNumber();

        List<LabResultBuilder> tempResults = new ArrayList<>(obs.getOBSERVATIONAll().size());
        List<ORU_R01_OBSERVATION> observationAll = obs.getOBSERVATIONAll();
        for (ORU_R01_OBSERVATION ob : observationAll) {
            OBX obx = ob.getOBX();
            List<NTE> notes = ob.getNTEAll();
            LabResultBuilder labResult = new LabResultBuilder(obx, obr)
                    .setTestIdentifiers(obx)
                    .populateResults(obx)
                    .setAbnormalFlagIgnoring(obx, null)
                    .populateComments(notes);
            tempResults.add(labResult);
        }
        // join some of the observations under this fact together (or ignore some of them)
        LabParser.mergeOrFilterResults(tempResults);
        msg.setLabResultMsgs(tempResults.stream().map(LabResultBuilder::getMessage).collect(Collectors.toList()));
    }

    /**
     * Construct parser from ABL ORU R30 message.
     * @param subMessageSourceId unique Id from the IDS
     * @param oruR30             ORU R30 message
     * @throws HL7Exception if HAPI does
     */
    private LabParser(String subMessageSourceId, ORU_R30 oruR30)
            throws HL7Exception {
        setSourceAndPatientIdentifiers(subMessageSourceId, oruR30.getMSH(), oruR30.getPID(), oruR30.getVISIT().getPV1());

        OBR obr = oruR30.getOBR();
        populateObrFields(obr);
        populateOrderInformation(obr);
        msg.setLabSpecimenNumber(obr.getObr3_FillerOrderNumber().getEi1_EntityIdentifier().getValueOrEmpty());

        List<ORU_R30_OBSERVATION> observations = oruR30.getOBSERVATIONAll();
        List<LabResultMsg> results = new ArrayList<>(observations.size());
        for (ORU_R30_OBSERVATION ob : observations) {
            OBX obx = ob.getOBX();
            List<NTE> notes = ob.getNTEAll();
            LabResultMsg labResult = new LabResultBuilder(obx, obr)
                    .setTestIdentifiers(obx)
                    .populateResults(obx)
                    .setAbnormalFlagIgnoring(obx, LabParser.ignoredABLFlag)
                    .populateComments(notes)
                    .getMessage();
            results.add(labResult);
        }
        msg.setLabResultMsgs(results);

    }

    /**
     * @return the underlying message we have now built
     */
    public LabOrderMsg getMessage() {
        return msg;
    }

    /**
     * Several orders for one patient can exist in the same message, so make one object for each.
     * @param idsUnid unique Id from the IDS
     * @param ormO01  the ORM message
     * @return list of LabOrder orders, one for each order
     * @throws HL7Exception              if HAPI does
     * @throws Hl7InconsistencyException if something about the HL7 message doesn't make sense
     */
    public static List<LabOrderMsg> buildLabOrders(String idsUnid, ORM_O01 ormO01)
            throws HL7Exception, Hl7InconsistencyException {
        List<ORM_O01_ORDER> hl7Orders = ormO01.getORDERAll();

        List<LabOrderMsg> interchangeOrders = new ArrayList<>(hl7Orders.size());
        int msgSuffix = 0;
        for (ORM_O01_ORDER order : hl7Orders) {
            msgSuffix++;
            String subMessageSourceId = String.format("%s_%02d", idsUnid, msgSuffix);
            LabOrderMsg labOrder;
            try {
                labOrder = new LabParser(subMessageSourceId, order, ormO01).msg;
                if (!LabParser.allowedOCIDs.contains(labOrder.getOrderControlId())) {
                    logger.trace("Ignoring order control ID ='{}'", labOrder.getOrderControlId());
                } else {
                    interchangeOrders.add(labOrder);
                }
            } catch (Hl7MessageIgnoredException e) {
                // if the entire message is being skipped, stop now
                return interchangeOrders;
            }
        }
        return interchangeOrders;
    }


    public static List<LabOrderMsg> buildLabOrders(String idsUnid, ORU_R30 oruR30) throws HL7Exception, Hl7InconsistencyException {
        LabOrderMsg labOrder = new LabParser(idsUnid, oruR30).msg;
        // only one observation per message
        List<LabOrderMsg> orders = new ArrayList<>(1);
        orders.add(labOrder);
        return orders;
    }

    /**
     * Several sets of results can exist in an ORU message, so build multiple LabOrder objects.
     * @param idsUnid unique Id from the IDS
     * @param oruR01  the HL7 message
     * @return a list of LabOrder messages built from the results message
     * @throws HL7Exception              if HAPI does
     * @throws Hl7InconsistencyException if, according to my understanding, the HL7 message contains errors
     */
    public static List<LabOrderMsg> buildLabOrders(String idsUnid, ORU_R01 oruR01)
            throws HL7Exception, Hl7InconsistencyException {
        if (oruR01.getPATIENT_RESULTReps() != 1) {
            throw new RuntimeException("not handling this yet");
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
            LabOrderMsg labOrder = new LabParser(subMessageSourceId, obs, msh, pid, pv1).msg;
            if (!allowedOCIDs.contains(labOrder.getOrderControlId())) {
                logger.trace("Ignoring order control ID = '{}'", labOrder.getOrderControlId());
            } else {
                orders.add(labOrder);
            }
        }
        LabParser.reparentOrders(orders);
        return orders;
    }

    /**
     * Set LabOrder message source information and patient/encounter identifiers.
     * @param subMessageSourceId unique Id from the IDS
     * @param msh                the MSH segment
     * @param pid                the PID segment
     * @param pv1                the PV1 segment
     * @throws HL7Exception if there is missing hl7 data
     */
    private void setSourceAndPatientIdentifiers(String subMessageSourceId, MSH msh, PID pid, PV1 pv1) throws HL7Exception {
        PatientInfoHl7 patientHl7 = new PatientInfoHl7(msh, pid, pv1);
        msg.setSourceMessageId(subMessageSourceId);
        msg.setSourceSystem(patientHl7.getSendingApplication());
        msg.setVisitNumber(patientHl7.getVisitNumber());
        msg.setMrn(patientHl7.getMrn());
    }

    /**
     * Use the HL7 fields to re-parent the (sensitivity) orders in this list so they point to the results
     * that they apply to. Parents and children must all be in the list supplied.
     * @param orders the list of all orders (usually with results(?)). This list
     *               will have items modified and/or deleted.
     */
    private static void reparentOrders(List<LabOrderMsg> orders) {
        // we may have multiple orders that are unrelated to each other (apart from
        // being for the same patient).
        for (int i = 0; i < orders.size(); i++) {
            LabOrderMsg orderToReparent = orders.get(i);
            if (!orderToReparent.getParentSubId().isEmpty()) {
                // The order has a parent, let's find it.
                // Not many elements, a linear search should be fine.
                // I'm assuming that the parent always appears before the child in the list.
                for (int j = 0; j < i; j++) {
                    LabOrderMsg possibleOrder = orders.get(j);
                    if (possibleOrder == null) {
                        // we already re-parented this one, skip
                        continue;
                    }
                    // An HL7 LabOrderBuilder will always contain HL7 LabResultBuilder objects for its results,
                    // so downcast will be safe. Find a better way of encoding this in the type system.
                    List<LabResultMsg> possibleParents = possibleOrder.getLabResultMsgs();
                    try {
                        LabResultMsg foundParent = possibleParents.stream().filter(par -> isChildOf(orderToReparent, par))
                                .findFirst().get();
                        // add the order to the list of sensitivities and delete from the original list
                        logger.debug("Reparenting sensitivity order {} onto {}", orderToReparent, foundParent);
                        foundParent.getLabSensitivities().add(orderToReparent);
                        orders.set(i, null);
                        break;
                    } catch (NoSuchElementException e) {
                        // should we do something here?
                    }
                }
            }
        }
        orders.removeIf(Objects::isNull);
    }

    /**
     * Use the sub IDs to see which observations (results) belong together
     * and should be combined. Eg. microbiology ISOLATE + CFU conc. appear in different OBX segments,
     * linked by a sub ID.
     * @param labResults the list of lab results to merge. This elements of the list will be modified and/or removed.
     */
    private static void mergeOrFilterResults(List<LabResultBuilder> labResults) {
        Map<String, LabResultBuilder> subIdMapping = new HashMap<>(labResults.size());
        for (int i = 0; i < labResults.size(); i++) {
            // can this "result" be ignored altogether?
            if (labResults.get(i).isIgnorable()) {
                labResults.set(i, null);
                continue;
            }
            // must this line of a result be merged with a previous line to give the
            // full result?
            String subId = labResults.get(i).getMessage().getObservationSubId();
            if (!subId.isEmpty()) {
                LabResultBuilder existing = subIdMapping.get(subId);
                if (existing == null) {
                    // save it for future results that will need to refer back to it
                    subIdMapping.put(subId, labResults.get(i));
                } else {
                    // the sub ID has already been seen, so merge this result
                    // into the existing result, and delete this result
                    existing.mergeResult(labResults.get(i).getMessage());
                    labResults.set(i, null);
                }
            }
        }
        // remove those which have been merged in and marked as null (all their data should have been incorporated in the merge)
        labResults.removeIf(Objects::isNull);
    }

    /**
     * Extract the fields found in the ORC segment (some context from OBR required), of which there is one of each per object.
     * @param orc the ORC segment
     * @param obr the OBR segment
     * @throws DataTypeException if HAPI does
     */
    private void populateOrderInformation(ORC orc, OBR obr) throws DataTypeException {
        // NA/NW/CA/CR/OC/XO
        msg.setOrderControlId(orc.getOrc1_OrderControl().getValue());
        epicCareOrderNumberOrc = orc.getOrc2_PlacerOrderNumber().getEi1_EntityIdentifier().getValueOrEmpty();
        String labSpecimen = orc.getOrc3_FillerOrderNumber().getEi1_EntityIdentifier().getValueOrEmpty();
        msg.setLabSpecimenNumber(labSpecimen);
        String labSpecimenOCS = orc.getOrc4_PlacerGroupNumber().getEi1_EntityIdentifier().getValueOrEmpty();
        msg.setSpecimenType(labSpecimenOCS.replace(labSpecimen, ""));
        msg.setOrderStatus(orc.getOrc5_OrderStatus().getValueOrEmpty());
        msg.setOrderType(orc.getOrc29_OrderType().getCwe1_Identifier().getValue());

        // The order time can only be got from an Epic->WinPath NW message. The ORC-9 means something different
        // in a status change (SC) message.
        Instant orc9 = HL7Utils.interpretLocalTime(orc.getOrc9_DateTimeOfTransaction());
        if ("NW".equals(msg.getOrderControlId())) {
            msg.setOrderDateTime(InterchangeValue.buildFromHl7(orc9));
        } else if ("SC".equals(msg.getOrderControlId())) {
            // possibly need to check for other result status codes that signify "in progress"?
            if ("I".equals(obr.getObr25_ResultStatus().getValueOrEmpty())) {
                // ORC-9 = time sample entered onto WinPath
                msg.setSampleEnteredTime(InterchangeValue.buildFromHl7(orc9));
            }
        }

        String resultStatus = obr.getObr25_ResultStatus().getValueOrEmpty();
        msg.setResultStatus(resultStatus);
    }

    /**
     * Populate order information from OBR segment (ABL 90 Flex).
     * @param obr OBR
     * @throws DataTypeException if HAPI does
     */
    private void populateOrderInformation(OBR obr) throws DataTypeException {
        Instant sampleReceived = HL7Utils.interpretLocalTime(obr.getObr14_SpecimenReceivedDateTime());
        msg.setSampleEnteredTime(InterchangeValue.buildFromHl7(sampleReceived));
        msg.setOrderDateTime(InterchangeValue.buildFromHl7(sampleReceived));
        msg.setStatusChangeTime(sampleReceived);
    }

    /**
     * Extract the fields found in the OBR segment, of which there is one of each per object.
     * @param obr the OBR segment
     * @throws DataTypeException if HAPI does
     */
    private void populateObrFields(OBR obr) throws DataTypeException {
        // The first ORM message from Epic->WinPath is only sent when the label for the sample is printed,
        // which is the closest we get to a "collection" time. The actual collection will happen some point
        // afterwards, we can't really tell. That's why an order message contains a non blank collection time.
        // This field is consistent throughout the workflow.
        msg.setObservationDateTime(HL7Utils.interpretLocalTime(obr.getObr7_ObservationDateTime()));
        Instant requestedTime = HL7Utils.interpretLocalTime(obr.getObr6_RequestedDateTime());
        msg.setRequestedDateTime(InterchangeValue.buildFromHl7(requestedTime));

        msg.setLabDepartment(obr.getObr24_DiagnosticServSectID().getValueOrEmpty());
        epicCareOrderNumberObr = obr.getObr2_PlacerOrderNumber().getEi1_EntityIdentifier().getValueOrEmpty();

        // this is the "last updated" field for results as well as changing to order "in progress"
        msg.setStatusChangeTime(HL7Utils.interpretLocalTime(obr.getObr22_ResultsRptStatusChngDateTime()));

        // identifies the battery of tests that has been performed/ordered (eg. FBC)
        CWE obr4 = obr.getObr4_UniversalServiceIdentifier();
        msg.setTestBatteryLocalCode(obr4.getCwe1_Identifier().getValueOrEmpty());
        msg.setTestBatteryLocalDescription(obr4.getCwe2_Text().getValueOrEmpty());
        msg.setTestBatteryCodingSystem(obr4.getCwe3_NameOfCodingSystem().getValueOrEmpty());

        PRL parent = obr.getObr26_ParentResult();

        // eg. "ISOLATE"
        // match to OBX-3.1
        msg.setParentObservationIdentifier(parent.getPrl1_ParentObservationIdentifier().getCwe1_Identifier().getValueOrEmpty());

        // match to OBX-4
        msg.setParentSubId(parent.getPrl2_ParentObservationSubIdentifier().getValueOrEmpty());
    }

    /**
     * HL7-specific way of determining parentage. The workings of this shouldn't be
     * exposed to the interchange format (ie. LabOrder).
     * @param possibleChild  the order to test whether possibleParent is a parent of it
     * @param possibleParent the result to test whether possibleChild is a child of it
     * @return whether possibleChild is a child (ie. a sensitivity order/result) of possibleParent
     */
    public static boolean isChildOf(LabOrderMsg possibleChild, LabResultMsg possibleParent) {
        if (possibleChild.getEpicCareOrderNumber().isEmpty()
                || possibleChild.getParentObservationIdentifier().isEmpty()
                || possibleChild.getParentSubId().isEmpty()) {
            return false;
        }

        return possibleChild.getEpicCareOrderNumber().equals(possibleParent.getEpicCareOrderNumber())
                && possibleChild.getParentObservationIdentifier().equals(possibleParent.getTestItemLocalCode())
                && possibleChild.getParentSubId().equals(possibleParent.getObservationSubId());
    }

    /**
     * EpicCareOrderNumber duplicated between the ORC and OBR segments.
     * Check that everything matches as expected. If it doesn't we might have to do some integration
     * here.
     * @throws Hl7InconsistencyException if anything doesn't match
     */
    private void validateAndSetEpicOrderNumber() throws Hl7InconsistencyException {
        // check we're not confused and these order numbers match - they can be empty though (eg. if ORC-1 = "SN")
        if (!epicCareOrderNumberOrc.equals(epicCareOrderNumberObr)) {
            throw new Hl7InconsistencyException(String.format("ORC-2 %s does not match OBR-2 %s", epicCareOrderNumberOrc, epicCareOrderNumberObr));
        }
        //once we've established they're identical, set the definitive value to be one of them
        msg.setEpicCareOrderNumber(epicCareOrderNumberOrc);
    }

}
