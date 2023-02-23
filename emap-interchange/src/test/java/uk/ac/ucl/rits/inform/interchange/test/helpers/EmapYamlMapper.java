package uk.ac.ucl.rits.inform.interchange.test.helpers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessage;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Pattern;

@Slf4j
public final class EmapYamlMapper {
    private static final Pattern SOURCE_ID_PATTERN = Pattern.compile("sourceMessageId:.*\n");
    private final ObjectMapper mapper;

    /**
     * Create an EMAP interchange Mapper object, configured to allow for serialising and deserialising YAML.
     */
    private EmapYamlMapper() {
        mapper = new ObjectMapper(new YAMLFactory());
        // Finds modules so instants can be parsed correctly
        mapper.findAndRegisterModules();
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }
    private static class SingletonHolder {
        private static final EmapYamlMapper INSTANCE = new EmapYamlMapper();
    }

    public static String convertToString(EmapOperationMessage message) {
        return SingletonHolder.INSTANCE.removeSourceAndConvertToString(message);
    }

    /**
     * Convert EMAP message to YAML string for easy comparison in an assert statement.
     * @param message EMAP message to convert to a string
     * @return YAML string of the message contents
     */
    private String removeSourceAndConvertToString(EmapOperationMessage message) {
        try {
            String json = mapper.writeValueAsString(message);
            return removeSourceMessageIdLine(json);
        } catch (JsonProcessingException e) {
            log.error("Message could not be parsed", e);
        }
        return null;
    }

    private static String removeSourceMessageIdLine(CharSequence yaml) {
        return SOURCE_ID_PATTERN.matcher(yaml).replaceFirst("");
    }

    static <T> T readValue(InputStream src, TypeReference<T> valueTypeRef) throws IOException {
        return SingletonHolder.INSTANCE.mapper.readValue(src, valueTypeRef);
    }

    static ObjectReader readerForUpdating(Object valueToUpdate) {
        return SingletonHolder.INSTANCE.mapper.readerForUpdating(valueToUpdate);
    }
}
