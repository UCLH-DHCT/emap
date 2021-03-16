package uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.labs.LabTestDefinition;

import java.util.Optional;

/**
 * Lab test definition repository.
 * @author Stef Piatek
 */
public interface LabTestDefinitionRepository extends CrudRepository<LabTestDefinition, Long> {
    Optional<LabTestDefinition> findByLabProviderAndLabDepartmentAndTestLabCode(
            String labProvider, String labDepartment, String labTestCode
    );

    /**
     * For testing.
     * @param testCode individual test code
     * @return LabTestDefinition
     */
    Optional<LabTestDefinition> findByTestLabCode(String testCode);

}
