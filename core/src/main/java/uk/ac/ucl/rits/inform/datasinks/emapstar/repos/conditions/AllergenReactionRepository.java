package uk.ac.ucl.rits.inform.datasinks.emapstar.repos.conditions;

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
     * @param symptomName      name of symptom
     * @param patientCondition patient condition instance (foreign key)
     * @return possible allergy reaction
     */
    Optional<AllergenReaction> findByNameAndPatientConditionId(String symptomName, PatientCondition patientCondition);

}
