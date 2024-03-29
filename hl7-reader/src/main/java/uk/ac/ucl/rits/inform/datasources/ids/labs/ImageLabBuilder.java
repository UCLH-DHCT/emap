package uk.ac.ucl.rits.inform.datasources.ids.labs;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.v26.group.ORU_R01_OBSERVATION;
import ca.uhn.hl7v2.model.v26.group.ORU_R01_ORDER_OBSERVATION;
import ca.uhn.hl7v2.model.v26.group.ORU_R01_PATIENT_RESULT;
import ca.uhn.hl7v2.model.v26.message.ORU_R01;
import ca.uhn.hl7v2.model.v26.segment.MSH;
import ca.uhn.hl7v2.model.v26.segment.NTE;
import ca.uhn.hl7v2.model.v26.segment.OBR;
import ca.uhn.hl7v2.model.v26.segment.OBX;
import ca.uhn.hl7v2.model.v26.segment.ORC;
import ca.uhn.hl7v2.model.v26.segment.PID;
import ca.uhn.hl7v2.model.v26.segment.PV1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ucl.rits.inform.datasources.ids.exceptions.Hl7InconsistencyException;
import uk.ac.ucl.rits.inform.datasources.ids.exceptions.Hl7MessageIgnoredException;
import uk.ac.ucl.rits.inform.datasources.ids.hl7.parser.PatientInfoHl7;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.OrderCodingSystem;
import uk.ac.ucl.rits.inform.interchange.lab.LabOrderMsg;
import uk.ac.ucl.rits.inform.interchange.lab.LabResultMsg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Build Imaging Lab Orders with results.
 * @author Stef Piatek
 */
public final class ImageLabBuilder extends LabOrderBuilder {
    /**
     * Allowed order control IDs for parsing.
     * <p>
     * ORU R01: RE (results)
     */
    private static final String[] ALLOWED_OC_IDS = {"RE"};
    private static final Logger logger = LoggerFactory.getLogger(ImageLabBuilder.class);
    private static final String QUESTION_SEPARATOR = "=";
    private static final Pattern QUESTION_PATTERN = Pattern.compile(QUESTION_SEPARATOR);
    private static final String NARRATIVE_CODE = "GDT";
    private static final String ADDENDA_CODE = "ADT";
    private static final String IMPRESSION_CODE = "IMP";
    private static final String SIGNATURE_CODE = "SIG";
    /**
     * OBX identifiers that will be used to build a report result.
     */
    private static final Map<String, String> RESULT_OBX_IDENTIFIERS = Map.of(
            ADDENDA_CODE, "ADDENDA", NARRATIVE_CODE, "NARRATIVE", IMPRESSION_CODE, "IMPRESSION", SIGNATURE_CODE, "SIGNATURE"
    );

    @Override
    protected void setLabSpecimenNumber(ORC orc) {
        String labFillerSpecimen = orc.getOrc3_FillerOrderNumber().getEi1_EntityIdentifier().getValueOrEmpty();
        String labPlacerSpecimen = orc.getOrc4_PlacerGroupNumber().getEi1_EntityIdentifier().getValueOrEmpty();
        getMsg().setLabSpecimenNumber(labFillerSpecimen.isEmpty() ? labPlacerSpecimen : labFillerSpecimen);
    }

    private void setOrderInformation(String subMessageSourceId, PatientInfoHl7 patientHl7, OBR obr, ORC orc, Collection<NTE> notes)
            throws HL7Exception, Hl7InconsistencyException {
        setBatteryCodingSystem();
        setSourceAndPatientIdentifiers(subMessageSourceId, patientHl7);
        setQuestions(notes, QUESTION_SEPARATOR, QUESTION_PATTERN);
        populateObrFields(obr, false);
        populateOrderInformation(orc, obr);
        setEpicOrderNumberFromORC();
    }

    private void setEpicOrderNumberFromORC() {
        String orcNumber = getEpicCareOrderNumberOrc();
        if (orcNumber.equals(getMsg().getLabSpecimenNumber())) {
            return;
        }

        getMsg().setEpicCareOrderNumber(InterchangeValue.buildFromHl7(orcNumber));
    }


    /**
     * Construct order details from a Image results (ORU^R01) message.
     * @param subMessageSourceId unique Id from the IDS
     * @param obs                the result group from HAPI (ORU_R01_ORDER_OBSERVATION)
     * @param patientHl7         patient hl7 info
     * @throws HL7Exception              if HAPI does
     * @throws Hl7InconsistencyException if, according to my understanding, the HL7 message contains errors
     */
    private ImageLabBuilder(
            String subMessageSourceId, ORU_R01_ORDER_OBSERVATION obs, PatientInfoHl7 patientHl7) throws HL7Exception, Hl7InconsistencyException {
        super(ALLOWED_OC_IDS, OrderCodingSystem.PACS);
        OBR obr = obs.getOBR();
        List<NTE> notes = obs.getNTEAll();
        setOrderInformation(subMessageSourceId, patientHl7, obr, obs.getORC(), notes);


        Map<String, List<OBX>> obxByIdentifier = getResultsByIdentifier(obs);

        List<LabResultMsg> results = new ArrayList<>(obs.getOBSERVATIONAll().size());
        for (Map.Entry<String, List<OBX>> entries : obxByIdentifier.entrySet()) {
            String identifier = entries.getKey();
            boolean isTextResult = RESULT_OBX_IDENTIFIERS.containsValue(identifier);
            ImageLabResultBuilder labResult = new ImageLabResultBuilder(isTextResult, identifier, entries.getValue(), obr);
            try {
                labResult.constructMsg();
                if (!labResult.isIgnored()) {
                    results.add(labResult.getMessage());
                }
            } catch (Hl7InconsistencyException e) {
                logger.error("HL7 inconsistency for message {}", subMessageSourceId, e);
            }
        }
        getMsg().setLabResultMsgs(results);
    }

