package uk.ac.ucl.rits.inform.datasources.ids.labs;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.ExtraComponents;
import ca.uhn.hl7v2.model.Type;
import ca.uhn.hl7v2.model.v26.datatype.CWE;
import ca.uhn.hl7v2.model.v26.datatype.FT;
import ca.uhn.hl7v2.model.v26.datatype.PRL;
import ca.uhn.hl7v2.model.v26.datatype.ST;
import ca.uhn.hl7v2.model.v26.datatype.TX;
import ca.uhn.hl7v2.model.v26.segment.NTE;
import ca.uhn.hl7v2.model.v26.segment.OBR;
import ca.uhn.hl7v2.model.v26.segment.ORC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ucl.rits.inform.datasources.ids.HL7Utils;
import uk.ac.ucl.rits.inform.datasources.ids.exceptions.Hl7InconsistencyException;
import uk.ac.ucl.rits.inform.datasources.ids.hl7parser.PatientInfoHl7;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.OrderCodingSystem;
import uk.ac.ucl.rits.inform.interchange.lab.LabOrderMsg;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static uk.ac.ucl.rits.inform.datasources.ids.HL7Utils.interpretLocalTime;

abstract class LabOrderBuilder {
    private static final Logger logger = LoggerFactory.getLogger(LabOrderBuilder.class);

    private final Collection<String> allowedOcIds;
    private String epicCareOrderNumberOrc;
    private String epicCareOrderNumberObr;
    private final OrderCodingSystem codingSystem;

    private final LabOrderMsg msg = new LabOrderMsg();

    /**
     * @param allowedOcIds Allowed order control Ids
     * @param codingSystem Coding system to use
     */
    LabOrderBuilder(String[] allowedOcIds, OrderCodingSystem codingSystem) {
        this.allowedOcIds = Set.of(allowedOcIds);
        this.codingSystem = codingSystem;
    }

    /**
     * @return Lab Order Msg.
     */
    public LabOrderMsg getMsg() {
        return msg;
    }

    /**
     * @return order coding system.
     */
    OrderCodingSystem getCodingSystem() {
        return codingSystem;
    }

    /**
     * @return ORC epic order number
     */
    String getEpicCareOrderNumberOrc() {
        return epicCareOrderNumberOrc;
    }

    /**
     * @return OBR epic order number
     */
    String getEpicCareOrderNumberObr() {
        return epicCareOrderNumberObr;
    }

    /**
     * Extract the fields found in the ORC segment (some context from OBR required), of which there is one of each per object.
     * @param orc the ORC segment
     * @param obr the OBR segment
     * @throws DataTypeException if HAPI does
     * @throws Hl7InconsistencyException if HL7 doesn't meet expected structure
     */
    void populateOrderInformation(ORC orc, OBR obr) throws DataTypeException, Hl7InconsistencyException {
        // NA/NW/CA/CR/OC/XO
        msg.setOrderControlId(orc.getOrc1_OrderControl().getValue());
        epicCareOrderNumberOrc = orc.getOrc2_PlacerOrderNumber().getEi1_EntityIdentifier().getValueOrEmpty();
        setLabSpecimenNumber(orc);
        setSpecimenTypeAndCollectionMethod(obr);
        msg.setOrderStatus(orc.getOrc5_OrderStatus().getValueOrEmpty());


        // ORC-9 has different meanings depending on message context
        Instant orc9 = interpretLocalTime(orc.getOrc9_DateTimeOfTransaction());
        switch (msg.getOrderControlId()) {
            case "NW":
            case "SN":
                msg.setOrderDateTime(InterchangeValue.buildFromHl7(orc9));
                msg.setStatusChangeTime(orc9);
                break;
            case "NA":
            case "CR":
            case "CA":
            case "OC":
                msg.setStatusChangeTime(orc9);
                break;
            case "SC":
                if ("I".equals(obr.getObr25_ResultStatus().getValueOrEmpty())) {
                    // ORC-9 = time sample entered onto WinPath
                    msg.setSampleReceivedTime(InterchangeValue.buildFromHl7(orc9));
                }
                if (msg.getStatusChangeTime() == null) {
                    msg.setStatusChangeTime(orc9);
                }
                break;
            default:
                break;
        }
    }

    /**
     * Set the specimen number from the ORC segment.
     * Each lab result that uses this appears to need a separate implementation of this.
     * @param orc ORC segment
     */
    protected abstract void setLabSpecimenNumber(ORC orc) throws Hl7InconsistencyException;


    void setBatteryCodingSystem() {
        msg.setTestBatteryCodingSystem(codingSystem.name());
    }

    private void setSpecimenTypeAndCollectionMethod(OBR obr) {
        String sampleType = obr.getObr15_SpecimenSource().getSps1_SpecimenSourceNameOrCode().getCwe1_Identifier().getValueOrEmpty();
        msg.setSpecimenType(InterchangeValue.buildFromHl7(sampleType));
        setCollectionMethods(obr.getObr15_SpecimenSource().getSps3_SpecimenCollectionMethod());
    }

    /**
     * Set collection method, adding comma separated extra components if they exist.
     * @param collectionField collection method field
     */
    private void setCollectionMethods(TX collectionField) {
        StringJoiner collectionMethods = new StringJoiner(", ");
        collectionMethods.add(collectionField.getValueOrEmpty());

        ExtraComponents extraComponents = collectionField.getExtraComponents();
        for (int i = 0; i < collectionField.getExtraComponents().numComponents(); i++) {
            Type extraComponent = extraComponents.getComponent(i).getData();
            collectionMethods.add(extraComponent.toString());
        }
        msg.setCollectionMethod(InterchangeValue.buildFromHl7(collectionMethods.toString()));
    }

