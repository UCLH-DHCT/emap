package uk.ac.ucl.rits.inform.interchange;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds interchange messages from yaml files.
 * Allows for easier setup for integration testing in hl7 sources and emap star
 */
public class InterchangeMessageFactory {
    private ObjectMapper mapper;

    public InterchangeMessageFactory() {
        mapper = new ObjectMapper(new YAMLFactory());
        // Finds modules so instants can be parsed correctly
        mapper.findAndRegisterModules();
    }


    /**
     * Builds pathology orders from yaml file given, overriding default values for pathology orders and pathology results
     * @param fileName filename in test resources, this is also the basename for the defaults
     *                 e.g. 'ORU_R01.yaml' has pathology result values overwritten from 'ORU_R01_pathology_result_defaults.yaml'
     *                 and pathology order values overwritten from 'ORU_R01_pathology_order_defaults.yaml'
     * @return List of pathology orders deserialised from the files
     */
    public List<PathologyOrder> getPathologyOrders(String fileName) {
        List<PathologyOrder> pathologyOrders = new ArrayList<>();
        try {
            File file = ResourceUtils.getFile("classpath:" + fileName);
            pathologyOrders = mapper.readValue(file, new TypeReference<List<PathologyOrder>>() {});
            for (PathologyOrder order : pathologyOrders) {
                updatePathologyOrderWithDefaults(order, fileName.replace(".yaml", "_pathology_order_defaults.yaml"));
                for (PathologyResult result : order.getPathologyResults()) {
                    updatePathologyResultWithDefaults(result, fileName.replace(".yaml", "_pathology_result_defaults.yaml"));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return pathologyOrders;
    }

    private PathologyOrder updatePathologyOrderWithDefaults(PathologyOrder order, String fileName) throws IOException {
        ObjectReader orderReader = mapper.readerForUpdating(order);
        return orderReader.readValue(ResourceUtils.getFile("classpath:" + fileName));
    }

    private PathologyResult updatePathologyResultWithDefaults(PathologyResult result, String fileName) throws IOException {
        ObjectReader resultReader = mapper.readerForUpdating(result);
        return resultReader.readValue(ResourceUtils.getFile("classpath:" + fileName));
    }
}
