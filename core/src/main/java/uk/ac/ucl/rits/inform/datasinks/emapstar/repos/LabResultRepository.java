package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.labs.LabNumber;
import uk.ac.ucl.rits.inform.informdb.labs.LabResult;
import uk.ac.ucl.rits.inform.informdb.labs.LabTestDefinition;

import java.util.Optional;

/**
 * Lab result repository.
 * @author Stef Piatek
 */
public interface LabResultRepository extends CrudRepository<LabResult, Long> {
    Optional<LabResult> findByLabNumberIdAndLabTestDefinitionId(LabNumber labNumber, LabTestDefinition labTestDefinition);

    /**
     * For testing.
     * @param labTestCode test code
     * @return
     */
    Optional<LabResult> findByLabTestDefinitionId_TestLabCode(String labTestCode);
}
