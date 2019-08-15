package uk.ac.ucl.rits.inform.pipeline.hl7;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.v26.datatype.CWE;
import ca.uhn.hl7v2.model.v26.datatype.PRL;
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
import uk.ac.ucl.rits.inform.pipeline.exceptions.Hl7InconsistencyException;
import uk.ac.ucl.rits.inform.pipeline.exceptions.MessageIgnoredException;

/**
 * The top level of the pathology tree, the order.
 * The HL7 parsing interfaces perhaps belong in their own parser class.
 * @author Jeremy Stein
 */
public class PathologyOrder {
    private static final Logger logger = LoggerFactory.getLogger(PathologyOrder.class);

    private List<PathologyResult> pathologyResults = new ArrayList<>();
    private String orderControlId;
    private String epicCareOrderNumberOrc;
    private String epicCareOrderNumberObr;
    private String labSpecimenNumber;
    private String labSpecimenNumberOCS;
    private Instant orderDateTime;
    private Instant sampleEnteredTime;
    private String orderStatus;
    private String orderType;
    private String visitNumber;
    private Instant requestedDateTime;
    private Instant observationDateTime;
    private String testBatteryLocalCode;
    private String testBatteryLocalDescription;
    private String testBatteryCodingSystem;
    private Instant statusChangeTime;

    private String parentObservationIdentifier;
    private String parentSubId;

    private static Set<String> allowedOCIDs = new HashSet<>(Arrays.asList("SC", "RE"));

    /**
     * Several orders for one patient can exist in the same message, so make one object for each.
     * @param ormO01 the ORM message
     * @return list of PathologyOrder orders, one for each order
     * @throws HL7Exception if HAPI does
     * @throws Hl7InconsistencyException if something about the HL7 message doesn't make sense
     */
    public static List<PathologyOrder> buildPathologyOrders(ORM_O01 ormO01) throws HL7Exception, Hl7InconsistencyException {
        List<PathologyOrder> orders = new ArrayList<>();
        List<ORM_O01_ORDER> orderAll = ormO01.getORDERAll();
        for (ORM_O01_ORDER order : orderAll) {
            PathologyOrder pathologyOrder;
            try {
                pathologyOrder = new PathologyOrder(order, ormO01);
                if (!allowedOCIDs.contains(pathologyOrder.getOrderControlId())) {
                    logger.warn("Ignoring order control ID = \"" + pathologyOrder.getOrderControlId() + "\"");
                } else {
                    orders.add(pathologyOrder);
                }
            } catch (MessageIgnoredException e) {
                // if the entire message is being skipped, stop now
                return orders;
            }
        }
        return orders;
    }

    /**
     * Several sets of results can exist in an ORU message, so build multiple PathologyOrder objects.
     * @param oruR01 the HL7 message
     * @return a list of PathologyOrder messages built from the results message
     * @throws HL7Exception if HAPI does
     * @throws Hl7InconsistencyException if, according to my understanding, the HL7 message contains errors
     */
    public static List<PathologyOrder> buildPathologyOrdersFromResults(ORU_R01 oruR01) throws HL7Exception, Hl7InconsistencyException {
        List<PathologyOrder> orders = new ArrayList<>();
        if (oruR01.getPATIENT_RESULTReps() != 1) {
            throw new RuntimeException("not handling this yet");
        }
        ORU_R01_PATIENT_RESULT patientResults = oruR01.getPATIENT_RESULT();
        List<ORU_R01_ORDER_OBSERVATION> orderObservations = patientResults.getORDER_OBSERVATIONAll();
        MSH msh = (MSH) oruR01.get("MSH");
        PID pid = patientResults.getPATIENT().getPID();
        PV1 pv1 = patientResults.getPATIENT().getVISIT().getPV1();

        for (ORU_R01_ORDER_OBSERVATION obs : orderObservations) {
            PathologyOrder pathologyOrder = new PathologyOrder(obs, msh, pid, pv1);
            String testBatteryLocalCode = pathologyOrder.getTestBatteryLocalCode();
            if (!allowedOCIDs.contains(pathologyOrder.getOrderControlId())) {
                logger.warn("Ignoring order control ID = \"" + pathologyOrder.getOrderControlId() + "\"");
            } else {
                orders.add(pathologyOrder);
            }
        }
        reparentOrders(orders);
        return orders;
    }

