package uk.ac.ucl.rits.inform.informdb.labs;

import uk.ac.ucl.rits.inform.informdb.TemporalCore;

public class LabResult extends TemporalCore<LabResult> {
    public LabResult() {}

    public LabResult(LabResult other) {
        super(other);
    }

    @Override
    public LabResult copy() {
        return new LabResult(this);
    }

}
