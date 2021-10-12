package uk.ac.ucl.rits.inform.datasources.ids;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v26.datatype.DT;
import ca.uhn.hl7v2.model.v26.datatype.DTM;
import ca.uhn.hl7v2.parser.ModelClassFactory;
import ca.uhn.hl7v2.parser.PipeParser;
import ca.uhn.hl7v2.util.Hl7InputStreamMessageIterator;
import ca.uhn.hl7v2.validation.ValidationContext;
import ca.uhn.hl7v2.validation.impl.ValidationContextFactory;
import uk.ac.ucl.rits.inform.datasources.ids.hl7.CustomModelWithDefaultVersion;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;

/**
 * Utilities for interpreting HL7 messages.
 * @author Jeremy Stein
 * @author Stef Piatek
 */
public final class HL7Utils {

    private static final String LONDON_TIMEZONE = "Europe/London";

    /**
     * Can't instantiate a util class.
     */
    private HL7Utils() {}

    /**
     * Our messages don't specify time zone, we are assuming all datetimes are in are local time.
     * Here local means for the hospital, NOT local time for the computer this code is running on.
     * @param hl7DTM the hl7 DTM object as it comes from the message
     * @return an Instant representing this same point in time, or null if no time
     * is specified in the DTM
     * @throws DataTypeException if HAPI does
     */
    public static Instant interpretLocalTime(DTM hl7DTM) throws DataTypeException {
        Calendar valueAsCal = hl7DTM.getValueAsCalendar();
        if (valueAsCal == null) {
            return null;
        }
        valueAsCal.setTimeZone(TimeZone.getTimeZone(LONDON_TIMEZONE));

        return valueAsCal.toInstant();
    }


    /**
     * Process date value from HL7.
     * @param hl7Date HAPI DT date
     * @return Local date
     * @throws DataTypeException if date cannot be parsed correctly.
     */
    static LocalDate interpretDate(DT hl7Date) throws DataTypeException {
        if (hl7Date == null) {
            return null;
        }
        return LocalDate.of(hl7Date.getYear(), hl7Date.getMonth(), hl7Date.getDay());
    }

    /**
     * Process date value from HL7.
     * @param hl7Date HAPI DT date
     * @return Local date
     * @throws DataTypeException if date cannot be parsed correctly.
     */
    public static LocalDate interpretDate(DTM hl7Date) throws DataTypeException {
        if (hl7Date == null) {
            return null;
        }
        return LocalDate.of(hl7Date.getYear(), hl7Date.getMonth(), hl7Date.getDay());
    }

    /**
     * Initialise the HAPI parser.
     * @return the HapiContext
     */
    public static HapiContext initializeHapiContext() {
        HapiContext context = new DefaultHapiContext();
        ValidationContext vc = ValidationContextFactory.noValidation();
        context.setValidationContext(vc);

        ModelClassFactory mcf = new CustomModelWithDefaultVersion("uk.ac.ucl.rits.inform.datasources.ids.hl7.custom", "2.6");

        context.setModelClassFactory(mcf);
        return context;
    }

    /**
     * Read text from the given resource file and make its line endings
     * HL7 friendly (ie. CR).
     * @param fileName The name of the resource file that's in the resource directory
     * @return string of the entire file contents with line endings converted to carriage returns
     * @throws IOException when reading file
     * @throws URISyntaxException if the fileName provided cannot be turned into URI
     */
    public static String readHl7FromResource(String fileName) throws IOException, URISyntaxException {
        URI uri = getPathFromResource(fileName);
        List<String> readAllLines = Files.readAllLines(Path.of(uri));
        return String.join("\r", readAllLines) + "\r";
    }

    /**
     * @param fileName the relative filename of the resource
     * @return the filename in the resource directory
     * @throws URISyntaxException if the fileName provided cannot be turned into URI
     */
    public static URI getPathFromResource(String fileName) throws URISyntaxException {
        // the class used here doesn't seem to matter
        ClassLoader classLoader = HL7Utils.class.getClassLoader();
        return Objects.requireNonNull(classLoader.getResource(fileName)).toURI();
    }

    /**
     * @param hl7Message string containing the hl7 message
     * @return the parsed message
     * @throws HL7Exception if HAPI does
     */
    public static Message parseHl7String(String hl7Message) throws HL7Exception {
        HapiContext context = initializeHapiContext();
        // do we want no context validation?
        ValidationContext vc = ValidationContextFactory.noValidation();
        context.setValidationContext(vc);

        PipeParser parser = context.getPipeParser();

        return parser.parse(hl7Message);
    }

    /**
     * @param multiHl7File file containing one or more HL7 messages sequentially
     * @return an Hl7InputStreamMessageIterator that allows the caller to get the messages one by one
     * @throws FileNotFoundException if file doesn't exist
     */
    public static Hl7InputStreamMessageIterator hl7Iterator(File multiHl7File) throws FileNotFoundException {
        HapiContext context = initializeHapiContext();
        InputStream is = new BufferedInputStream(new FileInputStream(multiHl7File));
        Hl7InputStreamMessageIterator hl7iter = new Hl7InputStreamMessageIterator(is, context);
        hl7iter.setIgnoreComments(true);
        return hl7iter;
    }
}
