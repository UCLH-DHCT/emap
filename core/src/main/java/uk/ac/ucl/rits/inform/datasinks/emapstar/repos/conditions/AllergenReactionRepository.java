package uk.ac.ucl.rits.inform.datasinks.emapstar.repos.conditions;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.conditions.AllergenReaction;
import uk.ac.ucl.rits.inform.informdb.conditions.PatientCondition;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Allergen reaction repository.
 * @author Tom Young
 */
public interface AllergenReactionRepository extends CrudRepository<AllergenReaction, Long> {

    /**
     * Get patient symptom, should be unique.
     * @param allergenName      name of symptom
     * @param patientCondition patient condition instance (foreign key)
     * @return possible allergy reaction
     */
    Optional<AllergenReaction> findByNameAndPatientConditionId(String allergenName, PatientCondition patientCondition);

    List<AllergenReaction> findAllByPatientConditionIdAndValidFromLessThanEqual(PatientCondition patientCondition, Instant time);
}
