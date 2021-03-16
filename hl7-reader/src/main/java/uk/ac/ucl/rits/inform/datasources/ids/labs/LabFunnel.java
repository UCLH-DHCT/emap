package uk.ac.ucl.rits.inform.datasources.ids.labs;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.v26.message.ORM_O01;
import ca.uhn.hl7v2.model.v26.message.ORR_O02;
import ca.uhn.hl7v2.model.v26.message.ORU_R01;
import ca.uhn.hl7v2.model.v26.message.ORU_R30;
import uk.ac.ucl.rits.inform.datasources.ids.exceptions.Hl7InconsistencyException;
import uk.ac.ucl.rits.inform.datasources.ids.exceptions.Hl7MessageIgnoredException;
import uk.ac.ucl.rits.inform.interchange.OrderCodingSystem;
import uk.ac.ucl.rits.inform.interchange.lab.LabOrderMsg;

import java.util.Collection;
import java.util.List;

/**
 * Determines which Lab Order builder subclass and builder method should be called.
 * @author Stef Piatek
 */
public class LabFunnel {
    private LabFunnel() {
    }

    /**
     * Several orders for one patient can exist in the same message, so make one object for each.
     * @param idsUnid      unique Id from the IDS
     * @param ormO01       the ORM message
     * @param codingSystem coding system for lab order
     * @return list of LabOrder orders, one for each order
     * @throws HL7Exception               if HAPI does
     * @throws Hl7InconsistencyException  if something about the HL7 message doesn't make sense
     * @throws Hl7MessageIgnoredException if coding sysstem not implemented
     */
    public static List<LabOrderMsg> buildMessages(String idsUnid, ORM_O01 ormO01, OrderCodingSystem codingSystem)
            throws HL7Exception, Hl7InconsistencyException, Hl7MessageIgnoredException {
        switch (codingSystem) {
            case WIN_PATH:
                return WinPathLabBuilder.build(idsUnid, ormO01, codingSystem);
            case CO_PATH:
                throw new Hl7MessageIgnoredException("Not parsing CoPath ORM^O01 messages");
            default:
                throw new Hl7MessageIgnoredException("Coding system for ORM^O01 not recognised");
        }
    }

    /**
     * Build lab orders from ORU R30 message.
     * @param idsUnid      unique Id from the IDS
     * @param oruR30       the Hl7 message
     * @param codingSystem coding system
     * @return single lab order in a list
     * @throws HL7Exception               if HAPI does
     * @throws Hl7MessageIgnoredException if it's a calibration or testing message
     * @throws Hl7InconsistencyException  if hl7 message is malformed
     */
    public static Collection<LabOrderMsg> buildMessages(String idsUnid, ORU_R30 oruR30, OrderCodingSystem codingSystem)
            throws HL7Exception, Hl7MessageIgnoredException, Hl7InconsistencyException {
        if (codingSystem == OrderCodingSystem.ABL90_FLEX_PLUS) {
            return AblLabBuilder.build(idsUnid, oruR30, codingSystem);
        }
        throw new Hl7MessageIgnoredException("Coding system for ORU^R30 not recognised");
    }


    /**
     * Several sets of results can exist in an ORU message, so build multiple LabOrder objects.
     * @param idsUnid      unique Id from the IDS
     * @param oruR01       the HL7 message
     * @param codingSystem coding system used by lab result
     * @return a list of LabOrder messages built from the results message
     * @throws HL7Exception               if HAPI does
     * @throws Hl7InconsistencyException  if, according to my understanding, the HL7 message contains errors
     * @throws Hl7MessageIgnoredException if coding system doesn't match known coding systems for ORU^R01
     */
    public static Collection<LabOrderMsg> buildMessages(String idsUnid, ORU_R01 oruR01, OrderCodingSystem codingSystem)
            throws HL7Exception, Hl7InconsistencyException, Hl7MessageIgnoredException {
        switch (codingSystem) {
            case WIN_PATH:
                return WinPathLabBuilder.build(idsUnid, oruR01, codingSystem);
            case CO_PATH:
                throw new Hl7MessageIgnoredException("CoPath lab results not implemented for now");
            case BANK_MANAGER:
                throw new Hl7MessageIgnoredException("Bank Manager lab results not implemented for now");
            case BIO_CONNECT:
                return BioConnectLabBuilder.build(idsUnid, oruR01, codingSystem);
            default:
                throw new Hl7MessageIgnoredException("Coding system for ORU^R01 not recognised");
        }
    }

    public static Collection<LabOrderMsg> buildMessages(String idsUnid, ORR_O02 msg, OrderCodingSystem codingSystem)
            throws HL7Exception, Hl7InconsistencyException, Hl7MessageIgnoredException {
        switch (codingSystem) {
            case WIN_PATH:
                return WinPathLabBuilder.build(idsUnid, msg, codingSystem);
            case CO_PATH:
                throw new Hl7MessageIgnoredException("CoPath ORR^O02 not implemented yet");
            default:
                throw new Hl7MessageIgnoredException("Coding system for ORR^O02 not recognised");
        }
    }
}
