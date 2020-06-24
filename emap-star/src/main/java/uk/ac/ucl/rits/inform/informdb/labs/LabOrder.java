package uk.ac.ucl.rits.inform.informdb.labs;

import uk.ac.ucl.rits.inform.informdb.TemporalCore;

public class LabOrder extends TemporalCore<LabOrder> {

    private long labOrderId;
    private long labOrderDurableId;

    private long labBatteryTypeDurableId;

    public LabOrder() {}

    public LabOrder(LabOrder other) {
        super(other);
    }

    @Override
    public LabOrder copy() {
        return new LabOrder(this);
    }

}
