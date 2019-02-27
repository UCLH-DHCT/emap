// MSHWrap.java
package uk.ac.ucl.rits.inform;

import ca.uhn.hl7v2.HL7Exception;

import ca.uhn.hl7v2.model.v27.datatype.CWE;
import ca.uhn.hl7v2.model.v27.datatype.CX;
import ca.uhn.hl7v2.model.v27.datatype.DTM;
import ca.uhn.hl7v2.model.v27.datatype.HD;
import ca.uhn.hl7v2.model.v27.datatype.ID;
import ca.uhn.hl7v2.model.v27.datatype.IS;
import ca.uhn.hl7v2.model.v27.datatype.MSG;
import ca.uhn.hl7v2.model.v27.datatype.PL;
import ca.uhn.hl7v2.model.v27.datatype.SAD;
import ca.uhn.hl7v2.model.v27.datatype.XAD;
import ca.uhn.hl7v2.model.v27.datatype.XCN;
import ca.uhn.hl7v2.model.v27.datatype.XPN;
import ca.uhn.hl7v2.model.v27.datatype.XTN;
import ca.uhn.hl7v2.model.AbstractType;

import ca.uhn.hl7v2.model.v27.segment.MSH;

/**
 * class MSHWrap
 * 
 * Wrapper around the HAPI parser's MSH segment object, to make it easier to use.
 * 
 * Other methods could be added: see https://hapifhir.github.io/hapi-hl7v2/v27/apidocs/ca/uhn/hl7v2/model/v27/segment/MSH.html
 * e.g. MSH-5 Receiving Application (“Receiving system”)
 */
public class MSHWrap {

    private MSH _msh;

    /**
     * Constructor
     * 
     * 
     * 
     * @param myMSH MSH segment, obtained by parsing the message to which this segment relates (msg.getMSH())
     */
    public MSHWrap(MSH myMSH) {
        _msh = myMSH;
    }


    /**
     * 
     * @return MSH-3 Sending Application (e.g. “CARECAST”)
     * @throws HL7Exception
     */
    public String getSendingApplication() throws HL7Exception {
        return _msh.getSendingApplication().getComponent(0).toString();
    }


    /**
     * 
     * @return MSH-4 Sending Facility (e.g. “UCLH”)
     * @throws HL7Exception
     */
    public String getSendingFacility() throws HL7Exception {
        return _msh.getSendingFacility().getComponent(0).toString();
    }


    /**
     * NB we might want to extract individual components of the timestamp too
     * 
     * @return MSH-7 Date/Time Of Message YYYYMMDDHHMM
     * @throws HL7Exception
     */
    public String getMessageTimestamp() throws HL7Exception {
        return _msh.getDateTimeOfMessage().toString();
    } 


    /**
     * 
     * @return MSH-9.1	Message Type (e.g. "ADT")
     * @throws HL7Exception
     */
    public String getMessageType() throws HL7Exception {
        return _msh.getMessageType().getMessageCode().toString();
    }


    /**
     * 
     * @return MSH-9.2	Trigger Event (e.g. "A01")
     * @throws HL7Exception
     */
    public String getTriggerEvent() throws HL7Exception {
        return _msh.getMessageType().getTriggerEvent().getValue();
    }

}
