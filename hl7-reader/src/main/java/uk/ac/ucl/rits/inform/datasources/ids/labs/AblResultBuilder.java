package uk.ac.ucl.rits.inform.datasources.ids.labs;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.v26.segment.NTE;
import ca.uhn.hl7v2.model.v26.segment.OBX;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ucl.rits.inform.datasources.ids.HL7Utils;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.ValueType;

import java.time.Instant;
import java.util.List;

/**
 * Build Lab Results for ABL 90 Flex.
 * @author Stef Piatek
 */
public class AblResultBuilder extends LabResultBuilder {
    private static final Logger logger = LoggerFactory.getLogger(AblResultBuilder.class);
    private static final String NORMAL_FLAG = "N";
    private final String codingSystem;

    /**
     * Builder for LabResults for ABL 90 Flex.
     * <p>
     * @param obx          the OBX segment for this result
     * @param notes        Notes for OBX
     * @param codingSystem Coding system to set
     */
    AblResultBuilder(OBX obx, List<NTE> notes, String codingSystem) {
        super(obx, notes, AblResultBuilder.NORMAL_FLAG);
        this.codingSystem = codingSystem;
    }

    /**
     * Set result time from OBX segment.
     * @throws DataTypeException if HAPI does
     */
    @Override
    public void setResultTime() throws DataTypeException {
        Instant resultTime = HL7Utils.interpretLocalTime(getObx().getObx14_DateTimeOfTheObservation());
        getMessage().setResultTime(resultTime);
    }

    /**
     * Set custom coding system.
     */
    @Override
    void setCustomOverrides() {
        getMessage().setTestItemCodingSystem(codingSystem);
    }

    /**
     * Always parse as numeric.
     * String values are also populated for debugging.
     */
    @Override
    protected void setValueAndMimeType() throws HL7Exception {
        setStringValueAndMimeType(getObx());
        getMessage().setMimeType(ValueType.NUMERIC);
        try {
            if (getMessage().getStringValue().isSave()) {
                setNumericValueAndResultOperator(getMessage().getStringValue().get());
            }
        } catch (NumberFormatException e) {
            logger.warn("LabResult numeric result couldn't be parsed. Will delete existing value: {}", getMessage().getStringValue());
            getMessage().setNumericValue(InterchangeValue.delete());
        }
    }
}
