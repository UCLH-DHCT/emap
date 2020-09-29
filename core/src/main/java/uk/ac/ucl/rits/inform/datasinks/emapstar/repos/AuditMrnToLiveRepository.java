package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.identity.AuditMrnToLive;

import java.util.List;

/**
 * Repository for interaction with the AuditMrnToLive table.
 */
public interface AuditMrnToLiveRepository extends CrudRepository<AuditMrnToLive, Integer> {
    /**
     * Get all by the liveMrnId's MRN string.
     * @param liveMrnString id from original table.
     * @return List of audit entities
     */
    List<AuditMrnToLive> getAllByLiveMrnIdMrn(String liveMrnString);

}
