package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.forms.Form;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;

import java.util.List;
import java.util.Optional;

public interface FormRepository extends CrudRepository<Form, Long> {
    Optional<Form> findByInternalId(String internalId);
    List<Form> findAllByHospitalVisitIdEncounter(String encounter);
    List<Form> findAllByHospitalVisitId(HospitalVisit hospitalVisit);
    List<Form> findAllByMrnId(Mrn mrn);
}
