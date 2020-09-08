package uk.ac.ucl.rits.inform.interchange;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.io.InputStream;
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
     * Builds an ADT message from yaml file.
     * Not sure it's as useful as other message factories but may be useful for testing a complex set of ADT messages together?
     * @param fileName        filename within test resources/AdtMessages
     * @param sourceMessageId source message ID
     * @return ADT message
     */
    public OldAdtMessage getAdtMessage(final String fileName, final String sourceMessageId) {
        OldAdtMessage adtMessage = new OldAdtMessage();
        String resourcePath = "/AdtMessages/" + fileName;
        try {
            InputStream inputStream = getClass().getResourceAsStream(resourcePath);
            adtMessage = mapper.readValue(inputStream, OldAdtMessage.class);
            adtMessage.setSourceMessageId(sourceMessageId);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return adtMessage;
    }


    /**
     * Builds pathology orders from yaml file given, overriding default values for pathology orders and pathology results
     * @param fileName            filename in test resources/PathologyOrders,
     *                            defaults are '{file_stem}_order_defaults.yaml' and '{file_stem}_order_defaults.yaml'
     *                            sensitivity defaults are '{file_stem}_sens_order_defaults.yaml' and '{file_stem}_sens_order_defaults.yaml'
     * @param sourceMessagePrefix message prefix
     * @return List of pathology orders deserialised from the files
     */
    public List<PathologyOrder> getPathologyOrders(final String fileName, final String sourceMessagePrefix) {
        List<PathologyOrder> pathologyOrders = new ArrayList<>();
        String resourcePath = "/PathologyOrders/" + fileName;
        try {
            InputStream inputStream = getClass().getResourceAsStream(resourcePath);
            pathologyOrders = mapper.readValue(inputStream, new TypeReference<List<PathologyOrder>>() {});
            int count = 1;
            for (PathologyOrder order : pathologyOrders) {
                String sourceMessageId = sourceMessagePrefix + "_" + String.format("%02d", count);
                updatePathologyOrderItsAndResults(order, sourceMessageId, resourcePath.replace(".yaml", ""));
                updatePathologySensitivities(order, sourceMessageId, resourcePath.replace(".yaml", "_sens"));
                count++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return pathologyOrders;
    }

    /**
     * Builds Vitalsigns from yaml file given, overriding default values from '{file_stem}_defaults.yaml'
     * @param fileName            yaml filename in test resouces/VitalSigns, default values from '{file_stem}_defaults.yaml'
     * @param sourceMessagePrefix message prefix - TODO: does emap star care about this or should I just get rid of it?
     * @return List of vitalsigns
     */
    public List<VitalSigns> getVitalSigns(final String fileName, final String sourceMessagePrefix) {
        List<VitalSigns> vitalSigns = new ArrayList<>();

        String resourcePath = "/VitalSigns/" + fileName;
        try {
            InputStream inputStream = getClass().getResourceAsStream(resourcePath);
            vitalSigns = mapper.readValue(inputStream, new TypeReference<List<VitalSigns>>() {});
            int count = 1;
            for (VitalSigns vitalsign : vitalSigns) {
                String sourceMessageId = sourceMessagePrefix + "$" + String.format("%02d", count);
                vitalsign.setSourceMessageId(sourceMessageId);

                // update order with yaml data
                ObjectReader orderReader = mapper.readerForUpdating(vitalsign);
                String orderDefaultPath = resourcePath.replace(".yaml", "_defaults.yaml");
                orderReader.readValue(getClass().getResourceAsStream(orderDefaultPath));

                count++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return vitalSigns;
    }

    /**
     * Update all of a pathology order's pathology results from yaml file
     * @param order              pathology order
     * @param resourcePathPrefix prefix in the form '{directory}/{file_stem}'
     * @throws IOException if files don't exist
     */
    private void updatePathologyResults(PathologyOrder order, final String resourcePathPrefix) throws IOException {
        String resultDefaultPath = resourcePathPrefix + "_result_defaults.yaml";
        for (PathologyResult result : order.getPathologyResults()) {
            //  update results from parent order data
            result.setEpicCareOrderNumber(order.getEpicCareOrderNumber());
            result.setResultTime(order.getStatusChangeTime());
            // update result with yaml data
            ObjectReader resultReader = mapper.readerForUpdating(result);
            resultReader.readValue(getClass().getResourceAsStream(resultDefaultPath));
        }
    }

    /**
     * Update a pathology order and its pathology results from yaml defaults files
     * @param order              pathology order
     * @param sourceMessageId    messageId
     * @param resourcePathPrefix prefix in the form '{directory}/{file_stem}'
     * @throws IOException if files don't exist
     */
    private void updatePathologyOrderItsAndResults(PathologyOrder order, final String sourceMessageId, final String resourcePathPrefix) throws IOException {
        order.setSourceMessageId(sourceMessageId);
        // update order with yaml data
        ObjectReader orderReader = mapper.readerForUpdating(order);
        String orderDefaultPath = resourcePathPrefix + "_order_defaults.yaml";
        order = orderReader.readValue(getClass().getResourceAsStream(orderDefaultPath));

        updatePathologyResults(order, resourcePathPrefix);
    }

    /**
     * If a pathology order's results has a sensitivity, update the sensitivity with default values
     * @param order           pathology order
     * @param sourceMessageId message Id
     * @param resourcePath    resource path in form '{directory}/{file_stem}_sens_'
     * @throws IOException if file doesn't exist
     */
    private void updatePathologySensitivities(PathologyOrder order, final String sourceMessageId, final String resourcePath) throws IOException {
        for (PathologyResult result : order.getPathologyResults()) {
            if (!result.getPathologySensitivities().isEmpty()) {
                for (PathologyOrder sensitivityPathologyOrder : result.getPathologySensitivities()) {
                    updatePathologyOrderItsAndResults(sensitivityPathologyOrder, sourceMessageId, resourcePath);
                }
            }
        }
    }
}
