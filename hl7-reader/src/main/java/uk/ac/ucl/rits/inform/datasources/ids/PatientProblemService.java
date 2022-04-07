package uk.ac.ucl.rits.inform.datasources.ids;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.v26.message.PPR_PC1;
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
     * @throws HL7Exception if a parsing problem occurs
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
            addNewProblemAndUpdateProgress(patientProblem, problems);
        }
        return problems;
    }

    /**
     * Turns a single PRB segment into a problem message interchange message.
     *
     * @param sourceId       the identifier given to the message in the source system
     * @param patientInfo    information about the patient the problem list belongs to
     * @param problemSegment the PRB segment that is parsed into an interchange message
     * @return a single patient problem
     * @throws HL7Exception if a parsing problem occurs
     */
    private PatientProblem buildPatientProblem(String sourceId, PatientInfoHl7 patientInfo, PRB problemSegment) throws HL7Exception {
        PatientProblem patientProblem = new PatientProblem();
        // generic information
        patientProblem.setSourceMessageId(sourceId);
        patientProblem.setSourceSystem(patientInfo.getSendingApplication());
        patientProblem.setMrn(patientInfo.getMrn());
        // problem list specific information
        patientProblem.setUpdatedDateTime(HL7Utils.interpretLocalTime(problemSegment.getActionDateTime()));
        patientProblem.setProblemCode(problemSegment.getPrb3_ProblemID().getCwe1_Identifier().getValueOrEmpty());
        patientProblem.setProblemAdded(HL7Utils.interpretLocalTime(problemSegment.getPrb7_ProblemEstablishedDateTime()));
        patientProblem.setProblemOnset(InterchangeValue.buildFromHl7(HL7Utils.interpretDate(problemSegment.getPrb16_ProblemDateOfOnset())));
        Instant problemResolved = HL7Utils.interpretLocalTime(problemSegment.getActualProblemResolutionDateTime());
        patientProblem.setProblemResolved(InterchangeValue.buildFromHl7(problemResolved));
        return patientProblem;
    }

    /**
     * Checks whether patient problem information needs to be processed further based on the timestamp.
     * @param patientProblem Patient problem potentially to be added to problem list (depending on timestamp)
     * @param problems       List of problems to which additional problem might be added
     */
    private void addNewProblemAndUpdateProgress(PatientProblem patientProblem, Collection<PatientProblem> problems) {
        Instant problemAdded = patientProblem.getProblemAdded();
        if (problemAdded == null || problemAdded.isBefore(problemListProgress)) {
            logger.debug("Problem list processing skipped as current problem list added time is {} and progress is {}",
                    problemAdded, problemListProgress);
            return;
        }
        problems.add(patientProblem);
        problemListProgress = patientProblem.getProblemAdded();
    }
}
