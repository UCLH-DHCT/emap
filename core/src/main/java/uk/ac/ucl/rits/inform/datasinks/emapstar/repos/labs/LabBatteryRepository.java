package uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.labs.LabBattery;

import java.util.Optional;

/**
 * Lab Battery repository.
 * @author Stef Piatek
 */
public interface LabBatteryRepository extends CrudRepository<LabBattery, Long> {
    Optional<LabBattery> findByBatteryCodeAndLabProvider(String batteryCode, String labProvider);
}
