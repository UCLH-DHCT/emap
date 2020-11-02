package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.demographics.CoreDemographicAudit;

import java.util.List;

/**
 * Repository for interaction with the AuditCoreDemographic table.
 */
public interface CoreDemographicAuditRepository extends CrudRepository<CoreDemographicAudit, Integer> {
    /**
     * @param coreDemographicId id from original table.
     * @return List of audit entities
     */
    List<CoreDemographicAudit> getAllByCoreDemographicId(long coreDemographicId);

    /**
     * Get all audit core demographic rows by the mrn string, for testing.
     * @param mrn MRN string
     * @return all core demographic entities
     */
    List<CoreDemographicAudit> getAllByMrnIdMrn(String mrn);
}
