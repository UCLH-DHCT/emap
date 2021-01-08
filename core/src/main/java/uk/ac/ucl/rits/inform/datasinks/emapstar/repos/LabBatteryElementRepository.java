package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.labs.LabBatteryElement;

/**
 * Lab battery type repository.
 * @author Stef Piatek
 */
public interface LabBatteryElementRepository extends CrudRepository<LabBatteryElement, Long> {
}
