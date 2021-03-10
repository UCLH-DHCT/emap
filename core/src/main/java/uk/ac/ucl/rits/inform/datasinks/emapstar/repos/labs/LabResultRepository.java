package uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.labs.LabNumber;
import uk.ac.ucl.rits.inform.informdb.labs.LabResult;
import uk.ac.ucl.rits.inform.informdb.labs.LabTestDefinition;

import java.util.List;
import java.util.Optional;

/**
 * Lab result repository.
 * @author Stef Piatek
 */
public interface LabResultRepository extends CrudRepository<LabResult, Long> {
    Optional<LabResult> findByLabNumberIdAndLabTestDefinitionId(LabNumber labNumber, LabTestDefinition labTestDefinition);

    Optional<LabResult> findByLabNumberIdAndLabTestDefinitionIdAndValueAsText(
            LabNumber labNumber, LabTestDefinition testDefinition, String isolateCodeAndText);


    /**
     * For testing.
     * @param labTestCode test code
     * @return optional of lab result
     */
    Optional<LabResult> findByLabTestDefinitionIdTestLabCode(String labTestCode);

    /**
     * For testing.
     * @param labTestCode test code
     * @param value       value
     * @return optional of lab result
     */
    Optional<LabResult> findByLabTestDefinitionIdTestLabCodeAndValueAsText(String labTestCode, String value);

    /**
     * for testing.
     * @param labNumber epic lab number.
     * @return List of all lab results
     */
    List<LabResult> findAllByLabNumberIdExternalLabNumber(String labNumber);
}
