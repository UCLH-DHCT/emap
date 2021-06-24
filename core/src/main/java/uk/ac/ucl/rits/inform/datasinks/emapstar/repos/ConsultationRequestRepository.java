package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.consults.ConsultationRequest;

import java.util.Optional;

/**
 * Consultation request repository.
 * @author Anika Cawthorn
 */
public interface ConsultationRequestRepository extends CrudRepository<ConsultationRequest, Long> {
    /**
     * Find consultation request by unique identifier.
     * @param consultId   Consultancy type
     * @return ConsultationRequest that relates to identifier
     */
    Optional<ConsultationRequest> findByConsultId(Long consultId);
}
