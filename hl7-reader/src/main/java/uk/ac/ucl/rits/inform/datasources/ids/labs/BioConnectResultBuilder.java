package uk.ac.ucl.rits.inform.datasources.ids.labs;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.v26.segment.NTE;
import ca.uhn.hl7v2.model.v26.segment.OBR;
import ca.uhn.hl7v2.model.v26.segment.OBX;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ucl.rits.inform.datasources.ids.HL7Utils;
import uk.ac.ucl.rits.inform.datasources.ids.exceptions.Hl7InconsistencyException;

import java.util.List;

/**
 * Builder for LabResults for BIO-CONNECT.
 * @author Stef Piatek
 */
public class BioConnectResultBuilder extends LabResultBuilder {
    private static final Logger logger = LoggerFactory.getLogger(BioConnectResultBuilder.class);
    private static final String NORMAL_FLAG = "N";
    private final OBR obr;
    private final String codingSystem;

    /**
     * @param obx   the OBX segment for this result
     * @param obr   the OBR segment for this result (will be the same segment shared with other OBXs)
     * @param notes Notes for the OBX segment
     * @param codingSystem coding system to manually set
     */
    BioConnectResultBuilder(OBX obx, OBR obr, List<NTE> notes, String codingSystem) {
        super(obx, notes, NORMAL_FLAG);
        this.codingSystem = codingSystem;
        this.obr = obr;
    }

    @Override
    void setResultTime() throws DataTypeException {
        getMessage().setResultTime(HL7Utils.interpretLocalTime(obr.getObr22_ResultsRptStatusChngDateTime()));
    }

    @Override
    void setCustomOverrides() {
        getMessage().setTestItemCodingSystem(codingSystem);

        String localCode = getObx().getObx3_ObservationIdentifier().getCwe1_Identifier().getValueOrEmpty();
        getMessage().setTestItemLocalCode(localCode);
    }

    /**
     * Parse single text or numeric data.
     * @throws Hl7InconsistencyException if data cannot be parsed.
     */
    @Override
    void setValueAndMimeType() throws Hl7InconsistencyException, HL7Exception {
        setSingleTextOrNumericValue();
    }
}
