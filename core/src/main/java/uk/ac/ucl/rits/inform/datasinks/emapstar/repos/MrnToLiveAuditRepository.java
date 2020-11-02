package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.identity.MrnToLiveAudit;

import java.util.List;

/**
 * Repository for interaction with the AuditMrnToLive table.
 */
public interface MrnToLiveAuditRepository extends CrudRepository<MrnToLiveAudit, Integer> {
    /**
     * Get all by the liveMrnId's MRN string.
     * @param liveMrnString id from original table.
     * @return List of audit entities
     */
    List<MrnToLiveAudit> getAllByLiveMrnIdMrn(String liveMrnString);

}
