package uk.ac.ucl.rits.inform.datasources.ids.labs;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.v26.segment.NTE;
import ca.uhn.hl7v2.model.v26.segment.OBR;
import ca.uhn.hl7v2.model.v26.segment.OBX;
import uk.ac.ucl.rits.inform.datasources.ids.exceptions.Hl7InconsistencyException;

import java.util.List;

public final class BankManagerResultBuilder extends LabResultBuilder {

    /**
     * @param obx        OBX segment
     * @param notes      Notes for OBX
     */
    BankManagerResultBuilder(OBR obr, OBX obx, List<NTE> notes) {
        super(obx, notes, null);
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
    void setCustomOverrides() throws Hl7InconsistencyException {

    }

    /**
     * Each parser should define how to parse their values.
     * Simplest case would just call {@link LabResultBuilder#setSingleTextOrNumericValue}
     * @throws Hl7InconsistencyException if data cannot be parsed.
     * @throws HL7Exception              If hl7value can't be decoded
     */
    @Override
    void setValueAndMimeType() throws Hl7InconsistencyException, HL7Exception {

    }
}
