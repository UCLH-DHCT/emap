package uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.informdb.labs.LabSample;

import java.util.Optional;

/**
 * Lab Sample repository.
 * @author Stef Piatek
 * @author Anika Cawthorn
 */
public interface LabSampleRepository extends CrudRepository<LabSample, Long> {
    Optional<LabSample> findByMrnIdAndExternalLabNumber(Mrn mrn, String externalLabNumber);

    /**
     * For question processing testing.
     * @param mrn
     * @param externalLabNumber
     * @return lab sample based on patient and external lab number.
     */
    Optional<LabSample> findByMrnIdAndExternalLabNumber(String mrn, String externalLabNumber);

    /**
     * For testing.
     * @param externalLabNumber external lab number
     * @return possible LabCollection
     */
    Optional<LabSample> findByExternalLabNumber(String externalLabNumber);
}
