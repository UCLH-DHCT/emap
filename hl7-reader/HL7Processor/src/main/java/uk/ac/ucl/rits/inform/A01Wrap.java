package uk.ac.ucl.rits.inform;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Random;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.RandomStringUtils;

import ca.uhn.hl7v2.model.v27.message.ADT_A01;

public class A01Wrap {

    /**
     * Populate the data by generating it randomly.
     */
    public A01Wrap() {
        random = new Random();

        mrn = randomString();
        NHSNumber = randomNHSNumber();
        familyName = randomString();
        givenName = randomString();
        middleName = randomString();
        administrativeSex = randomString();
        eventTime = Timestamp.from(Instant.now());
    }

    /**
     * Populate the data from an HL7 message.
     * 
     * @param fromMsg the passed in HL7 message
     */
    public A01Wrap(ADT_A01 fromMsg) {
        throw new NotImplementedException();
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

    private Random random;

    private String mrn; // patient ID PID-3.1[1] // internal UCLH hospital number
    private String NHSNumber; // patient ID PID-3.1[2]
    private String familyName; // PID-5.1
    private String givenName; // PID-5.2
    private String middleName;
    private String administrativeSex;
    private Timestamp eventTime;

    public String getAdministrativeSex() {
        return administrativeSex;
    }

    public void setAdministrativeSex(String administrativeSex) {
        this.administrativeSex = administrativeSex;
    }

    public String getMrn() {
        return mrn;
    }

    public String getNHSNumber() {
        return NHSNumber;
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

    public Timestamp getEventTime() {
        return eventTime;
    }

}
