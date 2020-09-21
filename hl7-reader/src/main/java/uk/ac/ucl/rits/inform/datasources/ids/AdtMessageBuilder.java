package uk.ac.ucl.rits.inform.datasources.ids;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v26.group.ADT_A39_PATIENT;
import ca.uhn.hl7v2.model.v26.message.ADT_A39;
import ca.uhn.hl7v2.model.v26.segment.EVN;
import ca.uhn.hl7v2.model.v26.segment.MRG;
import ca.uhn.hl7v2.model.v26.segment.MSH;
import ca.uhn.hl7v2.model.v26.segment.PID;
import ca.uhn.hl7v2.model.v26.segment.PV1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ucl.rits.inform.datasources.ids.exceptions.Hl7MessageNotImplementedException;
import uk.ac.ucl.rits.inform.interchange.Hl7Value;
import uk.ac.ucl.rits.inform.interchange.adt.AdmitPatient;
import uk.ac.ucl.rits.inform.interchange.adt.AdtCancellation;
import uk.ac.ucl.rits.inform.interchange.adt.AdtMessage;
import uk.ac.ucl.rits.inform.interchange.adt.CancelAdmitPatient;
import uk.ac.ucl.rits.inform.interchange.adt.CancelDischargePatient;
import uk.ac.ucl.rits.inform.interchange.adt.CancelTransferPatient;
import uk.ac.ucl.rits.inform.interchange.adt.DischargePatient;
import uk.ac.ucl.rits.inform.interchange.adt.MergePatient;
import uk.ac.ucl.rits.inform.interchange.adt.TransferPatient;
import uk.ac.ucl.rits.inform.interchange.adt.UpdatePatientInfo;

/**
 * Build an AdtMessage Emap interchange object from an HL7 message.
 * @author Jeremy Stein
 */
public class AdtMessageBuilder {
    private static final long serialVersionUID = 2925921017121050081L;
    private static final Logger logger = LoggerFactory.getLogger(AdtMessageBuilder.class);
    private MSH msh;
    private PV1 pv1;
    private MRG mrg;
    private PID pid;
    private EVN evn;
    private Hl7MessageNotImplementedException delayedException;
    private AdtMessage msg;

    /**
     * Construct from an HL7 message.
     * @param hl7Msg   the HL7 message
     * @param sourceId the unique source Id message
     * @throws HL7Exception if HAPI does
     */
    public AdtMessageBuilder(Message hl7Msg, String sourceId) throws HL7Exception {
        PatientInfoHl7 patientInfoHl7 = initialiseSegments(hl7Msg);

        buildAdtMessageSubclass(patientInfoHl7);
        msg.setSourceMessageId(sourceId);


        if (pv1 != null) {
            // will we want demographics to be included in pathology messages too?
            msg.setAdmissionDateTime(Hl7Value.buildFromHl7(patientInfoHl7.getAdmissionDateTime()));
            msg.setAdmitSource(Hl7Value.buildFromHl7(patientInfoHl7.getAdmitSource()));
            msg.setCurrentBed(Hl7Value.buildFromHl7(patientInfoHl7.getCurrentBed()));
            msg.setCurrentRoomCode(Hl7Value.buildFromHl7(patientInfoHl7.getCurrentRoomCode()));
            msg.setCurrentWardCode(Hl7Value.buildFromHl7(patientInfoHl7.getCurrentWardCode()));
            msg.setFullLocationString(Hl7Value.buildFromHl7(patientInfoHl7.getFullLocationString()));
            msg.setHospitalService(Hl7Value.buildFromHl7(patientInfoHl7.getHospitalService()));
            msg.setPatientClass(Hl7Value.buildFromHl7(patientInfoHl7.getPatientClass())); // make an enum
            msg.setPatientType(Hl7Value.buildFromHl7(patientInfoHl7.getPatientType()));
            msg.setVisitNumber(patientInfoHl7.getVisitNumber());
        }
        if (pid != null) {
            msg.setEthnicGroup(Hl7Value.buildFromHl7(patientInfoHl7.getEthnicGroup()));
            msg.setMrn(patientInfoHl7.getMrn());
            msg.setNhsNumber(patientInfoHl7.getNHSNumber());
            msg.setPatientBirthDate(Hl7Value.buildFromHl7(patientInfoHl7.getPatientBirthDate()));

            // Despite what the HL7 spec hints at, this death information can occur
            // in any message, not just A03
            String hl7DeathIndicator = patientInfoHl7.getPatientDeathIndicator();
            if (hl7DeathIndicator.equals("Y")) {
                msg.setPatientDeathIndicator(new Hl7Value<>(true));
            } else if (hl7DeathIndicator.equals("N")
                    || hl7DeathIndicator.equals("")) {
                msg.setPatientDeathIndicator(new Hl7Value<>(false));
            }
            // set the death time even if indicator says they're not dead (it happens...)
            msg.setPatientDeathDateTime(Hl7Value.buildFromHl7(patientInfoHl7.getPatientDeathDateTime()));

            msg.setPatientFamilyName(Hl7Value.buildFromHl7(patientInfoHl7.getPatientFamilyName()));
            msg.setPatientFullName(Hl7Value.buildFromHl7(patientInfoHl7.getPatientFullName()));
            msg.setPatientGivenName(Hl7Value.buildFromHl7(patientInfoHl7.getPatientGivenName()));
            msg.setPatientMiddleName(Hl7Value.buildFromHl7(patientInfoHl7.getPatientMiddleName()));
            msg.setPatientReligion(Hl7Value.buildFromHl7(patientInfoHl7.getPatientReligion()));
            msg.setPatientSex(Hl7Value.buildFromHl7(patientInfoHl7.getPatientSex()));
            msg.setPatientTitle(Hl7Value.buildFromHl7(patientInfoHl7.getPatientTitle()));
            msg.setPatientZipOrPostalCode(Hl7Value.buildFromHl7(patientInfoHl7.getPatientZipOrPostalCode()));
        }
        if (evn != null) {
            msg.setRecordedDateTime(HL7Utils.interpretLocalTime(evn.getEvn2_RecordedDateTime()));
            msg.setEventReasonCode(evn.getEvn4_EventReasonCode().getValue());
            msg.setOperatorId(evn.getEvn5_OperatorID(0).getXcn1_IDNumber().getValue());
            msg.setEventOccurredDateTime(HL7Utils.interpretLocalTime(evn.getEvn6_EventOccurred()));
        }
    }

