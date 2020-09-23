package uk.ac.ucl.rits.inform.datasources.ids;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Type;
import ca.uhn.hl7v2.model.Varies;
import ca.uhn.hl7v2.model.v26.datatype.FT;
import ca.uhn.hl7v2.model.v26.datatype.NM;
import ca.uhn.hl7v2.model.v26.group.ORU_R01_OBSERVATION;
import ca.uhn.hl7v2.model.v26.group.ORU_R01_ORDER_OBSERVATION;
import ca.uhn.hl7v2.model.v26.group.ORU_R01_PATIENT_RESULT;
import ca.uhn.hl7v2.model.v26.message.ORU_R01;
import ca.uhn.hl7v2.model.v26.segment.MSH;
import ca.uhn.hl7v2.model.v26.segment.NTE;
import ca.uhn.hl7v2.model.v26.segment.OBX;
import ca.uhn.hl7v2.model.v26.segment.PID;
import ca.uhn.hl7v2.model.v26.segment.PV1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ucl.rits.inform.interchange.ResultStatus;
import uk.ac.ucl.rits.inform.interchange.VitalSigns;

import java.util.ArrayList;
import java.util.List;

/**
 * Build one or more vitalsigns from HL7 message.
 */
public class VitalSignBuilder {
    private static final Logger logger = LoggerFactory.getLogger(VitalSignBuilder.class);
    private List<VitalSigns> vitalSigns = new ArrayList<>();

    /**
     * @return VitalSign messages populated from contstuctor.
     */
    public List<VitalSigns> getMessages() {
        return vitalSigns;
    }

    /**
     * Populates VitalSign messages from a ORU R01 HL7message.
     * Allows multiple: OBR segments, OBX segments, NTE segments, OBX lines and NTE lines
     * @param idsUnid Unique id from UDS
     * @param oruR01  ORU R01 HL7 VitalSign message from EPIC
     */
    public VitalSignBuilder(String idsUnid, ORU_R01 oruR01) {
        // define Id here so can use it for logging if MSH or order obs throws HL7 Exception
        String subMessageSourceId = idsUnid;

        try {
            ORU_R01_PATIENT_RESULT patientResult = oruR01.getPATIENT_RESULT();
            PID pid = patientResult.getPATIENT().getPID();
            MSH msh = (MSH) oruR01.get("MSH");
            PV1 pv1 = patientResult.getPATIENT().getVISIT().getPV1();

            // Allow multiple OBRs per message
            List<ORU_R01_ORDER_OBSERVATION> orderObs = patientResult.getORDER_OBSERVATIONAll();
            for (ORU_R01_ORDER_OBSERVATION orderObr : orderObs) {
                List<ORU_R01_OBSERVATION> observations = orderObr.getOBSERVATIONAll();

                int msgSuffix = 0;
                for (ORU_R01_OBSERVATION observation : observations) {
                    msgSuffix++;
                    subMessageSourceId = String.format("%s$%02d", idsUnid, msgSuffix);
                    try {
                        VitalSigns vitalSign = createVitalSign(subMessageSourceId, observation, msh, pid, pv1);
                        vitalSigns.add(vitalSign);
                    } catch (IllegalArgumentException e) {
                        logger.error(String.format("Vitalsign could not be parsed for msg %s", subMessageSourceId), e);
                    }
                }
            }
        } catch (HL7Exception e) {
            logger.error(String.format("HL7 Exception encountered for msg %s", subMessageSourceId), e);
        }
    }

