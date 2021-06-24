package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.consults.ConsultationType;

import java.util.Optional;

public interface ConsultationTypeRepository extends CrudRepository<ConsultationType, Long> {
    /**
     * @param consultationType    type of consultation request, i.e. consultation code as used by EPIC.
     * @return possible consultation type
     */
    Optional<ConsultationType> findByStandardisedCode(String consultationType);
}



