package uk.ac.ucl.rits.inform.datasources.ids.hl7parser;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ethnicity code lookup to description.
 * @author Stef Piatek
 */
@Component
public class EthnicGroup {
    private static final Logger logger = LoggerFactory.getLogger(EthnicGroup.class);
    private static final Resource CSV_RESOURCE = new ClassPathResource("ethnic_group.csv");
    private Map<String, String> ethnicGroups = new HashMap<>();

    public EthnicGroup() throws IOException {
        logger.info("Building EthnicGroup map");
        buildMap();
    }

    private void buildMap() throws IOException {
        CsvMapper mapper = new CsvMapper();
        mapper.enable(CsvParser.Feature.WRAP_AS_ARRAY);
        CsvSchema schema = CsvSchema.emptySchema().withSkipFirstDataRow(true);
        InputStreamReader reader = new InputStreamReader(CSV_RESOURCE.getInputStream());
        try (MappingIterator<List<String>> mappingIterator = mapper.readerFor(List.class)
                .with(schema)
                .readValues(reader)) {
            while (mappingIterator.hasNext()) {
                List<String> row = mappingIterator.next();
                ethnicGroups.put(row.get(0), row.get(1));
            }
        }
    }

    /**
     * Covert ethnic code to name.
     * @param ethnicCode EPIC ethic group code
     * @return EPIC ethnic group name, or if not found then the original code.
     */
    public String convertCodeToName(String ethnicCode) {
        return ethnicGroups.getOrDefault(ethnicCode, ethnicCode);
    }
}
