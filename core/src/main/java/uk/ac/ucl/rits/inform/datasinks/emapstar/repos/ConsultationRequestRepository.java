package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.consults.ConsultationRequest;
import uk.ac.ucl.rits.inform.informdb.consults.ConsultationRequestType;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;

import java.util.Optional;
import java.time.Instant;

/**
 * Consultation request repository.
 * @author: Anika Cawthorn
 */
public interface ConsultationRequestRepository extends CrudRepository<ConsultationRequest, Long> {
    /**
     * Find consultation request by patient, hospital visit, consultation type and request date.
     * @param mrn                       Patient identifier
     * @param visit                     Hospital visit
     * @param consultationRequestType   Consultancy type
     * @param requestedDateTime         Date when consultant was requested
     * @return ConsultationRequest that relates to identifiers
     */
    Optional<ConsultationRequest> findByMrnAndHospitalVisitAndConsultationRequestTypeNameAndRequestedDateTime(
            Mrn mrn, HospitalVisit visit, ConsultationRequestType consultationRequestType, Instant requestedDateTime
    );
}
