package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.consults.ConsultationRequest;
import uk.ac.ucl.rits.inform.informdb.consults.ConsultationRequestType;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;

import java.util.Optional;

/**
 * Consultation request repository.
 * @author: Anika Cawthorn
 */
public interface ConsultationRequestRepository extends CrudRepository<ConsultationRequest, Long> {
    Optional<ConsultationRequest> findByMrnIdAndConsultRequestTypeId(
            Mrn mrn, ConsultationRequestType consultationRequestType
    );
}
