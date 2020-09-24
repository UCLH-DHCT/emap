package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.demographics.AuditCoreDemographic;

/**
 * Repository for interaction with the AuditCoreDemographic table.
 */
public interface AuditCoreDemographicRepository extends CrudRepository<AuditCoreDemographic, Integer> {
}
