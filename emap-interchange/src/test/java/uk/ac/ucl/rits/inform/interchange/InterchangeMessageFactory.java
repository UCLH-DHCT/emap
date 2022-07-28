package uk.ac.ucl.rits.inform.interchange;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.lang.Nullable;
import uk.ac.ucl.rits.inform.interchange.adt.AdtMessage;
import uk.ac.ucl.rits.inform.interchange.form.FormMetadataMsg;
import uk.ac.ucl.rits.inform.interchange.form.FormQuestionMetadataMsg;
import uk.ac.ucl.rits.inform.interchange.lab.LabIsolateMsg;
import uk.ac.ucl.rits.inform.interchange.lab.LabOrderMsg;
import uk.ac.ucl.rits.inform.interchange.lab.LabResultMsg;
import uk.ac.ucl.rits.inform.interchange.lab.LabMetadataMsg;
import uk.ac.ucl.rits.inform.interchange.form.FormAnswerMsg;
import uk.ac.ucl.rits.inform.interchange.form.FormMsg;
import uk.ac.ucl.rits.inform.interchange.visit_observations.Flowsheet;
import uk.ac.ucl.rits.inform.interchange.visit_observations.FlowsheetMetadata;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
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
     * @throws IOException if files don't exist
     */
    public <T extends AdtMessage> T getAdtMessage(final String fileName) throws IOException {
        String resourcePath = "/AdtMessages/" + fileName;
        InputStream inputStream = getClass().getResourceAsStream(resourcePath);

        return mapper.readValue(inputStream, new TypeReference<>() {});
    }


    /**
     * @param fileName filename within the ConsultRequest folder
     * @return consult message
     * @throws IOException if the file doesn't exist
     */
    public ConsultRequest getConsult(final String fileName) throws IOException {
        InputStream inputStream = getClass().getResourceAsStream(String.format("/ConsultRequest/%s", fileName));
        return mapper.readValue(inputStream, new TypeReference<>() {});
    }

    /**
     * @param fileName filename within the ConsultMetadata folder
     * @return consult message
     * @throws IOException if the file doesn't exist
     */
    public ConsultMetadata getConsultMetadata(final String fileName) throws IOException {
        InputStream inputStream = getClass().getResourceAsStream(String.format("/ConsultMetadata/%s", fileName));
        return mapper.readValue(inputStream, new TypeReference<>() {});
    }

    /**
     * @param fileName filename within the AdvancedDecisions messages folder
     * @return advanced decision message
     * @throws IOException if the file doesn't exist
     */
    public AdvanceDecisionMessage getAdvanceDecision(final String fileName) throws IOException {
        InputStream inputStream = getClass().getResourceAsStream(String.format("/AdvanceDecision/%s", fileName));
        return mapper.readValue(inputStream, new TypeReference<>() {});
    }


    /**
     * Builds lab orders from yaml file given, overriding default values for lab orders and lab results
     * @param fileName            filename in test resources/LabOrders,
     *                            defaults are '{file_stem}_order_defaults.yaml' and '{file_stem}_order_defaults.yaml'
     *                            sensitivity defaults are '{file_stem}_sens_order_defaults.yaml' and '{file_stem}_sens_order_defaults.yaml'
     * @param sourceMessagePrefix message prefix
     * @return List of lab orders deserialised from the files
     * @throws IOException if files don't exist
     */
    public List<LabOrderMsg> getLabOrders(final String fileName, final String sourceMessagePrefix) throws IOException {
        String resourcePath = "/LabOrders/" + fileName;
        InputStream inputStream = getClass().getResourceAsStream(resourcePath);
        List<LabOrderMsg> labOrderMsgs = mapper.readValue(inputStream, new TypeReference<>() {});
        int count = 1;
        for (LabOrderMsg order : labOrderMsgs) {
            String sourceMessageId = sourceMessagePrefix + "_" + String.format("%02d", count);
            updateLabOrderAndResults(order, sourceMessageId, resourcePath.replace(".yaml", ""));
            updateLabIsolates(order, resourcePath.replace(".yaml", "_micro"));
            count++;
        }

        return labOrderMsgs;
    }

    /**
     * Get lab order from single oru_r01 yaml file.
     * @param filePath filepath from lab orders
     * @return Lab order message
     * @throws IOException if files don't exist
     */
    public LabOrderMsg getLabOrder(final String filePath) throws IOException {
        String resourcePath = String.format("/LabOrders/%s", filePath);
        InputStream inputStream = getClass().getResourceAsStream(resourcePath);
        return mapper.readValue(inputStream, new TypeReference<>() {});
    }

    public List<PatientInfection> getPatientInfections(final String fileName) throws IOException {
        String resourcePath = "/PatientInfection/" + fileName;
        InputStream inputStream = getClass().getResourceAsStream(resourcePath);
        return mapper.readValue(inputStream, new TypeReference<>() {});
    }

    /**
     * Build flowsheet metadata expected data from specified file.
     * @param fileName the file from which to build the data
     * @return the data as expected Interchange messages
     * @throws IOException if reading failed
     */
    public List<FlowsheetMetadata> getFlowsheetMetadata(final String fileName) throws IOException {
        List<FlowsheetMetadata> flowsheetMetadata = new ArrayList<>();
        String resourcePath = "/FlowsheetMetadata/" + fileName;
        InputStream inputStream = getClass().getResourceAsStream(resourcePath);
        flowsheetMetadata = mapper.readValue(inputStream, new TypeReference<>() {});
        return flowsheetMetadata;
    }

    /**
     * Build location metadata expected data from specified file.
     * @param fileName the file from which to build the data
     * @return the data as expected Interchange message
     * @throws IOException if reading failed
     */
    public LocationMetadata getLocationMetadata(final String fileName) throws IOException {
        String resourcePath = "/LocationMetadata/" + fileName;
        InputStream inputStream = getClass().getResourceAsStream(resourcePath);
        return mapper.readValue(inputStream, new TypeReference<>() {});
    }

    public List<LabMetadataMsg> getLabMetadataMsgs(final String fileName) throws IOException {
        InputStream resourceAsStream = getClass().getResourceAsStream("/LabsMetadata/" + fileName);
        return mapper.readValue(resourceAsStream, new TypeReference<>() {});
    }

    /**
     * Builds Flowsheets from yaml file given, overriding default values from '{file_stem}_defaults.yaml'
     * @param fileName            yaml filename in test resources/Flowsheets, default values from '{file_stem}_defaults.yaml'
     * @param sourceMessagePrefix message prefix
     * @return List of Flowsheets
     * @throws IOException if files don't exist
     */
    public List<Flowsheet> getFlowsheets(final String fileName, final String sourceMessagePrefix) throws IOException {
        String resourcePath = "/Flowsheets/" + fileName;
        InputStream inputStream = getClass().getResourceAsStream(resourcePath);
        List<Flowsheet> flowsheets = mapper.readValue(inputStream, new TypeReference<>() {});
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
        String epicOrderNumber = order.getEpicCareOrderNumber().isSave() ? order.getEpicCareOrderNumber().get() : null;
        updateLabResults(order.getLabResultMsgs(), resourcePathPrefix, order.getStatusChangeTime(), epicOrderNumber);

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

    public LabOrderMsg buildLabOrderOverridingDefaults(String defaultsFile, String overridingFile) throws IOException {
        String defaultsPath = String.format("/LabOrders/%s", defaultsFile);
        String overridingPath = String.format("/LabOrders/%s", overridingFile);
        InputStream inputStream = getClass().getResourceAsStream(defaultsPath);
        LabOrderMsg defaults = mapper.readValue(inputStream, new TypeReference<>() {});
        ObjectReader orderReader = mapper.readerForUpdating(defaults);

        return orderReader.readValue(getClass().getResourceAsStream(overridingPath));
    }

    public FormMsg getFormMsgTemp() {
        FormMsg formMsg = new FormMsg();
        formMsg.setSourceSystem("ids");
        formMsg.setSourceMessageId("SomeDerivedFormInstanceIdentifier");
        formMsg.setFormId("SmartForm1234");
        formMsg.setFirstFiledDatetime(Instant.parse("2022-04-01T11:59:00Z"));
        formMsg.setMrn("examplemrn");
        formMsg.setVisitNumber("examplevisit");
        FormAnswerMsg sde1 = new FormAnswerMsg();
        sde1.setSourceMessageId("567765");
        sde1.setQuestionId("UCLH#123");
        sde1.setStringValue(InterchangeValue.buildFromHl7("Right arm"));
        formMsg.getFormAnswerMsgs().add(sde1);

        FormAnswerMsg sde2 = new FormAnswerMsg();
        sde2.setSourceMessageId("567766");
        sde2.setQuestionId("UCLH#345");
        sde2.setStringValue(InterchangeValue.delete());
        formMsg.getFormAnswerMsgs().add(sde2);
        return formMsg;
    }

    public List<FormMsg> getFormMsgs(final String fileName) throws IOException {
        String resourcePath = "/Form/" + fileName;
        InputStream inputStream = getClass().getResourceAsStream(resourcePath);
        List<FormMsg> formMsgs = mapper.readValue(inputStream, new TypeReference<List<FormMsg>>() {});
        for (FormMsg msg : formMsgs) {
            // derived source Id, so generate programmatically here
            msg.setSourceMessageId(String.format("%s_%s_%s", msg.getFirstFiledDatetime(), msg.getMrn(), msg.getFormId()));
        }
        return formMsgs;
    }

    public FormMetadataMsg getFormMetadataMsg() {
        FormMetadataMsg formMetadataMsg = new FormMetadataMsg();
        // TODO: let's find a second real example that isn't the example that's already in the test db...
        formMetadataMsg.setSourceSystem("Clarity");
        formMetadataMsg.setSourceMessageId("2056");
        formMetadataMsg.setValidFrom(Instant.parse("2022-01-01T13:00:00Z"));
        formMetadataMsg.setFormName("UCLH TEP ADVANCED");
        // A mixture of questions we do and don't already know about
        formMetadataMsg.getQuestionIds().addAll(Arrays.asList(
                // These are to test adding new questions to the form that we didn't previously know
                // were used in the form.
                // We have different levels of pre-existing question metadata for them (eg. none, partial, full)
                "TEST#1000", "TEST#1001", "TEST#1002",
                // These are the questions already in the populate script
                "UCLH#1205", "UCLH#1209", "UCLH#1210", "UCLH#1211", "UCLH#1213", "UCLH#1218",
                "UCLH#1219", "UCLH#1220", "UCLH#1221", "UCLH#1222", "UCLH#1223", "UCLH#1224",
                // deliberately omit "UCLH#4480 to test deletion"
                "UCLH#1225", "UCLH#1227", "UCLH#4479", /*"UCLH#4480",*/ "UCLH#4481", "UCLH#4482",
                "UCLH#4497", "UCLH#4498", "UCLH#4499", "UCLH#4500", "UCLH#4501", "UCLH#4502",
                "UCLH#4503", "UCLH#4505", "UCLH#4512", "UCLH#4731", "UCLH#4732"));
        return formMetadataMsg;
    }

    public List<FormQuestionMetadataMsg> getFormQuestionMetadataMsg() {
        List<FormQuestionMetadataMsg> formQuestionMetadataMsgs = new ArrayList<>();
        FormQuestionMetadataMsg q1 = new FormQuestionMetadataMsg();
        q1.setSourceMessageId("UCLH#1205");
        q1.setValidFrom(Instant.parse("2022-01-01T13:00:00Z"));
        q1.setName("ICU Discussion");
        q1.setAbbrevName("ICU Discussion");
        q1.setInternalDataType("Boolean");
        formQuestionMetadataMsgs.add(q1);
        return formQuestionMetadataMsgs;
    }

}
