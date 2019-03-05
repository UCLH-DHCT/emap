package uk.ac.ucl.rits.inform;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Random;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.RandomStringUtils;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v27.datatype.CX;
import ca.uhn.hl7v2.model.v27.datatype.ST;
import ca.uhn.hl7v2.model.v27.message.ADT_A01;
import ca.uhn.hl7v2.model.v27.segment.MSH;
import ca.uhn.hl7v2.model.v27.segment.PV1;
import ca.uhn.hl7v2.parser.CanonicalModelClassFactory;
import ca.uhn.hl7v2.parser.PipeParser;
import ca.uhn.hl7v2.validation.ValidationContext;
import ca.uhn.hl7v2.validation.impl.ValidationContextFactory;

public class A01Wrap {

    private String administrativeSex;

    private Timestamp eventTime;

    private String familyName; // PID-5.1

    private String givenName; // PID-5.2

    private String middleName;

    private String mrn; // patient ID PID-3.1[1] // internal UCLH hospital number

    private String NHSNumber; // patient ID PID-3.1[2]
    private Random random;
    private String visitNumber; // PV1-19

    private MSHWrap mshwrap;
    private PV1Wrap pv1wrap;
    private PIDWrap pidwrap;
    private PD1Wrap pd1wrap;

    public MSHWrap getMSHWrap () {
        return mshwrap;
    }

    /**
     * I'm not sure if we would ever use this, as opposed to creating the mshwrap object in the constructor.
     * 
     * @param mshwrap
     */
    public void setMSHWrap (MSHWrap mshwrap) {
        mshwrap = mshwrap;
    }

    public PV1Wrap getPV1Wrap () {
        return pv1wrap;
    }

    public PIDWrap getPIDWrap() {
        return pidwrap;
    }

    public PD1Wrap getPD1Wrap() {
        return pd1wrap;
    }

    /**
     * Populate the data by generating it randomly.
     */
    public A01Wrap() {
        random = new Random();

        mrn = randomString();
        NHSNumber = randomNHSNumber();
        familyName = randomString();
        givenName = randomString();
        // what is the format for this number?
        // CSNs will probably change this again
        visitNumber = RandomStringUtils.randomNumeric(8);
        middleName = randomString();
        administrativeSex = randomString();
        eventTime = Timestamp.from(Instant.now());
    }

    /**
     * Populate the data from an HL7 message.
     * 
     * @param fromMsg the passed in HL7 message
     * @throws HL7Exception
     */
    public A01Wrap(ADT_A01 adt_01) throws HL7Exception {

        /**
         * NOTE: MSH-9.2 Trigger Event is an important field. 
         * The HAPI parser uses some object types to represent other messages, 
         * due to the similarities in the message structure. e.g. An A01 object 
         * can represent an A01 message but can also be used to represent 
         * A04, A08 and A13 messages. Thus it is always necessary to check 
         * the Trigger Event field to see what message type we are dealing with.
         * 
         */



        // 1. MSH (Message Header) - mostly don't appear to be useful
        MSH msh = adt_01.getMSH();
        mshwrap = new MSHWrap(msh);
        pv1wrap = new PV1Wrap(adt_01.getPV1());
        pidwrap = new PIDWrap(adt_01.getPID());
        pd1wrap = new PD1Wrap(adt_01.getPD1());

        System.out.println("\n************** MSH segment **************************");
        // MSH-1 Field Separator
        // MSH-2 Encoding Characters
        System.out.println("sending application = " + mshwrap.getSendingApplication());
        //+ msh.getSendingApplication().getComponent(0).toString());// MSH-3
                                                                                                              // Sending
                                                                                                              // Application
                                                                                                              // (“CARECAST”)
        System.out.println("sending facility = " + mshwrap.getSendingFacility()); //.getComponent(0).toString()); // MSH-4
                                                                                                         // Sending
                                                                                                         // Facility
                                                                                                         // (“UCLH”)
        // MSH-5 Receiving Application (“Receiving system”)
        System.out.println("messageTimestamp = " + mshwrap.getMessageTimestamp());//msh.getDateTimeOfMessage().toString()); // MSH-7 Date/Time Of
                                                                                           // Message
                                                                                           // YYYYMMDDHHMM
        System.out.println("message type = " + mshwrap.getMessageType()); //.getMessageCode().toString()); // MSH-9.1 Message
                                                                                                  // Type (ADT)
        System.out.println("trigger event = " + mshwrap.getTriggerEvent());//msh.getMessageType().getTriggerEvent().getValue()); // MSH-9.2
                                                                                                    // Trigger
        
        System.out.println("current bed = " + pv1wrap.getCurrentBed());

        PV1 pv1 = adt_01.getPV1();
        CX visitNumber2 = pv1.getVisitNumber();
        ST idNumber = visitNumber2.getIDNumber();
        idNumber.getValue();

    }

    public String getAdministrativeSex() {
        return administrativeSex;
    }

    public Timestamp getEventTime() {
        return eventTime;
    }

    public String getFamilyName() {
        return familyName;
    }

    public String getGivenName() {
        return givenName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public String getMrn() {
        return mrn;
    }

    public String getNHSNumber() {
        return NHSNumber;
    }

    public String getVisitNumber() {
        return visitNumber;
    }

    private String randomNHSNumber() {
        // New-style 3-3-4 nhs number - will need to generate old style ones eventually.
        // This doesn't generate the check digit correctly as a real NHS number would.
        // NHS numbers starting with a 9 haven't been issued (yet) so there is no
        // danger of this clashing with a real number at the time of writing.
        return String.format("987 %03d %04d", random.nextInt(1000), random.nextInt(10000));
    }

    /**
     * @return random alpha string with random length
     */
    private String randomString() {
        int length = 9 + Math.round((float) (4 * random.nextGaussian()));
        if (length < 0)
            length = 0;
        return randomString(length);
    }

    /**
     * @return random alpha string of given length
     */
    private String randomString(int length) {
        return RandomStringUtils.randomAlphabetic(length);
    }

    public void setAdministrativeSex(String administrativeSex) {
        this.administrativeSex = administrativeSex;
    }

}
