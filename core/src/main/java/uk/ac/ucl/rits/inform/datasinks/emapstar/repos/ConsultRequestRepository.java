package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.consults.ConsultRequest;
import uk.ac.ucl.rits.inform.informdb.consults.ConsultRequestType;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;

import java.util.Optional;

/**
 * Consultation request repository.
 * @author: Anika Cawthorn
 */
public interface ConsultRequestRepository extends CrudRepository<ConsultRequest, Long> {
    Optional<ConsultRequest> findByMrnIdAndConsultRequestTypeId(
            Mrn mrn, ConsultRequestType consultRequestType
    );
}
