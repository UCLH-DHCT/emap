package uk.ac.ucl.rits.inform.interchange;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.lang.Nullable;
import uk.ac.ucl.rits.inform.interchange.adt.AdtMessage;
import uk.ac.ucl.rits.inform.interchange.lab.LabIsolateMsg;
import uk.ac.ucl.rits.inform.interchange.lab.LabOrderMsg;
import uk.ac.ucl.rits.inform.interchange.lab.LabResultMsg;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds interchange messages from yaml files.
 * Allows for easier setup for integration testing in hl7 sources and emap star
 */
public class InterchangeMessageFactory {
    private final ObjectMapper mapper;

    public InterchangeMessageFactory() {
        mapper = new ObjectMapper(new YAMLFactory());
        // Finds modules so instants can be parsed correctly
        mapper.findAndRegisterModules();
    }

    /**
     * Builds an ADT message from yaml file.
     * @param fileName filename within test resources/AdtMessages
     * @return ADT message
     */
    public <T extends AdtMessage> T getAdtMessage(final String fileName) {
        T adtMessage = null;
        String resourcePath = "/AdtMessages/" + fileName;
        try {
            InputStream inputStream = getClass().getResourceAsStream(resourcePath);
            adtMessage = mapper.readValue(inputStream, new TypeReference<AdtMessage>() {});
        } catch (IOException e) {
            e.printStackTrace();
        }
        return adtMessage;
    }


