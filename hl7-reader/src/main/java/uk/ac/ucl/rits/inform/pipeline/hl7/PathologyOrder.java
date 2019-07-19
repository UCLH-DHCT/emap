package uk.ac.ucl.rits.inform.pipeline.hl7;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.hl7v2.model.Message;

/**
 * The top level of the pathology tree, the order.
 * @author Jeremy Stein
 */
public class PathologyOrder {
    private static final Logger logger = LoggerFactory.getLogger(PathologyOrder.class);
    private List<PathologyBatteryResult> pathologyBatteryResults = new ArrayList<>();

    /**
     * Build a pathology order structure from a pathology order message.
     * Since the order message doesn't contain any results this will be sparse to start with.
     * @param ormMsg the ORM^O01 message
     */
    public PathologyOrder(Message ormMsg) {
    }
}
