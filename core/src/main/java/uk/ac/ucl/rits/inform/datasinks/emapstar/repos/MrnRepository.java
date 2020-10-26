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
     * Helper method to find MRNs (by mrn string and nhs number depending on what exists).
     * @param mrn       MRN string
     * @param nhsNumber NHS number
     * @return optional MRN
     * @throws IllegalArgumentException if mrn and nhsNumber are both null
     */
    default Optional<Mrn> findByMrnOrNhsNumber(String mrn, String nhsNumber) throws IllegalArgumentException {
        if (mrn == null && nhsNumber == null) {
            throw new IllegalArgumentException("Both the Mrn and NHS number can't be null");
        }
        if (nhsNumber == null) {
            return findByMrnEquals(mrn);
        }
        if (mrn == null) {
            return findByNhsNumberEquals(nhsNumber);
        }
        return findByMrnEqualsOrMrnIsNullAndNhsNumberEquals(mrn, nhsNumber);
    }

    /**
     * @param mrn MRN string
     * @return optional MRN
     */
    Optional<Mrn> findByMrnEquals(String mrn);

    /**
     * @param nhsNumber NHS number
     * @return optional MRN
     */
    Optional<Mrn> findByNhsNumberEquals(String nhsNumber);

    /**
     * @param mrn       MRN string
     * @param nhsNumber NHS number
     * @return optional MRN
     */
    Optional<Mrn> findByMrnEqualsOrMrnIsNullAndNhsNumberEquals(String mrn, String nhsNumber);

    /**
     * Get all MRNs which match by a non null MRN or a non null Nhs Number.
     * @param mrn       MRN string
     * @param nhsNumber NHS number
     * @return optional MRN
     * @throws IllegalArgumentException if mrn and nhsNumber are both null
     */
    default Optional<List<Mrn>> findAllByMrnOrNhsNumber(String mrn, String nhsNumber) throws IllegalArgumentException {
        if (mrn == null && nhsNumber == null) {
            throw new IllegalArgumentException("Both the Mrn and NHS number can't be null");
        }
        if (nhsNumber == null) {
            return findAllByMrnEquals(mrn);
        }
        if (mrn == null) {
            return findAllByNhsNumberEquals(nhsNumber);
        }
        return findAllByMrnIsNotNullAndMrnEqualsOrNhsNumberIsNotNullAndNhsNumberEquals(mrn, nhsNumber);
    }

    /**
     * @param mrn MRN string
     * @return optional MRNs
     */
    Optional<List<Mrn>> findAllByMrnEquals(String mrn);

    /**
     * @param nhsNumber NHS number
     * @return optional MRNs
     */
    Optional<List<Mrn>> findAllByNhsNumberEquals(String nhsNumber);

    /**
     * @param mrn       MRN string
     * @param nhsNumber NHS number
     * @return optional MRNs
     */
    Optional<List<Mrn>> findAllByMrnIsNotNullAndMrnEqualsOrNhsNumberIsNotNullAndNhsNumberEquals(String mrn, String nhsNumber);

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

    /**
     * @param mrn MRN string
     * @return MRN
     */
    Optional<Object> getAllByMrnEquals(String mrn);
}
