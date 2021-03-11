package uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.labs.LabOrder;
import uk.ac.ucl.rits.inform.informdb.labs.LabResult;
import uk.ac.ucl.rits.inform.informdb.labs.LabTestDefinition;

import java.util.List;
import java.util.Optional;

/**
 * Lab result repository.
 * @author Stef Piatek
 */
public interface LabResultRepository extends CrudRepository<LabResult, Long> {
    Optional<LabResult> findByLabOrderIdAndLabTestDefinitionId(LabOrder labOrder, LabTestDefinition labTestDefinition);

    /**
     * For testing.
     * @param labTestCode test code
     * @return optional of lab result
     */
    Optional<LabResult> findByLabTestDefinitionIdTestLabCode(String labTestCode);

    /**
     * for testing.
     * @param labNumber epic lab number.
     * @return List of all lab results
     */
    List<LabResult> findAllByLabOrderIdInternalLabNumber(String labNumber);
}
