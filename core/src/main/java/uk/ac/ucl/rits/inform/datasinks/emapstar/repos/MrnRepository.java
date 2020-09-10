package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;

import java.util.Optional;

/**
 * Repository for interaction with MRN table.
 */
public interface MrnRepository extends CrudRepository<Mrn, Integer> {
    /**
     * @param mrn       MRN string
     * @param nhsNumber NHS number
     * @return optional MRN
     */
    Optional<Mrn> getByMrnEqualsOrMrnIsNullAndNhsNumberEquals(String mrn, String nhsNumber);
}
