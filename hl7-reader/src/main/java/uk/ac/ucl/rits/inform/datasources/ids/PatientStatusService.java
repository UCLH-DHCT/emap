package uk.ac.ucl.rits.inform.datasources.ids;

import ca.uhn.hl7v2.HL7Exception;
import org.springframework.stereotype.Component;
import uk.ac.ucl.rits.inform.datasources.ids.customhl7.AdtA05Epic;
import uk.ac.ucl.rits.inform.datasources.ids.customhl7.Infection;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessage;
import uk.ac.ucl.rits.inform.interchange.PatientInfection;

import java.util.ArrayList;
import java.util.Collection;

@Component
public class PatientStatusService {
    public Collection<? extends EmapOperationMessage> buildMessages(String sourceId, AdtA05Epic msg) throws HL7Exception {
        return buildPatientInfections(msg);
    }

    private Collection<PatientInfection> buildPatientInfections(AdtA05Epic msg) throws HL7Exception {
        ArrayList<PatientInfection> infections = new ArrayList<>();
        int reps = msg.getZIF().getInfectionReps();
        Infection infection = msg.getZIF().getInfection(0);
        return infections;
    }
}
