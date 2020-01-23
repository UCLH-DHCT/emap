package uk.ac.ucl.rits.inform.datasources.ids;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.Type;
import ca.uhn.hl7v2.model.Varies;
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
import java.util.Objects;

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

            // assumes that only one result
            //todo: check only one result per message is expected
            ORU_R01_ORDER_OBSERVATION orderObs = patientResult.getORDER_OBSERVATION();
            List<ORU_R01_OBSERVATION> observations = orderObs.getOBSERVATIONAll();

            int msgSuffix = 0;
            for (ORU_R01_OBSERVATION observation : observations) {
                msgSuffix++;
                subMessageSourceId = String.format("%s$%02d", idsUnid, msgSuffix);
                VitalSigns vitalSign = createVitalSign(subMessageSourceId, observation, msh, pid, pv1);
                vitalSigns.add(vitalSign);
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
        NTE nte = observation.getNTE();

        // set generic information
        PatientInfoHl7 patientHl7 = new PatientInfoHl7(msh, pid, pv1);
        vitalSign.setMrn(patientHl7.getMrn());
        vitalSign.setVisitNumber(patientHl7.getVisitNumber());
        vitalSign.setSourceMessageId(subMessageSourceId);

        // set information from obx
        String observationId = obx.getObx3_ObservationIdentifier().getCwe1_Identifier().getValueOrEmpty();
        vitalSign.setVitalSignIdentifier(String.format("%s$%s", "EPIC", observationId));

        String resultStatus = obx.getObx11_ObservationResultStatus().getValueOrEmpty();
        // todo: define that these are required fields. If they don't have them, then issue with HL7 source.
        if (resultStatus.equals("D")) {
            vitalSign.setResultStatus(ResultStatus.DELETE);
        } else if (!(resultStatus.equals("F") || resultStatus.equals("C"))) {
            // Always keep default resultStatus value of SAVE, if not F or C then log as an error.
            logger.error(String.format("msg %s result status ('%s') was not recognised.", subMessageSourceId, resultStatus));
        }

        Varies dataVaries = obx.getObx5_ObservationValue(0);
        Type data = dataVaries.getData();
        // HAPI can return null so use nullDefault as empty string
        String value = Objects.toString(data, "");
        if (data instanceof NM) {
            try {
                vitalSign.setNumericValue(Double.parseDouble(value));
            } catch (NumberFormatException e) {
                logger.error(String.format("Numeric result expected for msg %s, instead '%s' was found", value, subMessageSourceId));
            }
        } else {
            vitalSign.setStringValue(value);
        }

        // todo: do we want a comment field for vitalsigns
        // todo: check to see crazy long message & separator characters
        // todo: can an empty comment be sent
        if (!nte.isEmpty()) {
            // assumes single comment
            String comment = nte.getNte3_Comment(0).getValue().trim();
            vitalSign.setStringValue(String.format("%s %s", vitalSign.getStringValue(), comment).trim());
        }

        vitalSign.setUnit(obx.getObx6_Units().getCwe1_Identifier().getValueOrEmpty());
        try {
            vitalSign.setObservationTimeTaken(HL7Utils.interpretLocalTime(obx.getObx14_DateTimeOfTheObservation()));
        } catch (DataTypeException e) {
            logger.error(String.format("Observation Time Taken could not be set for msg %s", subMessageSourceId), e);
        }
        return vitalSign;
    }


}
