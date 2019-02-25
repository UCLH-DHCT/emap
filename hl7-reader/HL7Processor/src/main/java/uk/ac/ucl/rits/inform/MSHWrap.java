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


public class MSHWrap {

    private MSH msh;

    /**
     * Constructor
     * 
     * 
     * 
     * @param myMSH MSH segment, obtained by parsing the message to which this segment relates (msg.getMSH())
     */
    public MSHWrap(MSH myMSH) {
        msh = myMSH;
    }

    /*
        System.out.println("sending facility = " + msh.getSendingFacility().getComponent(0).toString()); // MSH-4 Sending Facility (“UCLH”)
        // MSH-5 Receiving Application (“Receiving system”)
        System.out.println("messageTimestamp = " + msh.getDateTimeOfMessage().toString()); // MSH-7 Date/Time Of Message YYYYMMDDHHMM
        System.out.println("message type = " + msh.getMessageType().getMessageCode().toString()); // MSH-9.1	Message Type (ADT)
        System.out.println("trigger event = " + msh.getMessageType().getTriggerEvent().getValue()); // MSH-9.2	Trigger Event (A01)
      */

    /**
     * 
     * @return MSH-3 Sending Application (e.g. “CARECAST”)
     */
    public String getSendingApplication() throws HL7Exception {
        return msh.getSendingApplication().getComponent(0).toString();
    }



}
