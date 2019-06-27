package uk.ac.ucl.rits.inform.informdb;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

/**
 */
public interface MrnRepository extends CrudRepository<Mrn, Integer> {
    /**
     * According to the schema as it stands, we can have multiple entries for an MRN.
     * @param mrn the MRN string to find
     * @return Mrn objects matching the given MRN string
     */
    @Query("select m from Mrn m where m.mrn=?1")
    List<Mrn> findByMrnString(String mrn);
}
