package uk.ac.ucl.rits.inform.pipeline.hl7;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v26.group.ORM_O01_ORDER;
import ca.uhn.hl7v2.model.v26.group.ORM_O01_ORDER_DETAIL;
import ca.uhn.hl7v2.model.v26.group.ORM_O01_PATIENT;
import ca.uhn.hl7v2.model.v26.message.ORM_O01;
import ca.uhn.hl7v2.model.v26.segment.MSH;
import ca.uhn.hl7v2.model.v26.segment.OBR;
import ca.uhn.hl7v2.model.v26.segment.ORC;
import ca.uhn.hl7v2.model.v26.segment.PID;
import ca.uhn.hl7v2.model.v26.segment.PV1;
import uk.ac.ucl.rits.inform.pipeline.exceptions.Hl7InconsistencyException;

/**
 * The top level of the pathology tree, the order.
 * The HL7 parsing interfaces perhaps belong in their own parser class.
 * @author Jeremy Stein
 */
public class PathologyOrder {
    private static final Logger logger = LoggerFactory.getLogger(PathologyOrder.class);

    // we won't get the order and the results in the same message, does this really
    // belong here?
    private List<PathologyBatteryResult> pathologyBatteryResults = new ArrayList<>();
    private String orderControlId;
    private String epicCareOrderNumber;
    private String labSpecimenNumber;
    private Instant orderDateTime;
    private String orderType;
    private String visitNumber;

    /**
     * Several orders for one patient can exist in the same message, so make one object for each.
     * @param ormMsg the ORM message
     * @return list of PathologyOrder orders, one for each order
     * @throws HL7Exception if HAPI does
     * @throws Hl7InconsistencyException if something about the HL7 message doesn't make sense
     */
    public static List<PathologyOrder> buildPathologyOrders(Message ormMsg) throws HL7Exception, Hl7InconsistencyException {
        List<PathologyOrder> orders = new ArrayList<>();
        ORM_O01 ormO01 = (ORM_O01) ormMsg;
        List<ORM_O01_ORDER> orderAll = ormO01.getORDERAll();
        for (ORM_O01_ORDER order : orderAll) {
            PathologyOrder pathologyOrder = new PathologyOrder(order, ormMsg);
            orders.add(pathologyOrder);
        }
        return orders;
    }

    /**
     * Build a pathology order structure from a pathology order message.
     * @param order one of the order groups in the message that is to be converted into an order structure
     * @param ormMsg the ORM^O01 message (contains multiple orders)
     * @throws HL7Exception if HAPI does
     * @throws Hl7InconsistencyException if something about the HL7 message doesn't make sense
     */
    public PathologyOrder(ORM_O01_ORDER order, Message ormMsg) throws HL7Exception, Hl7InconsistencyException {
        ORM_O01 ormO01 = (ORM_O01) ormMsg;
        MSH msh = (MSH) ormMsg.get("MSH");
        ORM_O01_PATIENT patient = ormO01.getPATIENT();
        PID pid = patient.getPID();
        PV1 pv1 = patient.getPATIENT_VISIT().getPV1();
        PatientInfoHl7 patientHl7 = new PatientInfoHl7(msh, pid, pv1);
        visitNumber = patientHl7.getVisitNumber();
        ORC orc = order.getORC();
        // NA/NW/CA/CR/OC/XO
        orderControlId = orc.getOrc1_OrderControl().getValue();
        epicCareOrderNumber = orc.getOrc2_PlacerOrderNumber().getEi1_EntityIdentifier().getValueOrEmpty();
        labSpecimenNumber = orc.getOrc4_PlacerGroupNumber().getEi1_EntityIdentifier().getValue();
        orderDateTime = HL7Utils.interpretLocalTime(orc.getOrc9_DateTimeOfTransaction());
        orderType = orc.getOrc29_OrderType().getCwe1_Identifier().getValue();
        OBR obr = order.getORDER_DETAIL().getOBR();
        // check we're not confused and these order numbers match - they can be empty though (eg. for "SN" ORC-1 value)
        String obrOrderNumber = obr.getObr2_PlacerOrderNumber().getEi1_EntityIdentifier().getValueOrEmpty();
        if (!epicCareOrderNumber.equals(obrOrderNumber)) {
            throw new Hl7InconsistencyException(String.format("ORC-2 %s does not match OBR-2 %s", epicCareOrderNumber, obrOrderNumber));
        }
        ORM_O01_ORDER_DETAIL orderDetail = order.getORDER_DETAIL();
        orderDetail.getOBSERVATIONAll();
    }

    // alternative way of generating the order details from the result that
    // gets called from where...?
    //public PathologyOrder(RESULT_SOMETHING res)

    /**
     * @return the pathology battery results for this order
     */
    public List<PathologyBatteryResult> getPathologyBatteryResults() {
        return pathologyBatteryResults;
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
        return epicCareOrderNumber;
    }

    /**
     * @return the lab number for this order
     */
    public String getLabSpecimenNumber() {
        return labSpecimenNumber;
    }

    /**
     * @return date the order was originally made
     */
    public Instant getOrderDateTime() {
        return orderDateTime;
    }

    /**
     * @return order type (inpatient or outpatient)
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
}
