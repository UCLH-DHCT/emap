package uk.ac.ucl.rits.inform.datasources.ids.labs;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.v26.datatype.CWE;
import ca.uhn.hl7v2.model.v26.datatype.PRL;
import ca.uhn.hl7v2.model.v26.group.ORU_R30_OBSERVATION;
import ca.uhn.hl7v2.model.v26.message.ORM_O01;
import ca.uhn.hl7v2.model.v26.message.ORR_O02;
import ca.uhn.hl7v2.model.v26.message.ORU_R01;
import ca.uhn.hl7v2.model.v26.message.ORU_R30;
import ca.uhn.hl7v2.model.v26.segment.MSH;
import ca.uhn.hl7v2.model.v26.segment.NTE;
import ca.uhn.hl7v2.model.v26.segment.OBR;
import ca.uhn.hl7v2.model.v26.segment.OBX;
import ca.uhn.hl7v2.model.v26.segment.PID;
import ca.uhn.hl7v2.model.v26.segment.PV1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ucl.rits.inform.datasources.ids.HL7Utils;
import uk.ac.ucl.rits.inform.datasources.ids.exceptions.Hl7InconsistencyException;
import uk.ac.ucl.rits.inform.datasources.ids.exceptions.Hl7MessageIgnoredException;
import uk.ac.ucl.rits.inform.datasources.ids.hl7parser.PatientInfoHl7;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.OrderCodingSystem;
import uk.ac.ucl.rits.inform.interchange.lab.LabOrderMsg;
import uk.ac.ucl.rits.inform.interchange.lab.LabResultMsg;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Build one or more LabOrder object(s) from an HL7 message.
 * @author Jeremy Stein
 * @author Stef Piatek
 */
public final class LabParser {

    private static final Logger logger = LoggerFactory.getLogger(LabParser.class);

    private String epicCareOrderNumberOrc;
    private String epicCareOrderNumberObr;

    private final LabOrderMsg msg = new LabOrderMsg();

    /**
     * Construct parser from ABL ORU R30 message.
     * @param subMessageSourceId unique Id from the IDS
     * @param oruR30             ORU R30 message
     * @throws HL7Exception               if HAPI does
     * @throws Hl7MessageIgnoredException if it's a calibration or test message
     * @throws Hl7InconsistencyException  if hl7 message is incorrectly formed
     */
    private LabParser(String subMessageSourceId, ORU_R30 oruR30) throws HL7Exception, Hl7MessageIgnoredException, Hl7InconsistencyException {
        setSourceAndPatientIdentifiers(subMessageSourceId, oruR30.getMSH(), oruR30.getPID(), oruR30.getVISIT().getPV1());

        OBR obr = oruR30.getOBR();
        populateSpecimenTypeOrIgnoreMessage(obr);
        populateObrFields(obr);
        populateOrderInformation(obr);
        msg.setLabSpecimenNumber(obr.getObr3_FillerOrderNumber().getEi1_EntityIdentifier().getValueOrEmpty());
        msg.setTestBatteryCodingSystem(msg.getSourceSystem());

        List<ORU_R30_OBSERVATION> observations = oruR30.getOBSERVATIONAll();
        List<LabResultMsg> results = new ArrayList<>(observations.size());
        for (ORU_R30_OBSERVATION ob : observations) {
            OBX obx = ob.getOBX();
            List<NTE> notes = ob.getNTEAll();
            LabResultBuilder resultBuilder = new AblResultBuilder(obx, notes, msg.getSourceSystem());
            resultBuilder.constructMsg();
            results.add(resultBuilder.getMessage());
        }
        msg.setLabResultMsgs(results);

    }


    /**
     * Populate the sample type information for ABL 90 flex.
     * @param obr OBR segment
     * @throws Hl7MessageIgnoredException if testing/calibration reading
     */
    private void populateSpecimenTypeOrIgnoreMessage(OBR obr) throws Hl7MessageIgnoredException {
        String sampleType = obr.getObr15_SpecimenSource().getSps1_SpecimenSourceNameOrCode().getCwe1_Identifier().getValueOrEmpty();

        if ("Proficiency Testing".equals(sampleType)) {
            throw new Hl7MessageIgnoredException("Test/Calibration reading, skipping processing");
        }
        msg.setSpecimenType(sampleType);
    }

