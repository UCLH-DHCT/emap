package uk.ac.ucl.rits.inform.datasources.ids;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.v26.group.ORM_O01_ORDER;
import ca.uhn.hl7v2.model.v26.message.ORM_O01;
import ca.uhn.hl7v2.model.v26.segment.NTE;
import ca.uhn.hl7v2.model.v26.segment.OBR;
import ca.uhn.hl7v2.model.v26.segment.ORC;
import org.springframework.stereotype.Component;
import uk.ac.ucl.rits.inform.datasources.ids.exceptions.Hl7InconsistencyException;
import uk.ac.ucl.rits.inform.datasources.ids.hl7.parser.NotesParser;
import uk.ac.ucl.rits.inform.datasources.ids.hl7.parser.PatientInfoHl7;
import uk.ac.ucl.rits.inform.interchange.AdvanceDecisionMessage;

import java.util.Collection;
import java.util.regex.Pattern;

/**
 * Parses advance decisions to interchange messages.
 * @author Anika Cawthorn
 */
@Component
public class AdvanceDecisionFactory {
    private static final String QUESTION_SEPARATOR = "->";
    private static final Pattern QUESTION_PATTERN = Pattern.compile(QUESTION_SEPARATOR);
    private static final String CANCELLATION_OCID = "OC";
    private static final String AUTOMATED_FROM_DISCHARGE = "DISCHAUTO";

    /**
     * @param sourceId source Id from sending application
     * @param ormO01   ORM O01 message
     * @return advance decision
     * @throws HL7Exception              if HAPI does
     * @throws Hl7InconsistencyException if there isn't exactly one advance decision in the message
     */
    AdvanceDecisionMessage makeAdvancedDecision(String sourceId, ORM_O01 ormO01) throws HL7Exception, Hl7InconsistencyException {
        if (ormO01.getORDERReps() != 1) {
            throw new Hl7InconsistencyException("Advance decision registration should always only have one advance decision");
        }
        PatientInfoHl7 patientInfo = new PatientInfoHl7(ormO01);
        AdvanceDecisionMessage advanceDecision = new AdvanceDecisionMessage(
                sourceId, patientInfo.getSendingApplication(), patientInfo.getMrn(), patientInfo.getVisitNumber());

        ORM_O01_ORDER order = ormO01.getORDER();
        addCancelledOrClosed(advanceDecision, order, patientInfo);
        addAdvancedDecisionInformation(advanceDecision, order);
        addQuestions(advanceDecision, order.getORDER_DETAIL().getNTEAll());
        return advanceDecision;
    }

    private void addAdvancedDecisionInformation(AdvanceDecisionMessage advanceDecisionMessage, ORM_O01_ORDER order)
            throws HL7Exception {
        ORC orc = order.getORC();
        OBR obr = order.getORDER_DETAIL().getOBR();
        advanceDecisionMessage.setAdvanceDecisionNumber(Long.decode(orc.getOrc2_PlacerOrderNumber().getEi1_EntityIdentifier().getValueOrEmpty()));
        advanceDecisionMessage.setStatusChangeTime(HL7Utils.interpretLocalTime(orc.getOrc9_DateTimeOfTransaction()));
        advanceDecisionMessage.setRequestedDatetime(HL7Utils.interpretLocalTime(obr.getObr36_ScheduledDateTime()));
        advanceDecisionMessage.setAdvanceCareCode(obr.getObr44_ProcedureCode().encode());
        advanceDecisionMessage.setAdvanceDecisionTypeName(obr.getObr4_UniversalServiceIdentifier().getCwe5_AlternateText().getValueOrEmpty());
    }

    private void addQuestions(AdvanceDecisionMessage advanceDecisionMessage, Collection<NTE> notes)
            throws Hl7InconsistencyException {
        NotesParser parser = new NotesParser(notes, QUESTION_SEPARATOR, QUESTION_PATTERN);
        advanceDecisionMessage.setQuestions(parser.getQuestions());
    }

    private void addCancelledOrClosed(AdvanceDecisionMessage advanceDecisionMessage, ORM_O01_ORDER order,
                                      PatientInfoHl7 patientInfo) {
        if (isOrderCancelled(order)) {
            if (isFromAutomatedDischarge(patientInfo)) {
                advanceDecisionMessage.setClosedDueToDischarge(true);
            } else {
                advanceDecisionMessage.setCancelled(true);
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
