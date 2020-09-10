package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.informdb.identity.MrnToLive;

/**
 * Repository for MrnToLive table.
 */
public interface MrnToLiveRepository extends CrudRepository<MrnToLive, Integer> {
    /**
     * @param mrn Mrn object to find
     * @return MrnToLive object
     */
    MrnToLive getByMrnIdEquals(Mrn mrn);
}