    /**
     * @return the underlying message we have now built
     */
    public LabOrderMsg getMessage() {
        return msg;
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
                return WinPathLabBuilder.build(idsUnid, ormO01);
            case CO_PATH:
                throw new Hl7MessageIgnoredException("Not parsing CoPath ORM^O01 messages");
            default:
                throw new Hl7MessageIgnoredException("Coding system for ORM^O01 not recognised");
        }
    }


    /**
     * Build lab orders from ORU R30 message (ABL 90 Flex).
     * @param idsUnid unique Id from the IDS
     * @param oruR30  the Hl7 message
     * @return single lab order in a list
     * @throws HL7Exception               if HAPI does
     * @throws Hl7MessageIgnoredException if it's a calibration or testing message
     * @throws Hl7InconsistencyException  if hl7 message is malformed
     */
    public static List<LabOrderMsg> buildMessages(String idsUnid, ORU_R30 oruR30)
            throws HL7Exception, Hl7MessageIgnoredException, Hl7InconsistencyException {
        List<LabOrderMsg> orders = new ArrayList<>(1);
        // skip message if it is "Proficiency Testing"
        LabOrderMsg labOrder = new LabParser(idsUnid, oruR30).msg;
        // only one observation per message
        orders.add(labOrder);
        return orders;
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
                throw new Hl7MessageIgnoredException("WinPath ORR^O02 not implemented yet");
            case CO_PATH:
                throw new Hl7MessageIgnoredException("CoPath ORR^O02 not implemented yet");
            default:
                throw new Hl7MessageIgnoredException("Coding system for ORR^O02 not recognised");
        }
    }


    /**
     * Set LabOrder message source information and patient/encounter identifiers.
     * @param subMessageSourceId unique Id from the IDS
     * @param msh                the MSH segment
     * @param pid                the PID segment
     * @param pv1                the PV1 segment
     * @throws HL7Exception if there is missing hl7 data
     */
    private void setSourceAndPatientIdentifiers(String subMessageSourceId, MSH msh, PID pid, PV1 pv1) throws HL7Exception {
        PatientInfoHl7 patientHl7 = new PatientInfoHl7(msh, pid, pv1);
        msg.setSourceMessageId(subMessageSourceId);
        msg.setSourceSystem(patientHl7.getSendingApplication());
        msg.setVisitNumber(patientHl7.getVisitNumber());
        msg.setMrn(patientHl7.getMrn());
    }

    /**
     * Populate order information from OBR segment (ABL 90 Flex).
     * @param obr OBR
     * @throws DataTypeException if HAPI does
     */
    private void populateOrderInformation(OBR obr) throws DataTypeException {
        Instant sampleReceived = HL7Utils.interpretLocalTime(obr.getObr14_SpecimenReceivedDateTime());
        msg.setSampleReceivedTime(InterchangeValue.buildFromHl7(sampleReceived));
        msg.setOrderDateTime(InterchangeValue.buildFromHl7(sampleReceived));
        msg.setStatusChangeTime(sampleReceived);
    }

    /**
     * Extract the fields found in the OBR segment, of which there is one of each per object.
     * @param obr the OBR segment
     * @throws DataTypeException         if HAPI does
     * @throws Hl7InconsistencyException If no collection time in message
     */
    private void populateObrFields(OBR obr) throws DataTypeException, Hl7InconsistencyException {
        // The first ORM message from Epic->WinPath is only sent when the label for the sample is printed,
        // which is the closest we get to a "collection" time. The actual collection will happen some point
        // afterwards, we can't really tell. That's why an order message contains a non blank collection time.
        // This field is consistent throughout the workflow.
        Instant collectionTime = HL7Utils.interpretLocalTime(obr.getObr7_ObservationDateTime());
        if (collectionTime == null) {
            throw new Hl7InconsistencyException("Collection time is required but missing");
        }
        msg.setCollectionDateTime(collectionTime);
        Instant requestedTime = HL7Utils.interpretLocalTime(obr.getObr6_RequestedDateTime());
        msg.setRequestedDateTime(InterchangeValue.buildFromHl7(requestedTime));
        msg.setLabDepartment(obr.getObr24_DiagnosticServSectID().getValueOrEmpty());
        String resultStatus = obr.getObr25_ResultStatus().getValueOrEmpty();
        msg.setResultStatus(resultStatus);

        epicCareOrderNumberObr = obr.getObr2_PlacerOrderNumber().getEi1_EntityIdentifier().getValueOrEmpty();

        // this is the "last updated" field for results as well as changing to order "in progress"
        msg.setStatusChangeTime(HL7Utils.interpretLocalTime(obr.getObr22_ResultsRptStatusChngDateTime()));

        String clinicalInformation = obr.getObr13_RelevantClinicalInformation().getValueOrEmpty();
        msg.setClinicalInformation(InterchangeValue.buildFromHl7(clinicalInformation));

        // identifies the battery of tests that has been performed/ordered (eg. FBC)
        CWE obr4 = obr.getObr4_UniversalServiceIdentifier();
        msg.setTestBatteryLocalCode(obr4.getCwe1_Identifier().getValueOrEmpty());
        msg.setTestBatteryCodingSystem(obr4.getCwe3_NameOfCodingSystem().getValueOrEmpty());

        PRL parent = obr.getObr26_ParentResult();

        // eg. "ISOLATE"
        // match to OBX-3.1
        msg.setParentObservationIdentifier(parent.getPrl1_ParentObservationIdentifier().getCwe1_Identifier().getValueOrEmpty());

        // match to OBX-4
        msg.setParentSubId(parent.getPrl2_ParentObservationSubIdentifier().getValueOrEmpty());
    }

}
