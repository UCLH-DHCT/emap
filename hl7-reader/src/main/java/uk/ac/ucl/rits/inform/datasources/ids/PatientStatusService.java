package uk.ac.ucl.rits.inform.datasources.ids;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.v26.segment.EVN;
import ca.uhn.hl7v2.model.v26.segment.MSH;
import ca.uhn.hl7v2.model.v26.segment.PID;
import ca.uhn.hl7v2.model.v26.segment.PV1;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.ac.ucl.rits.inform.datasources.ids.hl7.custom.v26.field.Infection;
import uk.ac.ucl.rits.inform.datasources.ids.hl7.custom.v26.message.ADT_A05;
import uk.ac.ucl.rits.inform.datasources.ids.hl7.parser.PatientInfoHl7;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.PatientInfection;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;

@Component
public class PatientStatusService {
    /**
     * The HL7 feed always sends entire history of patient infections.
     * This field is used to only parse new patient infections, from the service start date onwards.
     */
    @Setter
    private Instant infectionProgress;
    private static final Logger logger = LoggerFactory.getLogger(PatientStatusService.class);

    public PatientStatusService(@Value("${ids.cfg.default-start-datetime}") Instant serviceStart) {
        infectionProgress = serviceStart;
    }

    /**
     * Build patient infections from message.
     * Infections with no added datetime, or infections where the added time time is before the current progress will be skipped
     * @param sourceId message sourceId
     * @param msg      hl7 message
     * @return list of patient infections
     * @throws HL7Exception
     */
    Collection<PatientInfection> buildPatientInfections(String sourceId, ADT_A05 msg) throws HL7Exception {
        MSH msh = msg.getMSH();
        PID pid = msg.getPID();
        PV1 pv1 = msg.getPV1();
        EVN evn = msg.getEVN();
        PatientInfoHl7 patientInfo = new PatientInfoHl7(msh, pid, pv1);
        int reps = msg.getZIF().getInfectionReps();
        Collection<PatientInfection> infections = new ArrayList<>(reps);
        for (int i = 0; i < reps; i++) {
            Infection infectionSegment = msg.getZIF().getInfection(i);
            PatientInfection patientInfection = buildPatientInfection(sourceId, evn, patientInfo, infectionSegment);
            addNewInfectionAndUpdateProgress(patientInfection, infections);
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
        patientInfection.setUpdatedDateTime(HL7Utils.interpretLocalTime(evn.getEvn2_RecordedDateTime()));
        // patient infection information
        patientInfection.setConditionCode(infectionSegment.getInfection1Name().getValueOrEmpty());
        patientInfection.setAddedTime(HL7Utils.interpretLocalTime(infectionSegment.getInfection2AddedDateTime()));
        Instant infectionResolved = HL7Utils.interpretLocalTime(infectionSegment.getInfection3ResolvedDateTime());
        patientInfection.setResolvedTime(InterchangeValue.buildFromHl7(infectionResolved));
        return patientInfection;
    }

    private void addNewInfectionAndUpdateProgress(PatientInfection patientInfection, Collection<PatientInfection> infections) {
        Instant infectionAdded = patientInfection.getAddedTime();
        if (infectionAdded == null || infectionAdded.isBefore(infectionProgress)) {
            logger.debug("Infection processing skipped as current infection added is {} and progress is {}", infectionAdded, infectionProgress);
            return;
        }
        infections.add(patientInfection);
        infectionProgress = patientInfection.getAddedTime();
    }
}
