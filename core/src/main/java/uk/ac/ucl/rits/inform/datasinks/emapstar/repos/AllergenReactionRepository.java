package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.conditions.AllergenReaction;
import uk.ac.ucl.rits.inform.informdb.conditions.PatientCondition;

import java.util.Optional;

/**
 * PatientSymptom repository.
 * @author Tom Young
 */
public interface AllergenReactionRepository extends CrudRepository<AllergenReaction, Long> {

    /**
     * Get patient symptom, should be unique.
     * @param symptomName   name of symptom
     * @return possible patient symptom
     */
    Optional<AllergenReaction> findByNameAndPatientConditionId(String symptomName, PatientCondition patientCondition);

}
