package uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.labs.LabIsolate;
import uk.ac.ucl.rits.inform.informdb.labs.LabResult;
import uk.ac.ucl.rits.inform.informdb.labs.LabSensitivity;

import java.util.Optional;

/**
 * Lab result sensitivity repository.
 * @author Stef Piatek
 */
public interface LabSensitivityRepository extends CrudRepository<LabSensitivity, Long> {
    Optional<LabSensitivity> findByLabIsolateIdAndAgent(LabIsolate labIsolateId, String agent);

    /**
     * For testing.
     * @param isolate isolate code
     * @param agent   antibiotic agent
     * @return optional sensitivity
     */
    Optional<LabSensitivity> findByLabIsolateIdIsolateCodeAndAgent(String isolate, String agent);

}
