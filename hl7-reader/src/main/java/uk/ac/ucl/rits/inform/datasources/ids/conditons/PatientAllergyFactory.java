package uk.ac.ucl.rits.inform.datasources.ids.conditons;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.v26.datatype.ST;
import ca.uhn.hl7v2.model.v26.segment.EVN;
import ca.uhn.hl7v2.model.v26.segment.MSH;
import ca.uhn.hl7v2.model.v26.segment.PID;
import ca.uhn.hl7v2.model.v26.segment.PV1;
import ca.uhn.hl7v2.model.v26.segment.IAM;
import ca.uhn.hl7v2.model.v26.message.ADT_A60;

import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import uk.ac.ucl.rits.inform.datasources.ids.HL7Utils;
import uk.ac.ucl.rits.inform.datasources.ids.hl7.parser.PatientInfoHl7;
import uk.ac.ucl.rits.inform.interchange.ConditionAction;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.PatientAllergy;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Factory to generate allergy interchange messages from HL7 (i.e. messages with type ADT^A60).
 * @author Anika Cawthorn
 * @author Tom Young
 */
@NoArgsConstructor
@Component
public class PatientAllergyFactory {

    /**
     * Build patient allergies from ADT_A60 HL7 message.
     * Allergies with no added datetime, or allergies where the added time is before the current progress will be skipped.
     * @param sourceId message sourceId
     * @param msg      hl7 message
     * @return list of patient infections
     * @throws HL7Exception if message cannot be parsed correctly
     */
    public Collection<PatientAllergy> buildPatientAllergies(String sourceId, ADT_A60 msg) throws HL7Exception {
        MSH msh = msg.getMSH();
        PID pid = msg.getPID();
        PV1 pv1 = msg.getPV1();
        EVN evn = msg.getEVN();
        PatientInfoHl7 patientInfo = new PatientInfoHl7(msh, pid, pv1);
        int reps = msg.getIAMReps();
        Collection<PatientAllergy> allergies = new ArrayList<>(reps);
        for (int i = 0; i < reps; i++) {
            IAM allergySegment = msg.getIAM(i);
            PatientAllergy patientAllergy = buildPatientAllergy(sourceId, evn, patientInfo, allergySegment);
            allergies.add(patientAllergy);
        }
        return allergies;
    }

    /**
     * Build patient allergy from one of the IAM segments of the message.
     * @param sourceId    message sourceId
     * @param evn         segment of the message relating to the specific trigger event of the message
     * @param patientInfo segment of the message relating to patient information
     * @param iam         hl7 message
     * @return A single patient allergy representative for one of the IAM segments in the message
     * @throws HL7Exception if message cannot be parsed correctly.
     */
    private PatientAllergy buildPatientAllergy(String sourceId, EVN evn, PatientInfoHl7 patientInfo, IAM iam) throws HL7Exception {

        PatientAllergy patientAllergy = new PatientAllergy();

        // generic information
        patientAllergy.setSourceMessageId(sourceId);
        patientAllergy.setSourceSystem(patientInfo.getSendingApplication());
        patientAllergy.setMrn(patientInfo.getMrn());
        patientAllergy.setUpdatedDateTime(HL7Utils.interpretLocalTime(evn.getEvn2_RecordedDateTime()));

        var allergyAction = iam.getIam6_AllergyActionCode().getCne1_Identifier().getValueOrEmpty();
        switch (allergyAction) {
            case "A":
                patientAllergy.setAction(ConditionAction.ADD);
                break;
            case "D":
                patientAllergy.setAction(ConditionAction.DELETE);
                break;
            case "U":  // Is an explicit "update"
            case "X":  // or a "no change"
                patientAllergy.setAction(ConditionAction.UPDATE);
                break;
            default:
                throw new HL7Exception("Failed to set the action from " + allergyAction);
        }

        // allergy specific information
        var allergyStatus = iam.getAllergyClinicalStatusCode().getCwe1_Identifier().getValueOrEmpty();
        patientAllergy.setStatus(InterchangeValue.buildFromHl7(allergyStatus));
        var allergyId = iam.getIam7_AllergyUniqueIdentifier().getEntityIdentifier().getValueOrEmpty();
        patientAllergy.setEpicConditionId(InterchangeValue.buildFromHl7(Long.valueOf(allergyId)));
        patientAllergy.setOnsetDate(InterchangeValue.buildFromHl7(HL7Utils.interpretDate(iam.getIam11_OnsetDate())));
        patientAllergy.setSubType(InterchangeValue.buildFromHl7(iam.getAllergenTypeCode().getCwe1_Identifier().getValueOrEmpty()));
        var allergyCode = iam.getAllergenCodeMnemonicDescription().getText().getValueOrEmpty();
        patientAllergy.setConditionCode(allergyCode);
        patientAllergy.setConditionName(InterchangeValue.buildFromHl7(allergyCode));
        patientAllergy.setAddedDatetime(HL7Utils.interpretLocalTime(iam.getReportedDateTime()));
        patientAllergy.setSeverity(InterchangeValue.buildFromHl7(iam.getAllergySeverityCode().getCwe1_Identifier().getValue()));

        // add reactions of which there can be multiple
        for (ST reactionCode : iam.getIam5_AllergyReactionCode()) {
            if (!reactionCode.isEmpty()) {
                patientAllergy.getReactions().add(reactionCode.getValue());
            }
        }
        return patientAllergy;
    }
}
