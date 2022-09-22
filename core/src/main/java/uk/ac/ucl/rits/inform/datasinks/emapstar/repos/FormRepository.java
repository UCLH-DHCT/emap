package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.forms.Form;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;

import java.util.List;

public interface FormRepository extends CrudRepository<Form, Long> {
    List<Form> findAllByHospitalVisitIdEncounter(String encounter);
    List<Form> findAllByHospitalVisitId(HospitalVisit hospitalVisit);
    List<Form> findAllByMrnId(Mrn mrn);

    Form findSingleByHospitalVisitIdEncounter(String encounter);
}
