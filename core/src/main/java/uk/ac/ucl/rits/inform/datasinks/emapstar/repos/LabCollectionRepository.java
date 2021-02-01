package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.labs.LabCollection;

import java.util.Optional;

/**
 * Lab Collection repository.
 * @author Stef Piatek
 */
public interface LabCollectionRepository extends CrudRepository<LabCollection, Long> {
    /**
     * For testing.
     * @param labNumber internal lab number
     * @return possible LabCollection
     */
    Optional<LabCollection> findByLabNumberIdInternalLabNumber(String labNumber);
}
