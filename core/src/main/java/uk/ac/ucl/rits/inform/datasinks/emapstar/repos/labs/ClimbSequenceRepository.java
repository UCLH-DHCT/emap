package uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.labs.ClimbSequence;
import uk.ac.ucl.rits.inform.informdb.labs.LabSample;

import java.util.Optional;

/**
 * ClimbSequence repository.
 * @author Stef Piatek
 */
public interface ClimbSequenceRepository extends CrudRepository<ClimbSequence, Long> {
    Optional<ClimbSequence> findByPheIdAndLabSampleId(String pheId, LabSample labSample);

    /**
     * For testing only.
     * @param pheId PHE ID to search from
     * @return optional climb sequence
     */
    Optional<ClimbSequence> findByPheId(String pheId);
}
