package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.informdb.identity.MrnToLive;

public interface MrnToLiveRepository extends CrudRepository<MrnToLive, Integer> {
    MrnToLive getByMrnIdEquals(Mrn mrn);
}
