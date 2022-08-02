package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.conditions.PatientCondition;
import uk.ac.ucl.rits.inform.informdb.conditions.ConditionVisits;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;

import java.util.List;
import java.util.Optional;

/**
 * PatientCondition repository.
 *
 * @author Tom Young
 */
public interface ConditionVisitLinkRepository extends CrudRepository<ConditionVisits, Long> {

    /**
     * Get the link record, should be unique.
     *
     * @param patientCondition Patient condition record
     * @param hospitalVisit Hospital visit record
     * @return possible patient condition
     */
    Optional<ConditionVisits> findByPatientConditionIdAndHospitalVisitId(
            PatientCondition patientCondition, HospitalVisit hospitalVisit
    );


    /**
     * For testing only.
     * @return All the records
     */
    List<ConditionVisits> findAll();
}
