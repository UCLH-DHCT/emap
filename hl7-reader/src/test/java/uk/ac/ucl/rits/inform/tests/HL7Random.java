package uk.ac.ucl.rits.inform.tests;

import java.util.Random;

import org.apache.commons.lang3.RandomStringUtils;

/**
 * Utilities for generating random elements of HL7 data.
 *
 * @author Jeremy Stein
 */
public class HL7Random {

    private Random random;

    /**
     * Create a new HL7 Random.
     */
    public HL7Random() {
        random = new Random();
    }

    /**
     * Generate a random new style NHS number. This will need to generate old style
     * ones eventually. This doesn't generate the check digit correctly as a real
     * NHS number would. NHS numbers starting with a 9 haven't been issued (at time
     * of writing) so there is no danger of this clashing with a real number.
     *
     * @return New-style 3-3-4 NHS number.
     */
    public String randomNHSNumber() {
        return String.format("987 %03d %04d", random.nextInt(1000), random.nextInt(10000));
    }

    /**
     * @return random alpha string with random length
     */
    public String randomString() {
        int length = 9 + Math.round((float) (4 * random.nextGaussian()));
        if (length < 5) {
            length = 5;
        }
        return randomString(length);
    }

    /**
     * @param length of string
     * @return random alpha string of given length
     */
    public String randomString(int length) {
        return RandomStringUtils.randomAlphabetic(length);
    }

    /**
     * Generate "random" numeric strings in a predictable way.
     *
     * @param seed   seed for the random number generator
     * @param length length of the string to return
     * @return a random numeric string of the given length, using the given seed
     */
    public String randomNumericSeeded(int seed, int length) {
        // Set the random generator the appropriate seed
        this.random.setSeed(seed);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        // Reset the random generator to random
        this.random.setSeed(System.currentTimeMillis());
        String res = sb.toString();
        return res;
    }

    /**
     * Generate a random sex for a patient.
     *
     * @return F or M
     */
    public String randomSex() {
        return this.random.nextBoolean() ? "F" : "M";
    }
}
