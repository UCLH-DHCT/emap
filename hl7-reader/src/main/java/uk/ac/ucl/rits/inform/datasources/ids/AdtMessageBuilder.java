package uk.ac.ucl.rits.inform.datasources.ids;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v26.group.ADT_A39_PATIENT;
import ca.uhn.hl7v2.model.v26.message.ADT_A39;
import ca.uhn.hl7v2.model.v26.segment.EVN;
import ca.uhn.hl7v2.model.v26.segment.MRG;
import ca.uhn.hl7v2.model.v26.segment.MSH;
import ca.uhn.hl7v2.model.v26.segment.PID;
import ca.uhn.hl7v2.model.v26.segment.PV1;
import uk.ac.ucl.rits.inform.datasources.ids.exceptions.Hl7MessageNotImplementedException;
import uk.ac.ucl.rits.inform.interchange.AdtMessage;
import uk.ac.ucl.rits.inform.interchange.AdtOperationType;

/**
 * Build an AdtMessage Emap interchange object from an HL7 message.
 * @author Jeremy Stein
 */
public class AdtMessageBuilder {
    private static final long serialVersionUID = 2925921017121050081L;
    private static final Logger logger = LoggerFactory.getLogger(AdtMessageBuilder.class);

    private static final Map<String, AdtOperationType> HL7_TRIGGER_EVENT_TO_OPERATION_TYPE = new HashMap<String, AdtOperationType>() {
        {
            put("A01", AdtOperationType.ADMIT_PATIENT);
            put("A02", AdtOperationType.TRANSFER_PATIENT);
            put("A03", AdtOperationType.DISCHARGE_PATIENT);
            put("A04", AdtOperationType.ADMIT_PATIENT);
            put("A06", AdtOperationType.TRANSFER_PATIENT);
            put("A07", AdtOperationType.TRANSFER_PATIENT);
            put("A08", AdtOperationType.UPDATE_PATIENT_INFO);
            put("A11", AdtOperationType.CANCEL_ADMIT_PATIENT);
            put("A12", AdtOperationType.CANCEL_TRANSFER_PATIENT);
            put("A13", AdtOperationType.CANCEL_DISCHARGE_PATIENT);
            put("A40", AdtOperationType.MERGE_BY_ID);
        }
    };

    private MSH msh;
    private PV1 pv1;
    private MRG mrg;
    private PID pid;
    private EVN evn;

    private Hl7MessageNotImplementedException delayedException;

    private AdtMessage msg = new AdtMessage();

