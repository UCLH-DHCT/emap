package uk.ac.ucl.rits.inform.hl7;

import java.time.Instant;
import java.util.Calendar;
import java.util.Random;
import java.util.TimeZone;

import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.v27.datatype.DTM;

public class HL7Utils {

    /**
     * HL7 messages may or may not specify a timezone, according to the spec. In
     * practice our messages don't, and our current assumption is that all times
     * are local time, which means local for the hospital, NOT local time for
     * the computer this code is running on.
     *
     * @param hl7DTM the hl7 DTM object as it comes from the message
     * @return an Instant representing this same point in time, or null if no time
     *         is specified in the DTM
     * @throws DataTypeException
     */
    public static Instant interpretLocalTime(DTM hl7DTM) throws DataTypeException {
        Calendar valueAsCal = hl7DTM.getValueAsCalendar();
        if (valueAsCal == null) {
            return null;
        }
        // BUG: If no timezone/offset is specified in the HL7 message,
        // the Calendar object still comes out with a timezone of Europe/London,
        // or presumably whatever the tz is on the computer this is running on.
        // We need to be able to tell between no TZ specified and an explicit Europe/London.
        // Is this somewhere in the HAPI config?
        // Chances are no HL7 messages we ever see will specify it, though...
        TimeZone before = valueAsCal.getTimeZone();
        // The hospital will always be in London, however this code could
        // theoretically run anywhere. In fact, CircleCI containers seem to be set on UTC.
        // Therefore forcibly interpret as this timezone (this should be in config).
        // There's the possibility that different equipment in the hospital will be in
        // different timezones (or will be manually updated at different times), but we'll
        // deal with that when we come to it.
        valueAsCal.setTimeZone(TimeZone.getTimeZone("Europe/London"));
        TimeZone after = valueAsCal.getTimeZone();
        Instant result = valueAsCal.toInstant();
        //System.out.println("before: " + hl7DTM + "|" + before + ", after: " + result + "|" + after);
        return result;
    }

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
