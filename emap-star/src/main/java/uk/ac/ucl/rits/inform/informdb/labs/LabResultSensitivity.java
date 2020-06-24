package uk.ac.ucl.rits.inform.informdb.labs;

import uk.ac.ucl.rits.inform.informdb.TemporalCore;

public class LabResultSensitivity extends TemporalCore<LabResultSensitivity> {
    public LabResultSensitivity() {}

    public LabResultSensitivity(LabResultSensitivity other) {
        super(other);
    }

    @Override
    public LabResultSensitivity copy() {
        return new LabResultSensitivity(this);
    }

}
