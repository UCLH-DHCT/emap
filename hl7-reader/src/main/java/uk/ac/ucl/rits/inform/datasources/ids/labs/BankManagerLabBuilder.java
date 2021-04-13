package uk.ac.ucl.rits.inform.datasources.ids.labs;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.v26.datatype.FT;
import ca.uhn.hl7v2.model.v26.group.ORU_R01_OBSERVATION;
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
import uk.ac.ucl.rits.inform.datasources.ids.hl7parser.PatientInfoHl7;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.OrderCodingSystem;
import uk.ac.ucl.rits.inform.interchange.lab.LabOrderMsg;
import uk.ac.ucl.rits.inform.interchange.lab.LabResultMsg;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static uk.ac.ucl.rits.inform.datasources.ids.HL7Utils.interpretLocalTime;

/**
 * Build Bank Manager LabOrders.
 * @author Stef Piatek
 */
public final class BankManagerLabBuilder extends LabOrderBuilder {

    /**
     * Construct parsed from Bank Manager ORU R01 message.
     * @param idsUnid        unique Id from the IDS
     * @param msh            MSG segment
     * @param patientResults patient results from HL7 message
     * @throws HL7Exception              if HAPI does
     * @throws Hl7InconsistencyException if hl7 message is malformed
     */
    private BankManagerLabBuilder(String idsUnid, MSH msh, ORU_R01_PATIENT_RESULT patientResults)
            throws HL7Exception, Hl7InconsistencyException {
        super(new String[]{"SC", "RE", "OC"}, OrderCodingSystem.BANK_MANAGER);
        setBatteryCodingSystem();

        ORU_R01_ORDER_OBSERVATION obs = patientResults.getORDER_OBSERVATION();
        PID pid = patientResults.getPATIENT().getPID();
        PV1 pv1 = patientResults.getPATIENT().getVISIT().getPV1();
        OBR obr = obs.getOBR();
        PatientInfoHl7 patientHl7 = new PatientInfoHl7(msh, pid, pv1);

        setSourceAndPatientIdentifiers(idsUnid, patientHl7);
        populateObrFields(obr);
        populateOrderInformation(obr);
        Instant statusChange = interpretLocalTime(obr.getObr22_ResultsRptStatusChngDateTime());
        getMsg().setStatusChangeTime(statusChange);
        getMsg().setOrderControlId(obs.getORC().getOrc1_OrderControl().getValue());
        getMsg().setLabSpecimenNumber(obr.getObr3_FillerOrderNumber().getEi1_EntityIdentifier().getValueOrEmpty());
        getMsg().setLabDepartment(getCodingSystem().name());
        setClinicalInformationFromNotes(obs.getNTEAll());

        List<LabResultMsg> results = new ArrayList<>(obs.getOBSERVATIONAll().size());
        for (ORU_R01_OBSERVATION ob : obs.getOBSERVATIONAll()) {
            OBX obx = ob.getOBX();
            List<NTE> notes = ob.getNTEAll();
            BankManagerResultBuilder labResult = new BankManagerResultBuilder(obx, notes);
            labResult.constructMsg();
            results.add(labResult.getMessage());
        }

        getMsg().setLabResultMsgs(results);
    }

    private void setClinicalInformationFromNotes(List<NTE> notes) {
        StringBuilder questionAndAnswer = new StringBuilder();
        for (NTE note : notes) {
            for (FT ft : note.getNte3_Comment()) {
                questionAndAnswer.append(ft.getValueOrEmpty()).append("\n");
            }
        }
        getMsg().setClinicalInformation(InterchangeValue.buildFromHl7(questionAndAnswer.toString().strip()));
    }


    /**
     * Build Lab Order from Bank Manager.
     * @param idsUnid unique Id from the IDS
     * @param oruR01  the HL7 message
     * @return a list of LabOrder messages built from the results message
     * @throws HL7Exception              if HAPI does
     * @throws Hl7InconsistencyException if hl7 message is malformed
     */
    public static Collection<LabOrderMsg> build(String idsUnid, ORU_R01 oruR01) throws HL7Exception, Hl7InconsistencyException {
        ORU_R01_PATIENT_RESULT patientResults = oruR01.getPATIENT_RESULT();
        MSH msh = (MSH) oruR01.get("MSH");
        if (patientResults.getORDER_OBSERVATIONReps() > 1) {
            throw new Hl7InconsistencyException("Bank Manager messages should only have one order");
        }
        List<LabOrderMsg> orders = new ArrayList<>(1);
        LabOrderBuilder labOrderBuilder = new BankManagerLabBuilder(idsUnid, msh, patientResults);
        labOrderBuilder.addMsgIfAllowedOcId(orders);
        return orders;

    }

    /**
     * Set the specimen number from the ORC segment.
     * Each lab result that uses this appears to need a separate implementation of this.
     * @param orc ORC segment
     */
    @Override
    protected void setLabSpecimenNumber(ORC orc) {
       return; // not used
    }
}
