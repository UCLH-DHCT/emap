package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.conditions.ConditionSymptom;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;

import java.time.Instant;
import java.util.Optional;

/**
 * PatientSymptom repository.
 * @author Tom Young
 */
public interface PatientSymptomRepository extends CrudRepository<ConditionSymptom, Long> {

    /**
     * Get patient symptom, should be unique.
     * @param mrn           MRN entity
     * @param symptomName   name of symptom
     * @param addedDateTime added date time
     * @return possible patient symptom
     */
    Optional<ConditionSymptom> findByMrnIdAndSymptomNameAndAddedDateTime(
            Mrn mrn, String symptomName, Instant addedDateTime
    );

}
