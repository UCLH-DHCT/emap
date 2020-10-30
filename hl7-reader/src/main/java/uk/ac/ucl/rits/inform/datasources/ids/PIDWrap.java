package uk.ac.ucl.rits.inform.datasources.ids;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.v26.datatype.DTM;
import ca.uhn.hl7v2.model.v26.segment.PID;

import java.time.Instant;

/**
 * Wrapper around the HAPI parser's PID segment object, to make it easier to use.
 * <p>
 * Reference: see https://hapifhir.github.io/hapi-hl7v2/v27/apidocs/ca/uhn/hl7v2/model/v27/segment/PID.html
 * <p>
 * Some functions have Epic and Carecast versions as they appear from the docs to return different information.
 * This needs to be verified with real data, which may remove the need for some of these different versions
 * - or, perhaps, add the need for more of them.
 * <p>
 * Example of *Carecast* PID segments:
 * <p>
 * <p>
 * <p>
 * Below are the encoding characters:
 * Field Separator (normally |)
 * Component Separator (normally ^)
 * Subcomponent Separator (normally &)
 * Field Repeat Separator (normally ~)
 * Escape Character (normally \)
 */
interface PIDWrap {
    /**
     * @return the PID segment
     */
    PID getPID();

    /**
     * @return true if the PID segment exists.
     */
    default boolean pidSegmentExists() {
        return getPID() != null;
    }

    /**
     * @return PID-3.1[1] - in Carecast this is the MRN. Will Epic follow this convention?
     * @throws HL7Exception if HAPI does
     */
    default String getMrn() throws HL7Exception {
        return getPID().getPatientIdentifierList(0).getComponent(0).toString();
    }

    /**
     * @return PID-3.1[2] - in Carecast this is the NHS number. Will Epic follow this convention?
     * @throws HL7Exception if HAPI does
     */
    default String getNHSNumber() throws HL7Exception {
        return getPID().getPatientIdentifierList(1).getComponent(0).toString();
    }

    /**
     * get name functions - these all assume patient has only one name.
     * If more are required then need to get use _pid.getPatientName(1)... etc
     * Certainly in Epic it appears that only one name data (XPN) object is stored.
     * XPN xpn = getPID().getPatientName(0); etc
     * <p>
     * NB Epic also has Suffix, Prefix, Academic degree, Name Type
     * @return PID-5.1 family name
     * @throws HL7Exception if HAPI does
     */
    default String getPatientFamilyName() throws HL7Exception {
        return getPID().getPatientName(0).getFamilyName().getSurname().getValue();
    }

    /**
     * @return PID-5.2 family name
     * @throws HL7Exception if HAPI does
     */
    default String getPatientGivenName() throws HL7Exception {
        return getPID().getPatientName(0).getGivenName().getValue();
    }

    /**
     * @return PID-5.3 family name
     * @throws HL7Exception if HAPI does
     */
    default String getPatientMiddleName() throws HL7Exception {
        return getPID().getPatientName(0).getSecondAndFurtherGivenNamesOrInitialsThereof().getValue();
    }

    /**
     * @return PID-5.5 family name
     * @throws HL7Exception if HAPI does
     */
    default String getPatientTitle() throws HL7Exception {
        return getPID().getPatientName(0).getPrefixEgDR().getValue();
    }

