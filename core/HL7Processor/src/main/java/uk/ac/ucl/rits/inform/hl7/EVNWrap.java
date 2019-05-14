package uk.ac.ucl.rits.inform.hl7;

import java.time.Instant;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.v27.segment.EVN;
import ca.uhn.hl7v2.model.v27.datatype.NULLDT; // Special datatype used in fields which have been withdrawn from the HL7 specification and should not contain a value.
import ca.uhn.hl7v2.model.v27.datatype.XCN;

/**
 * class EVNWrap
 * 
 * Wrapper around the HAPI parser's EVN segment object, to make it easier to use.
 * 
 * Reference page: https://hapifhir.github.io/hapi-hl7v2/v27/apidocs/ca/uhn/hl7v2/model/v27/segment/EVN.html 
 * 
 */

public interface EVNWrap {
    EVN getEVN();

    /**
     * @return Is this a test object which should generate synthetic data instead
     * of using the HL7 message data?
     */
    boolean isTest();

    default boolean EVNSegmentExists() {
        return getEVN() != null;
    }
    
    /**
     * 
     * @return EVN-1 Event Type Code (e.g. A01)
     * @throws HL7Exception
     */
    default String getEventType() throws HL7Exception {
        return getEVN().getEvn1_EventTypeCode().toString();
    }


    /**
     * @return EVN-2 Recorded Date/Time
     * @throws HL7Exception
     */
    default Instant getRecordedDateTime() throws HL7Exception {
        return HL7Utils.interpretLocalTime(getEVN().getEvn2_RecordedDateTime());
    }


     /**
     * 
     * @return EVN-4 Event Reason Code (e.g. ADM)
     * @throws HL7Exception
     */
    default String getEventReasonCode() throws HL7Exception {
        return getEVN().getEvn4_EventReasonCode().getComponent(0).toString();
    }


     /**
     * Carecast has EVN-5.1 Operator ID, EVN-5.2 Operator Surname, 
     * EVN-5.3 Operator First name. Epic has ID of the user who triggered the message
     * 
     * @return EVN-5 Operator ID - could be multiple but we just take first for now
     * @throws HL7Exception
     */
    default String getOperatorID() throws HL7Exception {
        return getEVN().getEvn5_OperatorID(0).getPersonIdentifier().toString();
    }


    /**
     * 
     * @return EVN-6 Event Occurred. Epic only. Eg. transfer date and time for an A02 message
     * If this is Epic only where does Carecast specify transfer times?
     * @throws HL7Exception
     */
    default Instant getEventOccurred() throws HL7Exception {
        if (isTest()) {
            return Instant.now();
        }
        return HL7Utils.interpretLocalTime(getEVN().getEvn6_EventOccurred());
    }

}