    /**
     * Builds lab orders from yaml file given, overriding default values for lab orders and lab results
     * @param fileName            filename in test resources/LabOrders,
     *                            defaults are '{file_stem}_order_defaults.yaml' and '{file_stem}_order_defaults.yaml'
     *                            sensitivity defaults are '{file_stem}_sens_order_defaults.yaml' and '{file_stem}_sens_order_defaults.yaml'
     * @param sourceMessagePrefix message prefix
     * @return List of lab orders deserialised from the files
     */
    public List<LabOrderMsg> getLabOrders(final String fileName, final String sourceMessagePrefix) {
        List<LabOrderMsg> labOrderMsgs = new ArrayList<>();
        String resourcePath = "/LabOrders/" + fileName;
        try {
            InputStream inputStream = getClass().getResourceAsStream(resourcePath);
            labOrderMsgs = mapper.readValue(inputStream, new TypeReference<List<LabOrderMsg>>() {});
            int count = 1;
            for (LabOrderMsg order : labOrderMsgs) {
                String sourceMessageId = sourceMessagePrefix + "_" + String.format("%02d", count);
                updateLabOrderAndResults(order, sourceMessageId, resourcePath.replace(".yaml", ""));
                updateLabIsolates(order, resourcePath.replace(".yaml", "_micro"));
                count++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return labOrderMsgs;
    }

    public List<PatientInfection> getPatientInfections(final String fileName) {
        List<PatientInfection> patientInfections = new ArrayList<>();

        String resourcePath = "/PatientInfection/" + fileName;
        try {
            InputStream inputStream = getClass().getResourceAsStream(resourcePath);
            patientInfections = mapper.readValue(inputStream, new TypeReference<List<PatientInfection>>() {});
        } catch (IOException e) {
            e.printStackTrace();
        }
        return patientInfections;
    }

    /**
     * Builds Flowsheets from yaml file given, overriding default values from '{file_stem}_defaults.yaml'
     * @param fileName            yaml filename in test resources/Flowsheets, default values from '{file_stem}_defaults.yaml'
     * @param sourceMessagePrefix message prefix
     * @return List of Flowsheets
     */
    public List<Flowsheet> getFlowsheets(final String fileName, final String sourceMessagePrefix) {
        List<Flowsheet> flowsheets = new ArrayList<>();

        String resourcePath = "/Flowsheets/" + fileName;
        try {
            InputStream inputStream = getClass().getResourceAsStream(resourcePath);
            flowsheets = mapper.readValue(inputStream, new TypeReference<List<Flowsheet>>() {});
            int count = 1;
            for (Flowsheet flowsheet : flowsheets) {
                String sourceMessageId = sourceMessagePrefix + "$" + String.format("%02d", count);
                flowsheet.setSourceMessageId(sourceMessageId);

                // update order with yaml data
                ObjectReader orderReader = mapper.readerForUpdating(flowsheet);
                String orderDefaultPath = resourcePath.replace(".yaml", "_defaults.yaml");
                orderReader.readValue(getClass().getResourceAsStream(orderDefaultPath));

                count++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return flowsheets;
    }

    /**
     * Utility wrapper for calling updateLabResults without updating the resultTime or epicCareOrderNumber.
     * @param results            lab results to update
     * @param resourcePathPrefix prefix in the form '{directory}/{file_stem}'
     * @throws IOException if files don't exist
     */
    private void updateLabResults(Iterable<LabResultMsg> results, final String resourcePathPrefix) throws IOException {
        updateLabResults(results, resourcePathPrefix, null, null);
    }

    /**
     * Update all of lab results from yaml file
     * @param results             lab results to update
     * @param resourcePathPrefix  prefix in the form '{directory}/{file_stem}'
     * @param resultTime          optional result time to add
     * @param epicCareOrderNumber optional epic care order number to add
     * @throws IOException if files don't exist
     */
    private void updateLabResults(Iterable<LabResultMsg> results, final String resourcePathPrefix,
                                  @Nullable Instant resultTime, @Nullable String epicCareOrderNumber)
            throws IOException {
        String resultDefaultPath = resourcePathPrefix + "_result_defaults.yaml";
        for (LabResultMsg result : results) {
            // update the epic order number and result time if either are set
            if (epicCareOrderNumber != null || resultTime != null) {
                result.setEpicCareOrderNumber(epicCareOrderNumber);
                result.setResultTime(resultTime);
            }

            // update result with yaml data
            ObjectReader resultReader = mapper.readerForUpdating(result);
            resultReader.readValue(getClass().getResourceAsStream(resultDefaultPath));
        }
    }

    /**
     * Update a lab order and its lab results from yaml defaults files
     * @param order              lab order
     * @param sourceMessageId    messageId
     * @param resourcePathPrefix prefix in the form '{directory}/{file_stem}'
     * @throws IOException if files don't exist
     */
    private void updateLabOrderAndResults(LabOrderMsg order, final String sourceMessageId, final String resourcePathPrefix) throws IOException {
        order.setSourceMessageId(sourceMessageId);
        // update order with yaml data
        ObjectReader orderReader = mapper.readerForUpdating(order);
        String orderDefaultPath = resourcePathPrefix + "_order_defaults.yaml";
        order = orderReader.readValue(getClass().getResourceAsStream(orderDefaultPath));

        updateLabResults(order.getLabResultMsgs(), resourcePathPrefix, order.getStatusChangeTime(), order.getEpicCareOrderNumber());
    }

    /**
     * If a lab order's results has isolates, update the sensitivity with default values
     * @param order        lab order
     * @param resourcePath resource path in form '{directory}/{file_stem}_micro_'
     * @throws IOException if file doesn't exist
     */
    private void updateLabIsolates(LabOrderMsg order, final String resourcePath) throws IOException {
        for (LabResultMsg result : order.getLabResultMsgs()) {
            updateLabIsolateAndSensitivities(result.getLabIsolate(), resourcePath);
        }
    }

    /**
     * Update a lab order and its lab results from yaml defaults files
     * @param isolateMsg         lab isolate message
     * @param resourcePathPrefix prefix in the form '{directory}/{file_stem}'
     * @throws IOException if files don't exist
     */
    private void updateLabIsolateAndSensitivities(LabIsolateMsg isolateMsg, final String resourcePathPrefix) throws IOException {
        if (isolateMsg == null) {
            return;
        }
        // update order with yaml data
        ObjectReader orderReader = mapper.readerForUpdating(isolateMsg);
        String isolateDefaultPath = resourcePathPrefix + "_isolate_defaults.yaml";
        isolateMsg = orderReader.readValue(getClass().getResourceAsStream(isolateDefaultPath));
        updateLabResults(isolateMsg.getSensitivities(), resourcePathPrefix);
    }
}
