package uk.ac.ucl.rits.inform.informdb.labs;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.ac.ucl.rits.inform.informdb.TemporalCore;
import uk.ac.ucl.rits.inform.informdb.annotation.AuditTable;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.Instant;

/**
 * A LabOrder contains the details of the request to perform a lab investigation.
 * A given LabSample may have multiple LabOrders. Each of the lab orders is defined by a unique combination of LabBattery and LabSample.
 * @author Roma Klapaukh
 * @author Stef Piatek
 */
@SuppressWarnings("serial")
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@AuditTable(indexes = {@Index(name = "loa_lab_sample_id", columnList = "labSampleId")})
@Table(indexes = {@Index(name = "lo_lab_battery_id", columnList = "labBatteryId"),
        @Index(name = "lo_lab_sample_id", columnList = "labSampleId")})
public class LabOrder extends TemporalCore<LabOrder, LabOrderAudit> {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long labOrderId;

    @ManyToOne
    @JoinColumn(name = "labSampleId", nullable = false)
    private LabSample labSampleId;

    /**
     * Can have labs that are not linked to a hospital visit.
     */
    @ManyToOne
    @JoinColumn(name = "hospitalVisitId")
    private HospitalVisit hospitalVisitId;

    @ManyToOne
    @JoinColumn(name = "labBatteryId", nullable = false)
    private LabBattery labBatteryId;

    @Column(columnDefinition = "timestamp with time zone")
    private Instant orderDatetime;

    @Column(columnDefinition = "timestamp with time zone")
    private Instant requestDatetime;

    @Column(columnDefinition = "text")
    private String clinicalInformation;

    /**
     * Lab number in the EHR.
     */
    private String internalLabNumber;

    private String sourceSystem;

    public LabOrder() {}

    public LabOrder(LabBattery labBatteryId, LabSample labSampleId) {
        this.labBatteryId = labBatteryId;
        this.labSampleId = labSampleId;
    }

    public LabOrder(LabOrder other) {
        super(other);
        this.labOrderId = other.labOrderId;
        this.labSampleId = other.labSampleId;
        this.labBatteryId = other.labBatteryId;
        this.hospitalVisitId = other.hospitalVisitId;
        this.orderDatetime = other.orderDatetime;
        this.requestDatetime = other.requestDatetime;
        this.clinicalInformation = other.clinicalInformation;
        this.internalLabNumber = other.internalLabNumber;
        this.sourceSystem = other.sourceSystem;
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
