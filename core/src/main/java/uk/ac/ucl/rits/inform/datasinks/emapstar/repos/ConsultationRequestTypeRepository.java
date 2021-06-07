package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.consults.ConsultationRequestType;

import java.util.Optional;

public interface ConsultationRequestTypeRepository extends CrudRepository<ConsultationRequestType, Long> {
    /**
     * @param consultationRequestType    type of consultation request, i.e. consultation code as used by EPIC.
     * @return possible consultation request
     */
    Optional<ConsultationRequestType> findByStandardisedCode(String consultationRequestType);
}




