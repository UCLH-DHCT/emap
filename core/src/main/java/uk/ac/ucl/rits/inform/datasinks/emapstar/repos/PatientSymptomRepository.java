package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.conditions.ConditionSymptom;

import java.util.Optional;

/**
 * PatientSymptom repository.
 * @author Tom Young
 */
public interface PatientSymptomRepository extends CrudRepository<ConditionSymptom, Long> {

    /**
     * Get patient symptom, should be unique.
     * @param symptomName   name of symptom
     * @return possible patient symptom
     */
    Optional<ConditionSymptom> findByName(String symptomName);

}