    /**
     * Re-parent the (sensitivity) orders in this list so they point to the results
     * that they apply to. Parents and children must all be in the list supplied.
     *
     * @param orders the list of all orders (usually with results(?)). This list
     *               will have items modified and/or deleted.
     */
    private static void reparentOrders(List<PathologyOrder> orders) {
        // we may have multiple orders that are unrelated to each other (apart from
        // being for the same patient).
        for (int i = 0; i < orders.size(); i++) {
            PathologyOrder orderToReparent = orders.get(i);
            if (!orderToReparent.getParentSubId().isEmpty()) {
                // The order has a parent, let's find it.
                // Not many elements, a linear search should be fine.
                // Assuming that the parent always appears before the child in the list.
                for (int j = 0; j < i; j++) {
                    List<PathologyResult> possibleParents = orders.get(j).getPathologyResults();
                    try {
                        PathologyResult foundParent = possibleParents.stream().filter(par -> orderToReparent.isChildOf(par))
                                .findFirst().get();
                        // add the order to the list of sensitivities and delete from the original list
                        logger.info("Reparenting sensitivity order " + orderToReparent + " onto " + foundParent);
                        foundParent.getPathologySensitivities().add(orderToReparent);
                        orders.set(i, null);
                        break;
                    } catch (NoSuchElementException e) {
                    }
                }
            }
        }
        orders.removeIf(o -> o == null);
    }

    /**
     * Build a pathology order structure from a pathology order (ORM)  message.
     * @param order one of the order groups in the message that is to be converted into an order structure
     * @param ormO01 the ORM^O01 message (can contain multiple orders) for extracting data common to the whole message
     * @throws HL7Exception if HAPI does
     * @throws Hl7InconsistencyException if something about the HL7 message doesn't make sense
     * @throws MessageIgnoredException if the entire message should be ignored
     */
    public PathologyOrder(ORM_O01_ORDER order, ORM_O01 ormO01) throws HL7Exception, Hl7InconsistencyException, MessageIgnoredException {
        MSH msh = (MSH) ormO01.get("MSH");
        ORM_O01_PATIENT patient = ormO01.getPATIENT();
        PID pid = patient.getPID();
        PV1 pv1 = patient.getPATIENT_VISIT().getPV1();
        PatientInfoHl7 patientHl7 = new PatientInfoHl7(msh, pid, pv1);
        visitNumber = patientHl7.getVisitNumber();
        String sendingApplication = patientHl7.getSendingApplication();
        if (!sendingApplication.equals("WinPath")) {
            throw new MessageIgnoredException("Only processing messages from WinPath, not \"" + sendingApplication + "\"");
        }
        ORC orc = order.getORC();
        OBR obr = order.getORDER_DETAIL().getOBR();
        populateFromOrcObr(orc, obr);
        validateRedundantFields();
    }

