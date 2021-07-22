package uk.ac.ucl.rits.inform.datasinks.emapstar.repos.vist_observations;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.visit_recordings.VisitObservationType;

import java.util.Optional;

/**
 * Visit Observation repository.
 * @author Stef Piatek
 */
public interface VisitObservationTypeRepository extends CrudRepository<VisitObservationType, Long> {

    /**
     * Query visit observation type by required fields.
     * @param id                    Id within the source application
     * @param sourceSystem          source system
     * @param sourceObservationType type of observation from the source system
     * @return optional visit observation type
     */
    Optional<VisitObservationType> findByIdInApplicationAndSourceSystemAndSourceObservationType(
            String id, String sourceSystem, String sourceObservationType);
}
