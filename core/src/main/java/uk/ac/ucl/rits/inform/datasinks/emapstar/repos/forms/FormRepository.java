package uk.ac.ucl.rits.inform.datasinks.emapstar.repos.forms;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.forms.Form;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface FormRepository extends CrudRepository<Form, Long> {
    Optional<Form> findByInternalId(String internalId);

    List<Form> findAllByHospitalVisitId(HospitalVisit hospitalVisit);

    List<Form> findAllByMrnIdAndValidFromBefore(Mrn mrn, Instant deletionTime);

    /**
     * For testing only.
     * @param encounter encounter string to query by
     * @return list of forms that match
     */
    List<Form> findAllByHospitalVisitIdEncounter(String encounter);
}