    /**
     * Populate vitalsign message from HL7 message segments.
     * @param subMessageSourceId Unique ID of message
     * @param observation        observation object
     * @param msh                MSH segment
     * @param pid                PID segment
     * @param pv1                PIV segment
     * @return Vitalsign
     * @throws HL7Exception if HL7 message cannot be parsed
     */
    private VitalSigns createVitalSign(String subMessageSourceId, ORU_R01_OBSERVATION observation, MSH msh, PID pid, PV1 pv1) throws HL7Exception {
        VitalSigns vitalSign = new VitalSigns();

        OBX obx = observation.getOBX();
        List<NTE> notes = observation.getNTEAll();

        // set generic information
        PatientInfoHl7 patientHl7 = new PatientInfoHl7(msh, pid, pv1);
        vitalSign.setMrn(patientHl7.getMrn());
        vitalSign.setVisitNumber(patientHl7.getVisitNumber());
        vitalSign.setSourceMessageId(subMessageSourceId);
        vitalSign.setSourceSystem(patientHl7.getSendingApplication());

        // set information from obx
        String observationId = obx.getObx3_ObservationIdentifier().getCwe1_Identifier().getValueOrEmpty();
        vitalSign.setVitalSignIdentifier(String.format("%s$%s", "EPIC", observationId));

        String resultStatus = obx.getObx11_ObservationResultStatus().getValueOrEmpty();
        if (resultStatus.equals("D")) {
            vitalSign.setResultStatus(ResultStatus.DELETE);
        } else if (!(resultStatus.equals("F") || resultStatus.equals("C"))) {
            // Always keep default resultStatus value of SAVE, if not F or C then log as an error.
            throw new IllegalArgumentException(
                    String.format("msg %s result status ('%s') was not recognised.", subMessageSourceId, resultStatus));
        }

        // Assuming numeric data will be a single value
        Type singularData = obx.getObservationValue(0).getData();
        // HAPI can return null so use nullDefault as empty string
        String value = singularData.toString();
        if (value == null) {
            value = "";
        }
        if (singularData instanceof NM) {
            try {
                vitalSign.setNumericValue(Double.parseDouble(value));
            } catch (NumberFormatException e) {
                if (vitalSign.getResultStatus() != ResultStatus.DELETE) {
                    throw new IllegalArgumentException(
                            String.format("Numeric result expected for msg %s, instead '%s' was found", subMessageSourceId, value));
                }
            }
        } else {
            // Skip empty string values
            if (!value.equals("")) {
                String stringValue = getStringValue(obx);
                vitalSign.setStringValue(stringValue.trim());
            }
        }

        if (!notes.isEmpty()) {
            String comment = getComments(notes);
            vitalSign.setComment(comment);
        }
        if (vitalSign.getComment() == null && value.equals("")) {
            throw new IllegalArgumentException(
                    String.format("msg %s has empty value and comment so was discarded", subMessageSourceId));
        }

        vitalSign.setUnit(obx.getObx6_Units().getCwe1_Identifier().getValueOrEmpty());
        vitalSign.setObservationTimeTaken(HL7Utils.interpretLocalTime(obx.getObx14_DateTimeOfTheObservation()));
        return vitalSign;
    }

    /**
     * Build comments from list of NTEs, trimmed and lines separated by newlines.
     * @param notes List of NTE objects
     * @return String of trimmed comment lines, joined by newlines
     */
    private String getComments(List<NTE> notes) {
        StringBuilder commentBuilder = new StringBuilder();
        // multiple NTE segments
        for (NTE note : notes) {
            FT[] allComments = note.getNte3_Comment();
            // Multiple lines in field
            for (FT comment : allComments) {
                if (commentBuilder.length() > 1) {
                    commentBuilder.append("\n");
                }
                commentBuilder.append(comment.getValueOrEmpty().trim());
            }
        }
        return commentBuilder.toString().trim();
    }

    /**
     * Extracts vitalsign string value from obx. Allows for multiple lines in an OBX (separated by newline characters)
     * @param obx OBX object
     * @return String of all whitespace trimmed lines separated by newlines
     */
    private String getStringValue(OBX obx) {
        // Strings can be made of multiple values
        Varies[] dataVaries = obx.getObx5_ObservationValue();
        StringBuilder valueBuilder = new StringBuilder();
        // Allow for multiple results
        for (Varies resultLine : dataVaries) {
            Type lineData = resultLine.getData();
            String lineValue = lineData.toString();
            if (lineValue != null) {
                if (valueBuilder.length() > 1) {
                    valueBuilder.append("\n");
                }
                valueBuilder.append(lineValue.trim());
            }
        }
        return valueBuilder.toString().trim();
    }


}
