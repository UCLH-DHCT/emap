package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import uk.ac.ucl.rits.inform.informdb.OldMrn;

/**
 */
public interface OldMrnRepository extends CrudRepository<OldMrn, Integer> {
    /**
     * @param mrn the MRN string to find
     * @return Mrn object matching the given MRN string or null if not exist
     */
    @Query("select m from OldMrn m where m.mrn=?1")
    OldMrn findByMrnString(String mrn);
}
