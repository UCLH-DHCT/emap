package uk.ac.ucl.rits.inform.datasources.ids;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.v26.message.ORM_O01;
import ca.uhn.hl7v2.model.v26.message.ORR_O02;
import ca.uhn.hl7v2.model.v26.message.ORU_R01;
import ca.uhn.hl7v2.model.v26.message.ORU_R30;
import ca.uhn.hl7v2.model.v26.segment.MSH;
import ca.uhn.hl7v2.model.v26.segment.OBR;
import org.springframework.stereotype.Component;
import uk.ac.ucl.rits.inform.datasources.ids.exceptions.Hl7InconsistencyException;
import uk.ac.ucl.rits.inform.datasources.ids.exceptions.Hl7MessageIgnoredException;
import uk.ac.ucl.rits.inform.datasources.ids.labs.LabParser;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessage;
import uk.ac.ucl.rits.inform.interchange.OrderCodingSystem;

import java.util.Collection;

/**
 * Decides what type of order or result and sends messages to the correct class and method.
 * <p>
 * Determines the correct class to build messages from (Flowsheets, Labs, Bloods...)
 * If required, determines the coding system to be used by the building class.
 * @author Stef Piatek
 */
@Component
public class OrderAndResultService {
    private FlowsheetFactory flowsheetFactory;

    public OrderAndResultService(FlowsheetFactory flowsheetFactory) {
        this.flowsheetFactory = flowsheetFactory;
    }

    Collection<? extends EmapOperationMessage> buildMessages(String sourceId, ORU_R01 msg)
            throws Hl7MessageIgnoredException, Hl7InconsistencyException, HL7Exception {
        MSH msh = msg.getMSH();
        String sendingApplication = msh.getMsh3_SendingApplication().getHd1_NamespaceID().getValueOrEmpty();
        String sendingFacility = msh.getMsh4_SendingFacility().getHd1_NamespaceID().getValueOrEmpty();

        if ("Vitals".equals(sendingFacility)) {
            return flowsheetFactory.getMessages(sourceId, msg);
        }

        OBR obr = msg.getPATIENT_RESULT().getORDER_OBSERVATION().getOBR();
        OrderCodingSystem codingSystem = determineCodingSystem(obr, sendingApplication);
        if (OrderCodingSystem.BLOOD_PRODUCTS == codingSystem) {
            throw new Hl7MessageIgnoredException("Bank Manager products not implemented for now");
        }
        return LabParser.buildMessages(sourceId, msg, codingSystem);
    }

    Collection<? extends EmapOperationMessage> buildMessages(String sourceId, ORU_R30 msg)
            throws Hl7MessageIgnoredException, Hl7InconsistencyException, HL7Exception {
        return LabParser.buildMessages(sourceId, msg);
    }

    Collection<? extends EmapOperationMessage> buildMessages(String sourceId, ORR_O02 msg) throws Hl7MessageIgnoredException {
        throw new Hl7MessageIgnoredException("WinPath ORR message not implemented for now");
    }

    Collection<? extends EmapOperationMessage> buildMessages(String sourceId, ORM_O01 msg)
            throws Hl7InconsistencyException, HL7Exception {
        OBR obr = msg.getORDER().getORDER_DETAIL().getOBR();
        OrderCodingSystem codingSystem = determineCodingSystem(obr);

        return LabParser.buildLabOrders(sourceId, msg, codingSystem);
    }

    private OrderCodingSystem determineCodingSystem(OBR obr) {
        return determineCodingSystem(obr, "");
    }


    private OrderCodingSystem determineCodingSystem(OBR obr, String sendingApplication) {
        String fillerId = obr.getObr3_FillerOrderNumber().getEi3_UniversalID().getValueOrEmpty();
        String codingSystem = obr.getObr4_UniversalServiceIdentifier().getCwe3_NameOfCodingSystem().getValueOrEmpty();
        String alternativeIdentifier = obr.getObr4_UniversalServiceIdentifier().getCwe4_AlternateIdentifier().getValueOrEmpty();

        if ("WinPath".equals(codingSystem)) {
            return OrderCodingSystem.WIN_PATH;
        } else if ("CoPathPlus".equals(fillerId) || "CPEAP".equals(codingSystem)) {
            return OrderCodingSystem.CO_PATH;
        } else if ("Profiles".equals(alternativeIdentifier)) {
            return OrderCodingSystem.BANK_MANAGER;
        } else if ("Products".equals(alternativeIdentifier)) {
            return OrderCodingSystem.BLOOD_PRODUCTS;
        } else if ("BIO-CONNECT".equals(sendingApplication)) {
            return OrderCodingSystem.BIO_CONNECT;
        }
        return OrderCodingSystem.UNKNOWN;
    }

}
