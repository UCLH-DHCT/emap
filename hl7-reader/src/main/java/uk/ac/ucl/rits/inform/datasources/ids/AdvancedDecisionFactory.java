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
import uk.ac.ucl.rits.inform.interchange.AdvancedDecisionMessage;

import java.util.Collection;
import java.util.regex.Pattern;

/**
 * Parses Advanced Decisions to interchange messages.
 * @author Anika Cawthorn
 */
@Component
public class AdvancedDecisionFactory {
    private static final String QUESTION_SEPARATOR = "->";
    private static final Pattern QUESTION_PATTERN = Pattern.compile(QUESTION_SEPARATOR);
    private static final String CANCELLATION_OCID = "OC";
    private static final String AUTOMATED_FROM_DISCHARGE = "DISCHAUTO";

    /**
     * @param sourceId source Id from sending application
     * @param ormO01   ORM O01 message
     * @return advanced decision
     * @throws HL7Exception              if HAPI does
     * @throws Hl7InconsistencyException if there isn't exactly one consult request in the message
     */
    AdvancedDecisionMessage makeAdvancedDecision(String sourceId, ORM_O01 ormO01) throws HL7Exception, Hl7InconsistencyException {
        if (ormO01.getORDERReps() != 1) {
            throw new Hl7InconsistencyException("Advanced decision should have always only one advanced decision");
        }
        PatientInfoHl7 patientInfo = new PatientInfoHl7(ormO01);
        AdvancedDecisionMessage advancedDecision = new AdvancedDecisionMessage(
                sourceId, patientInfo.getSendingApplication(), patientInfo.getMrn(), patientInfo.getVisitNumber());

        ORM_O01_ORDER order = ormO01.getORDER();
        addCancelledOrClosed(advancedDecision, order, patientInfo);
        addRequestInformation(advancedDecision, order);
        addQuestions(advancedDecision, order.getORDER_DETAIL().getNTEAll());
        return advancedDecision;
    }

    private void addRequestInformation(AdvancedDecisionMessage advancedDecisionMessage, ORM_O01_ORDER order)
            throws HL7Exception {
        ORC orc = order.getORC();
        OBR obr = order.getORDER_DETAIL().getOBR();
        advancedDecisionMessage.setAdvancedDecisionNumber(Long.decode(orc.getOrc2_PlacerOrderNumber().getEi1_EntityIdentifier().getValueOrEmpty()));
        advancedDecisionMessage.setStatusChangeTime(HL7Utils.interpretLocalTime(orc.getOrc9_DateTimeOfTransaction()));
        advancedDecisionMessage.setRequestedDateTime(HL7Utils.interpretLocalTime(obr.getObr36_ScheduledDateTime()));
        advancedDecisionMessage.setAdvancedCareCode(obr.getObr44_ProcedureCode().encode());
        advancedDecisionMessage.setAdvancedDecisionTypeName(obr.getObr4_UniversalServiceIdentifier().getCwe5_AlternateText().getValueOrEmpty());
    }

    private void addQuestions(AdvancedDecisionMessage advancedDecisionMessage, Collection<NTE> notes)
            throws Hl7InconsistencyException {
        NotesParser parser = new NotesParser(notes, QUESTION_SEPARATOR, QUESTION_PATTERN);
        advancedDecisionMessage.setQuestions(parser.getQuestions());
    }

    private void addCancelledOrClosed(AdvancedDecisionMessage advancedDecisionMessage, ORM_O01_ORDER order,
                                      PatientInfoHl7 patientInfo) {
        if (isOrderCancelled(order)) {
            if (isFromAutomatedDischarge(patientInfo)) {
                advancedDecisionMessage.setClosedDueToDischarge(true);
            } else {
                advancedDecisionMessage.setCancelled(true);
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
