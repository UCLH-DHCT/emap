package uk.ac.ucl.rits.inform.informdb.labs;

import java.time.Instant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.ac.ucl.rits.inform.informdb.AuditCore;
import uk.ac.ucl.rits.inform.informdb.TemporalCore;
import uk.ac.ucl.rits.inform.informdb.annotation.AuditTable;

/**
 * A LabOrder contains the details of the request to perform a lab. A given
 * LabNumber may have multiple LabOrders.
 *
 * @author Roma Klapaukh
 *
 */
@SuppressWarnings("serial")
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@AuditTable
public class LabOrder extends TemporalCore<LabOrder, LabOrderAudit> {

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

    @Override
    public LabOrderAudit createAuditEntity(Instant validUntil, Instant storedUntil) {
        return new LabOrderAudit(this, validUntil, storedUntil);
    }
}
