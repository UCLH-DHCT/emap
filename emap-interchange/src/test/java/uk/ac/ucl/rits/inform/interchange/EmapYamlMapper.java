package uk.ac.ucl.rits.inform.interchange;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Pattern;

@Slf4j
public class EmapYamlMapper {
    private static final Pattern SOURCE_ID_PATTERN = Pattern.compile("sourceMessageId:.*\n");
    private final ObjectMapper mapper;

    /**
     * Create an EMAP interchange Mapper object, configured to allow for serialising and deserialising YAML.
     */
    public EmapYamlMapper() {
        mapper = new ObjectMapper(new YAMLFactory());
        // Finds modules so instants can be parsed correctly
        mapper.findAndRegisterModules();
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    /**
     * Convert EMAP message to YAML string for easy comparison in an assert statement.
     * @param message EMAP message to convert to a string
     * @return YAML string of the message contents
     */
    public String convertToString(EmapOperationMessage message) {
        try {
            String json = mapper.writeValueAsString(message);
            return SOURCE_ID_PATTERN.matcher(json).replaceFirst("");
        } catch (JsonProcessingException e) {
            log.error("Message could not be parsed", e);
        }
        return null;
    }

    <T> T readValue(InputStream src, TypeReference<T> valueTypeRef) throws IOException {
        return mapper.readValue(src, valueTypeRef);
    }

    ObjectReader readerForUpdating(Object valueToUpdate) {
        return mapper.readerForUpdating(valueToUpdate);
    }
}
