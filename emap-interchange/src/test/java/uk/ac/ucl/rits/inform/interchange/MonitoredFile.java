package uk.ac.ucl.rits.inform.interchange;

import lombok.Getter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;


/**
 * A file for which the access count is monitored.
 */
@Getter
public class MonitoredFile {

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

    /**
     * Determine the "source system" of this file. If it's an interchange yaml file then there should be a line with
     * "sourceSystem:" in it. Extract and return the value from this key. This is useful to check if the file
     * corresponds to a hl7 message.
     * @return Source system string
     * @throws IOException If the file does not exist
     */
    public Optional<String> sourceSystem() throws IOException {

        try (BufferedReader br = new BufferedReader(new FileReader(String.valueOf(filePath)))) {
            String line;
            while ((line = br.readLine()) != null) {

                if (line.contains("sourceSystem: ")) {
                    return Optional.of(line.split(": ")[1]);
                }
            }
        }
        return Optional.empty();
    }
}
