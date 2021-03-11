package uk.ac.ucl.rits.inform.datasources.ids.labs;

import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.Type;
import ca.uhn.hl7v2.model.Varies;
import ca.uhn.hl7v2.model.v26.datatype.CE;
import ca.uhn.hl7v2.model.v26.datatype.ST;
import ca.uhn.hl7v2.model.v26.segment.NTE;
import ca.uhn.hl7v2.model.v26.segment.OBR;
import ca.uhn.hl7v2.model.v26.segment.OBX;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ucl.rits.inform.datasources.ids.HL7Utils;
import uk.ac.ucl.rits.inform.datasources.ids.exceptions.Hl7InconsistencyException;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.ValueType;
import uk.ac.ucl.rits.inform.interchange.lab.LabIsolateMsg;
import uk.ac.ucl.rits.inform.interchange.lab.LabResultMsg;

import java.util.List;

/**
 * Builder for LabResults for CoPath.
 * @author Stef Piatek
 */
public class CoPathResultBuilder extends LabResultBuilder {
    private static final Logger logger = LoggerFactory.getLogger(CoPathResultBuilder.class);
    private final OBR obr;

    /**
     * @param obx   the OBX segment for this result
     * @param obr   the OBR segment for this result (will be the same segment shared with other OBXs)
     * @param notes Notes for the OBX segment
     */
    CoPathResultBuilder(OBX obx, OBR obr, List<NTE> notes) {
        super(obx, notes, null);
        this.obr = obr;
    }

    /**
     * Set the result time.
     * @throws DataTypeException if the result time can't be parsed by HAPI
     */
    @Override
    void setResultTime() throws DataTypeException {

    }

    /**
     * Any custom overriding methods to populate individual field data.
     */
    @Override
    void setCustomOverrides() {

    }
}
