// EVNWrap.java

package uk.ac.ucl.rits.inform;

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

public class EVNWrap {

    private EVN _evn;

    /**
     * Constructor
     * 
     * @param myEVN EVN segment, obtained by parsing the message to which this segment relates (msg.getEVN())
     */
    public EVNWrap(EVN myEVN) {
        _evn = myEVN;
    }


     /**
     * 
     * @return EVN-1 Event Type Code (e.g. A01)
     * @throws HL7Exception
     */
    public String getEventType() throws HL7Exception {
        return _evn.getEvn1_EventTypeCode().toString(); // need to test
    }


     /**
     * NB we might want to extract individual components of the timestamp too
     * 
     * @return EVN-2 Recorded Date/Time - Current Date/time in format YYYYMMDDHHMM
     * @throws HL7Exception
     */
    public String getRecordedDateTime() throws HL7Exception {
        return _evn.getEvn2_RecordedDateTime().toString(); // need to test
    } 




     /**
     * Carecast has EVN-5.1 Operator ID, EVN-5.2 Operator Surname, 
     * EVN-5.3 Operator First name. Epic has ID of the user who triggered the message
     * 
     * @return EVN-5 Operator ID - could be multiple but we just take first for now
     * @throws HL7Exception
     */
    public String getOperatorID() throws HL7Exception {
        return _evn.getEvn5_OperatorID(0).getPersonIdentifier().toString(); // need to test
    }

}