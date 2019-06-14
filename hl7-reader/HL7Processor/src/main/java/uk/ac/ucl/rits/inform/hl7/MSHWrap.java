package uk.ac.ucl.rits.inform.hl7;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.v27.segment.MSH;

/**
 * Wrapper around the HAPI parser's MSH segment object, to make it easier to use.
 * Other methods could be added: see https://hapifhir.github.io/hapi-hl7v2/v27/apidocs/ca/uhn/hl7v2/model/v27/segment/MSH.html
 * e.g. MSH-5 Receiving Application (“Receiving system”)
 */
public interface MSHWrap {
    /**
     * How to get the MSH segment.
     * @return the MSH segment
     */
    MSH getMSH();

    /**
     * @return MSH-3 Sending Application (e.g. “CARECAST”)
     * @throws HL7Exception if HAPI does
     */
    default String getSendingApplication() throws HL7Exception {
        return getMSH().getSendingApplication().getComponent(0).toString();
    }

    /**
     * @return MSH-4 Sending Facility (e.g. “UCLH”)
     * @throws HL7Exception if HAPI does
     */
    default String getSendingFacility() throws HL7Exception {
        return getMSH().getSendingFacility().getComponent(0).toString();
    }

    /**
     * @return MSH-5 Receiving Application
     * @throws HL7Exception if HAPI does
     */
    default String getReceivingApplication() throws HL7Exception {
        return getMSH().getMsh5_ReceivingApplication().getComponent(0).toString();
    }

    /**
     * @return MSH-6 Receiving Facility. Not used in Carecast.
     * @throws HL7Exception if HAPI does
     */
    default String getReceivingFacility() throws HL7Exception {
        return getMSH().getMsh6_ReceivingFacility().getComponent(0).toString();
    }

    /**
     * NB we might want to extract individual components of the timestamp too.
     * @return MSH-7 Date/Time Of Message YYYYMMDDHHMM
     * @throws HL7Exception if HAPI does
     */
    default String getMessageTimestamp() throws HL7Exception {
        return getMSH().getDateTimeOfMessage().toString();
    }

    /**
     * @return MSH-9.1 Message Type (e.g. "ADT")
     * @throws HL7Exception if HAPI does
     */
    default String getMessageType() throws HL7Exception {
        return getMSH().getMessageType().getMessageCode().toString();
    }

    /**
     * @return MSH-9.2 Trigger Event (e.g. "A01")
     * @throws HL7Exception if HAPI does
     */
    default String getTriggerEvent() throws HL7Exception {
        return getMSH().getMessageType().getTriggerEvent().getValue();
    }

    /**
     * @return MSH-10 Message Control ID
     * @throws HL7Exception if HAPI does
     */
    default String getMessageControlID() throws HL7Exception {
        return getMSH().getMsh10_MessageControlID().toString();
    }

    /**
     * @return MSH-11 Processing ID e.g. D (debugging), P (production), T (training)
     * @throws HL7Exception if HAPI does
     */
    default String getProcessingID() throws HL7Exception {
        return getMSH().getMsh11_ProcessingID().getProcessingID().toString();
    }

    /**
     * @return MSH-12 HL7 version used (original version e.g. 2.2 even if we have "converted" to to something else e.g. 2.7)
     * @throws HL7Exception if HAPI does
     */
    default String getVersionID() throws HL7Exception {
        return getMSH().getMsh12_VersionID().getVersionID().toString();
    }

    /**
     * @return MSH-13 Sequence Number. NB Format is Numeric so might not be an int. Epic only.
     * HAPI NM class: A NM contains a single String value.
     * @throws HL7Exception if HAPI does
     */
    default String /*int*/ getSequenceNumber() throws HL7Exception {
        return getMSH().getMsh13_SequenceNumber().toString(); // to be verified
    }

    /**
     * @return MSH-14 Continuation Pointer. Epic only.
     * @throws HL7Exception if HAPI does
     */
    default String getContinuationPointer() throws HL7Exception {
        return getMSH().getMsh14_ContinuationPointer().toString(); // to be verified
    }

    /**
     * @return MSH-15 Accept Acknowledgement Type e.g. AL, NE, ER, SU.
     * @throws HL7Exception if HAPI does
     */
    default String getAcceptAcknowledgementType() throws HL7Exception {
        return getMSH().getMsh15_AcceptAcknowledgmentType().toString(); // to be verified
    }

    /**
     * @return MSH-16 Application Acknowledgement Type e.g. AL, NE, ER, SU. Carecast only
     * @throws HL7Exception if HAPI does
     */
    default String getApplicationAcknowledgementType() throws HL7Exception {
        return getMSH().getMsh16_ApplicationAcknowledgmentType().toString(); // to be verified
    }

    /**
     * @return MSH-18 Character Set. Epic only. I've assumed we only want the first one,
     * but this field can contain multiple character sets
     * @throws HL7Exception if HAPI does
     */
    default String getCharacterSet() throws HL7Exception {
        return getMSH().getCharacterSet(0).toString(); // to be verified
    }
}
