package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.informdb.labs.LabNumber;

import java.util.Optional;

/**
 * Lab number repository.
 * @author Stef Piatek
 */
public interface LabNumberRepository extends CrudRepository<LabNumber, Long> {
    Optional<LabNumber> findByMrnIdAndHospitalVisitIdAndInternalLabNumberAndExternalLabNumber(
            Mrn mrn, HospitalVisit visit, String internalOrderNumber, String externalOrderNumber);
}