    /**
     * Construct order details from a results (ORU) message. For simplicity we are using the
     * same structure in both cases. Most/all of the details of the order are contained in the
     * results message - at least the order numbers are present so we can look up the order
     * which we should already know about from a preceding ORM message.
     *
     * ORU_R01_PATIENT_RESULT repeating
     *      ORU_R01_PATIENT optional
     *          PID (Patient Identification)
     *          PRT (Participation Information) optional repeating
     *          PD1 (Patient Additional Demographic) optional
     *          NTE (Notes and Comments) optional repeating
     *          NK1 (Next of Kin / Associated Parties) optional repeating
     *          ORU_R01_PATIENT_OBSERVATION (a Group object) optional repeating
     *              OBX (Observation/Result)
     *              PRT (Participation Information) optional repeating
     *          ORU_R01_VISIT (a Group object) optional
     *              PV1 (Patient Visit)
     *              PV2 (Patient Visit - Additional Information) optional
     *              PRT (Participation Information) optional repeating
     *      ORU_R01_ORDER_OBSERVATION repeating
     *          ORC (Common Order) optional
     *          OBR (Observation Request)
     *          NTE (Notes and Comments) optional repeating
     *          PRT (Participation Information) optional repeating
     *          ORU_R01_TIMING_QTY (a Group object) optional repeating
     *          CTD (Contact Data) optional
     *          ORU_R01_OBSERVATION (a Group object) optional repeating
     *              OBX (Observation/Result)
     *              PRT (Participation Information) optional repeating
     *              NTE (Notes and Comments) optional repeating
     *          FT1 (Financial Transaction) optional repeating
     *          CTI (Clinical Trial Identification) optional repeating
     *          ORU_R01_SPECIMEN (a Group object) optional repeating
     *              SPM (Specimen)
     *              ORU_R01_SPECIMEN_OBSERVATION (a Group object) optional repeating
     *                  OBX (Observation/Result)
     *                  PRT (Participation Information) optional repeating
     *
     * @param obs the result group from HAPI (ORU_R01_ORDER_OBSERVATION)
     * @param msh the MSH segment
     * @param pid the PID segment
     * @param pv1 the PV1 segment
     * @throws HL7Exception if HAPI does
     * @throws Hl7InconsistencyException if, according to my understanding, the HL7 message contains errors
     */
    public PathologyOrder(ORU_R01_ORDER_OBSERVATION obs, MSH msh, PID pid, PV1 pv1) throws HL7Exception, Hl7InconsistencyException {
        // Can only seem to get these segments at the ORU_R01_PATIENT_RESULT level.
        // Could there really be more than one patient per message?
        PatientInfoHl7 patientHl7 = new PatientInfoHl7(msh, pid, pv1);
        visitNumber = patientHl7.getVisitNumber();
        OBR obr = obs.getOBR();
        ORC orc = obs.getORC();
        populateFromOrcObr(orc, obr);
        validateRedundantFields();

        List<ORU_R01_OBSERVATION> observationAll = obs.getOBSERVATIONAll();
        for (ORU_R01_OBSERVATION ob : observationAll) {
            OBX obx = ob.getOBX();
            List<NTE> notes = ob.getNTEAll();
            PathologyResult pathologyResult = new PathologyResult(obx, obr, notes);
            pathologyResults.add(pathologyResult);
        }
        // join some of the observations under this fact together (or ignore some of them)
        mergeOrFilterResults(this.pathologyResults);
    }

    /**
     * Use the sub IDs to see which observations (results) belong together
     * and should be combined. Eg. microbiology ISOLATE + CFU conc. appear in different OBX segments,
     * linked by a sub ID.
     * @param pathologyResults the list of pathology results to merge. This elements of the list will be modified and/or removed.
     */
    private static void mergeOrFilterResults(List<PathologyResult> pathologyResults) {
        Map<String, PathologyResult> subIdMapping = new HashMap<>();
        for (int i = 0; i < pathologyResults.size(); i++) {
            // can this "result" be ignored altogether?
            if (pathologyResults.get(i).isIgnorable()) {
                pathologyResults.set(i, null);
                continue;
            }
            // must this line of a result be merged with a previous line to give the
            // full result?
            String subId = pathologyResults.get(i).getObservationSubId();
            if (!subId.isEmpty()) {
                PathologyResult existing = subIdMapping.get(subId);
                if (existing == null) {
                    // save it for future results that will need to refer back to it
                    subIdMapping.put(subId, pathologyResults.get(i));
                } else {
                    // the sub ID has already been seen, so merge this result
                    // into the existing result, and delete this result
                    existing.mergeResult(pathologyResults.get(i));
                    pathologyResults.set(i, null);
                }
            }
        }
        // remove those which have been merged in and marked as null (all their data should have been incorporated in the merge)
        pathologyResults.removeIf(pr -> pr == null);
    }

