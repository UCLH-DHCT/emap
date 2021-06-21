package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.consults.ConsultationType;

import java.util.Optional;

public interface ConsultationRequestTypeRepository extends CrudRepository<ConsultationType, Long> {
    /**
     * @param consultationRequestType    type of consultation request, i.e. consultation code as used by EPIC.
     * @return possible consultation request
     */
    Optional<ConsultationType> findByStandardisedCode(String consultationRequestType);
}




