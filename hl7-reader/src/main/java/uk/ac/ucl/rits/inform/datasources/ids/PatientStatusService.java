package uk.ac.ucl.rits.inform.datasources.ids;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.v26.segment.MSH;
import ca.uhn.hl7v2.model.v26.segment.PID;
import ca.uhn.hl7v2.model.v26.segment.PV1;
import org.springframework.stereotype.Component;
import uk.ac.ucl.rits.inform.datasources.ids.hl7.custom.v26.field.Infection;
import uk.ac.ucl.rits.inform.datasources.ids.hl7.custom.v26.message.ADT_A05;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessage;
import uk.ac.ucl.rits.inform.interchange.PatientInfection;

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

        ArrayList<PatientInfection> infections = new ArrayList<>();
        int reps = msg.getZIF().getInfectionReps();
        for (int i = 0; i < reps; i++) {
            Infection infectionSegment = msg.getZIF().getInfection(i);
            PatientInfection patientInfection = new PatientInfection();
            patientInfection.setMrn(pid.getPid3_PatientIdentifierList());
            patientInfection.setInfection(infectionSegment.getInfection1Name().getValueOrEmpty());

        }
        return infections;
    }
}
