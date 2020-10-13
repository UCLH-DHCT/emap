package uk.ac.ucl.rits.inform.datasources.ids;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v26.group.ADT_A39_PATIENT;
import ca.uhn.hl7v2.model.v26.group.ADT_A45_MERGE_INFO;
import ca.uhn.hl7v2.model.v26.message.ADT_A39;
import ca.uhn.hl7v2.model.v26.message.ADT_A45;
import ca.uhn.hl7v2.model.v26.message.ORM_O01;
import ca.uhn.hl7v2.model.v26.message.ORU_R01;
import ca.uhn.hl7v2.model.v26.segment.EVN;
import ca.uhn.hl7v2.model.v26.segment.MRG;
import ca.uhn.hl7v2.model.v26.segment.MSH;
import ca.uhn.hl7v2.model.v26.segment.PID;
import ca.uhn.hl7v2.model.v26.segment.PV1;
import ca.uhn.hl7v2.model.v26.segment.PV2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.ac.ucl.rits.inform.datasources.ids.exceptions.Hl7MessageNotImplementedException;
import uk.ac.ucl.rits.inform.interchange.Hl7Value;
import uk.ac.ucl.rits.inform.interchange.adt.AdmitPatient;
import uk.ac.ucl.rits.inform.interchange.adt.AdtCancellation;
import uk.ac.ucl.rits.inform.interchange.adt.AdtMessage;
import uk.ac.ucl.rits.inform.interchange.adt.CancelAdmitPatient;
import uk.ac.ucl.rits.inform.interchange.adt.CancelDischargePatient;
import uk.ac.ucl.rits.inform.interchange.adt.CancelTransferPatient;
import uk.ac.ucl.rits.inform.interchange.adt.ChangePatientIdentifiers;
import uk.ac.ucl.rits.inform.interchange.adt.DeletePersonInformation;
import uk.ac.ucl.rits.inform.interchange.adt.DischargePatient;
import uk.ac.ucl.rits.inform.interchange.adt.MergePatient;
import uk.ac.ucl.rits.inform.interchange.adt.MoveVisitInformation;
import uk.ac.ucl.rits.inform.interchange.adt.PatientClass;
import uk.ac.ucl.rits.inform.interchange.adt.PreviousIdentifiers;
import uk.ac.ucl.rits.inform.interchange.adt.RegisterPatient;
import uk.ac.ucl.rits.inform.interchange.adt.SwapLocations;
import uk.ac.ucl.rits.inform.interchange.adt.TransferPatient;
import uk.ac.ucl.rits.inform.interchange.adt.UpdatePatientInfo;

/**
 * Build an AdtMessage Emap interchange object from an HL7 message.
 * @author Jeremy Stein
 */
@Component
public class AdtMessageFactory {
    private static final Logger logger = LoggerFactory.getLogger(AdtMessageFactory.class);

    /**
     * Default constructor.
     */
    public AdtMessageFactory() {
    }

    /**
     * Construct from an HL7 message.
     * @param hl7Msg   the HL7 message
     * @param sourceId the unique source Id message
     * @return AdtMessage built from the hl7 message
     * @throws HL7Exception                      if HAPI does
     * @throws Hl7MessageNotImplementedException if the ADT type hasn't been implemented
     */
    public AdtMessage getAdtMessage(final Message hl7Msg, final String sourceId) throws HL7Exception, Hl7MessageNotImplementedException {
        MSH msh = getMsh(hl7Msg);
        PID pid = getPid(hl7Msg, false);
        PV1 pv1 = getPv1(hl7Msg, false);
        PV2 pv2 = getPv2(hl7Msg);

        PatientInfoHl7 patientInfoHl7 = new PatientInfoHl7(msh, pid, pv1, pv2);
        EVN evn = getEvn(hl7Msg);
        AdtMessage msg = buildAdtMessageSubclass(patientInfoHl7, hl7Msg, evn, msh.getMessageType().getTriggerEvent().getValueOrEmpty());
        addGenericDataToAdtMessage(sourceId, patientInfoHl7, evn, msg);

        return msg;
    }

