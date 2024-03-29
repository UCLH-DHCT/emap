package uk.ac.ucl.rits.inform.datasources.ids;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.Type;
import ca.uhn.hl7v2.model.Varies;
import ca.uhn.hl7v2.model.v26.datatype.DT;
import ca.uhn.hl7v2.model.v26.datatype.NM;
import ca.uhn.hl7v2.model.v26.group.ORU_R01_OBSERVATION;
import ca.uhn.hl7v2.model.v26.group.ORU_R01_ORDER_OBSERVATION;
import ca.uhn.hl7v2.model.v26.group.ORU_R01_PATIENT_RESULT;
import ca.uhn.hl7v2.model.v26.message.ORU_R01;
import ca.uhn.hl7v2.model.v26.segment.EVN;
import ca.uhn.hl7v2.model.v26.segment.MSH;
import ca.uhn.hl7v2.model.v26.segment.NTE;
import ca.uhn.hl7v2.model.v26.segment.OBX;
import ca.uhn.hl7v2.model.v26.segment.PID;
import ca.uhn.hl7v2.model.v26.segment.PV1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.ac.ucl.rits.inform.datasources.ids.exceptions.Hl7InconsistencyException;
import uk.ac.ucl.rits.inform.datasources.ids.hl7.parser.NotesParser;
import uk.ac.ucl.rits.inform.datasources.ids.hl7.parser.PatientInfoHl7;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.ValueType;
import uk.ac.ucl.rits.inform.interchange.visit_observations.Flowsheet;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Build one or more flowsheets from HL7 message.
 * @author Stef Piatek
 */
@Component
public class FlowsheetFactory {
    private static final Logger logger = LoggerFactory.getLogger(FlowsheetFactory.class);
    private static final Collection<String> ALLOWED_STATUSES = Set.of("C", "F", "D");

    /**
     * Builds Flowsheet messages from an ORU R01 message.
     * Allows multiple: OBR segments, OBX segments, NTE segments, OBX lines and NTE lines
     * @param idsUnid Unique id from UDS
     * @param msg     ORU R01 HL7 flowsheet message from EPIC
     * @return Flowsheet messages populated from constructor.
     */
    List<Flowsheet> getMessages(String idsUnid, Message msg) {
        List<Flowsheet> flowsheets = new ArrayList<>();
        try {
            ORU_R01_PATIENT_RESULT patientResult = ((ORU_R01) msg).getPATIENT_RESULT();
            PID pid = patientResult.getPATIENT().getPID();
            MSH msh = (MSH) msg.get("MSH");
            PV1 pv1 = patientResult.getPATIENT().getVISIT().getPV1();
            EVN evn = (EVN) msg.get("EVN");
            Instant recordedDateTime = HL7Utils.interpretLocalTime(evn.getRecordedDateTime());

            flowsheets = buildAllFlowsheets(idsUnid, pid, msh, pv1, recordedDateTime, patientResult.getORDER_OBSERVATIONAll());
        } catch (HL7Exception e) {
            FlowsheetFactory.logger.error("HL7 Exception encountered for msg {}", idsUnid, e);
        }
        return flowsheets;
    }

    /**
     * @param idsUnid          IDS unid
     * @param msh              MSH segment
     * @param pid              PID segment
     * @param pv1              PIV segment
     * @param recordedDateTime Event datetime of the message
     * @param orderObs         ORU R01 order observations from the HL7 message
     * @return Flowsheet entities built from ORU R01 order observations
     * @throws HL7Exception when Flowsheet HL7 message could not be parsed
     */
    private List<Flowsheet> buildAllFlowsheets(
            final String idsUnid, final PID pid, final MSH msh, final PV1 pv1,
            final Instant recordedDateTime, final Iterable<ORU_R01_ORDER_OBSERVATION> orderObs) throws HL7Exception {
        List<Flowsheet> flowsheets = new ArrayList<>();
        for (ORU_R01_ORDER_OBSERVATION orderObr : orderObs) {
            List<ORU_R01_OBSERVATION> observations = orderObr.getOBSERVATIONAll();

            int msgSuffix = 0;
            for (ORU_R01_OBSERVATION observation : observations) {
                msgSuffix++;
                String subMessageSourceId = String.format("%s$%02d", idsUnid, msgSuffix);
                try {
                    Flowsheet flowsheet = buildFlowsheet(subMessageSourceId, observation, msh, pid, pv1, recordedDateTime);
                    flowsheets.add(flowsheet);
                } catch (Hl7InconsistencyException e) {
                    logger.error("Flowsheet could not be parsed for msg {}\n {}", subMessageSourceId, e.getMessage());
                }
            }
        }
        return flowsheets;
    }

