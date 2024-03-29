package uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.labs.LabBattery;
import uk.ac.ucl.rits.inform.informdb.labs.LabBatteryElement;
import uk.ac.ucl.rits.inform.informdb.labs.LabTestDefinition;

import java.util.Optional;

/**
 * Lab battery Element repository.
 * @author Stef Piatek
 */
public interface LabBatteryElementRepository extends CrudRepository<LabBatteryElement, Long> {
    Optional<LabBatteryElement> findByLabBatteryIdAndLabTestDefinitionId(LabBattery batteryId, LabTestDefinition testDefinition);

    /**
     * For testing.
     * @param testLabCode Lab code for testing.
     * @return LabBatteryElement
     */
    Optional<LabBatteryElement> findByLabTestDefinitionIdTestLabCode(String testLabCode);
}
