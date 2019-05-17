// PIDWrap.java
package uk.ac.ucl.rits.inform.hl7;

import java.time.Instant;
import java.util.Vector;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.v27.segment.PID;

/**
 * class PIDWrap
 * 
 * Wrapper around the HAPI parser's PID segment object, to make it easier to use.
 * 
 * Reference: see https://hapifhir.github.io/hapi-hl7v2/v27/apidocs/ca/uhn/hl7v2/model/v27/segment/PID.html
 * 
 * Some functions have Epic and Carecast versions as they appear from the docs to return different information.
 * This needs to be verified with real data, which may remove the need for some of these different versions
 * - or, perhaps, add the need for more of them.
 * 
 * Example of *Carecast* PID segments:
 * 
 * 
 * 
 * Below are the encoding characters:
    Field Separator (normally |)
    Component Separator (normally ^)
    Subcomponent Separator (normally &)
    Field Repeat Separator (normally ~)
    Escape Character (normally \)

 */
public class PIDWrap {

    private PID _pid;

    /**
     * Constructor
     * 
     * @param myPID PID segment, obtained by parsing the message to which this segment relates (msg.getPID())
     */
    public PIDWrap(PID myPID) {
        _pid = myPID;
    }

    
    /**
     * 
     * @return PID-3.1[1] - in Carecast this is the MRN. Will Epic follow this convention?
     * @throws HL7Exception
     */
    public String getPatientFirstIdentifier() throws HL7Exception {
        return _pid.getPatientIdentifierList(0).getComponent(0).toString();
    }

    /**
     * 
     * @return PID-3.1[2] - in Carecast this is the NHS number. Will Epic follow this convention?
     * @throws HL7Exception
     */
    public String getPatientSecondIdentifier() throws HL7Exception {
        return _pid.getPatientIdentifierList(1).getComponent(0).toString();
    }

    

    /**
     * get name functions - these all assume patient has only one name.
     * If more are required then need to get use _pid.getPatientName(1)... etc
     * Certainly in Epic it appears that only one name data (XPN) object is stored.
     * XPN xpn = pid.getPatientName(0); etc
     * 
     * NB Epic also has Suffix, Prefix, Academic degree, Name Type
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
     * PID-6 Mother's Maiden Name. This does not appear in the Carecast spec. 
     * Epic: optional. Escape characters can be translated.
     * @return
     * @throws HL7Exception
     */


    /**
     * We could probably use the HAPI functions to obtain the different components of this result,
     * e.g. for easier conversion to Postgres timestamp format.
     * 
     * @return PID-7.1 birthdatetime
     * @throws HL7Exception
     */
    public Instant getPatientBirthDate() throws HL7Exception {
        return HL7Utils.interpretLocalTime(_pid.getDateTimeOfBirth());
    }


    /**
     * 
     * @return PID-8 sex
     * @throws HL7Exception
     */
    public String getPatientSex() throws HL7Exception {
        return _pid.getAdministrativeSex().getIdentifier().getValue();
    }


    /**
     * PID-9 Patient Alias. Epic only. 
     * If the seventh component (name type code) of the field is M, the value in the first component 
     * will be treated as patient maiden name. The example format to receive patient maiden name:
     * <Patient maiden name>^^^^^^M~
     * @return
     * @throws HL7Exception
     */


    /**
     * PID-10 Race. Epic only(?)
     * Format: <race>^^^^^^^^~<race>^^^^^^^^
     * @return
     * @throws HL7Exception
     */


    /**
     * @return the patient postcode (PID-11, first rep, component 5)
     */
    public String getPatientZipOrPostalCode() {
        return _pid.getPatientAddress(0).getZipOrPostalCode().getValueOrEmpty();
    }

    /**
     * May be different for Epic
     * @return Carecast - PID-13.1 (1st repeat) home phone
     * @throws HL7Exception
     */
    public String getCarecastPatientHomePhoneNumber() throws HL7Exception {
        return _pid.getPhoneNumberHome(0).getTelephoneNumber().getValue();
    }

    /**
     * May be different for Epic
     * @return Carecast - PID-13.1 (2nd repeat) mobile
     * @throws HL7Exception
     */
    public String getCarecastPatientMobilePhoneNumber() throws HL7Exception {
        return _pid.getPhoneNumberHome(1).getTelephoneNumber().getValue();
    }

    /**
     * May be different for Epic
     * @return Carecast - PID-13.1 (3rd repeat) email
     * @throws HL7Exception
     */
    public String getCarecastPatientEmail() throws HL7Exception {
        return _pid.getPhoneNumberHome(2).getTelephoneNumber().getValue();
    }

   

    /**
     * 
     * @return
     * @throws HL7Exception
     */
    public String getEpicPatientHomePhoneNumber() throws HL7Exception {
        return "Not yet implemented";
    }

    /**
     * 
     * @return
     * @throws HL7Exception
     */
    public String getEpicPatientEmail() throws HL7Exception {
        return "Not yet implemented";
    }

