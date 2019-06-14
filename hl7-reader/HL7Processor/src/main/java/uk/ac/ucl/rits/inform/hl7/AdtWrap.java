package uk.ac.ucl.rits.inform.hl7;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v27.group.ADT_A39_PATIENT;
import ca.uhn.hl7v2.model.v27.message.ADT_A39;
import ca.uhn.hl7v2.model.v27.segment.EVN;
import ca.uhn.hl7v2.model.v27.segment.MRG;
import ca.uhn.hl7v2.model.v27.segment.MSH;
import ca.uhn.hl7v2.model.v27.segment.PD1;
import ca.uhn.hl7v2.model.v27.segment.PID;
import ca.uhn.hl7v2.model.v27.segment.PV1;

/**
 * Wrapper for an ADT message so we can find what we need more easily.
 */
public class AdtWrap implements PV1Wrap, EVNWrap, MSHWrap {
    private static final Logger logger = LoggerFactory.getLogger(AdtWrap.class);

    private String administrativeSex; // PID-8
    private String familyName; // PID-5.1
    private String givenName; // PID-5.2
    private String middleName; // PID-5.3 middle name or initial
    private String mrn; // patient ID PID-3.1[1] // internal UCLH hospital number
    private String nhsNumber; // patient ID PID-3.1[2]
    private String triggerEvent;
    private Instant dob;
    private String postcode;

    private MSH msh;
    private PV1 pv1;
    private PIDWrap pidwrap;
    private PD1Wrap pd1wrap;
    private EVN evn;
    private MRG mrg;
    private boolean isTest;

    @Override
    public PV1 getPV1() {
        return pv1;
    }

    @Override
    public EVN getEVN() {
        return evn;
    }

    @Override
    public MSH getMSH() {
        return msh;
    }

    /**
     * Populate the data by generating it randomly.
     */
    public AdtWrap() {
        isTest = true;
        HL7Random random = new HL7Random();
        mrn = random.randomString();
        nhsNumber = random.randomNHSNumber();
        postcode = random.randomString();
        familyName = random.randomString();
        givenName = random.randomString();
        middleName = random.randomString();
        administrativeSex = random.randomString();
    }

    /**
     * Populate the data from an HL7 message.
     *
     * @param adtMsg the passed in HL7 message
     * @throws HL7Exception if HAPI does
     */
    public AdtWrap(Message adtMsg) throws HL7Exception {
        isTest = false;

        msh = (MSH) adtMsg.get("MSH");

        try {
            pv1 = (PV1) adtMsg.get("PV1");
        } catch (HL7Exception e) {
            // sections are allowed not to exist
        }

        // I want the "MRG" segment for A40 messages, is this really
        // the best way to get it? Why do we have to get the PID segment in
        // a different way for an A39/A40 message?
        if (adtMsg instanceof ADT_A39) {
            ADT_A39_PATIENT a39Patient = (ADT_A39_PATIENT) adtMsg.get("PATIENT");
            mrg = a39Patient.getMRG();
            pidwrap = new PIDWrap(a39Patient.getPID());
        } else {
            pidwrap = new PIDWrap((PID) adtMsg.get("PID"));
        }

        try {
            administrativeSex = pidwrap.getPatientSex();
            dob = pidwrap.getPatientBirthDate();
            postcode = pidwrap.getPatientZipOrPostalCode();
            familyName = pidwrap.getPatientFamilyName();
            givenName = pidwrap.getPatientGivenName();
            middleName = pidwrap.getPatientMiddleName();
            mrn = pidwrap.getPatientFirstIdentifier();
            nhsNumber = pidwrap.getPatientSecondIdentifier();
        } catch (HL7Exception e) {
        }

        try {
            pd1wrap = new PD1Wrap((PD1) adtMsg.get("PD1"));
        } catch (HL7Exception e) {
        }
        try {
            evn = (EVN) adtMsg.get("EVN");
        } catch (HL7Exception e) {
            // EVN is allowed not to exist
        }
    }

    /**
     * Print some basic things in the HL7 message.
     * @throws HL7Exception if HAPI does
     */
    private void prettyPrint() throws HL7Exception {
        System.out.println("\n************** MSH segment **************************");
        // MSH-1 Field Separator
        // MSH-2 Encoding Characters
        System.out.println("sending application = " + getSendingApplication());
        //+ msh.getSendingApplication().getComponent(0).toString());// MSH-3
        // Sending
        // Application
        // (“CARECAST”)
        System.out.println("sending facility = " + getSendingFacility());
        // Sending
        // Facility
        // (“UCLH”)
        // MSH-5 Receiving Application (“Receiving system”)
        System.out.println("messageTimestamp = " + getMessageTimestamp());
        // Message
        // YYYYMMDDHHMM
        System.out.println("message type = " + getMessageType()); //.getMessageCode().toString()); // MSH-9.1 Message
        // Type (ADT)
        System.out.println("trigger event = " + getTriggerEvent());
        // Trigger

        System.out.println("current bed = " + getCurrentBed());

        //// Minimal info needed //////
        System.out.println("patient name = " + pidwrap.getPatientFullName());
        System.out.println("patient MRN = " + pidwrap.getPatientFirstIdentifier());
        System.out.println("admission time = " + getAdmissionDateTime());
    }

    /**
     * @return sex (PID-8)
     */
    public String getAdministrativeSex() {
        return administrativeSex;
    }

    /**
     * @return family name (PID-5.1)
     */
    public String getFamilyName() {
        return familyName;
    }

    /**
     * @return family name (PID-5.2)
     */
    public String getGivenName() {
        return givenName;
    }

    /**
     * @return family name (PID-5.3)
     */
    public String getMiddleName() {
        return middleName;
    }

    /**
     * @return MRN (PID-3.1[1])
     */
    public String getMrn() {
        return mrn;
    }

    /**
     * @return NHS number (PID-3.1[2])
     */
    public String getNHSNumber() {
        return nhsNumber;
    }

    /**
     * @return date of birth (PID-7.1)
     */
    public Instant getDob() {
        return dob;
    }

    /**
     * @return the non-surviving patient ID from a merge message
     */
    public String getMergedPatientId() {
        return mrg.getMrg1_PriorPatientIdentifierList(0).getIDNumber().toString();
    }

    @Override
    public boolean isTest() {
        return isTest;
    }

    /**
     * @return the patient postcode (PID-11, first rep, component 5)
     */
    public String getPatientZipOrPostalCode() {
        return postcode;
    }

}