    /**
     * Group OBX segments by identifier with human-readable names.
     * <p>
     * For text results we can receive ADDENDA (&ADT), NARRATIVE (&GDT) and IMPRESSIONs (&IMP).
     * To match with EPIC we do some further parsing, skipping some addenda and impression lines, and
     * any narrative lines after an impression should be skipped.
     * @param obs observations
     * @return OBX segments grouped by their identifier (using primitive identifier as a fallback)
     * @throws HL7Exception if HAPI does
     */
    private Map<String, List<OBX>> getResultsByIdentifier(ORU_R01_ORDER_OBSERVATION obs) throws HL7Exception {
        List<OBX> obxSegments = obs.getOBSERVATIONAll().stream().map(ORU_R01_OBSERVATION::getOBX).collect(Collectors.toList());
        int maximumNumberOfResults = obs.getOBSERVATIONAll().size();
        Map<String, List<OBX>> obxByIdentifier = new HashMap<>(maximumNumberOfResults);
        // for report data, the expected order is "ADT", "GDT", "IMP", "SIG
        // SIG here is a GTD, which we're assuming to be 3 lines long and starts with "Signed by:
        boolean signatureFound = false;
        int signatureStartLine = maximumNumberOfResults - 3;
        int obxLine = 0;
        for (OBX obx : obxSegments) {
            String rawIdentifier = getIdentifierTypeOrEmpty(obx);
            if (startOfSignature(signatureStartLine, obxLine, obx, rawIdentifier)) {
                signatureFound = true;
            }
            obxLine += 1;

            if (signatureFound) {
                rawIdentifier = SIGNATURE_CODE;
            }

            if (!RESULT_OBX_IDENTIFIERS.containsKey(rawIdentifier)) {
                // non-text results should only have one OBX (e.g. INDICATIONS), so just make a singleton list
                obxByIdentifier.put(rawIdentifier, List.of(obx));
                continue;
            }

            String textIdentifier = RESULT_OBX_IDENTIFIERS.get(rawIdentifier);
            obxByIdentifier.computeIfAbsent(textIdentifier, key -> new ArrayList<>(maximumNumberOfResults)).add(obx);

        }
        return obxByIdentifier;
    }

    private boolean startOfSignature(int signatureStartLine, int obxLine, OBX obx, String rawIdentifier) throws HL7Exception {
        return NARRATIVE_CODE.equals(rawIdentifier) && obxLine == signatureStartLine && "Signed by:".equals(obx.getObx5_ObservationValue(0).encode());
    }


    private String getIdentifierTypeOrEmpty(OBX obx) {
        String identifier = obx.getObx3_ObservationIdentifier().getCwe1_Identifier().getValueOrEmpty();
        if (!identifier.isEmpty()) {
            return identifier;
        }
        try {
            // fallback to primitive identifier
            return obx.getObx3_ObservationIdentifier().getCwe1_Identifier().getExtraComponents().getComponent(0).getData().encode();
        } catch (HL7Exception e) {
            logger.error("Could not parse OBX identifier type", e);
            return "";
        }
    }

    /**
     * Build order with results from ORU R01.
     * @param idsUnid unique Id from the IDS
     * @param oruR01  hl7 message
     * @return interchange messages
     * @throws HL7Exception               if HAPI does
     * @throws Hl7InconsistencyException  if the HL7 message contains errors
     * @throws Hl7MessageIgnoredException if message is ignored
     */
    public static Collection<LabOrderMsg> build(String idsUnid, ORU_R01 oruR01)
            throws HL7Exception, Hl7InconsistencyException, Hl7MessageIgnoredException {
        if (oruR01.getPATIENT_RESULTReps() != 1) {
            throw new Hl7MessageIgnoredException("Not expecting Imaging to have multiple patient results in one message");
        }
        ORU_R01_PATIENT_RESULT patientResults = oruR01.getPATIENT_RESULT();
        List<ORU_R01_ORDER_OBSERVATION> orderObservations = patientResults.getORDER_OBSERVATIONAll();
        MSH msh = (MSH) oruR01.get("MSH");
        PID pid = patientResults.getPATIENT().getPID();
        PV1 pv1 = patientResults.getPATIENT().getVISIT().getPV1();
        PatientInfoHl7 patientInfo = new PatientInfoHl7(msh, pid, pv1);

        List<LabOrderMsg> orders = new ArrayList<>(orderObservations.size());
        int msgSuffix = 0;
        for (ORU_R01_ORDER_OBSERVATION obs : orderObservations) {
            msgSuffix++;
            String subMessageSourceId = String.format("%s_%02d", idsUnid, msgSuffix);
            LabOrderBuilder labOrderBuilder = new ImageLabBuilder(subMessageSourceId, obs, patientInfo);
            labOrderBuilder.addMsgIfAllowedOcId(idsUnid, orders);
        }
        return orders;
    }


}