    /**
     * Populate flowsheet message from HL7 message segments.
     * @param subMessageSourceId Unique ID of message
     * @param observation        observation object
     * @param msh                MSH segment
     * @param pid                PID segment
     * @param pv1                PIV segment
     * @param recordedDateTime   recorded date time
     * @return Flowsheet
     * @throws HL7Exception              if HL7 message cannot be parsed
     * @throws Hl7InconsistencyException if message does not have required data
     */
    Flowsheet buildFlowsheet(String subMessageSourceId, ORU_R01_OBSERVATION observation, MSH msh, PID pid, PV1 pv1, Instant recordedDateTime)
            throws HL7Exception, Hl7InconsistencyException {
        Flowsheet flowsheet = new Flowsheet();

        OBX obx = observation.getOBX();
        List<NTE> notes = observation.getNTEAll();

        // set generic information
        PatientInfoHl7 patientHl7 = new PatientInfoHl7(msh, pid, pv1);
        flowsheet.setMrn(patientHl7.getMrn());
        flowsheet.setVisitNumber(patientHl7.getVisitNumber());
        flowsheet.setSourceMessageId(subMessageSourceId);
        flowsheet.setSourceSystem(patientHl7.getSendingApplication());
        flowsheet.setSourceApplication(patientHl7.getSendingApplication());
        flowsheet.setIsRealTime(true);
        flowsheet.setUpdatedTime(recordedDateTime);

        // set information from obx
        String observationId = obx.getObx3_ObservationIdentifier().getCwe1_Identifier().getValueOrEmpty();
        flowsheet.setInterfaceId(observationId);

        setFlowsheetValueAndValueType(subMessageSourceId, flowsheet, obx);

        if (!notes.isEmpty()) {
            String comment = getComments(notes);
            flowsheet.setComment(InterchangeValue.buildFromHl7(comment));
        }
        if (flowsheet.getComment().isUnknown() && flowsheet.getStringValue().isUnknown()
                && flowsheet.getNumericValue().isUnknown() && flowsheet.getDateValue().isUnknown()) {
            throw new Hl7InconsistencyException(String.format("msg %s has empty value and comment so was discarded", subMessageSourceId));
        }

        flowsheet.setUnit(InterchangeValue.buildFromHl7(getUnits(obx)));
        flowsheet.setObservationTime(HL7Utils.interpretLocalTime(obx.getObx14_DateTimeOfTheObservation()));
        return flowsheet;
    }

    /**
     * Get units from the OBX segment.
     * @param obx OBX segment
     * @return units
     */
    private String getUnits(OBX obx) {
        return obx.getObx6_Units().getCwe1_Identifier().getValueOrEmpty();
    }

    /**
     * Sets value type and the appropriate value.
     * @param subMessageSourceId Message ID along with the sub message Id
     * @param flowsheet          flowsheet to add the values to
     * @param obx                OBX segment
     * @throws Hl7InconsistencyException If the result status is unknown or numeric result can't be parsed
     * @throws HL7Exception              If value can't be decoded
     */
    private void setFlowsheetValueAndValueType(String subMessageSourceId, Flowsheet flowsheet, OBX obx)
            throws Hl7InconsistencyException, HL7Exception {
        String resultStatus = obx.getObx11_ObservationResultStatus().getValueOrEmpty();

        if (!ALLOWED_STATUSES.contains(resultStatus)) {
            throw new Hl7InconsistencyException(String.format("msg %s result status ('%s') was not recognised.", subMessageSourceId, resultStatus));
        }

        // Assuming numeric data will be a single value
        Type singularData = obx.getObservationValue(0).getData();
        // HAPI can return null so use nullDefault as empty string
        String value = singularData.toString();
        if (value == null) {
            throw new Hl7InconsistencyException("Null value field for flowsheet");
        }

        if (singularData instanceof NM) {
            flowsheet.setValueType(ValueType.NUMERIC);
            if ("D".equals(resultStatus)) {
                flowsheet.setNumericValue(InterchangeValue.delete());
            } else {
                try {
                    flowsheet.setNumericValue(InterchangeValue.buildFromHl7(Double.parseDouble(value)));
                } catch (NumberFormatException e) {
                    throw new Hl7InconsistencyException(
                            String.format("Numeric result expected for msg %s, instead '%s' was found", subMessageSourceId, value));
                }
            }
        } else if (singularData instanceof DT) {
            flowsheet.setValueType(ValueType.DATE);
            if ("D".equals(resultStatus)) {
                flowsheet.setDateValue(InterchangeValue.delete());
            } else {
                LocalDate date = HL7Utils.interpretDate((DT) singularData);
                flowsheet.setDateValue(InterchangeValue.buildFromHl7(date));
            }
        } else {
            // to match hoover, default to all other types being text
            flowsheet.setValueType(ValueType.TEXT);
            if ("D".equals(resultStatus)) {
                flowsheet.setStringValue(InterchangeValue.delete());
            } else {
                String stringValue = getStringValue(obx);
                flowsheet.setStringValue(InterchangeValue.buildFromHl7(stringValue.strip()));
            }
        }
    }

    /**
     * Build comments from list of NTEs, leading & lagging whitespace removed, and lines separated by newlines.
     * @param notes NTE objects
     * @return String of leading & lagging whitespace removed comment lines, joined by newlines
     */
    private String getComments(Collection<NTE> notes) {
        NotesParser parser = new NotesParser(notes);
        return parser.getComments();
    }

    /**
     * Extracts string value from obx. Allows for multiple lines in an OBX (separated by newline characters)
     * @param obx OBX object
     * @return String of all whitespace trimmed lines separated by newlines
     * @throws HL7Exception If value can't be decoded
     */
    private String getStringValue(OBX obx) throws HL7Exception {
        // Strings can be made of multiple values
        Varies[] dataVaries = obx.getObx5_ObservationValue();
        StringBuilder valueBuilder = new StringBuilder();
        // Allow for multiple results
        for (Varies resultLine : dataVaries) {
            Type lineData = resultLine.getData();
            String lineValue = lineData.encode();
            if (lineValue != null) {
                if (valueBuilder.length() > 1) {
                    valueBuilder.append("\n");
                }
                valueBuilder.append(lineValue.strip());
            }
        }
        return valueBuilder.toString().strip();
    }


}
