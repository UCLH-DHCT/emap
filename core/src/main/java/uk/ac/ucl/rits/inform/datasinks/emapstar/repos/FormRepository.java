package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.forms.Form;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.informdb.labs.LabOrder;

import java.util.List;

public interface FormRepository extends CrudRepository<Form, Long> {
//    @Override
    // #{#entityName} need to eager fetch
//    Iterable<Form> findAll();

    List<Form> findAllByHospitalVisitId(HospitalVisit hospitalVisit);
    List<Form> findAllByLabOrderId(LabOrder labOrder);
    List<Form> findAllByMrnId(Mrn mrn);
}
