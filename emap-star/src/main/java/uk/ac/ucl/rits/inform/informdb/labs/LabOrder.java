package uk.ac.ucl.rits.inform.informdb.labs;

import java.time.Instant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import uk.ac.ucl.rits.inform.informdb.TemporalCore;

/**
 * A LabOrder contains the details of the request to perform a lab. A given
 * LabNumber may have multiple LabOrders.
 *
 * @author Roma Klapaukh
 *
 */
@Entity
public class LabOrder extends TemporalCore<LabOrder> {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long    labOrderId;
    private long    labOrderDurableId;

    private long    labNumberId;
    private long    labBatteryTypeDurableId;

    @Column(columnDefinition = "timestamp with time zone")
    private Instant orderDatetime;

    public LabOrder() {}

    public LabOrder(LabOrder other) {
        super(other);

        this.labOrderDurableId = other.labOrderDurableId;

        this.labNumberId = other.labNumberId;
        this.labBatteryTypeDurableId = other.labBatteryTypeDurableId;

        this.orderDatetime = other.orderDatetime;
    }

    @Override
    public LabOrder copy() {
        return new LabOrder(this);
    }

}
