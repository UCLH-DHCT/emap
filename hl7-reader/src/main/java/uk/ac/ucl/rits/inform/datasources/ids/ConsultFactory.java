package uk.ac.ucl.rits.inform.datasources.ids;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.v26.group.ORM_O01_ORDER;
import ca.uhn.hl7v2.model.v26.group.ORM_O01_PATIENT;
import ca.uhn.hl7v2.model.v26.message.ORM_O01;
import ca.uhn.hl7v2.model.v26.segment.MSH;
import ca.uhn.hl7v2.model.v26.segment.NTE;
import ca.uhn.hl7v2.model.v26.segment.OBR;
import ca.uhn.hl7v2.model.v26.segment.ORC;
import ca.uhn.hl7v2.model.v26.segment.PID;
import ca.uhn.hl7v2.model.v26.segment.PV1;
import org.springframework.stereotype.Component;
import uk.ac.ucl.rits.inform.datasources.ids.exceptions.Hl7InconsistencyException;
import uk.ac.ucl.rits.inform.datasources.ids.hl7.parser.NotesParser;
import uk.ac.ucl.rits.inform.datasources.ids.hl7.parser.PatientInfoHl7;
import uk.ac.ucl.rits.inform.interchange.ConsultRequest;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;

import java.util.List;
import java.util.regex.Pattern;

@Component
public class ConsultFactory {
    private static final String QUESTION_SEPARATOR = "->";
    private static final Pattern QUESTION_PATTERN = Pattern.compile(QUESTION_SEPARATOR);
    private static final String CANCELLATION_OCID = "OC";
    private static final String AUTOMATED_FROM_DISCHARGE = "DISCHAUTO";

    public ConsultRequest makeConsult(String sourceId, ORM_O01 ormO01) throws HL7Exception, Hl7InconsistencyException {
        if (ormO01.getORDERReps() != 1) {
            throw new Hl7InconsistencyException("Consult request should always have one request");
        }
        PatientInfoHl7 patientInfo = buildPatientInfo(ormO01);
        ConsultRequest consult = new ConsultRequest(
                sourceId, patientInfo.getSendingApplication(), patientInfo.getMrn(), patientInfo.getVisitNumber());

        ORM_O01_ORDER order = ormO01.getORDER();
        addCancelledOrClosed(consult, order, patientInfo);
        addRequestInformation(consult, order);
        addQuestionsAndComments(consult, order.getORDER_DETAIL().getNTEAll());
        return consult;
    }


    private PatientInfoHl7 buildPatientInfo(ORM_O01 ormO01) {
        MSH msh = ormO01.getMSH();
        ORM_O01_PATIENT patient = ormO01.getPATIENT();
        PID pid = patient.getPID();
        PV1 pv1 = patient.getPATIENT_VISIT().getPV1();
        return new PatientInfoHl7(msh, pid, pv1);
    }

    private void addRequestInformation(ConsultRequest consult, ORM_O01_ORDER order) throws HL7Exception {
        ORC orc = order.getORC();
        OBR obr = order.getORDER_DETAIL().getOBR();
        consult.setEpicConsultId(Long.decode(orc.getOrc2_PlacerOrderNumber().getEi1_EntityIdentifier().getValueOrEmpty()));
        consult.setStatusChangeTime(HL7Utils.interpretLocalTime(orc.getOrc9_DateTimeOfTransaction()));
        consult.setRequestedDateTime(HL7Utils.interpretLocalTime(obr.getObr36_ScheduledDateTime()));
        consult.setConsultationType(obr.getObr44_ProcedureCode().encode());
    }

    private void addQuestionsAndComments(ConsultRequest consult, List<NTE> notes) {
        NotesParser parser = new NotesParser(notes, QUESTION_SEPARATOR, QUESTION_PATTERN);
        consult.setQuestions(parser.getQuestions());
        consult.setNotes(InterchangeValue.buildFromHl7(parser.getComments()));
    }

    private void addCancelledOrClosed(ConsultRequest consult, ORM_O01_ORDER order, PatientInfoHl7 patientInfo) {
        if (isOrderCancelled(order)) {
            if (isFromAutomatedDischarge(patientInfo)) {
                consult.setClosedDueToDischarge(true);
            } else {
                consult.setCancelled(true);
            }
        }
    }

    private boolean isOrderCancelled(ORM_O01_ORDER order) {
        return CANCELLATION_OCID.equals(order.getORC().getOrc1_OrderControl().getValue());
    }

    private boolean isFromAutomatedDischarge(PatientInfoHl7 patientInfo) {
        return AUTOMATED_FROM_DISCHARGE.equals(patientInfo.getSecurityCode());
    }
}
