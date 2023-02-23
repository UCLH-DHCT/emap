package uk.ac.ucl.rits.inform.datasources.ids.labs;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.v26.segment.NTE;
import ca.uhn.hl7v2.model.v26.segment.OBX;
import uk.ac.ucl.rits.inform.datasources.ids.HL7Utils;
import uk.ac.ucl.rits.inform.datasources.ids.exceptions.Hl7InconsistencyException;
import uk.ac.ucl.rits.inform.interchange.OrderCodingSystem;

import java.time.Instant;
import java.util.List;

final class BankManagerResultBuilder extends LabResultBuilder {
    private static final OrderCodingSystem CODING_SYSTEM = OrderCodingSystem.BANK_MANAGER;

    /**
     * @param obx   OBX segment
     * @param notes Notes for OBX
     */
    BankManagerResultBuilder(OBX obx, List<NTE> notes) {
        super(obx, notes, null);
    }

    /**
     * Set the result time.
     * @throws DataTypeException if the result time can't be parsed by HAPI
     */
    @Override
    void setResultTime() throws DataTypeException {
        Instant resultTime = HL7Utils.interpretLocalTime(getObx().getObx14_DateTimeOfTheObservation());
        getMessage().setResultTime(resultTime);
    }

    /**
     * Any custom overriding methods to populate individual field data.
     */
    @Override
    void setCustomOverrides() throws Hl7InconsistencyException {
        getMessage().setTestItemCodingSystem(CODING_SYSTEM.name());
    }

    /**
     * Parse single string or numeric value.
     * @throws Hl7InconsistencyException if data cannot be parsed.
     * @throws HL7Exception              If hl7value can't be decoded
     */
    @Override
    void setValueAndMimeType() throws Hl7InconsistencyException, HL7Exception {
        setSingleTextOrNumericValue();
    }
}
