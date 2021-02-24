package uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.labs.LabCollection;
import uk.ac.ucl.rits.inform.informdb.labs.LabNumber;

import java.util.Optional;

/**
 * Lab Collection repository.
 * @author Stef Piatek
 */
public interface LabCollectionRepository extends CrudRepository<LabCollection, Long> {
    Optional<LabCollection> findByLabNumberId(LabNumber labNumber);

    /**
     * For testing.
     * @param labNumber internal lab number
     * @return possible LabCollection
     */
    Optional<LabCollection> findByLabNumberIdInternalLabNumber(String labNumber);
}