    /**
     * Set LabOrder message source information and patient/encounter identifiers.
     * @param subMessageSourceId unique Id from the IDS
     * @param patientHl7         patient hl7 info
     * @throws HL7Exception if there is missing hl7 data
     */
    void setSourceAndPatientIdentifiers(String subMessageSourceId, PatientInfoHl7 patientHl7) throws HL7Exception {
        msg.setSourceMessageId(subMessageSourceId);
        String sourceApplication = patientHl7.getSendingApplication().isEmpty() ? "Not in Message" : patientHl7.getSendingApplication();
        msg.setSourceSystem(sourceApplication);
        msg.setVisitNumber(patientHl7.getVisitNumber());
        msg.setMrn(patientHl7.getMrn());
    }

    /**
     * Populate order information from OBR segment (ABL 90 Flex).
     * @param obr OBR
     * @throws DataTypeException if HAPI does
     */
    void populateOrderInformation(OBR obr) throws DataTypeException {
        Instant sampleReceived = interpretLocalTime(obr.getObr14_SpecimenReceivedDateTime());
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
    void populateObrFields(OBR obr) throws DataTypeException, Hl7InconsistencyException {
        // The first ORM message from Epic->WinPath is only sent when the label for the sample is printed,
        // which is the closest we get to a "collection" time. The actual collection will happen some point
        // before or after this, we can't really tell. That's why an order message contains a non blank collection time.
        // This field is consistent throughout the workflow.
        Instant collectionTime = interpretLocalTime(obr.getObr7_ObservationDateTime());
        if (collectionTime == null) {
            throw new Hl7InconsistencyException("Collection time is required but missing");
        }
        msg.setCollectionDateTime(collectionTime);
        Instant requestedTime = interpretLocalTime(obr.getObr6_RequestedDateTime());
        msg.setRequestedDateTime(InterchangeValue.buildFromHl7(requestedTime));
        msg.setLabDepartment(obr.getObr24_DiagnosticServSectID().getValueOrEmpty());
        String resultStatus = obr.getObr25_ResultStatus().getValueOrEmpty();
        msg.setResultStatus(resultStatus);
        setSpecimenTypeAndCollectionMethod(obr);

        epicCareOrderNumberObr = obr.getObr2_PlacerOrderNumber().getEi1_EntityIdentifier().getValueOrEmpty();

        // this is the "last updated" field for results as well as changing to order "in progress"
        // Will be set from ORC if status change time is not in message type
        msg.setStatusChangeTime(HL7Utils.interpretLocalTime(obr.getObr22_ResultsRptStatusChngDateTime()));

        String reasonForStudy = List.of(obr.getObr31_ReasonForStudy()).stream()
                .map(CWE::getCwe2_Text)
                .map(ST::getValueOrEmpty)
                .collect(Collectors.joining("\n"))
                .strip();

        String clinicalInformation = obr.getObr13_RelevantClinicalInformation().getValueOrEmpty();
        msg.setClinicalInformation(InterchangeValue.buildFromHl7(clinicalInformation.isEmpty() ? reasonForStudy : clinicalInformation));

        // identifies the battery of tests that has been performed/ordered (eg. FBC)
        CWE obr4 = obr.getObr4_UniversalServiceIdentifier();
        msg.setTestBatteryLocalCode(obr4.getCwe1_Identifier().getValueOrEmpty());

        PRL parent = obr.getObr26_ParentResult();

        // For results with multiple parts, e.g. WinPath ISOLATES, CoPath results
        // matches OBX-3.1
        msg.setParentObservationIdentifier(parent.getPrl1_ParentObservationIdentifier().getCwe1_Identifier().getValueOrEmpty());
        // matches OBX-4
        msg.setParentSubId(parent.getPrl2_ParentObservationSubIdentifier().getValueOrEmpty());
    }


    protected void addMsgIfAllowedOcId(String idsUnid, List<LabOrderMsg> orders) {
        if (msg.getOrderControlId() != null && allowedOcIds.contains(msg.getOrderControlId())) {
            orders.add(msg);
        } else {
            logger.warn("Ignoring unid {} because order control ID not allowed '{}'", idsUnid, msg.getOrderControlId());
        }
    }

    /**
     * Set questions from notes.
     * @param notes             notes for an order.
     * @param questionSeparator to join the answer if it contains the question pattern
     * @param questionPattern   pattern between the question and answer
     */
    protected void setQuestions(Iterable<NTE> notes, final String questionSeparator, final Pattern questionPattern) {
        for (NTE note : notes) {
            StringBuilder questionAndAnswer = new StringBuilder();
            for (FT ft : note.getNte3_Comment()) {
                questionAndAnswer.append(ft.getValueOrEmpty()).append("\n");
            }
            String[] parts = questionPattern.split(questionAndAnswer.toString().strip());
            if (parts.length > 1) {
                String question = parts[0];
                // allow for separator to be in the answer
                String answer = String.join(questionSeparator, Arrays.copyOfRange(parts, 1, (parts.length)));
                getMsg().getQuestions().put(question, answer);
            }
        }
    }
}
