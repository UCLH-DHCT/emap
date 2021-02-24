package uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.labs.LabBattery;
import uk.ac.ucl.rits.inform.informdb.labs.LabBatteryElement;
import uk.ac.ucl.rits.inform.informdb.labs.LabNumber;
import uk.ac.ucl.rits.inform.informdb.labs.LabOrder;

import java.util.Optional;

/**
 * Lab order repository.
 * @author Stef Piatek
 */
public interface LabOrderRepository extends CrudRepository<LabOrder, Long> {
    Optional<LabOrder> findByLabBatteryIdAndLabNumberId(LabBattery battery, LabNumber number);

    /**
     * for testing.
     * @param labNumber laboratory internal lab number
     * @return LabOrder
     */
    Optional<LabOrder> findByLabNumberIdInternalLabNumber(String labNumber);
}