    /**
     * Extract the fields found in the ORC+OBR segments, of which there is one of each per object.
     * @param orc the ORC segment
     * @param obr the OBR segment
     * @throws DataTypeException if HAPI does
     */
    private void populateFromOrcObr(ORC orc, OBR obr) throws DataTypeException {
        // NA/NW/CA/CR/OC/XO
        orderControlId = orc.getOrc1_OrderControl().getValue();
        epicCareOrderNumberOrc = orc.getOrc2_PlacerOrderNumber().getEi1_EntityIdentifier().getValueOrEmpty();
        labSpecimenNumber = orc.getOrc3_FillerOrderNumber().getEi1_EntityIdentifier().getValueOrEmpty();
        labSpecimenNumberOCS = orc.getOrc4_PlacerGroupNumber().getEi1_EntityIdentifier().getValueOrEmpty();

        orderStatus = orc.getOrc5_OrderStatus().getValueOrEmpty();
        String resultStatus = obr.getObr25_ResultStatus().getValueOrEmpty();

        // The order time can only be got from an Epic->WinPath NW message. The ORC-9 means something different
        // in a status change (SC) message.
        Instant orc9 = HL7Utils.interpretLocalTime(orc.getOrc9_DateTimeOfTransaction());
        if (orderControlId.equals("NW")) {
            orderDateTime = orc9;
        } else if (orderControlId.equals("SC")) {
            // possibly need to check for other result status codes that signify "in progress"?
            if (resultStatus.equals("I")) {
                // ORC-9 = time sample entered onto WinPath
                sampleEnteredTime = orc9;
            }
        }
        orderType = orc.getOrc29_OrderType().getCwe1_Identifier().getValue();

        // The first ORM message from Epic->WinPath is only sent when the label for the sample is printed,
        // which is the closest we get to a "collection" time. The actual collection will happen some point
        // afterwards, we can't really tell. That's why an order message contains a non blank collection time.
        // This field is consistent throughout the workflow.
        observationDateTime = HL7Utils.interpretLocalTime(obr.getObr7_ObservationDateTime());
        requestedDateTime = HL7Utils.interpretLocalTime(obr.getObr6_RequestedDateTime());

        epicCareOrderNumberObr = obr.getObr2_PlacerOrderNumber().getEi1_EntityIdentifier().getValueOrEmpty();

        // this is the "last updated" field for results as well as changing to order "in progress"
        statusChangeTime = HL7Utils.interpretLocalTime(obr.getObr22_ResultsRptStatusChngDateTime());

        // only present in Epic -> WinPath msg
        String labSpecimenNum = obr.getObr20_FillerField1().getValueOrEmpty();

        // identifies the battery of tests that has been performed/ordered (eg. FBC)
        CWE obr4 = obr.getObr4_UniversalServiceIdentifier();
        testBatteryLocalCode = obr4.getCwe1_Identifier().getValueOrEmpty();
        testBatteryLocalDescription = obr4.getCwe2_Text().getValueOrEmpty();
        testBatteryCodingSystem = obr4.getCwe3_NameOfCodingSystem().getValueOrEmpty();

        PRL parent = obr.getObr26_ParentResult();

        // eg. "ISOLATE"
        // match to OBX-3.1
        parentObservationIdentifier = parent.getPrl1_ParentObservationIdentifier().getCwe1_Identifier().getValueOrEmpty();

        // match to OBX-4
        parentSubId = parent.getPrl2_ParentObservationSubIdentifier().getValueOrEmpty();
    }

    /**
     * @param possibleParent the result to test whether "this" is a child of it
     * @return whether "this" is a child (ie. a sensitivity order/result) of possibleParent
     */
    public boolean isChildOf(PathologyResult possibleParent) {
        return !getEpicCareOrderNumber().isEmpty()
                && getEpicCareOrderNumber().equals(possibleParent.getEpicCareOrderNumber())
                && !parentObservationIdentifier.isEmpty()
                && parentObservationIdentifier.equals(possibleParent.getTestItemLocalCode())
                && !parentSubId.isEmpty()
                && parentSubId.equals(possibleParent.getObservationSubId());
    }

