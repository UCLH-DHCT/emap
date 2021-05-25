package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.consults.ConsultRequest;

/**
 * Consultation request repository.
 * @author: Anika Cawthorn
 */
public interface ConsultRequestRepository extends CrudRepository<ConsultRequest, Long> {

}
