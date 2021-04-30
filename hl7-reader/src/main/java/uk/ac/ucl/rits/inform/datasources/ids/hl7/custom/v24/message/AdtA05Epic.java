package uk.ac.ucl.rits.inform.datasources.ids.hl7.custom.v24.message;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.v26.message.ADT_A05;
import ca.uhn.hl7v2.parser.DefaultModelClassFactory;
import ca.uhn.hl7v2.parser.ModelClassFactory;
import uk.ac.ucl.rits.inform.datasources.ids.hl7.custom.v24.segment.ZIF;

import java.util.Arrays;

/**
 * Example custom model class. This is a ADT^A05^EPIC, which is an ADT^A05 with an
 * extra ZIF segment after the PV2 segment
 * <p>
 * Since we're extending an existing HL7 message type, we just extend from the
 * model class representing that type
 */
@SuppressWarnings("serial")
public class AdtA05Epic extends ADT_A05 {

    /**
     * Constructor.
     */
    public AdtA05Epic() throws HL7Exception {
        this(new DefaultModelClassFactory());
    }

    /**
     * Constructor.
     * <p>
     * We always have to have a constructor with this one argument
     * @param factory model class factory
     * @throws HL7Exception if HL7 parsing error
     */
    public AdtA05Epic(ModelClassFactory factory) throws HL7Exception {
        super(factory);
        init();
        log.debug("Built ADT A05 EPIC");
    }

    private void init() throws HL7Exception {
        // Add the ZIF segment at the right spot
        String[] segmentNames = getNames();
        int indexOfPid = Arrays.asList(segmentNames).indexOf("PV2");
        int index = indexOfPid + 1;
        Class<ZIF> type = ZIF.class;

        // segment not required, can repeat
        this.add(type, false, true, index);
    }

    /**
     * Add an accessor for the ZIF segment.
     * @return ZIF segment
     */
    public ZIF getZIF() {
        return getTyped("ZIF", ZIF.class);
    }

}
