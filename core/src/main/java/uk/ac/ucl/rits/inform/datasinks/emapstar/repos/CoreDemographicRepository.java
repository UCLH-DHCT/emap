package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.demographics.CoreDemographic;

/**
 * Repository for interaction with the CoreDemographic table.
 */
public interface CoreDemographicRepository extends CrudRepository<CoreDemographic, Integer> {
}
