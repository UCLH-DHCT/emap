package uk.ac.ucl.rits.inform.datasources.hl7;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.v26.segment.MSH;

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
        return getMSH().getSendingApplication().getHd1_NamespaceID().getValueOrEmpty();
    }

    /**
     * @return MSH-4 Sending Facility (e.g. “UCLH”)
     * @throws HL7Exception if HAPI does
     */
    default String getSendingFacility() throws HL7Exception {
        return getMSH().getSendingFacility().getHd1_NamespaceID().getValueOrEmpty();
    }

    /**
     * @return MSH-5 Receiving Application
     * @throws HL7Exception if HAPI does
     */
    default String getReceivingApplication() throws HL7Exception {
        return getMSH().getMsh5_ReceivingApplication().getHd1_NamespaceID().getValueOrEmpty();
    }

    /**
     * @return MSH-6 Receiving Facility. Not used in Carecast.
     * @throws HL7Exception if HAPI does
     */
    default String getReceivingFacility() throws HL7Exception {
        return getMSH().getMsh6_ReceivingFacility().getHd1_NamespaceID().getValueOrEmpty();
    }

    /**
     * NB we might want to extract individual components of the timestamp too.
     * @return MSH-7 Date/Time Of Message YYYYMMDDHHMM
     * @throws HL7Exception if HAPI does
     */
    default String getMessageTimestamp() throws HL7Exception {
        return getMSH().getDateTimeOfMessage().getValue();
    }

    /**
     * @return MSH-9.1 Message Type (e.g. "ADT")
     * @throws HL7Exception if HAPI does
     */
    default String getMessageType() throws HL7Exception {
        return getMSH().getMessageType().getMessageCode().getValueOrEmpty();
    }

    /**
     * @return MSH-9.2 Trigger Event (e.g. "A01")
     * @throws HL7Exception if HAPI does
     */
    default String getTriggerEvent() throws HL7Exception {
        return getMSH().getMessageType().getTriggerEvent().getValueOrEmpty();
    }

    /**
     * @return MSH-10 Message Control ID
     * @throws HL7Exception if HAPI does
     */
    default String getMessageControlID() throws HL7Exception {
        return getMSH().getMsh10_MessageControlID().getValueOrEmpty();
    }

    /**
     * @return MSH-11 Processing ID e.g. D (debugging), P (production), T (training)
     * @throws HL7Exception if HAPI does
     */
    default String getProcessingID() throws HL7Exception {
        return getMSH().getMsh11_ProcessingID().getProcessingID().getValueOrEmpty();
    }

    /**
     * @return MSH-12 HL7 version used (original version e.g. 2.2 even if we have "converted" to to something else e.g. 2.7)
     * @throws HL7Exception if HAPI does
     */
    default String getVersionID() throws HL7Exception {
        return getMSH().getMsh12_VersionID().getVersionID().getValueOrEmpty();
    }
}
