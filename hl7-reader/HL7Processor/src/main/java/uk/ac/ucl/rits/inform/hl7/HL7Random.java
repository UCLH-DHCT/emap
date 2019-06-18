package uk.ac.ucl.rits.inform.hl7;

import java.util.Random;

import org.apache.commons.lang3.RandomStringUtils;

/**
 * Utilities for generating random bits of HL7 data.
 */
public class HL7Random {
    private Random random;

    /**
     * .
     */
    public HL7Random() {
        random = new Random();
    }

    /**
     * @return New-style 3-3-4 nhs number - will need to generate old style ones
     *         eventually. This doesn't generate the check digit correctly as a real
     *         NHS number would. NHS numbers starting with a 9 haven't been issued
     *         (yet) so there is no danger of this clashing with a real number at
     *         the time of writing.
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
     * @param seed seed for the random number generator
     * @param length length of the string to return
     * @return a random numeric string of the given length, using the given seed
     */
    public static String randomNumericSeeded(int seed, int length) {
        Random random = new Random(seed);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        String res = sb.toString();
        return res;
    }
}
