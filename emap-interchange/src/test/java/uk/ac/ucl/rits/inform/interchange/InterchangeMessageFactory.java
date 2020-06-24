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
    public List<PathologyOrder> getPathologyOrders(String fileName, String sourceMessagePrefix) {
        List<PathologyOrder> pathologyOrders = new ArrayList<>();
        try {
            File file = ResourceUtils.getFile("classpath:" + fileName);
            pathologyOrders = mapper.readValue(file, new TypeReference<List<PathologyOrder>>() {});
            int count = 1;
            for (PathologyOrder order : pathologyOrders) {
                String orderDefaultPath = fileName.replace(".yaml", "_pathology_order_defaults.yaml");
                updatePathologyOrder(order, orderDefaultPath, sourceMessagePrefix, count);
                for (PathologyResult result : order.getPathologyResults()) {
                    String resultDefaultPath = fileName.replace(".yaml", "_pathology_result_defaults.yaml");
                    updatePathologyResult(result, resultDefaultPath, order.getEpicCareOrderNumber());
                }
                count++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return pathologyOrders;
    }

    private void updatePathologyOrder(PathologyOrder order, final String fileName,
                                      final String sourceMessagePrefix, final int count) throws IOException {
        ObjectReader orderReader = mapper.readerForUpdating(order);
        PathologyOrder updatedOrder = orderReader.readValue(ResourceUtils.getFile("classpath:" + fileName));
        updatedOrder.setSourceMessageId(sourceMessagePrefix + "_" + String.format("%02d", count));
    }

    private void updatePathologyResult(PathologyResult result, final String fileName, final String epicCareOrderNumber) throws IOException {
        ObjectReader resultReader = mapper.readerForUpdating(result);
        PathologyResult updatedResult = resultReader.readValue(ResourceUtils.getFile("classpath:" + fileName));
        updatedResult.setEpicCareOrderNumber(epicCareOrderNumber);
    }
}
