package uk.ac.ucl.rits.inform.informdb.labs;

import uk.ac.ucl.rits.inform.informdb.TemporalCore;

public class LabCollection extends TemporalCore<LabCollection> {
    public LabCollection() {}

    public LabCollection(LabCollection other) {
        super(other);
    }

    @Override
    public LabCollection copy() {
        return new LabCollection(this);
    }
}
