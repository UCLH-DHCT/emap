package uk.ac.ucl.rits.inform.informdb.labs;

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

import java.time.Instant;

/**
 * This represents all the different batteries of test that can be ordered, and
 * what we know about which constituent tests make them up.
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
public class LabBatteryElement extends TemporalCore<LabBatteryElement, LabBatteryElementAudit> {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long   labBatteryTypeId;

    private long   labBatteryTypeDurableId;

    private String battery;

    private String labTestDefinitionDurableId;

    public LabBatteryElement() {}

    public LabBatteryElement(LabBatteryElement other) {
        super(other);
        this.labBatteryTypeDurableId = other.labBatteryTypeDurableId;
        this.battery = other.battery;
        this.labTestDefinitionDurableId = other.labTestDefinitionDurableId;
    }

    @Override
    public LabBatteryElement copy() {
        return new LabBatteryElement(this);
    }

    @Override
    public LabBatteryElementAudit createAuditEntity(Instant validUntil, Instant storedFrom) {
        return new LabBatteryElementAudit(this, validUntil, storedFrom);
    }

}
