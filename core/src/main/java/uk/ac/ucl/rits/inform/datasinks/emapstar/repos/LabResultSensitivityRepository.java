package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.labs.LabResult;
import uk.ac.ucl.rits.inform.informdb.labs.LabResultSensitivity;

import javax.swing.text.html.Option;
import java.util.Optional;

/**
 * Lab result sensitivity repository.
 * @author Stef Piatek
 */
public interface LabResultSensitivityRepository extends CrudRepository<LabResultSensitivity, Long> {
    Optional<LabResultSensitivity> findByLabResultIdAndAgent(LabResult labResultId, String agent);

    /**
     * For testing.
     * @param isolate isolate and text
     * @param agent antibiotic agent
     * @return optional sensitivity
     */
    Optional<LabResultSensitivity> findByLabResultIdValueAsTextAndAgent(String isolate, String agent);

}
