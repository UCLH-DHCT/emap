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
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import java.time.Instant;

/**
 * This represents all the different batteries of test that can be ordered, and
 * what we know about which constituent tests make them up.
 * @author Roma Klapaukh
 */
@SuppressWarnings("serial")
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@AuditTable
public class LabBatteryType extends TemporalCore<LabBatteryType, LabBatteryTypeAudit> {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long labBatteryTypeId;


    @ManyToOne
    @JoinColumn(name = "labTestDefinitionId", nullable = false)
    private LabTestDefinition labTestDefinitionId;

    private String battery;

    /**
     * Department that has defined the battery.
     */
    private String labDepartment;

    public LabBatteryType() {}

    public LabBatteryType(LabBatteryType other) {
        super(other);
        this.labBatteryTypeId = other.labBatteryTypeId;
        this.battery = other.battery;
        this.labTestDefinitionId = other.labTestDefinitionId;
        this.labDepartment = other.labDepartment;
    }

    @Override
    public LabBatteryType copy() {
        return new LabBatteryType(this);
    }

    @Override
    public LabBatteryTypeAudit createAuditEntity(Instant validUntil, Instant storedFrom) {
        return new LabBatteryTypeAudit(this, validUntil, storedFrom);
    }

}