    /**
     * Add data that all ADT messages may have.
     * @param sourceId       source Id
     * @param patientInfoHl7 patientInfo
     * @param evn            EVN segment
     * @param msg            AdtMessage to be altered
     * @throws HL7Exception If HAPI does
     */
    private void addGenericDataToAdtMessage(final String sourceId, final PatientInfoHl7 patientInfoHl7, final EVN evn,
                                            AdtMessage msg) throws HL7Exception {
        msg.setSourceMessageId(sourceId);
        msg.setSourceSystem(patientInfoHl7.getSendingApplication());
        if (patientInfoHl7.pv1SegmentExists()) {
            // will we want demographics to be included in pathology messages too?
            msg.setAdmitSource(Hl7Value.buildFromHl7(patientInfoHl7.getAdmitSource()));
            msg.setCurrentBed(Hl7Value.buildFromHl7(patientInfoHl7.getCurrentBed()));
            msg.setCurrentRoomCode(Hl7Value.buildFromHl7(patientInfoHl7.getCurrentRoomCode()));
            msg.setCurrentWardCode(Hl7Value.buildFromHl7(patientInfoHl7.getCurrentWardCode()));
            msg.setFullLocationString(Hl7Value.buildFromHl7(patientInfoHl7.getFullLocationString()));
            msg.setHospitalService(Hl7Value.buildFromHl7(patientInfoHl7.getHospitalService()));
            try {
                msg.setPatientClass(Hl7Value.buildFromHl7(PatientClass.findByHl7Code(patientInfoHl7.getPatientClass())));
            } catch (IllegalArgumentException e) {
                throw new HL7Exception("Patient class was not recognised", e);
            }
            msg.setPatientType(Hl7Value.buildFromHl7(patientInfoHl7.getPatientType()));
            msg.setVisitNumber(patientInfoHl7.getVisitNumber());
        }
        if (patientInfoHl7.pv2SegmentExists()) {
            msg.setModeOfArrival(Hl7Value.buildFromHl7(patientInfoHl7.getModeOfArrivalCode()));
        }
        if (patientInfoHl7.pidSegmentExists()) {
            msg.setEthnicGroup(Hl7Value.buildFromHl7(patientInfoHl7.getEthnicGroup()));
            msg.setMrn(patientInfoHl7.getMrn());
            msg.setNhsNumber(patientInfoHl7.getNHSNumber());
            msg.setPatientBirthDate(Hl7Value.buildFromHl7(patientInfoHl7.getPatientBirthDate()));

            // Despite what the HL7 spec hints at, this death information can occur
            // in any message, not just A03
            String hl7DeathIndicator = patientInfoHl7.getPatientDeathIndicator();
            if (hl7DeathIndicator.equals("Y")) {
                msg.setPatientIsAlive(new Hl7Value<>(false));
            } else if (hl7DeathIndicator.equals("N") || hl7DeathIndicator.equals("")) {
                msg.setPatientIsAlive(new Hl7Value<>(true));
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
     * For building up test IDS, get patient information from HL7 message.
     * @param hl7Msg HL7 message
     * @return PatientInfoHl7 object
     * @throws HL7Exception if HAPI does
     */
    public PatientInfoHl7 getPatientInfo(final Message hl7Msg) throws HL7Exception {
        MSH msh = getMsh(hl7Msg);
        PID pid = getPid(hl7Msg, false);
        PV1 pv1 = getPv1(hl7Msg, false);
        return new PatientInfoHl7(msh, pid, pv1);
    }

    /**
     * Build correct AdtMessage subtype and add any specific data.
     * @param patientInfoHl7 Patient HL7 info
     * @param hl7Msg         HL7 message
     * @param evn            EVN segment, required for any cancellation event
     * @param triggerEvent   ADT message trigger event
     * @return ADT Message build with specific subtype added
     * @throws HL7Exception                      if hl7 message can't be parsed
     * @throws Hl7MessageNotImplementedException if message hasn't been implemented yet
     */
    private AdtMessage buildAdtMessageSubclass(final PatientInfoHl7 patientInfoHl7, final Message hl7Msg,
                                               final EVN evn, final String triggerEvent) throws HL7Exception, Hl7MessageNotImplementedException {

        AdtMessage msg;
        switch (triggerEvent) {
            case "A01":
                //3
                AdmitPatient admitPatient = new AdmitPatient();
                admitPatient.setAdmissionDateTime(Hl7Value.buildFromHl7(patientInfoHl7.getAdmissionDateTime()));
                msg = admitPatient;
                break;
            case "A02":
            case "A06":
            case "A07":
                // 3
                TransferPatient transferPatient = new TransferPatient();
                transferPatient.setAdmissionDateTime(Hl7Value.buildFromHl7(patientInfoHl7.getAdmissionDateTime()));
                msg = transferPatient;
                break;
            case "A03":
                //3
                DischargePatient dischargeMsg = new DischargePatient();
                dischargeMsg.setAdmissionDateTime(Hl7Value.buildFromHl7(patientInfoHl7.getAdmissionDateTime()));
                dischargeMsg.setDischargeDateTime(patientInfoHl7.getDischargeDateTime());
                dischargeMsg.setDischargeDisposition(patientInfoHl7.getDischargeDisposition());
                dischargeMsg.setDischargeLocation(patientInfoHl7.getDischargeLocation());
                msg = dischargeMsg;
                break;
            case "A04":
                //3
                RegisterPatient registerPatient = new RegisterPatient();
                registerPatient.setPresentationDateTime(Hl7Value.buildFromHl7(patientInfoHl7.getAdmissionDateTime()));
                msg = registerPatient;
                break;
            // We are receiving A05 and A14, A38 messages but not doing any scheduling yet
            case "A05":
            case "A14":
            case "A38":
                throw new Hl7MessageNotImplementedException(String.format("Scheduling ADT trigger event not implemented: %s", triggerEvent));
            case "A08":
            case "A28":
            case "A31":
            case "R01": // build update patient info from non-ADT HL7 messages
            case "O01":
                // 2 maybe stage 3
                msg = new UpdatePatientInfo();
                break;
            case "A11":
                //3
                CancelAdmitPatient cancelAdmitPatient = new CancelAdmitPatient();
                setCancellationTime(cancelAdmitPatient, evn);
                msg = cancelAdmitPatient;
                break;
            case "A12":
                //3
                CancelTransferPatient cancelTransferPatient = new CancelTransferPatient();
                setCancellationTime(cancelTransferPatient, evn);
                msg = cancelTransferPatient;
                break;
            case "A13":
                //3
                CancelDischargePatient cancelDischargePatient = new CancelDischargePatient();
                setCancellationTime(cancelDischargePatient, evn);
                msg = cancelDischargePatient;
                break;
            case "A17":
                // special swap locations part 3
                msg = buildSwapLocations(hl7Msg);
                break;
            case "A29":
                msg = new DeletePersonInformation();
                break;
            case "A40":
                MergePatient mergeMsg = new MergePatient();
                setPreviousIdentifiers(mergeMsg, hl7Msg);
                msg = mergeMsg;
                break;
            case "A45":
                MoveVisitInformation moveVisitInformation = new MoveVisitInformation();
                MRG mrg = setPreviousIdentifiers(moveVisitInformation, hl7Msg);
                moveVisitInformation.setPreviousVisitNumber(mrg.getMrg5_PriorVisitNumber().getIDNumber().toString());
                msg = moveVisitInformation;
                break;
            case "A47":
                ChangePatientIdentifiers changePatientIdentifiers = new ChangePatientIdentifiers();
                setPreviousIdentifiers(changePatientIdentifiers, hl7Msg);
                msg = changePatientIdentifiers;
                break;
            default:
                // to keep processes running even if it does not build a valid interchange message, delay exception
                // and create default message type
                throw new Hl7MessageNotImplementedException(String.format("Unimplemented ADT trigger event %s", triggerEvent));
        }
        return msg;
    }

    private MRG setPreviousIdentifiers(PreviousIdentifiers msg, Message hl7Msg) throws HL7Exception {
        MRG mrg = getMrg(hl7Msg);

        assert (mrg != null);
        msg.setPreviousMrn(mrg.getMrg1_PriorPatientIdentifierList(0).getIDNumber().toString());
        msg.setPreviousNhsNumber(mrg.getMrg1_PriorPatientIdentifierList(1).getIDNumber().toString());
        return mrg;
    }

    private SwapLocations buildSwapLocations(Message hl7Msg) throws HL7Exception {
        PID pid = getPid(hl7Msg, true);
        PV1 pv1 = getPv1(hl7Msg, true);

        PatientInfoHl7 otherPatientInfo = new PatientInfoHl7(null, pid, pv1, null);
        SwapLocations msg = new SwapLocations();
        msg.setOtherVisitNumber(otherPatientInfo.getVisitNumber());
        msg.setOtherCurrentBed(Hl7Value.buildFromHl7(otherPatientInfo.getCurrentBed()));
        msg.setOtherCurrentRoomCode(Hl7Value.buildFromHl7(otherPatientInfo.getCurrentRoomCode()));
        msg.setOtherCurrentWardCode(Hl7Value.buildFromHl7(otherPatientInfo.getCurrentWardCode()));
        msg.setOtherFullLocationString(Hl7Value.buildFromHl7(otherPatientInfo.getFullLocationString()));

        msg.setOtherMrn(otherPatientInfo.getMrn());
        msg.setOtherNhsNumber(otherPatientInfo.getNHSNumber());

        return msg;
    }

    /**
     * get MRG segment from hl7 message.
     * @param hl7Msg hl7 message
     * @return MRG segment
     * @throws HL7Exception if HAPI does
     */
    private MRG getMrg(Message hl7Msg) throws HL7Exception {
        MRG mrg;
        if (hl7Msg instanceof ADT_A39) {
            ADT_A39_PATIENT a39Patient = (ADT_A39_PATIENT) hl7Msg.get("PATIENT");
            mrg = a39Patient.getMRG();
        } else if (hl7Msg instanceof ADT_A45) {
            ADT_A45_MERGE_INFO mergeInfo = (ADT_A45_MERGE_INFO) hl7Msg.get("MERGE_INFO");
            mrg = mergeInfo.getMRG();
        } else {
            mrg = (MRG) hl7Msg.get("MRG");
        }
        return mrg;
    }


    /**
     * get EVN segment if it exists.
     * @param hl7Msg hl7 message
     * @return EVN segment
     */
    private EVN getEvn(Message hl7Msg) {
        EVN evn = null;
        try {
            evn = (EVN) hl7Msg.get("EVN");
        } catch (HL7Exception e) {
            // EVN is allowed not to exist
        }
        return evn;
    }

    /**
     * get PV1 segment if it exists.
     * Parses from ADT sources, and non-ADT sources (e.g. Pathology)
     * @param hl7Msg        hl7 message
     * @param secondSegment get the second segment
     * @return PV1
     */
    private PV1 getPv1(Message hl7Msg, boolean secondSegment) throws HL7Exception {
        PV1 pv1 = null;
        String segmentName = secondSegment ? "PV12" : "PV1";

        if (!(hl7Msg instanceof ADT_A45)) {
            try {
                pv1 = (PV1) hl7Msg.get(segmentName);
            } catch (HL7Exception e) {
                // some sections are allowed not to exist
            }
        } else {
            ADT_A45_MERGE_INFO mergeInfo = (ADT_A45_MERGE_INFO) hl7Msg.get("MERGE_INFO");
            pv1 = mergeInfo.getPV1();
        }
        // non ADT message processing
        if (hl7Msg instanceof ORU_R01) {
            ORU_R01 oruR01 = (ORU_R01) hl7Msg;
            pv1 = oruR01.getPATIENT_RESULT().getPATIENT().getVISIT().getPV1();
        } else if (hl7Msg instanceof ORM_O01) {
            ORM_O01 ormO01 = (ORM_O01) hl7Msg;
            pv1 = ormO01.getPATIENT().getPATIENT_VISIT().getPV1();
        }

        return pv1;
    }

    private PV2 getPv2(Message hl7Msg) {
        PV2 pv2 = null;
        try {
            pv2 = (PV2) hl7Msg.get("PV2");
        } catch (HL7Exception e) {
            // allowed to not exist
        }
        return pv2;
    }

    /**
     * Get PID if it exists.
     * Parses from ADT sources, and non-ADT sources (e.g. Pathology)
     * @param hl7Msg        hl7 message
     * @param secondSegment Get the second segment
     * @return PID
     * @throws HL7Exception if PID doesn't exist for a merge message
     */
    private PID getPid(Message hl7Msg, boolean secondSegment) throws HL7Exception {
        // I want the "MRG" segment for A40 messages, is this really
        // the best way to get it? Why do we have to get the PID segment in
        // a different way for an A39/A40 message?
        PID pid = null;
        if (hl7Msg instanceof ADT_A39) {
            ADT_A39_PATIENT a39Patient = (ADT_A39_PATIENT) hl7Msg.get("PATIENT");
            pid = a39Patient.getPID();
        } else {
            String segmentName = secondSegment ? "PID2" : "PID";

            try {
                pid = (PID) hl7Msg.get(segmentName);
            } catch (HL7Exception e) {
                // empty PID2 is allowed
            }
        }
        // processing of non ADT messages
        if ((hl7Msg instanceof ORU_R01)) {
            ORU_R01 msg = (ORU_R01) hl7Msg;
            pid = msg.getPATIENT_RESULT().getPATIENT().getPID();
        } else if (hl7Msg instanceof ORM_O01) {
            ORM_O01 ormO01 = (ORM_O01) hl7Msg;
            pid = ormO01.getPATIENT().getPID();
        }
        return pid;
    }

    /**
     * get MSH segment.
     * @param hl7Msg hl7 message
     * @return MSH segment
     * @throws HL7Exception if MSH doesn't exist
     */
    private MSH getMsh(Message hl7Msg) throws HL7Exception {
        return (MSH) hl7Msg.get("MSH");
    }

    /**
     * Set cancellation time from the evn segment.
     * @param adtCancellation adt cancellation message
     * @param evn             EVN segment
     * @throws DataTypeException if the datetime can't be interpreted as local time
     */
    private void setCancellationTime(AdtCancellation adtCancellation, EVN evn) throws DataTypeException {
        adtCancellation.setCancelledDateTime(HL7Utils.interpretLocalTime(evn.getEvn6_EventOccurred()));
    }
}
