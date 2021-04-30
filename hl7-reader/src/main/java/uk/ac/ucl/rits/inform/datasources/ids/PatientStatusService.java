package uk.ac.ucl.rits.inform.datasources.ids;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.v26.segment.EVN;
import ca.uhn.hl7v2.model.v26.segment.MSH;
import ca.uhn.hl7v2.model.v26.segment.PID;
import ca.uhn.hl7v2.model.v26.segment.PV1;
import org.springframework.stereotype.Component;
import uk.ac.ucl.rits.inform.datasources.ids.hl7.custom.v26.field.Infection;
import uk.ac.ucl.rits.inform.datasources.ids.hl7.custom.v26.message.ADT_A05;
import uk.ac.ucl.rits.inform.datasources.ids.hl7.parser.PatientInfoHl7;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessage;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.PatientInfection;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;

@Component
public class PatientStatusService {
    public Collection<? extends EmapOperationMessage> buildMessages(String sourceId, ADT_A05 msg) throws HL7Exception {
        return buildPatientInfections(sourceId, msg);
    }

    private Collection<PatientInfection> buildPatientInfections(String sourceId, ADT_A05 msg) throws HL7Exception {
        MSH msh = msg.getMSH();
        PID pid = msg.getPID();
        PV1 pv1 = msg.getPV1();
        EVN evn = msg.getEVN();
        PatientInfoHl7 patientInfo = new PatientInfoHl7(msh, pid, pv1);
        ArrayList<PatientInfection> infections = new ArrayList<>();
        int reps = msg.getZIF().getInfectionReps();
        for (int i = 0; i < reps; i++) {
            Infection infectionSegment = msg.getZIF().getInfection(i);
            infections.add(buildPatientInfection(sourceId, evn, patientInfo, infectionSegment));
        }
        return infections;
    }

    private PatientInfection buildPatientInfection(
            String sourceId, EVN evn, PatientInfoHl7 patientInfo, Infection infectionSegment) throws HL7Exception {
        PatientInfection patientInfection = new PatientInfection();
        // generic information
        patientInfection.setSourceMessageId(sourceId);
        patientInfection.setSourceSystem(patientInfo.getSendingApplication());
        patientInfection.setMrn(patientInfo.getMrn());
        patientInfection.setUpdatedDateTime(HL7Utils.interpretLocalTime(evn.getEvn6_EventOccurred()));
        // patient infection information
        patientInfection.setInfection(infectionSegment.getInfection1Name().getValueOrEmpty());
        patientInfection.setInfectionAdded(HL7Utils.interpretLocalTime(infectionSegment.getInfection2AddedDateTime()));
        Instant infectionResolved = HL7Utils.interpretLocalTime(infectionSegment.getInfection3ResolvedDateTime());
        patientInfection.setInfectionResolved(InterchangeValue.buildFromHl7(infectionResolved));
        return patientInfection;
    }
}
