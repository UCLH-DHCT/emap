package uk.ac.ucl.rits.inform.pipeline.hl7;

import java.time.Instant;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.v26.segment.EVN;

/**
 * Wrapper around the HAPI parser's EVN segment object, to make it easier to use.
 * Reference page: https://hapifhir.github.io/hapi-hl7v2/v27/apidocs/ca/uhn/hl7v2/model/v27/segment/EVN.html
 */

public interface EVNWrap {
    /**
     * @return the EVN object from the HL7 message
     */
    EVN getEVN();

    /**
     * @return whether the EVN segment exists
     */
    default boolean evnSegmentExists() {
        return getEVN() != null;
    }

    /**
     * @return EVN-1 Event Type Code (e.g. A01)
     * @throws HL7Exception if HAPI does
     */
    default String getEventType() throws HL7Exception {
        return getEVN().getEvn1_EventTypeCode().toString();
    }

    /**
     * @return EVN-2 Recorded Date/Time
     * @throws HL7Exception if HAPI does
     */
    default Instant getRecordedDateTime() throws HL7Exception {
        return HL7Utils.interpretLocalTime(getEVN().getEvn2_RecordedDateTime());
    }

    /**
     * @return EVN-4 Event Reason Code (e.g. ADM)
     * @throws HL7Exception if HAPI does
     */
    default String getEventReasonCode() throws HL7Exception {
        return getEVN().getEvn4_EventReasonCode().getValue();
    }

     /**
     * Carecast has EVN-5.1 Operator ID, EVN-5.2 Operator Surname,
     * EVN-5.3 Operator First name. Epic has ID of the user who triggered the message
     *
     * @return EVN-5 Operator ID - could be multiple but we just take first for now
     * @throws HL7Exception if HAPI does
     */
    default String getOperatorID() throws HL7Exception {
        return getEVN().getEvn5_OperatorID(0).getXcn1_IDNumber().getValue();
    }


    /**
     * @return EVN-6 Event Occurred. Epic only. Eg. transfer date and time for an A02 message
     * If this is Epic only where does Carecast specify transfer times?
     * @throws HL7Exception if HAPI does
     */
    default Instant getEventOccurred() throws HL7Exception {
        return HL7Utils.interpretLocalTime(getEVN().getEvn6_EventOccurred());
    }

}
