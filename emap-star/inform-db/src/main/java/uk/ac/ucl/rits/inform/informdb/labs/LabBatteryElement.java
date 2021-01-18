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
 * This represents all the different batteries of test that can be ordered, and
 * what we know about which constituent tests make them up.
 * @author Roma Klapaukh
 * @author Stef Piatek
 */
@SuppressWarnings("serial")
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@AuditTable
public class LabBatteryElement extends TemporalCore<LabBatteryElement, LabBatteryElementAudit> {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long labBatteryElementId;


    @ManyToOne
    @JoinColumn(name = "labTestDefinitionId", nullable = false)
    private LabTestDefinition labTestDefinitionId;

    private String battery;

    /**
     * What system this code belongs to. Examples could be WinPath, or Epic.
     */
    @Column(nullable = false)
    private String labProvider;

    public LabBatteryElement() {}

    public LabBatteryElement(LabTestDefinition labTestDefinitionId, String battery, String labProvider) {
        this.labTestDefinitionId = labTestDefinitionId;
        this.battery = battery;
        this.labProvider = labProvider;
    }

    public LabBatteryElement(LabBatteryElement other) {
        super(other);
        this.labBatteryElementId = other.labBatteryElementId;
        this.battery = other.battery;
        this.labTestDefinitionId = other.labTestDefinitionId;
        this.labProvider = other.labProvider;
    }

    @Override
    public LabBatteryElement copy() {
        return new LabBatteryElement(this);
    }

    @Override
    public LabBatteryElementAudit createAuditEntity(Instant validUntil, Instant storedUntil) {
        return new LabBatteryElementAudit(this, validUntil, storedUntil);
    }

}
