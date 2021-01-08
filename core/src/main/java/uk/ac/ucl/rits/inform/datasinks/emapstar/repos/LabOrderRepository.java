package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.labs.LabBatteryElement;
import uk.ac.ucl.rits.inform.informdb.labs.LabNumber;
import uk.ac.ucl.rits.inform.informdb.labs.LabOrder;

import java.time.Instant;
import java.util.Optional;

/**
 * Lab order repository.
 * @author Stef Piatek
 */
public interface LabOrderRepository extends CrudRepository<LabOrder, Long> {
    Optional<LabOrder> findByLabBatteryElementIdAndLabNumberIdAndOrderDatetime(LabBatteryElement element, LabNumber number, Instant orderDateTime);
}
