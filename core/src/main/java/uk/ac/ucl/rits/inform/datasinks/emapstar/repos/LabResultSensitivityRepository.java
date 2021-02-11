package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.labs.LabResult;
import uk.ac.ucl.rits.inform.informdb.labs.LabResultSensitivity;

import java.util.Optional;

/**
 * Lab result sensitivity repository.
 * @author Stef Piatek
 */
public interface LabResultSensitivityRepository extends CrudRepository<LabResultSensitivity, Long> {
    Optional<LabResultSensitivity> findByLabResultIdAndAgent(LabResult labResultId, String agent);

}
