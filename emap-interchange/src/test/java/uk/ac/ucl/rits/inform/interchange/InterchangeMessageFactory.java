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
     * Builds an ADT message from yaml file Not sure it's as useful as other message factories
     * @param fileName filename within test resources/AdtMessages
     * @return ADT message
     */
    public AdtMessage getAdtMessage(final String fileName, final String sourceMessageId){
        AdtMessage adtMessage = new AdtMessage();
        String resourcePath = "classpath:AdtMessages/" + fileName;
        try {
            File file = ResourceUtils.getFile(resourcePath);
            adtMessage = mapper.readValue(file, AdtMessage.class);
            adtMessage.setSourceMessageId(sourceMessageId);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return adtMessage;
    }


    /**
     * Builds pathology orders from yaml file given, overriding default values for pathology orders and pathology results
     * @param fileName filename in test resources, this is also the basename for the defaults
     *                 e.g. 'ORU_R01.yaml' has pathology result values overwritten from 'ORU_R01_pathology_result_defaults.yaml'
     *                 and pathology order values overwritten from 'ORU_R01_pathology_order_defaults.yaml'
     * @return List of pathology orders deserialised from the files
     */
    public List<PathologyOrder> getPathologyOrders(final String fileName, final String sourceMessagePrefix) {
        List<PathologyOrder> pathologyOrders = new ArrayList<>();
        String resourcePath = "classpath:PathologyOrders/" + fileName;
        try {
            File file = ResourceUtils.getFile(resourcePath);
            pathologyOrders = mapper.readValue(file, new TypeReference<List<PathologyOrder>>() {});
            int count = 1;
            for (PathologyOrder order : pathologyOrders) {
                updatePathologyOrderItsAndResults(sourceMessagePrefix, resourcePath, count, order);
                updatePathologySensitivities(sourceMessagePrefix, resourcePath, count, order);
                count++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return pathologyOrders;
    }

    private void updatePathologyResults(PathologyOrder order, final String resourcePath) throws IOException {
        String resultDefaultPath = resourcePath.replace(".yaml", "_result_defaults.yaml");
        for (PathologyResult result : order.getPathologyResults()) {
            ObjectReader resultReader = mapper.readerForUpdating(result);
            PathologyResult updatedResult = resultReader.readValue(ResourceUtils.getFile(resultDefaultPath));
            updatedResult.setEpicCareOrderNumber(order.getEpicCareOrderNumber());
            updatedResult.setResultTime(order.getStatusChangeTime());
        }
    }

    private void updatePathologyOrderItsAndResults(String sourceMessagePrefix, String resourcePath, int count, PathologyOrder order) throws IOException {
        String orderDefaultPath = resourcePath.replace(".yaml", "_order_defaults.yaml");
        ObjectReader orderReader = mapper.readerForUpdating(order);
        PathologyOrder updatedOrder = orderReader.readValue(ResourceUtils.getFile(orderDefaultPath));
        updatedOrder.setSourceMessageId(sourceMessagePrefix + "_" + String.format("%02d", count));
        updatePathologyResults(order, resourcePath);
        }

    private void updatePathologySensitivities(String sourceMessagePrefix, String resourcePath, int count, PathologyOrder order) throws IOException {
        for (PathologyResult result: order.getPathologyResults()) {
            if (!result.getPathologySensitivities().isEmpty()) {
                for (PathologyOrder subOrder: result.getPathologySensitivities()) {
                    updatePathologyOrderItsAndResults(sourceMessagePrefix, resourcePath, count, subOrder);
                }
            }
        }
    }
}
