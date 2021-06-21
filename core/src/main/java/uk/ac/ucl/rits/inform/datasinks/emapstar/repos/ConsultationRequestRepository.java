package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.consults.ConsultationRequest;
import uk.ac.ucl.rits.inform.informdb.consults.ConsultationType;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;

import java.util.Optional;
// import java.time.Instant;

/**
 * Consultation request repository.
 * @author: Anika Cawthorn
 */
public interface ConsultationRequestRepository extends CrudRepository<ConsultationRequest, Long> {
    /**
     * Find consultation request by patient, hospital visit, consultation type and request date.
     * @param mrn                       Patient identifier
     * @param visit                     Hospital visit
     * @param consultationType   Consultancy type
     * @return ConsultationRequest that relates to identifiers
     */
    Optional<ConsultationRequest> findByMrnIdAndHospitalVisitIdAndConsultationRequestTypeId(
            Mrn mrn, HospitalVisit visit, ConsultationType consultationType
    );

    /**
     * Find consultation request by patient, hospital visit, consultation type and request date.
     * @param mrn                       Patient identifier
     * @param visit                     Hospital visit
     * @param consultationRequestType   Consultancy type
     * @return ConsultationRequest that relates to identifiers
     */
    Optional<ConsultationRequest> findByMrnIdMrnAndHospitalVisitIdEncounterAndConsultationRequestTypeIdStandardisedCode(
            String mrn, String visit, String consultationRequestType
    );
}
