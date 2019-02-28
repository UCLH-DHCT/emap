// PIDWrap.java
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

import ca.uhn.hl7v2.model.v27.segment.PID;

/**
 * class PIDWrap
 * 
 * Wrapper around the HAPI parser's PID segment object, to make it easier to use.
 * 
 * Reference: see https://hapifhir.github.io/hapi-hl7v2/v27/apidocs/ca/uhn/hl7v2/model/v27/segment/PID.html
 */
public class PIDWrap {

    private PID _pid;

    /**
     * Constructor
     * 
     * 
     * 
     * @param myPID PID segment, obtained by parsing the message to which this segment relates (msg.getPID())
     */
    public PIDWrap(PID myPID) {
        _pid = myPID;
    }

    // Will Epic follow this convention?
    // System.out.println("PID-3.1[1] MRN = " + pid.getPatientIdentifierList(0).getComponent(0).toString());
    // System.out.println("PID-3.1[2] NHSNumber = " + pid.getPatientIdentifierList(1).getComponent(0).toString());


    /**
     * 
     * @return PID-3.1[1] - in Carecast this is the MRN
     * @throws HL7Exception
     */
    public String getFirstIdentifier() throws HL7Exception {
        return _pid.getPatientIdentifierList(0).getComponent(0).toString();
    }

    /**
     * 
     * @return PID-3.1[2] - in Carecast this is the NHS number
     * @throws HL7Exception
     */
    public String getSecondIdentifier() throws HL7Exception {
        return _pid.getPatientIdentifierList(1).getComponent(0).toString();
    }

    /*
        XPN xpn = pid.getPatientName(0);
        
        System.out.println("PID-5.1 family name is " + xpn.getFamilyName().getSurname().getValue());
        System.out.println("PID-5.2 given name is " + xpn.getGivenName().getValue());
        System.out.println("PID-5.3 middle name or initial: " + xpn.getSecondAndFurtherGivenNamesOrInitialsThereof().getValue()); 
        System.out.println("PID-5.5 title is " + xpn.getPrefixEgDR().getValue());
        
    */

    /**
     * get name functions - these all assume patient has only one name.
     * If more are required then need to get use _pid.getPatientName(1)... etc
     * Certainly in Epic it appears that only one name data (XPN) object is stored.
     * 
     * @return PID-5.1 family name
     * @throws HL7Exception
     */
    public String getPatientFamilyName() throws HL7Exception {
        return _pid.getPatientName(0).getFamilyName().getSurname().getValue();
    }

    // PID-5.2 given name
    public String getPatientGivenName() throws HL7Exception {
        return _pid.getPatientName(0).getGivenName().getValue();
    }

    // PID-5.3 middle name or initial
    public String getPatientMiddleName() throws HL7Exception {
        return _pid.getPatientName(0).getSecondAndFurtherGivenNamesOrInitialsThereof().getValue();
    }

    // PID-5.5 title
    public String getPatientTitle() throws HL7Exception {
        return _pid.getPatientName(0).getPrefixEgDR().getValue();
    }

    // convenience method - title + firstname + middlename + surname
    public String getPatientFullName() throws HL7Exception {
        String result = this.getPatientTitle() + " " + this.getPatientGivenName() + " "
            + this.getPatientMiddleName() + " " + this.getPatientFamilyName();
        return result;
    }


    /**
     * We could probably use the HAPI functions to obtain the different components of this result,
     * e.g. for easier conversion to Postgres timestamp format.
     * 
     * @return PID-7.1 birthdatetime
     * @throws HL7Exception
     */
    public String getPatientBirthDate() throws HL7Exception {
        return _pid.getDateTimeOfBirth().toString();
    }


    /**
     * 
     * @return PID-8 sex
     * @throws HL7Exception
     */
    public String getPatientSex() throws HL7Exception {
        return _pid.getAdministrativeSex().getIdentifier().getValue();
    }





}
