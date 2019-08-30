package uk.ac.ucl.rits.inform.tests;

import java.time.Instant;
import java.util.Vector;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.v26.segment.EVN;
import ca.uhn.hl7v2.model.v26.segment.MSH;
import ca.uhn.hl7v2.model.v26.segment.PID;
import ca.uhn.hl7v2.model.v26.segment.PV1;
import uk.ac.ucl.rits.inform.datasources.hl7.AdtWrap;
import uk.ac.ucl.rits.inform.datasources.hl7.Doctor;

/**
 * Generates random data while pretending to be an ADT parser.
 * Ideally we would generate more plausible data.
 *
 * @author Jeremy Stein
 *
 */
public class AdtWrapMock extends AdtWrap {
    private String postcode;
    private String familyName;
    private String givenName;
    private String middleName;
    private String administrativeSex;
    private String nhsNumber;
    private String mrn;
    private Instant eventOccurred;
    private String visitNumber;

    /**
     * Generate the synthetic data once so it's the same for the life of this object.
     */
    public AdtWrapMock() {
        HL7Random random = new HL7Random();
        postcode = random.randomString();
        familyName = random.randomString();
        givenName = random.randomString();
        middleName = random.randomString();
        nhsNumber = random.randomNHSNumber();
        mrn = random.randomString();
        administrativeSex = random.randomSex();
        eventOccurred = Instant.now();
        visitNumber = random.randomString(8);

    }

    @Override
    public PID getPID() {
        throw new RuntimeException("If anything calls this it's not mocking properly");
    }

    @Override
    public MSH getMSH() {
        throw new RuntimeException("If anything calls this it's not mocking properly");
    }

    @Override
    public EVN getEVN() {
        throw new RuntimeException("If anything calls this it's not mocking properly");
    }

    @Override
    public PV1 getPV1() {
        throw new RuntimeException("If anything calls this it's not mocking properly");
    }

    @Override
    public String getMrn() {
        return mrn;
    }

    @Override
    public String getNHSNumber() {
        return nhsNumber;
    }

    @Override
    public String getCurrentWardCode() throws HL7Exception {
        return "Test poc location";
    }

    @Override
    public String getCurrentRoomCode() throws HL7Exception {
        return "Test room location";
    }

    @Override
    public String getCurrentBed() throws HL7Exception {
        return "Test bed location";
    }

    @Override
    public String getFullLocationString() throws HL7Exception {
        return String.join("^", getCurrentWardCode(), getCurrentRoomCode(), getCurrentBed());
    }

    @Override
    public Instant getEventOccurred() throws HL7Exception {
        return eventOccurred;
    }

    @Override
    public String getVisitNumber() throws HL7Exception {
        return this.visitNumber;
    }

    @Override
    public Instant getAdmissionDateTime() throws HL7Exception {
        return Instant.parse("2014-05-06T07:08:09Z");
    }

    @Override
    public Instant getDischargeDateTime() throws HL7Exception {
        return Instant.parse("2014-05-06T12:34:56Z");
    }

    @Override
    public Vector<Doctor> getAttendingDoctors() throws HL7Exception {
        Vector<Doctor> v = new Vector<Doctor>();
        return v;
    }

    @Override
    public String getPatientClass() throws HL7Exception {
        return "??";
    }

    @Override
    public String getPatientGivenName() throws HL7Exception {
        return givenName;
    }

    @Override
    public String getPatientMiddleName() throws HL7Exception {
        return middleName;
    }

    @Override
    public String getPatientFamilyName() throws HL7Exception {
        return familyName;
    }

    @Override
    public Instant getPatientBirthDate() throws HL7Exception {
        return Instant.parse("1944-05-06T12:34:56Z");
    }

    @Override
    public String getPatientZipOrPostalCode() {
        return postcode;
    }

    @Override
    public String getPatientSex() throws HL7Exception {
        return administrativeSex;
    }
}
