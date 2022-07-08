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
import lombok.Getter;
import uk.ac.ucl.rits.inform.datasources.ids.hl7.CustomModelWithDefaultVersion;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;
import java.util.Objects;
import java.util.TimeZone;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utilities for interpreting HL7 messages.
 * @author Jeremy Stein
 * @author Stef Piatek
 * @author Tom Young
 */
public final class HL7Utils {

    private static final String LONDON_TIMEZONE = "Europe/London";

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

    public FileStoreWithMonitoredAccess createMonitoredFileStore(String[] folderPaths) throws IOException {
        return new FileStoreWithMonitoredAccess(folderPaths);
    }

    public static class FileStoreWithMonitoredAccess implements Iterable<MonitoredFile> {

        private final List<MonitoredFile> files;

        /**
         * A repository of file paths that have a particular extension, each of which has an access-count.
         * @param folderPaths Paths of the folders below which files are searched for. e.g. src/test/resources/
         * @throws IOException If the folder path does not exist in the file system
         */
        FileStoreWithMonitoredAccess(String[] folderPaths) throws IOException {

            this.files = new ArrayList<>();

            for (var folderPath : folderPaths) {
                for (var file : listFiles(Paths.get(folderPath))) {
                    this.files.add(new MonitoredFile(file));
                }
            }
        }

        /**
         * List all the files in the current and child directories.
         * @param path  Directory to search from
         * @return List of paths
         * @throws IOException If the walk fails
         */
        private List<Path> listFiles(Path path) throws IOException {

            List<Path> result;
            try (Stream<Path> walk = Files.walk(path)) {
                result = walk
                        .filter(Files::isRegularFile)
                        .collect(Collectors.toList());
            }
            return result;
        }

        /**
         * Access a filename within the store and increment the access count.
         * @param fileName Name of the file
         * @return fileName
         * @throws IOException If the file is not in the store
         */
        public String get(String fileName) throws IOException {

            for (MonitoredFile file : files) {
                if (file.fileNameInPath(fileName)) {
                    file.incrementAccessCount();
                    return fileName;
                }
            }

            throw new IOException("Failed to find " + fileName + " in the list of message files");
        }

        @Override
        public Iterator<MonitoredFile> iterator() {
            return files.iterator();
        }

        public Stream<MonitoredFile> stream() {
            return files.stream();
        }
    }

    /**
     * A file for which the access count is monitored.
     */
    @Getter
    public static class MonitoredFile {

        private Integer accessCount;
        private final Path filePath;

        MonitoredFile(Path filePath) {
            this.filePath = filePath;
            this.accessCount = 0;
        }

        public boolean hasBeenAccessed() {
            return this.accessCount > 0;
        }

        public void incrementAccessCount() {
            this.accessCount += 1;
        }

        public boolean fileNameInPath(String filename) {
            return this.filePath.endsWith(filename);
        }

        public boolean fileNameEndsWith(String ext) {
            return filePath.toString().endsWith(ext);
        }

        public Optional<String> sourceSystem() throws IOException {

            try (BufferedReader br = new BufferedReader(new FileReader(String.valueOf(filePath)))) {
                String line;
                while ((line = br.readLine()) != null) {

                    if (line.contains("sourceSystem: ")){
                        return Optional.of(line.split(": ")[1]);
                    }
                }
            }
            return Optional.empty();
        }
    }
}
