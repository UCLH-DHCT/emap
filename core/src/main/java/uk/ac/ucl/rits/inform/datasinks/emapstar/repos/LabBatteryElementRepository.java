package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.labs.LabBatteryElement;
import uk.ac.ucl.rits.inform.informdb.labs.LabTestDefinition;

import java.util.Optional;

/**
 * Lab battery type repository.
 * @author Stef Piatek
 */
public interface LabBatteryElementRepository extends CrudRepository<LabBatteryElement, Long> {
    Optional<LabBatteryElement> findByBatteryAndLabTestDefinitionIdAndLabDepartment(
            String battery, LabTestDefinition testDefinition, String labDepartment);
}
