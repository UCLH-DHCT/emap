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
 * This represents all the different batteries of test that can be ordered.
 * @author Stef Piatek
 */
@SuppressWarnings("serial")
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@AuditTable
public class LabBattery extends TemporalCore<LabBattery, LabBatteryAudit> {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long labBatteryId;


    private String battery;

    /**
     * What system this code belongs to. Examples could be WinPath, or Epic.
     */
    @Column(nullable = false)
    private String labProvider;

    public LabBattery() {}

    public LabBattery(LabTestDefinition labTestDefinitionId, String battery, String labProvider) {
        this.battery = battery;
        this.labProvider = labProvider;
    }

    public LabBattery(LabBattery other) {
        super(other);
        this.battery = other.battery;
        this.labProvider = other.labProvider;
    }

    @Override
    public LabBattery copy() {
        return new LabBattery(this);
    }

    @Override
    public LabBatteryAudit createAuditEntity(Instant validUntil, Instant storedUntil) {
        return new LabBatteryAudit(this, validUntil, storedUntil);
    }

}
