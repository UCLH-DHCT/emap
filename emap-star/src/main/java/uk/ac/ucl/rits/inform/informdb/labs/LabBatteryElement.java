package uk.ac.ucl.rits.inform.informdb.labs;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import uk.ac.ucl.rits.inform.informdb.TemporalCore;

/**
 * This represents all the different batteries of test that can be ordered, and
 * what we know about which constituent tests make them up.
 *
 * @author Roma Klapaukh
 *
 */
public class LabBatteryElement extends TemporalCore<LabBatteryElement> {

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

}
