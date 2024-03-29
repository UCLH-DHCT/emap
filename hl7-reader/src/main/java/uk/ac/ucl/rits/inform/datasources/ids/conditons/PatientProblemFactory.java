package uk.ac.ucl.rits.inform.datasources.ids.conditons;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.v26.datatype.CWE;
import ca.uhn.hl7v2.model.v26.datatype.DTM;
import ca.uhn.hl7v2.model.v26.message.PPR_PC1;
import ca.uhn.hl7v2.model.v26.segment.MSH;
import ca.uhn.hl7v2.model.v26.segment.PID;
import ca.uhn.hl7v2.model.v26.segment.PRB;
import ca.uhn.hl7v2.model.v26.segment.PV1;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;
import uk.ac.ucl.rits.inform.datasources.ids.HL7Utils;
import uk.ac.ucl.rits.inform.datasources.ids.hl7.parser.PatientInfoHl7;
import uk.ac.ucl.rits.inform.interchange.ConditionAction;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.PatientProblem;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;

@Component
@NoArgsConstructor
public class PatientProblemFactory {

    /**
     * Build patient problems from message.
     * As a problem message can have multiple PRB segments, these are individually processed.
     * @param sourceId message sourceId
     * @param msg      hl7 message
     * @return list of patient problems
     * @throws HL7Exception if a parsing problem occurs
     */
    public Collection<PatientProblem> buildPatientProblems(String sourceId, PPR_PC1 msg) throws HL7Exception {
        MSH msh = msg.getMSH();
        PID pid = msg.getPID();
        PV1 pv1 = msg.getPATIENT_VISIT().getPV1();

        PatientInfoHl7 patientInfo = new PatientInfoHl7(msh, pid, pv1);
        int reps = msg.getPROBLEMReps();
        Collection<PatientProblem> problems = new ArrayList<>(reps);
        for (int i = 0; i < reps; i++) {
            String comment = "";
            for (int j = 0; j < msg.getPROBLEM(i).getNTEReps(); j++) {
                for (int k = 0; k < msg.getPROBLEM(i).getNTE(j).getCommentReps(); k++) {
                    comment += (" " + msg.getPROBLEM(i).getNTE(j).getComment(k).getValueOrEmpty());
                }
            }
            PatientProblem patientProblem = buildPatientProblem(sourceId, patientInfo, msg.getPROBLEM(i).getPRB());
            patientProblem.setComment(InterchangeValue.buildFromHl7(comment));
            problems.add(patientProblem);
        }
        return problems;
    }

    /**
     * Turns a single PRB segment into a problem message interchange message.
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
        patientProblem.setVisitNumber(InterchangeValue.buildFromHl7(patientInfo.getVisitNumberFromPv1orPID()));

        // problem list specific information
        patientProblem.setAction(ConditionAction.findByHl7Value(problemSegment.getPrb1_ActionCode().getValueOrEmpty()));
        patientProblem.setUpdatedDateTime(HL7Utils.interpretLocalTime(problemSegment.getPrb2_ActionDateTime()));
        CWE conditionType = problemSegment.getPrb3_ProblemID();
        patientProblem.setConditionCode(conditionType.getCwe1_Identifier().getValueOrEmpty());
        patientProblem.setConditionName(InterchangeValue.buildFromHl7(conditionType.getCwe2_Text().getValueOrEmpty()));
        patientProblem.setAddedDate(HL7Utils.interpretDate(problemSegment.getPrb7_ProblemEstablishedDateTime()));
        LocalDate problemResolved = HL7Utils.interpretDate(problemSegment.getPrb9_ActualProblemResolutionDateTime());
        patientProblem.setResolvedDate(problemResolved);
        String problemStatus = problemSegment.getPrb13_ProblemConfirmationStatus().getCwe1_Identifier().getValueOrEmpty();
        patientProblem.setStatus(InterchangeValue.buildFromHl7(problemStatus));
        String problemId = problemSegment.getPrb4_ProblemInstanceID().getEntityIdentifier().getValueOrEmpty();
        patientProblem.setEpicConditionId(InterchangeValue.buildFromHl7(Long.valueOf(problemId)));
        DTM problemOnset = problemSegment.getPrb16_ProblemDateOfOnset();
        if (problemOnset.getValue() != null) {
            patientProblem.setOnsetDate(InterchangeValue.buildFromHl7(HL7Utils.interpretDate(problemOnset)));
        }

        return patientProblem;
    }
}
