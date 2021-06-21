package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.consults.ConsultationRequest;
import uk.ac.ucl.rits.inform.informdb.consults.ConsultationType;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;

import java.util.Optional;

/**
 * Consultation request repository.
 * @author Anika Cawthorn
 */
public interface ConsultationRequestRepository extends CrudRepository<ConsultationRequest, Long> {
    /**
     * Find consultation request by hospital visit and consultation type.
     * @param visit              Hospital visit
     * @param consultationType   Consultancy type
     * @return ConsultationRequest that relates to identifiers
     */
    Optional<ConsultationRequest> findByHospitalVisitIdAndConsultationRequestTypeId(
            HospitalVisit visit, ConsultationType consultationType
    );
}
