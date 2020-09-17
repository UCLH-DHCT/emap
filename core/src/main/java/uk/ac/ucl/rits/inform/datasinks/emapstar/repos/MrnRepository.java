package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;

import java.util.List;
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

    /**
     * Get all MRNs which match by a non null MRN or a non null Nhs Number.
     * @param mrn       MRN string
     * @param nhsNumber NHS number
     * @return optional MRNs
     */
    Optional<List<Mrn>> getAllByMrnIsNotNullAndMrnEqualsOrNhsNumberIsNotNullAndNhsNumberEquals(String mrn, String nhsNumber);

    /**
     * @param mrn MRN string
     * @return MRN
     */
    Mrn getByMrnEquals(String mrn);

    /**
     * @param nhsNumber nhs number
     * @return mrn
     */
    List<Mrn> getAllByNhsNumberEquals(String nhsNumber);
}
