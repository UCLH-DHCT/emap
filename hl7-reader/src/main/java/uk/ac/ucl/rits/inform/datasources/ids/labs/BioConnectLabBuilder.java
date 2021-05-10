package uk.ac.ucl.rits.inform.datasources.ids.labs;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.v26.group.ORU_R01_ORDER_OBSERVATION;
import ca.uhn.hl7v2.model.v26.group.ORU_R01_PATIENT_RESULT;
import ca.uhn.hl7v2.model.v26.message.ORU_R01;
import ca.uhn.hl7v2.model.v26.segment.MSH;
import ca.uhn.hl7v2.model.v26.segment.NTE;
import ca.uhn.hl7v2.model.v26.segment.OBR;
import ca.uhn.hl7v2.model.v26.segment.OBX;
import ca.uhn.hl7v2.model.v26.segment.ORC;
import ca.uhn.hl7v2.model.v26.segment.PID;
import ca.uhn.hl7v2.model.v26.segment.PV1;
import uk.ac.ucl.rits.inform.datasources.ids.exceptions.Hl7InconsistencyException;
import uk.ac.ucl.rits.inform.datasources.ids.exceptions.Hl7MessageIgnoredException;
import uk.ac.ucl.rits.inform.datasources.ids.hl7parser.PatientInfoHl7;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.OrderCodingSystem;
import uk.ac.ucl.rits.inform.interchange.lab.LabOrderMsg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.util.Collections.singletonList;

/**
 * Build Bio-connect LabOrders.
 * @author Stef Piatek
 */
public final class BioConnectLabBuilder extends LabOrderBuilder {

    /**
     * Construct parsed from BIO-CONNECT ORU R01 message.
     * @param idsUnid        unique Id from the IDS
     * @param msh            MSG segment
     * @param patientResults patient results from HL7 message
     * @throws HL7Exception              if HAPI does
     * @throws Hl7InconsistencyException if hl7 message is malformed
     */
    private BioConnectLabBuilder(String idsUnid, MSH msh, ORU_R01_PATIENT_RESULT patientResults)
            throws HL7Exception, Hl7InconsistencyException {
        super(new String[]{"NW"}, OrderCodingSystem.BIO_CONNECT);
        setBatteryCodingSystem();

        ORU_R01_ORDER_OBSERVATION obs = patientResults.getORDER_OBSERVATION();
        if (obs.getOBSERVATIONReps() > 1) {
            throw new Hl7InconsistencyException("BIO-CONNECT messages should only have one OBX result segment");
        }
        PID pid = patientResults.getPATIENT().getPID();
        PV1 pv1 = patientResults.getPATIENT().getVISIT().getPV1();
        OBR obr = obs.getOBR();
        PatientInfoHl7 patientHl7 = new PatientInfoHl7(msh, pid, pv1);

        setSourceAndPatientIdentifiers(idsUnid, patientHl7);
        populateObrFields(obr);
        populateOrderInformation(obr);
        getMsg().setOrderControlId(obs.getORC().getOrc1_OrderControl().getValue());

        // although the request datetime is in the message, doesn't seem to make sense to set it
        getMsg().setRequestedDateTime(InterchangeValue.unknown());
        getMsg().setLabSpecimenNumber(obr.getObr2_PlacerOrderNumber().getEi1_EntityIdentifier().getValueOrEmpty());


        OBX obx = obs.getOBSERVATION().getOBX();
        List<NTE> notes = obs.getOBSERVATION().getNTEAll();
        LabResultBuilder labResult = new BioConnectResultBuilder(obx, obr, notes, getMsg().getSourceSystem());
        labResult.constructMsg();

        getMsg().setLabResultMsgs(singletonList(labResult.getMessage()));
    }

    @Override
    protected void setLabSpecimenNumber(ORC orc) {
        return; // not used
    }

    /**
     * Build Lab Order from BIO-CONNECT point of care device.
     * @param idsUnid      unique Id from the IDS
     * @param oruR01       the HL7 message
     * @return a list of LabOrder messages built from the results message
     * @throws HL7Exception              if HAPI does
     * @throws Hl7InconsistencyException if hl7 message is malformed
     */
    public static Collection<LabOrderMsg> build(String idsUnid, ORU_R01 oruR01)
            throws HL7Exception, Hl7InconsistencyException, Hl7MessageIgnoredException {
        ORU_R01_PATIENT_RESULT patientResults = oruR01.getPATIENT_RESULT();
        MSH msh = (MSH) oruR01.get("MSH");
        if (patientResults.getORDER_OBSERVATIONReps() > 1) {
            throw new Hl7InconsistencyException("BIO-CONNECT messages should only have one order");
        }
        List<LabOrderMsg> orders = new ArrayList<>(1);
        LabOrderBuilder labOrderBuilder = new BioConnectLabBuilder(idsUnid, msh, patientResults);
        labOrderBuilder.addMsgIfAllowedOcId(idsUnid, orders);
        return orders;

    }
}
