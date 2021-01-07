package uk.ac.ucl.rits.inform.informdb.labs;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.ac.ucl.rits.inform.informdb.TemporalCore;
import uk.ac.ucl.rits.inform.informdb.annotation.AuditTable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.time.Instant;

/**
 * A LabOrder contains the details of the request to perform a lab. A given
 * LabNumber may have multiple LabOrders.
 * @author Roma Klapaukh
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
    private long labOrderId;

    @ManyToOne
    @JoinColumn(name = "labNumberId", nullable = false)
    private LabNumber labNumberId;

    @ManyToOne
    @JoinColumn(name = "labBatteryElementId", nullable = false)
    private LabBatteryElement labBatteryElementId;

    @Column(columnDefinition = "timestamp with time zone")
    private Instant orderDatetime;

    public LabOrder() {}

    public LabOrder(LabOrder other) {
        super(other);
        this.labOrderId = other.labOrderId;
        this.labNumberId = other.labNumberId;
        this.labBatteryElementId = other.labBatteryElementId;
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