    /**
     * Construct from an HL7 message.
     * @param hl7Msg the HL7 message
     * @param sourceId the unique source Id message
     * @throws HL7Exception if HAPI does
     */
    public AdtMessageBuilder(Message hl7Msg, String sourceId) throws HL7Exception {
        msg.setSourceMessageId(sourceId);
        msh = (MSH) hl7Msg.get("MSH");

        try {
            pv1 = (PV1) hl7Msg.get("PV1");
        } catch (HL7Exception e) {
            // some sections are allowed not to exist
        }

        // I want the "MRG" segment for A40 messages, is this really
        // the best way to get it? Why do we have to get the PID segment in
        // a different way for an A39/A40 message?
        if (hl7Msg instanceof ADT_A39) {
            ADT_A39_PATIENT a39Patient = (ADT_A39_PATIENT) hl7Msg.get("PATIENT");
            mrg = a39Patient.getMRG();
            pid = a39Patient.getPID();
        } else {
            try {
                pid = (PID) hl7Msg.get("PID");
            } catch (HL7Exception e) {
            }
        }
        try {
            evn = (EVN) hl7Msg.get("EVN");
        } catch (HL7Exception e) {
            // EVN is allowed not to exist
        }

        String triggerEvent = msh.getMessageType().getTriggerEvent().getValueOrEmpty();
        AdtOperationType adtOperationType = HL7_TRIGGER_EVENT_TO_OPERATION_TYPE.get(triggerEvent);
        if (adtOperationType == null) {
            delayedException = new Hl7MessageNotImplementedException("Unimplemented ADT trigger event " + triggerEvent);
        }
        msg.setOperationType(adtOperationType);

        // PatientInfoHl7 uses mshwrap, pidwrap, pv1wrap - XXX: these wrappers could be combined and then moved into PatientInfoHl7?
        PatientInfoHl7 patientInfoHl7 = new PatientInfoHl7(msh, pid, pv1);

        if (pv1 != null) {
            // will we want demographics to be included in pathology messages too?
            msg.setAdmissionDateTime(patientInfoHl7.getAdmissionDateTime());
            msg.setAdmitSource(patientInfoHl7.getAdmitSource());
            msg.setCurrentBed(patientInfoHl7.getCurrentBed());
            msg.setCurrentRoomCode(patientInfoHl7.getCurrentRoomCode());
            msg.setCurrentWardCode(patientInfoHl7.getCurrentWardCode());
            msg.setDischargeDateTime(patientInfoHl7.getDischargeDateTime());
            msg.setDischargeDisposition(patientInfoHl7.getDischargeDisposition());
            msg.setDischargeLocation(patientInfoHl7.getDischargeLocation());
            msg.setFullLocationString(patientInfoHl7.getFullLocationString());
            msg.setHospitalService(patientInfoHl7.getHospitalService());
            msg.setPatientClass(patientInfoHl7.getPatientClass()); // make an enum
            msg.setPatientType(patientInfoHl7.getPatientType());
            msg.setVisitNumber(patientInfoHl7.getVisitNumber());
        }
        if (pid != null) {
            msg.setEthnicGroup(patientInfoHl7.getEthnicGroup());
            msg.setMrn(patientInfoHl7.getMrn());
            msg.setNhsNumber(patientInfoHl7.getNHSNumber());
            msg.setPatientBirthDate(patientInfoHl7.getPatientBirthDate());

            // Dead = Y should only happen in an A03, according to the HL7 spec,
            // For other messages leave this null.
            if (triggerEvent.equals("A03")) {
                String hl7DeathIndicator = patientInfoHl7.getPatientDeathIndicator();
                if (hl7DeathIndicator.equals("Y")) {
                    msg.setPatientDeathIndicator(true);
                } else if (hl7DeathIndicator.equals("N")
                        || hl7DeathIndicator.equals("")) {
                    msg.setPatientDeathIndicator(false);
                }
                // set the death time even if indicator says they're not dead (it happens...)
                msg.setPatientDeathDateTime(patientInfoHl7.getPatientDeathDateTime());
            }

            msg.setPatientFamilyName(patientInfoHl7.getPatientFamilyName());
            msg.setPatientFullName(patientInfoHl7.getPatientFullName());
            msg.setPatientGivenName(patientInfoHl7.getPatientGivenName());
            msg.setPatientMiddleName(patientInfoHl7.getPatientMiddleName());
            msg.setPatientReligion(patientInfoHl7.getPatientReligion());
            msg.setPatientSex(patientInfoHl7.getPatientSex());
            msg.setPatientTitle(patientInfoHl7.getPatientTitle());
            msg.setPatientZipOrPostalCode(patientInfoHl7.getPatientZipOrPostalCode());
        }
        if (evn != null) {
            msg.setRecordedDateTime(HL7Utils.interpretLocalTime(evn.getEvn2_RecordedDateTime()));
            msg.setEventReasonCode(evn.getEvn4_EventReasonCode().getValue());
            msg.setOperatorId(evn.getEvn5_OperatorID(0).getXcn1_IDNumber().getValue());
            msg.setEventOccurredDateTime(HL7Utils.interpretLocalTime(evn.getEvn6_EventOccurred()));
        }
        if (mrg != null) {
            msg.setMergedPatientId(mrg.getMrg1_PriorPatientIdentifierList(0).getIDNumber().toString());
        }
    }

    /**
     * @return the now-built AdtMessage
     * @throws Hl7MessageNotImplementedException if we haven't implemented this message type yet.
     */
    public AdtMessage getAdtMessage() throws Hl7MessageNotImplementedException {
        // Defer some error checking until the message is actually required.
        // This allows client code that only wants to do certain simple tasks with the HL7
        // message to still operate even if a valid message can't be produced (eg. the IDS filler).
        if (delayedException != null) {
            throw delayedException;
        }
        return msg;
    }

    /**
     * @return the HAPI MSH segment
     */
    public MSH getMsh() {
        return msh;
    }

    /**
     * @return the HAPI PV1 segment
     */
    public PV1 getPv1() {
        return pv1;
    }

    /**
     * @return the HAPI PID segment
     */
    public PID getPid() {
        return pid;
    }
}