    /**
     * @return full name
     * @throws HL7Exception if HAPI does
     */
    default String getPatientFullName() throws HL7Exception {
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
     * Carry out timezone correction if the datetime is not truncated to the day.
     * @return PID-7.1 birthdatetime, null if field is empty
     * @throws DataTypeException if HAPI does
     */
    default Instant getPatientBirthDate() throws DataTypeException {
        DTM dateTime = getPID().getDateTimeOfBirth();
        if (dateTime.getValue() == null) {
            return null;
        }

        if (isNotTruncatedToDay(dateTime)) {
            return HL7Utils.interpretLocalTime(dateTime);
        }
        dateTime.setOffset(0);
        return dateTime.getValueAsDate().toInstant();
    }

    /**
     * Is the date time truncated to the day.
     * @param dateTime date time object
     * @return true if there is only year, month and day given
     */
    private boolean isNotTruncatedToDay(DTM dateTime) {
        return dateTime.getValue().length() != 8;
    }


    /**
     * @return PID-8 sex
     * @throws HL7Exception if HAPI does
     */
    default String getPatientSex() throws HL7Exception {
        return getPID().getAdministrativeSex().getValue();
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
    default String getPatientZipOrPostalCode() {
        return getPID().getPatientAddress(0).getZipOrPostalCode().getValue();
    }

    /**
     * May be different for Epic.
     * @return Carecast - PID-13.1 (1st repeat) home phone
     * @throws HL7Exception if HAPI does
     */
    default String getCarecastPatientHomePhoneNumber() throws HL7Exception {
        return getPID().getPhoneNumberHome(0).getTelephoneNumber().getValue();
    }

    /**
     * May be different for Epic.
     * @return Carecast - PID-13.1 (2nd repeat) mobile
     * @throws HL7Exception if HAPI does
     */
    default String getCarecastPatientMobilePhoneNumber() throws HL7Exception {
        return getPID().getPhoneNumberHome(1).getTelephoneNumber().getValue();
    }

    /**
     * May be different for Epic.
     * @return Carecast - PID-13.1 (3rd repeat) email
     * @throws HL7Exception if HAPI does
     */
    default String getCarecastPatientEmail() throws HL7Exception {
        return getPID().getPhoneNumberHome(2).getTelephoneNumber().getValue();
    }

    /**
     * @return patient home phone number
     * @throws HL7Exception if HAPI does
     */
    default String getEpicPatientHomePhoneNumber() throws HL7Exception {
        return "Not yet implemented";
    }

    /**
     * @return patient email
     * @throws HL7Exception if HAPI does
     */
    default String getEpicPatientEmail() throws HL7Exception {
        return "Not yet implemented";
    }

    /**
     * @return Carecast - PID-14.1 business phone
     * @throws HL7Exception if HAPI does
     */
    default String getCarecastPatientBusinessPhoneNumber() throws HL7Exception {
        return getPID().getPhoneNumberBusiness(0).getTelephoneNumber().getValue();
    }

    /**
     * Epic has different possible formats, according to their doc:
     * <p>
     * (nnn)nnn-nnnnx<extension>
     * or
     * ^^^^^<City/area code>^<Number>^<Extension>
     * or
     * #<text>
     * <p>
     * I don't know how HAPI will deal with this. Need to see real messages.
     * @return the patient business phone number
     * @throws HL7Exception if HAPI does
     */
    default String getEpicPatientBusinessPhoneNumber() throws HL7Exception {
        return "Not yet implemented";
    }

    // language - seems different for Carecast and Epic
    // Carecast has PID-15.1 Interpreter code and PID-15.4 Language code.
    // Epic just seems to have one code value. Format: <code>^^^^^^^^~
    // CWE c = getPID().getPid15_PrimaryLanguage()
    // component 1: c.getCwe1_Identifier() [ST type]
    // component 4: c.getCwe4_AlternateIdentifier() [ST]


    // PID-16 Marital Status. NOTE: The Carecast spec says that it uses the locally-defined field ZLC.15 instead.
    // Epic: Format: <code>^^^^^^^^
    // cwe c = getPID().getPid16_MaritalStatus()

    /**
     * @return PID-17 Religion - or should this be PID-17.1?
     * @throws HL7Exception if HAPI does
     */
    default String getPatientReligion() throws HL7Exception {
        return getPID().getReligion().getComponent(0).toString();
    }

    /**
     * @return PID-18.1 (or PID-18?) Patient Account Number.
     * @throws HL7Exception if HAPI does
     */
    default String getPatientAccountNumber() throws HL7Exception {
        return getPID().getPatientAccountNumber().getComponent(0).toString();
    }

    // PID-21 Mother's Identifier (for newborns). This is not mapped in the Carecast spec
    // Epic: This field is used to link a newborn to his or her mother. It should contain the MRN or a visit
    // identifier for the mother of the patient in the PID segment of the message.
    // CX[] c = getPID().getPid21_MotherSIdentifier()

    // PID-22 Ethnic Group.  In place of PID-22 Carecast has PID-10.1 Ethnicity, although according to the spec it doesn't have PID-10!!!
    // Epic: Patient ethnic group. Only the first component is used
    // CWE[] getPid22_EthnicGroup() Returns all repetitions of Ethnic Group (PID-22).
    // Epic: Patient ethnic group. Only the first component is used. Format: <ethnic group>^^^^^^^^~

    /**
     * Looks like Carecast and Epic may be the same (need to see real data to check).
     * @return the ethnic group
     * @throws HL7Exception if HAPI does
     */
    default String getEthnicGroup() throws HL7Exception {
        return getPID().getPid22_EthnicGroup(0).getText().toString(); // ???need data to check this is correct.
    }

    // PID-23 Birth Place This is not mapped in the Carecast spec.
    // Epic: Birth city and state. HL7 address format is used rather than the string format defined in the standard.
    // State can be mapped using a translation table
    // Need to see real messages to understand what they mean in line above.
    // Possibly getPID().getPid23_BirthPlace() [ST]

    /**
     * PID-24 Multiple Birth Indicator. This is not mapped in the Carecast spec.
     * Epic: This field can be set to "Y" or "N" to denote whether the patient was born as part of a multiple birth (twins, triplets, etc.).
     * @return PID-24 Multiple Birth Indicator
     * @throws HL7Exception if HAPI does
     */
    default String getMultipleBirthIndicator() throws HL7Exception {
        return getPID().getPid24_MultipleBirthIndicator().toString(); // ??? need to see data to check
    }

    // PID-26 Citizenship. This is not mapped in the Carecast spec. Epic: This field can be mapped using a translation table.

    // PID-28 Nationality. This is not mapped in the Carecast spec. Standard: The PID-28 field was retained for backward compatibility
    // only as of v2.4 and was withdrawn and removed from this message structure as of v2.7. It is recommended to refer to
    // PID-10 - Race, PID-22 - Ethnic group and PID-26 - Citizenship. Epic: This field can be mapped using a translation table.

    /**
     * @return PID-29 (or PID-29.1?) Patient Death Date and Time. Carecast: YYYYMMDDHHmm format though hhmm is mainly 0000
     * @throws HL7Exception if HAPI does
     */
    default Instant getPatientDeathDateTime() throws HL7Exception {
        return HL7Utils.interpretLocalTime(getPID().getPatientDeathDateAndTime());
    }

    /**
     * @return PID-30 Patient Death Indicator. Carcast: Y = dead, N = alive.
     * @throws HL7Exception if HAPI does
     */
    default String getPatientDeathIndicator() throws HL7Exception {
        return getPID().getPatientDeathIndicator().getValueOrEmpty();
    }

    // Epic - PID-32 Identity Reliability Code ??
    // From HAPI docs looks like there can be multiple such codes.
}
