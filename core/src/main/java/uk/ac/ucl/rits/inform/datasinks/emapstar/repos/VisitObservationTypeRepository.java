package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.visit_recordings.VisitObservationType;

import java.util.Optional;

/**
 * Visit Observation repository.
 * @author Stef Piatek
 */
public interface VisitObservationTypeRepository extends CrudRepository<VisitObservationType, Long> {

    /**
     * @param Id                Id within the source application
     * @param sourceSystem      source system
     * @param sourceApplication source application
     * @return
     */
    Optional<VisitObservationType> findByIdInApplicationAndSourceSystemAndSourceApplication(String Id, String sourceSystem, String sourceApplication);
}
