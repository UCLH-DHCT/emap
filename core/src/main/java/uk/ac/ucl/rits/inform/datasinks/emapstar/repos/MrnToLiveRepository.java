package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.informdb.identity.MrnToLive;

import java.util.List;

/**
 * Repository for MrnToLive table.
 */
public interface MrnToLiveRepository extends CrudRepository<MrnToLive, Long> {
    /**
     * @param mrn Mrn object to find
     * @return MrnToLive object
     */
    MrnToLive getByMrnIdEquals(Mrn mrn);

    /**
     * @param mrn Live Mrn
     * @return List of all rows
     */
    List<MrnToLive> getAllByLiveMrnIdEquals(Mrn mrn);
}
