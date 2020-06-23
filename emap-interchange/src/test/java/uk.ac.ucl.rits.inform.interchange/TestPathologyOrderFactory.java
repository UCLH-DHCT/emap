package uk.ac.ucl.rits.inform.interchange;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestPathologyOrderFactory {
    String firstOrderNumber = "12121212";
    String labSpecimenNumber = "13U444444";
    String labSpecimenNumberOCS = "13U4444441";
    String mrn = "40800000";
    String visitNumber = "123412341234";
    String orderStatus = "CM";
    String resultStatus = "F";
    String orderControlId = "RE";
    Instant requestedDateTime = Instant.parse("2013-07-24T16:46:00Z");
    Instant observationDateTime = Instant.parse("2013-07-24T15:41:00Z");
    Instant statusChangeTime = Instant.parse("2013-07-25T12:58:00Z");
    String codingSource = "Winpath";
    String sourceMessageIdPrefix = "0000000000_";

    public TestPathologyOrderFactory() {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.findAndRegisterModules();
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        try {
            File file = ResourceUtils.getFile("classpath:ORU_R01.yaml");
            List<PathologyOrder> pathologyOrders = mapper.readValue(file, new TypeReference<List<PathologyOrder>>() {});
            System.out.println(pathologyOrders.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private PathologyResult buildPathologyResult(String orderNumber, Map<String, String> resultData) {
        PathologyResult result = new PathologyResult();

        // set common data
        result.setResultStatus(resultStatus);
        result.setResultTime(statusChangeTime);
        result.setTestItemCodingSystem(codingSource);

        // set data that changes per result
        result.setEpicCareOrderNumber(orderNumber);
        result.setValueType(resultData.get("valueType"));
        result.setTestItemLocalCode(resultData.get("testItemLocalCode"));
        result.setTestItemLocalDescription(resultData.get("testItemLocalDescription"));
        result.setValueType(resultData.get("stringValue"));
        if (resultData.containsKey("numericValue")) {
            result.setNumericValue(Double.valueOf(resultData.get("numericValue")));
        }
        result.setUnits(resultData.get("units"));
        result.setReferenceRange(resultData.get("referenceRange"));

        return result;
    }


    private PathologyOrder buildPathologyOrder(String sourceMessageId, String epicCareOrderNumber,
                                               Map<String, String> orderData, List<Map<String, String>> resultsData) {

        PathologyOrder order = new PathologyOrder();
        // Set common data for test case
        order.setOrderControlId(orderControlId);
        order.setLabSpecimenNumber(labSpecimenNumber);
        order.setLabSpecimenNumberOCS(labSpecimenNumberOCS);
        order.setOrderStatus(orderStatus);
        order.setResultStatus(resultStatus);
        order.setMrn(mrn);
        order.setVisitNumber(visitNumber);
        order.setRequestedDateTime(requestedDateTime);
        order.setObservationDateTime(observationDateTime);
        order.setStatusChangeTime(statusChangeTime);
        order.setTestBatteryCodingSystem(codingSource);
        order.setParentObservationIdentifier("");
        order.setParentSubId("");
        // set data that changes per pathology order
        order.setSourceMessageId(sourceMessageId);
        order.setLabDepartment(orderData.get("labDepartment"));
        order.setEpicCareOrderNumber(epicCareOrderNumber);
        order.setTestBatteryLocalCode(orderData.get("testBatteryLocalCode"));
        order.setTestBatteryLocalDescription(orderData.get("testBatteryLocalDescription"));
        // build pathology results
        for (Map<String, String> resultData : resultsData) {
            PathologyResult result = buildPathologyResult(epicCareOrderNumber, resultData);
            order.addPathologyResult(result);
        }
        return order;
    }

    public List<PathologyOrder> buildPathologyOrders() {
        // Set up all data for orders
        Map<String, String> singleOrderData = new HashMap<>();
        singleOrderData.put("labDepartment", "CC");
        singleOrderData.put("testBatteryLocalCode", "BON");
        singleOrderData.put("testBatteryLocalDescription", "BONE PROFILE");

        List<Map<String, String>> ordersData = Arrays.asList(singleOrderData);

        // Set up all data for the results within the orders
        Map<String, String> singleResultData = new HashMap<>();
        singleResultData.put("valueType", "NM");
        singleResultData.put("testItemLocalCode", "ALP");
        singleResultData.put("testItemLocalDescription", "Alkaline phosphatase");
        singleResultData.put("stringValue", "104");
        singleResultData.put("numericValue", "104.0");
        singleResultData.put("units", "IU/L");
        singleResultData.put("referenceRange", "45-104");


        List<Map<String, String>> resultData = Arrays.asList(singleResultData);
        List<List<Map<String, String>>> allResultsData = Arrays.asList(resultData);

        List<PathologyOrder> orders = new ArrayList<>();
        int i = 0;
        for (Map<String, String> orderData : ordersData) {
            String sourceMessageId = sourceMessageIdPrefix + String.format("%02d", i);
            String epicCareOrderNumber = Long.valueOf(firstOrderNumber + 1 + i).toString();
            PathologyOrder order = buildPathologyOrder(sourceMessageId, epicCareOrderNumber, orderData, allResultsData.get(i));
            orders.add(order);
            i++;
        }

        return orders;
    }


}
