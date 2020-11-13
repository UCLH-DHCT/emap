package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.RequiredDataMissingException;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;

import java.util.List;
import java.util.Optional;

/**
 * Repository for interaction with MRN table.
 */
public interface MrnRepository extends CrudRepository<Mrn, Long> {
    /**
     * Helper method to find MRNs (by mrn string and nhs number depending on what exists).
     * If both MRN and NHS number are given, get by MRN and then add in NHS number if it doesn't exist.
     * @param mrn       MRN string
     * @param nhsNumber NHS number
     * @return optional MRN
     * @throws IllegalArgumentException if mrn and nhsNumber are both null
     */
    default Optional<Mrn> findByMrnOrNhsNumber(String mrn, String nhsNumber) throws RequiredDataMissingException {
        if (mrn == null && nhsNumber == null) {
            throw new RequiredDataMissingException("Both the Mrn and NHS number can't be null");
        }
        if (mrn == null) {
            return findFirstByNhsNumberEquals(nhsNumber);
        }
        Optional<Mrn> mrnResult = findByMrnEquals(mrn);
        if (mrnResult.isEmpty() && nhsNumber != null) {
            // final attempt, try and find a row with an nhs number, but no MRN
            mrnResult = findByNhsNumberEqualsAndMrnIsNull(nhsNumber);
        }
        return mrnResult;
    }

    /**
     * @param mrn MRN string
     * @return optional MRN
     */
    Optional<Mrn> findByMrnEquals(String mrn);

    /**
     * Allow for multiple MRNs per NHS number.
     * @param nhsNumber NHS number
     * @return optional MRN
     */
    Optional<Mrn> findFirstByNhsNumberEquals(String nhsNumber);

    /**
     * Already know that no MRN matches current.
     * @param nhsNumber NHS number
     * @return optional MRN
     */
    Optional<Mrn> findByNhsNumberEqualsAndMrnIsNull(String nhsNumber);

    /**
     * Get all MRNs which match by a non null MRN or a non null Nhs Number.
     * @param mrn       MRN string
     * @param nhsNumber NHS number
     * @return optional MRN
     * @throws RequiredDataMissingException if mrn and nhsNumber are both null
     */
    default Optional<List<Mrn>> findAllByMrnOrNhsNumber(String mrn, String nhsNumber) throws RequiredDataMissingException {
        if (mrn == null && nhsNumber == null) {
            throw new RequiredDataMissingException("Both the Mrn and NHS number can't be null");
        }
        if (nhsNumber == null) {
            return findAllByMrnEquals(mrn);
        }
        if (mrn == null) {
            return findAllByNhsNumberEquals(nhsNumber);
        }
        return findAllByMrnEqualsOrNhsNumberEquals(mrn, nhsNumber);
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
    Optional<List<Mrn>> findAllByMrnEqualsOrNhsNumberEquals(String mrn, String nhsNumber);

    /**
     * @param mrn MRN string
     * @return MRN
     */
    Optional<Mrn> getByMrnEquals(String mrn);

    /**
     * @param nhsNumber nhs number
     * @return mrn
     */
    List<Mrn> getAllByNhsNumberEquals(String nhsNumber);

}