    public Vector<String> getEpicPatientMultipleEmailAddresses() throws HL7Exception {
        Vector v = new Vector(1,1);
        String s = "Not yet implemented";
        v.add(s);
        return v;
    }

     /**
     * 
     * @return Carecast - PID-14.1 business phone
     * @throws HL7Exception
     */
    public String getCarecastPatientBusinessPhoneNumber() throws HL7Exception {
        return _pid.getPhoneNumberBusiness(0).getTelephoneNumber().getValue();
    } 

    /**
     * Epic has different possible formats, according to their doc:
     * 
     * (nnn)nnn-nnnnx<extension>
     * or
     * ^^^^^<City/area code>^<Number>^<Extension>
     * or
     * #<text>
     * 
     * I don't know how HAPI will deal with this. Need to see real messages.
     * 
     * @return 
     * @throws HL7Exception
     */
    public String getEpicPatientBusinessPhoneNumber() throws HL7Exception {
        return "Not yet implemented";
    }

    // language - seems different for Carecast and Epic
    // Carecast has PID-15.1 Interpreter code and PID-15.4 Language code.
    // Epic just seems to have one code value. Format: <code>^^^^^^^^~
    // CWE c = pid.getPid15_PrimaryLanguage()
    // component 1: c.getCwe1_Identifier() [ST type]
    // component 4: c.getCwe4_AlternateIdentifier() [ST]


    // PID-16 Marital Status. NOTE: The Carecast spec says that it uses the locally-defined field ZLC.15 instead. 
    // Epic: Format: <code>^^^^^^^^
    // cwe c = pid.getPid16_MaritalStatus()

    /**
     * 
     * @return PID-17 Religion - or should this be PID-17.1?
     * @throws HL7Exception
     */
    public String getPatientReligion() throws HL7Exception {
        return _pid.getReligion().getComponent(0).toString();
    }

    
    /**
     * 
     * @return PID-18.1 (or PID-18?) Patient Account Number.
     * @throws HL7Exception
     */
    public String getPatientAccountNumber() throws HL7Exception {
        return _pid.getPatientAccountNumber().getComponent(0).toString();
    }

    // PID-21 Mother's Identifier (for newborns). This is not mapped in the Carecast spec
    // Epic: This field is used to link a newborn to his or her mother. It should contain the MRN or a visit
    // identifier for the mother of the patient in the PID segment of the message.
    // CX[] c = pid.getPid21_MotherSIdentifier()

    // PID-22 Ethnic Group.  In place of PID-22 Carecast has PID-10.1 Ethnicity, although according to the spec it doesn't have PID-10!!!
    // Epic: Patient ethnic group. Only the first component is used
    // CWE[] getPid22_EthnicGroup() Returns all repetitions of Ethnic Group (PID-22).
    // Epic: Patient ethnic group. Only the first component is used. Format: <ethnic group>^^^^^^^^~
    /**
     * getEthnicGroup. Looks like Carecast and Epic may be the same (need to see real data to check)
     * @return
     * @throws HL7Exception
     */
    public String getEthnicGroup() throws HL7Exception {
        return _pid.getPid22_EthnicGroup(0).getText().toString(); // ???need data to check this is correct.
    }

    // PID-23 Birth Place This is not mapped in the Carecast spec.
    // Epic: Birth city and state. HL7 address format is used rather than the string format defined in the standard. State can be mapped using a translation table 
    // Need to see real messages to understand what they mean in line above.
    // Possibly pid.getPid23_BirthPlace() [ST]

    // PID-24 Multiple Birth Indicator. This is not mapped in the Carecast spec.
    // Epic: This field can be set to "Y" or "N" to denote whether the patient was born as part of a multiple birth (twins, triplets, etc.).
    // ID getPid24_MultipleBirthIndicator()
    public String getMultipleBirthIndicator() throws HL7Exception {
        return _pid.getPid24_MultipleBirthIndicator().toString(); // ??? need to see data to check
    }

    // PID-26 Citizenship. This is not mapped in the Carecast spec. Epic: This field can be mapped using a translation table.

    // PID-28 Nationality. This is not mapped in the Carecast spec. Standard: The PID-28 field was retained for backward compatibility
    // only as of v2.4 and was withdrawn and removed from this message structure as of v2.7. It is recommended to refer to 
    // PID-10 - Race, PID-22 - Ethnic group and PID-26 - Citizenship. Epic: This field can be mapped using a translation table.

    /**
     * 
     * @return PID-29 (or PID-29.1?) Patient Death Date and Time. Carecast: YYYYMMDDHHmm format though hhmm is mainly 0000
     * @throws HL7Exception
     */
    public String getPatientDeathDateTime() throws HL7Exception {
        return _pid.getPatientDeathDateAndTime().toString();
    }

    /**
     * 
     * @return PID-30 Patient Death Indicator. Carcast: Y = dead, N = alive.
     * @throws HL7Exception
     */
    public String getPatientDeathIndicator() throws HL7Exception {
        return _pid.getPatientDeathIndicator().getValue(); 
    }

    // Epic - PID-32 Identity Reliability Code ??
    // From HAPI docs looks like there can be multiple such codes.
}
