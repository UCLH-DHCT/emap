package uk.ac.ucl.rits.inform.datasources.ids.hl7.custom.v26.message;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.parser.DefaultModelClassFactory;
import ca.uhn.hl7v2.parser.ModelClassFactory;
import uk.ac.ucl.rits.inform.datasources.ids.hl7.custom.v26.segment.ZIF;

import java.util.Arrays;

/**
 * This is an EPIC implementation of ADT^A05, which overrides the normal ADT^A05 with an extra ZIF segment after the PV2 segment.
 * <p>
 * Since we're extending an existing HL7 message type, we just extend from the model class representing that type.
 * The message type (e.g. ADT^A05) is converted to the class name that is used (e.g. ADT_A05) so we have to use the same class name.
 * The hl7 version used is the ModelClassFactory's default version so we're using 2.6 (instead of 2.4 in the message)
 */
@SuppressWarnings("serial")
public class ADT_A05 extends ca.uhn.hl7v2.model.v26.message.ADT_A05 {

    /**
     * Constructor.
     */
    public ADT_A05() throws HL7Exception {
        this(new DefaultModelClassFactory());
    }

    /**
     * Constructor.
     * <p>
     * We always have to have a constructor with this one argument
     * @param factory model class factory
     * @throws HL7Exception if HL7 parsing error
     */
    public ADT_A05(ModelClassFactory factory) throws HL7Exception {
        super(factory);
        // Add the ZIF segment at the right spot
        String[] segmentNames = getNames();
        int indexOfPid = Arrays.asList(segmentNames).indexOf("PV2");
        int index = indexOfPid + 1;
        Class<ZIF> type = ZIF.class;

        // segment not required, can repeat
        this.add(type, false, true, index);
        log.debug("Built ADT A05 EPIC");
    }

    /**
     * Add an accessor for the ZIF segment.
     * @return ZIF segment
     */
    public ZIF getZIF() {
        return getTyped("ZIF", ZIF.class);
    }

}
