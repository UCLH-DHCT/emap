package uk.ac.ucl.rits.inform.interchange;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TestPathologyOrderFactory {
    private ObjectMapper mapper;

    public TestPathologyOrderFactory() {
        mapper = new ObjectMapper(new YAMLFactory());
        mapper.findAndRegisterModules();
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }


    public List<PathologyOrder> pathologyOrderFactory(String fileName) {
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
        PathologyOrder updatedOrder = orderReader.readValue(ResourceUtils.getFile("classpath:" + fileName));
        return updatedOrder;
    }

    private PathologyResult updatePathologyResultWithDefaults(PathologyResult result, String fileName) throws IOException {
        ObjectReader resultReader = mapper.readerForUpdating(result);
        PathologyResult updatedResult = resultReader.readValue(ResourceUtils.getFile("classpath:" + fileName));
        return updatedResult;
    }
}