    /**
     * There are a number of fields that seem to be duplicated between the ORC and OBR segments.
     * Check that everything matches as expected. If it doesn't we might have to do some integration
     * here.
     * @throws Hl7InconsistencyException if anything doesn't match
     */
    private void validateRedundantFields() throws Hl7InconsistencyException {
        // check we're not confused and these order numbers match - they can be empty though (eg. if ORC-1 = "SN")
        if (!epicCareOrderNumberOrc.equals(epicCareOrderNumberObr)) {
            throw new Hl7InconsistencyException(String.format("ORC-2 %s does not match OBR-2 %s", epicCareOrderNumberOrc, epicCareOrderNumberObr));
        }
    }

    /**
     * @return the order control ID in the message
     */
    public String getOrderControlId() {
        return orderControlId;
    }

    /**
     * @return the EpicCare order number for this order
     */
    public String getEpicCareOrderNumber() {
        return epicCareOrderNumberOrc;
    }

    /**
     * @return the lab number for this order (known as the accession number by Epic)
     */
    public String getLabSpecimenNumber() {
        return labSpecimenNumber;
    }

    /**
     * @return the lab number with an extra character appended (known as the OCS number in WinPath)
     */
    public String getLabSpecimenNumberOCS() {
        return labSpecimenNumberOCS;
    }

    /**
     * @return date the order was originally made
     */
    public Instant getOrderDateTime() {
        return orderDateTime;
    }

    /**
     * @return date the sample was entered onto WinPath
     */
    public Instant getSampleEnteredTime() {
        return sampleEnteredTime;
    }

    /**
     * @return (patient) type for order (inpatient or outpatient)
     */
    public String getOrderType() {
        return orderType;
    }

    /**
     * @return the visit number (CSN) of the patient
     */
    public String getVisitNumber() {
        return visitNumber;
    }

    /**
     * @return The results for this order (will be empty if constructed from an ORM message)
     */
    public List<PathologyResult> getPathologyResults() {
        return pathologyResults;
    }

    /**
     * @return when the sample was taken
     */
    public Instant getObservationDateTime() {
        return observationDateTime;
    }

    /**
     * @return the local code (eg. WinPath code) for the test battery
     */
    public String getTestBatteryLocalCode() {
        return testBatteryLocalCode;
    }

    /**
     * @return the local description (eg. in WinPath) of the test battery
     */
    public String getTestBatteryLocalDescription() {
        return testBatteryLocalDescription;
    }

    /**
     * @return The local coding system in use (eg. WinPath)
     */
    public String getTestBatteryCodingSystem() {
        return testBatteryCodingSystem;
    }

    /**
     * @return the time the status of the results last changed
     */
    public Instant getStatusChangeTime() {
        return statusChangeTime;
    }

    /**
     * @return the requested date/time - how is this different to order time?
     */
    public Instant getRequestedDateTime() {
        return requestedDateTime;
    }

    /**
     * @return Order status (final, incomplete, etc.).
     * A,CA,CM,DC,ER,HD,IP,RP,SC (HL7 Table 0038)
     */
    public String getOrderStatus() {
        return orderStatus;
    }

    /**
     * Try to infer whether this order is a sensitivity order.
     * @return is this order a sensitivity order?
     */
    public boolean isSensitivity() {
        // a better test might be the test ID = "Micro^Sensitivities^WinPath"
        boolean emptyOrc5 = getOrderStatus().isEmpty();
        return emptyOrc5;
    }

    /**
     * @return the HL7 field to indicate the test identifier of the parent order for
     *         this order, if it has one. Arguably this shouldn't be stored in the
     *         JSON as it's a temporary value we use for building the structure and
     *         is HL7 specific.
     */
    public String getParentObservationIdentifier() {
        return parentObservationIdentifier;
    }

    /**
     * @return the HL7 field to indicate the sub ID of the parent order for this
     *         order, if it has one. Arguably this shouldn't be stored in the JSON
     *         as it's a temporary value we use for building the structure and is
     *         HL7 specific.
     */
    public String getParentSubId() {
        return parentSubId;
    }
}
