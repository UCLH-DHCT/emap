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

    /**
     * @return the labOrderId
     */
    public long getLabOrderId() {
        return labOrderId;
    }

    /**
     * @param labOrderId the labOrderId to set
     */
    public void setLabOrderId(long labOrderId) {
        this.labOrderId = labOrderId;
    }

    /**
     * @return the labOrderDurableId
     */
    public long getLabOrderDurableId() {
        return labOrderDurableId;
    }

    /**
     * @param labOrderDurableId the labOrderDurableId to set
     */
    public void setLabOrderDurableId(long labOrderDurableId) {
        this.labOrderDurableId = labOrderDurableId;
    }

    /**
     * @return the labNumberId
     */
    public long getLabNumberId() {
        return labNumberId;
    }

    /**
     * @param labNumberId the labNumberId to set
     */
    public void setLabNumberId(long labNumberId) {
        this.labNumberId = labNumberId;
    }

    /**
     * @return the labBatteryTypeDurableId
     */
    public long getLabBatteryTypeDurableId() {
        return labBatteryTypeDurableId;
    }

    /**
     * @param labBatteryTypeDurableId the labBatteryTypeDurableId to set
     */
    public void setLabBatteryTypeDurableId(long labBatteryTypeDurableId) {
        this.labBatteryTypeDurableId = labBatteryTypeDurableId;
    }

    /**
     * @return the orderDatetime
     */
    public Instant getOrderDatetime() {
        return orderDatetime;
    }

    /**
     * @param orderDatetime the orderDatetime to set
     */
    public void setOrderDatetime(Instant orderDatetime) {
        this.orderDatetime = orderDatetime;
    }

    @Override
    public LabOrder copy() {
        return new LabOrder(this);
    }

}
