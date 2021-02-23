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
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.time.Instant;

/**
 * A LabOrder contains the details of the request to perform a lab. A given
 * LabNumber may have multiple LabOrders.
 * @author Roma Klapaukh
 * @author Stef Piatek
 */
@SuppressWarnings("serial")
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@AuditTable
@Table(indexes = {@Index(name = "lo_lab_number_id", columnList = "labNumberId"),
        @Index(name = "lo_lab_battery_element_id", columnList = "labBatteryElementId")})
public class LabOrder extends TemporalCore<LabOrder, LabOrderAudit> {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long labOrderId;

    @ManyToOne
    @JoinColumn(name = "labNumberId", nullable = false)
    private LabNumber labNumberId;

    @ManyToOne
    @JoinColumn(name = "labBatteryId", nullable = false)
    private LabBattery labBatteryId;

    @Column(columnDefinition = "timestamp with time zone")
    private Instant orderDatetime;

    @Column(columnDefinition = "timestamp with time zone")
    private Instant requestDatetime;

    @Column(columnDefinition = "timestamp with time zone")
    private Instant sampleDatetime;

    @Column(columnDefinition = "text")
    private String clinicalInformation;

    public LabOrder() {}

    public LabOrder(LabBattery labBatteryId, LabNumber labNumberId) {
        this.labBatteryId = labBatteryId;
        this.labNumberId = labNumberId;
    }

    public LabOrder(LabOrder other) {
        super(other);
        this.labOrderId = other.labOrderId;
        this.labNumberId = other.labNumberId;
        this.labBatteryId = other.labBatteryId;
        this.orderDatetime = other.orderDatetime;
        this.requestDatetime = other.requestDatetime;
        this.sampleDatetime = other.sampleDatetime;
        this.clinicalInformation = other.clinicalInformation;
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
