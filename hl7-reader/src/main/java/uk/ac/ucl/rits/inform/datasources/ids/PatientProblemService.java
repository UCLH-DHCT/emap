package uk.ac.ucl.rits.inform.datasources.ids;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.v26.message.PPR_PC1;
import ca.uhn.hl7v2.model.v26.segment.EVN;
import ca.uhn.hl7v2.model.v26.segment.MSH;
import ca.uhn.hl7v2.model.v26.segment.PID;
import ca.uhn.hl7v2.model.v26.segment.PV1;
import ca.uhn.hl7v2.model.v26.segment.PRB;

import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.ac.ucl.rits.inform.datasources.ids.hl7.parser.PatientInfoHl7;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.PatientProblem;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;

@Component
public class PatientProblemService {
    /**
     * The HL7 feed always sends entire histories of patient problems.
     * This field is used to only parse new patient problems, from the service start date onwards.
     */
    @Setter
    private Instant problemListProgress;
    private static final Logger logger = LoggerFactory.getLogger(PatientProblemService.class);

    public PatientProblemService(@Value("${ids.cfg.default-start-datetime}") Instant serviceStart) {
        problemListProgress = serviceStart;
    }

    /**
     * Build patient problems from message. Problems with no added datetime, or problems where the added time is before
     * the current progress will be skipped. As a problem message can have multiple PRB segments, these are individually
     * processed.
     *
     * @param sourceId message sourceId
     * @param msg      hl7 message
     * @return list of patient problems
     * @throws HL7Exception
     */
    Collection<PatientProblem> buildPatientProblems(String sourceId, PPR_PC1 msg) throws HL7Exception {
        MSH msh = msg.getMSH();
        PID pid = msg.getPID();
        PV1 pv1 = msg.getPATIENT_VISIT().getPV1();

        PatientInfoHl7 patientInfo = new PatientInfoHl7(msh, pid, pv1);
        int reps = msg.getPROBLEMReps();
        Collection<PatientProblem> problems = new ArrayList<>(reps);
        for (int i = 0; i < reps; i++) {
            PatientProblem patientProblem = buildPatientProblem(sourceId, patientInfo, msg.getPROBLEM(i).getPRB());
            addNewInfectionAndUpdateProgress(patientProblem, problems);
        }
        return problems;
    }

    /**
     * Turns a single PRB segment into a problem message interchange message.
     * @param sourceId       the identifier given to the message in the source system
     * @param patientInfo    information about the patient the problem list belongs to
     * @param problemSegment the PRB segment that is parsed into an interchange message
     * @return a single patient problem
     * @throws HL7Exception
     */
    private PatientProblem buildPatientProblem(String sourceId, PatientInfoHl7 patientInfo, PRB problemSegment) throws HL7Exception {
        PatientProblem patientInfection = new PatientProblem();
        // generic information
        patientInfection.setSourceMessageId(sourceId);
        patientInfection.setSourceSystem(patientInfo.getSendingApplication());
        patientInfection.setMrn(patientInfo.getMrn());
        patientInfection.setUpdatedDateTime(HL7Utils.interpretLocalTime(evn.getEvn2_RecordedDateTime()));
        // patient infection information
        patientInfection.setInfectionCode(infectionSegment.getInfection1Name().getValueOrEmpty());
        patientInfection.setInfectionAdded(HL7Utils.interpretLocalTime(infectionSegment.getInfection2AddedDateTime()));
        Instant infectionResolved = HL7Utils.interpretLocalTime(infectionSegment.getInfection3ResolvedDateTime());
        patientInfection.setInfectionResolved(InterchangeValue.buildFromHl7(infectionResolved));
        return patientInfection;
    }

    /**
     * Checks whether patient problem information needs to be processed further based
     * @param patientProblem
     * @param problems
     */
    private void addNewInfectionAndUpdateProgress(PatientProblem patientProblem, Collection<PatientProblem> problems) {
        Instant problemAdded = patientProblem.getProblemAdded();
        if (problemAdded == null || problemAdded.isBefore(problemListProgress)) {
            logger.debug("Problem list processing skipped as current problem list added time is {} and progress is {}", infectionAdded, infectionProgress);
            return;
        }
        problems.add(patientProblem);
        problemListProgress = patientProblem.getProblemAdded();
    }
}