    /**
     * Build correct AdtMessage subtype based on triggerEvent string.
     * @param patientInfoHl7 Patient HL7 info
     * @throws HL7Exception if hl7 message can't be parsed
     */
    private void buildAdtMessageSubclass(PatientInfoHl7 patientInfoHl7) throws HL7Exception {
        String triggerEvent = msh.getMessageType().getTriggerEvent().getValueOrEmpty();

        switch (triggerEvent) {
            case "A01":
            case "A04":
                msg = new AdmitPatient();
                break;
            case "A02":
            case "A06":
            case "A07":
                msg = new TransferPatient();
                break;
            case "A03":
                DischargePatient dischargeMsg = new DischargePatient();
                dischargeMsg.setDischargeDateTime(patientInfoHl7.getDischargeDateTime());
                dischargeMsg.setDischargeDisposition(patientInfoHl7.getDischargeDisposition());
                dischargeMsg.setDischargeLocation(patientInfoHl7.getDischargeLocation());
                msg = dischargeMsg;
                break;
            case "A08":
                msg = new UpdatePatientInfo();
                break;
            case "A11":
                CancelAdmitPatient cancelAdmitPatient = new CancelAdmitPatient();
                setCancellationTime(cancelAdmitPatient);
                msg = cancelAdmitPatient;
                break;
            case "A12":
                CancelTransferPatient cancelTransferPatient = new CancelTransferPatient();
                setCancellationTime(cancelTransferPatient);
                msg = cancelTransferPatient;
                break;
            case "A13":
                CancelDischargePatient cancelDischargePatient = new CancelDischargePatient();
                setCancellationTime(cancelDischargePatient);
                msg = cancelDischargePatient;
                break;
            case "A40":
                MergePatient mergeMsg = new MergePatient();
                if (mrg != null) {
                    mergeMsg.setRetiredMrn(mrg.getMrg1_PriorPatientIdentifierList(0).getIDNumber().toString());
                    mergeMsg.setRetiredNhsNumber(mrg.getMrg1_PriorPatientIdentifierList(1).getIDNumber().toString());
                }
                msg = mergeMsg;
                break;
            default:
                // to keep processes running even if it does not build a valid interchange message, delay exception
                // and create default message type
                delayedException = new Hl7MessageNotImplementedException("Unimplemented ADT trigger event " + triggerEvent);
                msg = new UpdatePatientInfo();
                break;
        }
    }

    /**
     * Read HL7 segments and initialise class data for these.
     * @param hl7Msg HL7 message
     * @return Patient info class
     * @throws HL7Exception if required segments can't be parsed
     */
    private PatientInfoHl7 initialiseSegments(Message hl7Msg) throws HL7Exception {
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
                // empty PID is allowed
            }
        }
        try {
            evn = (EVN) hl7Msg.get("EVN");
        } catch (HL7Exception e) {
            // EVN is allowed not to exist
        }
        // PatientInfoHl7 uses mshwrap, pidwrap, pv1wrap - XXX: these wrappers could be combined and then moved into PatientInfoHl7?
        return new PatientInfoHl7(msh, pid, pv1);
    }

    /**
     * Set cancellation time from the evn segment.
     * @param adtCancellation adt cancellation message
     * @throws DataTypeException if the datetime can't be interpreted as local time
     */
    private void setCancellationTime(AdtCancellation adtCancellation) throws DataTypeException {
        adtCancellation.setCancelledDateTime(HL7Utils.interpretLocalTime(evn.getEvn6_EventOccurred()));
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
