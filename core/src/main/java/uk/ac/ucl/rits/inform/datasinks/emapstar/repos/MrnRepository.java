package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import uk.ac.ucl.rits.inform.informdb.Mrn;

/**
 */
public interface MrnRepository extends CrudRepository<Mrn, Integer> {
    /**
     * @param mrn the MRN string to find
     * @return Mrn object matching the given MRN string or null if not exist
     */
    @Query("select m from Mrn m where m.mrn=?1")
    Mrn findByMrnString(String mrn);
}
